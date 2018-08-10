package com.island.ohara.configurator.call

import java.util.Properties
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{CountDownLatch, Executors, LinkedBlockingQueue, TimeUnit}

import com.island.ohara.config.UuidUtil
import com.island.ohara.data.{OharaData, OharaException}
import com.island.ohara.io.CloseOnce
import com.island.ohara.kafka.{Consumer, KafkaClient, KafkaUtil}
import com.island.ohara.serialization.{OharaDataSerializer, Serializer}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.errors.WakeupException

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * A request-> response based call queue. This implementation is based on kafka topic. It have a kafka consumer and a
  * kafka producer internally. The consumer is used to accept the client's request. And the producer is used to send
  * response back to client. Call queue client should form the request(k, v) into (OharaRequest, REQUEST). And the call queue
  * server should from the response(k,v) into (OharaResponse, RESPONSE(or OharaException)).
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
private class CallQueueServerImpl[Request <: OharaData: ClassTag, Response <: OharaData](
  brokers: String,
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
  private[this] implicit val executor = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  /**
    * We have to trace each request so we need a incrementable index.
    */
  private[this] val indexer = new AtomicLong(0)

  /**
    * the uuid for this instance
    */
  private[this] val uuid = UuidUtil.uuid()

  if (!topicOptions
        .get(TopicConfig.CLEANUP_POLICY_CONFIG)
        .map(_.equals(TopicConfig.CLEANUP_POLICY_DELETE))
        .getOrElse(true))
    throw new IllegalArgumentException(
      s"The topic store require the ${TopicConfig.CLEANUP_POLICY_CONFIG}=${TopicConfig.CLEANUP_POLICY_DELETE}")

  /**
    * Initialize the topic
    */
  CloseOnce.doClose(KafkaClient(brokers)) { client =>
    client.topicCreator
      .topicName(topicName)
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      // enable kafka delete all stale requests
      .topicOptions(topicOptions + (TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_DELETE))
      .timeout(initializationTimeout)
      .build()
  }

  private[this] val producer = newOrClose {
    val props = new Properties()
    props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    new KafkaProducer[OharaData, OharaData](props,
                                            KafkaUtil.wrapSerializer(OharaDataSerializer),
                                            KafkaUtil.wrapSerializer(OharaDataSerializer))
  }

  private[this] val consumer = newOrClose {
    Consumer
      .builder(Serializer.OHARA_DATA, Serializer.OHARA_DATA)
      .brokers(brokers)
      .fromBegin(false)
      // the uuid of requestConsumer is configurable. If user assign multi node with same uuid, it means user want to
      // distribute the request.
      .groupId(groupId)
      .topicName(topicName)
      .build()
  }

  private[call] val undealtTasks = new LinkedBlockingQueue[CallQueueTask[Request, Response]]()
  private[call] val processingTasks = new LinkedBlockingQueue[CallQueueTask[Request, Response]]()

  private[this] def sendToKafka(key: OharaData, value: OharaData) = {
    producer.send(new ProducerRecord[OharaData, OharaData](topicName, null, key, value, null))
    producer.flush()
  }

  private[this] def createCallQueueTask(internalRequest: OharaRequest, clientRequest: Request) =
    new CallQueueTask[Request, Response]() {
      private[this] var response: OharaData = null
      override val request: Request = clientRequest

      override def complete(oharaData: Response): Unit = if (response != null)
        throw new IllegalArgumentException(s"you have assigned the response:${response.toJson.toString()}")
      else {
        response = oharaData
        send(OharaResponse.apply(responseUuid, internalRequest.uuid), response)
      }

      override def complete(exception: Throwable): Unit = if (response != null)
        throw new IllegalArgumentException(s"you have assigned the response:${response.toJson.toString}")
      else {
        response = OharaException.apply(exception.getMessage, exception)
        send(OharaResponse.apply(responseUuid, internalRequest.uuid), response)
      }

      def send(key: OharaData, value: OharaData): Unit = try {
        processingTasks.remove(this)
        sendToKafka(key, value)
      } catch {
        case e: Throwable => {
          logger.error("Failed to send the response back to client", e)
          // put back this task
          response = null
          undealtTasks.put(this)
        }
      }
    }

  /**
    * Used to check whether consumer have completed the first poll.
    */
  private[this] val initializeConsumerLatch = new CountDownLatch(1)

  /**
    * a single thread to handle the request and response. For call queue server, the supported consumer's key is OharaRequest,
    * and the supported consumer's value is REQUEST. the supported producer's key is OharaResponse,
    * the supported producer's value is RESPONSE.
    */
  private[this] val requestWorker = Future[Unit] {
    try {
      while (!this.isClosed) {
        try {
          val records = consumer.poll(pollTimeout)
          initializeConsumerLatch.countDown()
          records
            .filter(_.topic.equals(topicName))
            .foreach(record => {
              record.key.foreach(k => {
                k match {
                  case internalRequest: OharaRequest =>
                    if (internalRequest.lease <= System.currentTimeMillis)
                      logger.debug(s"the lease of request is violated")
                    else
                      record.value.foreach(v =>
                        v match {
                          case clientRequest: Request =>
                            undealtTasks.put(createCallQueueTask(internalRequest, clientRequest))
                          case _ =>
                            try sendToKafka(OharaResponse.apply(responseUuid, internalRequest.uuid),
                                            OharaException.apply(new IllegalArgumentException(s"Unsupported type:$v")))
                            catch {
                              case _: Throwable => logger.error("Failed to response the unsupported request")
                            }
                      })
                  case _: OharaResponse => // this is for the call queue client
                  case _ =>
                    logger.error(s"unsupported key:$k. The supported key by call queue server is OharaRequest")
                }
              })
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
