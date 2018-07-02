package com.island.ohara.configurator.call

import com.island.ohara.configurator.data.OharaData

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
  private[this] var topicName: Option[String] = None
  private[this] var groupId: Option[String] = None
  private[this] var numberOfPartitions: Option[Int] = Some(CallQueue.DEFAULT_PARTITION_NUMBER)
  private[this] var numberOfReplications: Option[Short] = Some(CallQueue.DEFAULT_REPLICATION_NUMBER)
  private[this] var pollTimeout: Option[Duration] = Some(CallQueue.DEFAULT_POLL_TIMEOUT)
  private[this] var initializationTimeout: Option[Duration] = Some(CallQueue.DEFAULT_INITIALIZATION_TIMEOUT)
  private[this] var topicOptions: Option[Map[String, String]] = Some(Map[String, String]())

  /**
    * set the kafka brokers information.
    * @param brokers kafka brokers
    * @return this builder
    */
  def brokers(brokers: String): CallQueueServerBuilder = {
    this.brokers = Some(brokers)
    this
  }

  /**
    * set the topic used to send/receive the request/response
    * @param topicName topic name
    * @return this builder
    */
  def topicName(topicName: String): CallQueueServerBuilder = {
    this.topicName = Some(topicName)
    this
  }

  /**
    * set the number of partition of initializing the topic
    * @param numberOfPartitions the number of partition
    * @return this builder
    */
  def numberOfPartitions(numberOfPartitions: Int): CallQueueServerBuilder = {
    this.numberOfPartitions = Some(numberOfPartitions)
    this
  }

  /**
    * set the number of replications of initializing the topic
    * @param numberOfReplications the number of partition
    * @return this builder
    */
  def numberOfReplications(numberOfReplications: Short): CallQueueServerBuilder = {
    this.numberOfReplications = Some(numberOfReplications)
    this
  }

  /**
    * the time to poll the consumer to receive the response.
    * @param pollTimeout poll time in millisecond
    * @return this builder
    */
  def pollTimeout(pollTimeout: Duration): CallQueueServerBuilder = {
    this.pollTimeout = Some(pollTimeout)
    this
  }

  /**
    * set the timeout of initializing the call queue client
    * @param initializationTimeout initial timeout
    * @return this builder
    */
  def initializationTimeout(initializationTimeout: Duration): CallQueueServerBuilder = {
    this.initializationTimeout = Some(initializationTimeout)
    this
  }

  /**
    * @param topicOptions extra configuration passed to call queue client to build the topic
    * @return this builder
    */
  def topicOptions(topicOptions: Map[String, String]): CallQueueServerBuilder = {
    this.topicOptions = Some(topicOptions)
    this
  }

  /**
    * the group id used to bind the consumer.
    * @param groupId group id
    * @return this builder
    */
  def groupId(groupId: String): CallQueueServerBuilder = {
    this.groupId = Some(groupId)
    this
  }

  /**
    * construct the call queue server
    * @tparam REQUEST the type of request
    * @tparam RESPONSE the type of response
    * @return a call queue server implementation
    */
  def build[REQUEST <: OharaData: ClassTag, RESPONSE <: OharaData](): CallQueueServer[REQUEST, RESPONSE] =
    new CallQueueServerImpl[REQUEST, RESPONSE](
      brokers.get,
      topicName.get,
      groupId.get,
      numberOfPartitions.get,
      numberOfReplications.get,
      pollTimeout.get,
      initializationTimeout.get,
      topicOptions.get
    )
}
