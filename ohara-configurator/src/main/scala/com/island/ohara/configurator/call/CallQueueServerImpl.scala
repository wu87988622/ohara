package com.island.ohara.configurator.call

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, LinkedBlockingQueue, TimeUnit}

import com.island.ohara.client.ConfiguratorJson.Error
import com.island.ohara.client.util.CloseOnce
import com.island.ohara.client.util.CloseOnce._
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.kafka.{Consumer, KafkaClient, Producer}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.errors.{TopicExistsException, WakeupException}

import scala.concurrent._
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
  * A request-> response based call queue. This implementation is based on kafka topic. It have a kafka consumer and a
  * kafka producer internally. The consumer is used to accept the client's request. And the producer is used to send
  * response back to client. Call queue client should form the request(k, v) into (CallQueueRequest, REQUEST). And the call queue
  * server should from the response(k,v) into (CallQueueResponse, RESPONSE(or OharaException)).
  *
  * User can use this class to send the (action, ohara data) to another node.
  * (If the store is a distributed impl, the another node can be a remote node). If you want to make a distributed
  * call queue server service, passing the same group id build the call queue server, and make the number from topic partition
  * be bigger than zero. The kafak balancer can make the request to call queue server be balanced
  *
  * @param brokers KAFKA server
  * @param groupId used to create the consumer to accept the request from client
  * @param pollTimeout the specified waiting time elapses to poll the consumer
  * @tparam Request the supported request type
  * @tparam Response the supported response type
  */
