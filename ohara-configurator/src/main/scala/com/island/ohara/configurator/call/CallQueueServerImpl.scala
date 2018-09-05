package com.island.ohara.configurator.call

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{CountDownLatch, Executors, LinkedBlockingQueue, TimeUnit}

import com.island.ohara.client.ConfiguratorJson.Error
import com.island.ohara.io.{CloseOnce, UuidUtil}
import com.island.ohara.kafka.{Consumer, KafkaClient, Producer}
import com.island.ohara.serialization.Serializer
import com.typesafe.scalalogging.Logger
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.errors.WakeupException

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.reflect.ClassTag

/**
  * A request-> response based call queue. This implementation is based on kafka topic. It have a kafka consumer and a
  * kafka producer internally. The consumer is used to accept the client's request. And the producer is used to send
  * response back to client. Call queue client should form the request(k, v) into (CallQueueRequest, REQUEST). And the call queue
  * server should from the response(k,v) into (CallQueueResponse, RESPONSE(or OharaException)).
  *
  * User can use this class to send the (action, ohara data) to another node.
  * (If the store is a distributed impl, the another node can be a remote node). If you want to make a distributed
  * call queue server service, passing the same group id build the call queue server, and make the number of topic partition
  * be bigger than zero. The kafak balancer can make the request to call queue server be balanced
  *
  * @param brokers KAFKA server
  * @param topicName topic name. the topic will be created automatically if it doesn't exist
  * @param groupId used to create the consumer to accept the request from client
  * @param numberOfPartitions the number of topic partition. Used to build the topic
  * @param numberOfReplications the number of topic partition. Used to build the topic
  * @param pollTimeout the specified waiting time elapses to poll the consumer
  * @param initializationTimeout the specified waiting time to ini
  *                             tialize this call queue server
  * @param topicOptions configuration
  * @tparam Request the supported request type
  * @tparam Response the supported response type
  */
private class CallQueueServerImpl[Request: ClassTag, Response](brokers: String,
                                                               topicName: String,
                                                               groupId: String,
                                                               numberOfPartitions: Int,
                                                               numberOfReplications: Short,
                                                               pollTimeout: Duration,
                                                               initializationTimeout: Duration,
                                                               topicOptions: Map[String, String])
    extends CallQueueServer[Request, Response] {

  private[this] val logger = Logger(getClass.getName)

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
  private[this] val uuid = UuidUtil.uuid()

  if (topicOptions.get(TopicConfig.CLEANUP_POLICY_CONFIG).fold(false)(_ != TopicConfig.CLEANUP_POLICY_DELETE))
    throw new IllegalArgumentException(
      s"The topic store require the ${TopicConfig.CLEANUP_POLICY_CONFIG}=${TopicConfig.CLEANUP_POLICY_DELETE}")

  /**
    * Initialize the topic
    */
  CloseOnce.doClose(KafkaClient(brokers)) { client =>
    client.topicCreator
      .name(topicName)
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      // enable kafka delete all stale requests
      .options(topicOptions + (TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_DELETE))
      .timeout(initializationTimeout)
      .build()
  }

  private[this] val producer = newOrClose {
    Producer.builder(Serializer.OBJECT, Serializer.OBJECT).brokers(brokers).build()
  }

  private[this] val consumer = newOrClose {
    Consumer
      .builder(Serializer.OBJECT, Serializer.OBJECT)
      .brokers(brokers)
      .offsetAfterLatest()
      // the uuid of requestConsumer is configurable. If user assign multi node with same uuid, it means user want to
      // distribute the request.
      .groupId(groupId)
      .topicName(topicName)
      .build()
  }

  private[call] val undealtTasks = new LinkedBlockingQueue[CallQueueTask[Request, Response]]()
  private[call] val processingTasks = new LinkedBlockingQueue[CallQueueTask[Request, Response]]()

  private[this] def sendToKafka(key: Any, value: Any): Unit = {
    producer.sender().topic(topicName).key(key).value(value).send()
    producer.flush()
  }

  private[this] def toError(e: Throwable) =
    Error(e.getClass.getName, if (e.getMessage == null) "None" else e.getMessage, ExceptionUtils.getStackTrace(e))

  private[this] def createCallQueueTask(internalRequest: CallQueueRequest, clientRequest: Request) =
    new CallQueueTask[Request, Response]() {
      private[this] var response: Any = _
      override def request: Request = clientRequest

      override def complete(_res: Response): Unit = if (response != null)
        throw new IllegalArgumentException(s"you have assigned the response:$response")
      else {
        response = _res
        send(CallQueueResponse(responseUuid, internalRequest.uuid), response)
      }

      override def complete(exception: Throwable): Unit = if (response != null)
        throw new IllegalArgumentException(s"you have assigned the response:$response")
      else {
        response = toError(exception)
        send(CallQueueResponse(responseUuid, internalRequest.uuid), response)
      }

      def send(key: Any, value: Any): Unit = try {
        processingTasks.remove(this)
        sendToKafka(key, value)
      } catch {
        case e: Throwable =>
          logger.error("Failed to send the response back to client", e)
          // put back this task
          response = null
          undealtTasks.put(this)
      }
    }

  /**
    * Used to check whether consumer have completed the first poll.
    */
  private[this] val initializeConsumerLatch = new CountDownLatch(1)

  /**
    * a single thread to handle the request and response. For call queue server, the supported consumer's key is CallQueueRequest,
    * and the supported consumer's value is REQUEST. the supported producer's key is CallQueueResponse,
    * the supported producer's value is RESPONSE.
    */
  private[this] val requestWorker = Future[Unit] {
    try {
      while (!this.isClosed) {
        try {
          val records = consumer.poll(pollTimeout)
          initializeConsumerLatch.countDown()
          records
            .filter(_.topic == topicName)
            .foreach(record => {
              record.key.foreach {
                case internalRequest: CallQueueRequest =>
                  if (internalRequest.lease.toMillis <= System.currentTimeMillis)
                    logger.debug(s"the lease of request is violated")
                  else
                    record.value.foreach {
                      case clientRequest: Request =>
                        undealtTasks.put(createCallQueueTask(internalRequest, clientRequest))
                      case _ =>
                        try sendToKafka(CallQueueResponse.apply(responseUuid, internalRequest.uuid),
                                        toError(new IllegalArgumentException(s"Unsupported type")))
                        catch {
                          case _: Throwable => logger.error("Failed to response the unsupported request")
                        }
                    }
                case _: CallQueueResponse => // this is for the call queue client
                case _ =>
                  logger.error(s"unsupported key. The supported key by call queue server is CallQueueRequest")
              }
            })
        } catch {
          case _: WakeupException => logger.debug("interrupted by ourself")
        }
      }
    } catch {
      case e: Throwable => logger.error("failure when running the requestWorker", e)
    } finally {
      initializeConsumerLatch.countDown()
      close()
    }
  }

  if (!initializeConsumerLatch.await(initializationTimeout.toMillis, TimeUnit.MILLISECONDS)) {
    close()
    throw new IllegalArgumentException(s"timeout to initialize the call queue server")
  }

  private[this] def responseUuid = s"$uuid-response${indexer.getAndIncrement()}"

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
