/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.configurator.call

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, LinkedBlockingQueue, TimeUnit}

import com.island.ohara.client.configurator.v0.ErrorApi
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.island.ohara.kafka.exception.OharaExecutionException
import com.island.ohara.kafka.{Consumer, KafkaClient, Producer}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.errors.{TopicExistsException, WakeupException}

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._
import scala.reflect.ClassTag

private object CallQueueServerImpl {
  val LOG = Logger(getClass.getName)
  val NUMBER_OF_PARTITIONS: Int = 1
  val NUMBER_OF_REPLICATIONS: Short = 1
  val TIMEOUT: Duration = 30 seconds
  def apply[Request: ClassTag, Response <: AnyRef](brokers: String,
                                                   requestTopic: String,
                                                   responseTopic: String,
                                                   groupId: String,
                                                   pollTimeout: Duration): CallQueueServerImpl[Request, Response] = {
    val client = KafkaClient.of(brokers)
    try {
      def createIfNeed(topicName: String): Unit = if (!client.exist(topicName))
        try client
          .topicCreator()
          .numberOfPartitions(CallQueueServerImpl.NUMBER_OF_PARTITIONS)
          .numberOfReplications(CallQueueServerImpl.NUMBER_OF_REPLICATIONS)
          // enable kafka save the latest message for each key
          .deleted()
          .timeout(java.time.Duration.ofNanos(CallQueueServerImpl.TIMEOUT.toNanos))
          .create(topicName)
        catch {
          case e: OharaExecutionException =>
            e.getCause match {
              case _: TopicExistsException => LOG.error(s"$topicName exists but we didn't notice this fact")
              case _                       => if (e.getCause == null) throw e else throw e.getCause
            }
        }
      createIfNeed(requestTopic)
      createIfNeed(responseTopic)
    } finally client.close()
    val uuid = CommonUtil.uuid()
    val producer = Producer.builder().brokers(brokers).build(Serializer.OBJECT, Serializer.OBJECT)
    val consumer = try Consumer
      .builder()
      .brokers(brokers)
      .offsetAfterLatest()
      // the uuid from requestConsumer is configurable. If user assign multi node with same uuid, it means user want to
      // distribute the request.
      .groupId(groupId)
      .topicName(requestTopic)
      .build(Serializer.OBJECT, Serializer.OBJECT)
    catch {
      case e: Throwable =>
        producer.close()
        throw e
    }
    new CallQueueServerImpl(requestTopic, responseTopic, groupId, pollTimeout, uuid, producer, consumer)
  }
}

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
private class CallQueueServerImpl[Request: ClassTag, Response <: AnyRef] private (requestTopic: String,
                                                                                  responseTopic: String,
                                                                                  groupId: String,
                                                                                  pollTimeout: Duration,
                                                                                  uuid: String,
                                                                                  producer: Producer[AnyRef, AnyRef],
                                                                                  consumer: Consumer[AnyRef, AnyRef])
    extends CallQueueServer[Request, Response] {

  import CallQueueServerImpl._

  /**
    * requestWorker thread.
    */
  private[this] implicit val executor: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  /**
    * We have to trace each request so we need a incrementable index.
    */
  private[this] val indexer = new AtomicLong(0)

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
        response = ErrorApi.of(exception)
        send(CallQueueResponse(responseUuid(), internalRequest.uuid), response)
      }

      def send(key: AnyRef, value: AnyRef): Unit = try {
        processingTasks.remove(this)
        sendToKafka(key, value)
      } catch {
        case e: Throwable =>
          LOG.error("Failed to send the response back to client", e)
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
          val records = consumer.poll(java.time.Duration.ofNanos(pollTimeout.toNanos)).asScala
          records
            .filter(_.topic == requestTopic)
            .foreach(record => {
              Option(record.key.orElse(null)).foreach {
                case internalRequest: CallQueueRequest =>
                  if (internalRequest.lease.toMillis <= CommonUtil.current())
                    LOG.debug(s"the lease from request is violated")
                  else
                    Option(record.value.orElse(null)).foreach {
                      case clientRequest: Request =>
                        undealtTasks.put(createCallQueueTask(internalRequest, clientRequest))
                      case _ =>
                        try sendToKafka(CallQueueResponse(responseUuid(), internalRequest.uuid),
                                        ErrorApi.of(new IllegalArgumentException(s"Unsupported type")))
                        catch {
                          case _: Throwable => LOG.error("Failed to response the unsupported request")
                        }
                    }
                case _: CallQueueResponse => // this is for the call queue client
                case _ =>
                  LOG.error(s"unsupported key. The supported key by call queue server is CallQueueRequest")
              }
            })
        } catch {
          case _: WakeupException => LOG.debug("interrupted by ourself")
        }
      }
    } catch {
      case e: Throwable => LOG.error("failure when running the requestWorker", e)
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
    ReleaseOnce.close(producer)
    ReleaseOnce.close(consumer)
    if (executor != null) {
      executor.shutdownNow()
      executor.awaitTermination(60, TimeUnit.SECONDS)
    }
  }

  override def countOfUndealtTasks: Int = undealtTasks.size()
  override def countOfProcessingTasks: Int = processingTasks.size()

}