private class CallQueueServerImpl[Request: ClassTag, Response <: AnyRef](brokers: String,
                                                                         requestTopic: String,
                                                                         responseTopic: String,
                                                                         groupId: String,
                                                                         pollTimeout: Duration)
    extends CallQueueServer[Request, Response] {

  private[this] val log = Logger(getClass.getName)

  /**
    * requestWorker thread.
    */
  private[this] implicit val executor: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  /**
    * We have to trace each request so we need a incrementable index.
    */
  private[this] val indexer = new AtomicLong(0)

  /**
    * the uuid for this instance
    */
  private[this] val uuid = CommonUtil.uuid()

  /**
    * Initialize the topic
    */
  CloseOnce.doClose(KafkaClient(brokers)) { client =>
    def createIfNeed(topicName: String): Unit = if (!client.exist(topicName))
      try client
        .topicCreator()
        .numberOfPartitions(CallQueueServerImpl.NUMBER_OF_PARTITIONS)
        .numberOfReplications(CallQueueServerImpl.NUMBER_OF_REPLICATIONS)
        // enable kafka save the latest message for each key
        .deleted()
        .timeout(CallQueueServerImpl.TIMEOUT)
        .create(topicName)
      catch {
        case e: ExecutionException =>
          e.getCause match {
            case _: TopicExistsException => log.error(s"$topicName exists but we didn't notice this fact")
            case _                       => if (e.getCause == null) throw e else throw e.getCause
          }
      }
    createIfNeed(requestTopic)
    createIfNeed(responseTopic)
  }

  private[this] val producer = newOrClose {
    Producer.builder().brokers(brokers).build(Serializer.OBJECT, Serializer.OBJECT)
  }

  private[this] val consumer = newOrClose {
    Consumer
      .builder()
      .brokers(brokers)
      .offsetAfterLatest()
      // the uuid from requestConsumer is configurable. If user assign multi node with same uuid, it means user want to
      // distribute the request.
      .groupId(groupId)
      .topicName(requestTopic)
      .build(Serializer.OBJECT, Serializer.OBJECT)
  }

  private[call] val undealtTasks = new LinkedBlockingQueue[CallQueueTask[Request, Response]]()
  private[call] val processingTasks = new LinkedBlockingQueue[CallQueueTask[Request, Response]]()

  private[this] def sendToKafka(key: AnyRef, value: AnyRef): Unit = {
    producer.sender().key(key).value(value).send(responseTopic)
    producer.flush()
  }

  private[this] def createCallQueueTask(internalRequest: CallQueueRequest, clientRequest: Request) =
    new CallQueueTask[Request, Response]() {
      private[this] var response: AnyRef = _
      override def request: Request = clientRequest

      override def complete(_res: Response): Unit = if (response != null)
        throw new IllegalArgumentException(s"you have assigned the response:$response")
      else {
        response = _res
        send(CallQueueResponse(responseUuid(), internalRequest.uuid), response)
      }

      override def complete(exception: Throwable): Unit = if (response != null)
        throw new IllegalArgumentException(s"you have assigned the response:$response")
      else {
        response = Error(exception)
        send(CallQueueResponse(responseUuid(), internalRequest.uuid), response)
      }

      def send(key: AnyRef, value: AnyRef): Unit = try {
        processingTasks.remove(this)
        sendToKafka(key, value)
      } catch {
        case e: Throwable =>
          log.error("Failed to send the response back to client", e)
          // put back this task
          response = null
          undealtTasks.put(this)
      }
    }

  /**
    * a single thread to handle the request and response. For call queue server, the supported consumer's key is CallQueueRequest,
    * and the supported consumer's value is REQUEST. the supported producer's key is CallQueueResponse,
    * the supported producer's value is RESPONSE.
    */
  private[this] val requestWorker: Future[Unit] = Future[Unit] {
    try {
      while (!this.isClosed) {
        try {
          val records = consumer.poll(pollTimeout)
          records
            .filter(_.topic == requestTopic)
            .foreach(record => {
              record.key.foreach {
                case internalRequest: CallQueueRequest =>
                  if (internalRequest.lease.toMillis <= CommonUtil.current())
                    log.debug(s"the lease from request is violated")
                  else
                    record.value.foreach {
                      case clientRequest: Request =>
                        undealtTasks.put(createCallQueueTask(internalRequest, clientRequest))
                      case _ =>
                        try sendToKafka(CallQueueResponse(responseUuid(), internalRequest.uuid),
                                        Error(new IllegalArgumentException(s"Unsupported type")))
                        catch {
                          case _: Throwable => log.error("Failed to response the unsupported request")
                        }
                    }
                case _: CallQueueResponse => // this is for the call queue client
                case _ =>
                  log.error(s"unsupported key. The supported key by call queue server is CallQueueRequest")
              }
            })
        } catch {
          case _: WakeupException => log.debug("interrupted by ourself")
        }
      }
    } catch {
      case e: Throwable => log.error("failure when running the requestWorker", e)
    } finally close()
  }

  private[this] def responseUuid() = s"$uuid-response${indexer.getAndIncrement()}"

  override def take(timeout: Duration): Option[CallQueueTask[Request, Response]] = {
    val task = undealtTasks.poll(timeout.toMillis, TimeUnit.MILLISECONDS)
    if (task == null) None
    else {
      processingTasks.put(task)
      Some(task)
    }
  }

  override def take(): CallQueueTask[Request, Response] = {
    val task = undealtTasks.take()
    processingTasks.put(task)
    task
  }

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    import scala.concurrent.duration._
    CloseOnce.release(
      () =>
        Iterator
          .continually(processingTasks.poll(1, TimeUnit.SECONDS))
          .takeWhile(_ != null)
          .foreach(_.complete(CallQueue.TERMINATE_TIMEOUT_EXCEPTION)))
    CloseOnce.release(
      () =>
        Iterator
          .continually(undealtTasks.poll(1, TimeUnit.SECONDS))
          .takeWhile(_ != null)
          .foreach(_.complete(CallQueue.TERMINATE_TIMEOUT_EXCEPTION)))
    if (consumer != null) consumer.wakeup()
    if (requestWorker != null) CloseOnce.release(() => Await.result(requestWorker, 60 seconds))
    CloseOnce.close(producer)
    CloseOnce.close(consumer)
    if (executor != null) {
      executor.shutdownNow()
      executor.awaitTermination(60, TimeUnit.SECONDS)
    }
  }

  override def countOfUndealtTasks: Int = undealtTasks.size()
  override def countOfProcessingTasks: Int = processingTasks.size()

}

object CallQueueServerImpl {
  val NUMBER_OF_PARTITIONS: Int = 3
  val NUMBER_OF_REPLICATIONS: Short = 3
  val TIMEOUT: Duration = 30 seconds
}
