package com.island.ohara.configurator.call

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
  * a helper class to build the call queue server. Excluding the #brokers, #topicName, and #groupId, other arguments
  * are optional. TODO: introduce a way to highlight the required arguments and optional arguments.
  * NOTED: the group id is used to bind the consumer. If you have two call queue servers bind on the same group id,
  * the request sent by call queue client will be sent to one of call queue server.
  */
class CallQueueServerBuilder private[call] {
  private[this] var brokers: Option[String] = None
  private[this] var requestTopic: Option[String] = None
  private[this] var responseTopic: Option[String] = None
  private[this] var groupId: Option[String] = None
  private[this] var pollTimeout: Option[Duration] = Some(CallQueue.DEFAULT_POLL_TIMEOUT)

  /**
    * set the kafka brokers information.
    *
    * @param brokers kafka brokers
    * @return this builder
    */
  def brokers(brokers: String): CallQueueServerBuilder = {
    this.brokers = Some(brokers)
    this
  }

  /**
    * set the topic used to receive request
    * @param requestTopic topic name
    * @return this builder
    */
  def requestTopic(requestTopic: String): CallQueueServerBuilder = {
    this.requestTopic = Some(requestTopic)
    this
  }

  /**
    * set the topic used to send response
    * @param responseTopic topic name
    * @return this builder
    */
  def responseTopic(responseTopic: String): CallQueueServerBuilder = {
    this.responseTopic = Some(responseTopic)
    this
  }

  /**
    * the time to poll the consumer to receive the response.
    *
    * @param pollTimeout poll time in millisecond
    * @return this builder
    */
  def pollTimeout(pollTimeout: Duration): CallQueueServerBuilder = {
    this.pollTimeout = Some(pollTimeout)
    this
  }

  /**
    * the group id used to bind the consumer.
    *
    * @param groupId group id
    * @return this builder
    */
  def groupId(groupId: String): CallQueueServerBuilder = {
    this.groupId = Some(groupId)
    this
  }

  /**
    * construct the call queue server
    *
    * @tparam REQUEST  the type of request
    * @tparam RESPONSE the type of response
    * @return a call queue server implementation
    */
  def build[REQUEST: ClassTag, RESPONSE](): CallQueueServer[REQUEST, RESPONSE] =
    new CallQueueServerImpl[REQUEST, RESPONSE](
      brokers.get,
      requestTopic.get,
      responseTopic.get,
      groupId.get,
      pollTimeout.get
    )
}
