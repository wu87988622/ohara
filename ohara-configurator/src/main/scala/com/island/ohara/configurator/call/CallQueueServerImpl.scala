package com.island.ohara.configurator.call

import java.util
import java.util.Properties
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{CountDownLatch, Executors, LinkedBlockingQueue, TimeUnit}

import com.island.ohara.config.UuidUtil
import com.island.ohara.configurator.data.{OharaData, OharaDataSerializer, OharaException}
import com.island.ohara.kafka.KafkaUtil
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, OffsetResetStrategy}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
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
  * @param partitions the number of topic partition. Used to build the topic
  * @param replications the number of topic partition. Used to build the topic
  * @param pollTimeout the specified waiting time elapses to poll the consumer
  * @param initializeTimeout the specified waiting time to initialize this call queue server
  * @param topicOptions configuration
  * @tparam Request the supported request type
  * @tparam Response the supported response type
  */
private class CallQueueServerImpl[Request <: OharaData: ClassTag, Response <: OharaData](
  brokers: String,
  topicName: String,
  groupId: String,
  partitions: Int,
  replications: Short,
  pollTimeout: Duration,
  initializeTimeout: Duration,
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
  private[this] val baseConfig: Properties = locally {
    val config = new Properties
    config.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    config.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.LATEST.name.toLowerCase)
    // the uuid of requestConsumer is configurable. If user assign multi node with same uuid, it means user want to
    // distribute the request.
    config.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    config
  }

  /**
    * Initialize the topic
    */
  KafkaUtil.createTopicIfNotExist(
    brokers,
    topicName,
    partitions,
    replications,
    topicOptions,
    initializeTimeout
  )

  private[this] val producer = newOrClose(
    new KafkaProducer[OharaData, OharaData](baseConfig,
                                            KafkaUtil.wrapSerializer(OharaDataSerializer),
                                            KafkaUtil.wrapSerializer(OharaDataSerializer)))

  private[this] val consumer = newOrClose(
    new KafkaConsumer[OharaData, OharaData](baseConfig,
                                            KafkaUtil.wrapDeserializer(OharaDataSerializer),
                                            KafkaUtil.wrapDeserializer(OharaDataSerializer)))
  consumer.subscribe(util.Arrays.asList(topicName))

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
  private[this] val initialLatch = new CountDownLatch(1)

  /**
    * a single thread to handle the request and response. For call queue server, the supported consumer's key is OharaRequest,
    * and the supported consumer's value is REQUEST. the supported producer's key is OharaResponse,
    * the supported producer's value is RESPONSE.
    */
  private[this] val requestWorker = Future[Unit] {
    var firstPoll = true
    try {
      while (!this.isClosed) {
        try {
          val records = consumer.poll(if (firstPoll) 0 else pollTimeout.toMillis)
          if (firstPoll) initialLatch.countDown()
          firstPoll = false
          if (records != null) {
            records
            // TODO: throw exception if there are data from unknown topic? by chia
              .records(topicName)
              .forEach(record => {
                record.key match {
                  case internalRequest: OharaRequest =>
                    if (internalRequest.lease <= System.currentTimeMillis)
                      logger.debug(s"the lease of request is violated")
                    else {
                      record.value match {
                        case clientRequest: Request =>
                          undealtTasks.put(createCallQueueTask(internalRequest, clientRequest))
                        case _ =>
                          try sendToKafka(
                            OharaResponse.apply(responseUuid, internalRequest.uuid),
                            OharaException.apply(
                              new IllegalArgumentException(
                                "Unsupported type:"
                                  + (if (record.value() == null) "null"
                                     else record.value().getClass.getName)))
                          )
                          catch {
                            case _: Throwable => logger.error("Failed to response the unsupported request")
                          }

                      }
                    }
                  case _: OharaResponse => // this is for the call queue client
                  case _ =>
                    logger.error(
                      s"unsupported key:" + (if (record.key() == null) "null" else record.key().getClass.getName)
                        + " the supported key by call queue server is OharaRequest")
                }
              })
          }
        } catch {
          case _: WakeupException => logger.debug("interrupted by ourself")
        }
      }
    } catch {
      case e: Throwable => logger.error("failure when running the requestWorker", e)
    } finally {
      if (firstPoll) initialLatch.countDown()
      close()
    }
  }

  if (!initialLatch.await(initializeTimeout.toMillis, TimeUnit.MILLISECONDS)) {
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
    Iterator
      .continually(processingTasks.poll(1, TimeUnit.SECONDS))
      .takeWhile(_ != null)
      .foreach(_.complete(CallQueue.TERMINATE_TIMEOUT_EXCEPTION))
    Iterator
      .continually(undealtTasks.poll(1, TimeUnit.SECONDS))
      .takeWhile(_ != null)
      .foreach(_.complete(CallQueue.TERMINATE_TIMEOUT_EXCEPTION))
    if (consumer != null) consumer.wakeup()
    if (requestWorker != null) Await.result(requestWorker, 60 seconds)
    if (producer != null) producer.close()
    if (executor != null) {
      executor.shutdownNow()
      executor.awaitTermination(60, TimeUnit.SECONDS)
    }
  }

  override def countOfUndealtTasks: Int = undealtTasks.size()
  override def countOfProcessingTasks: Int = processingTasks.size()

}
