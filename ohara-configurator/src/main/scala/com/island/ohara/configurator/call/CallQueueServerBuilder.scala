package com.island.ohara.configurator.call

import com.island.ohara.config.OharaConfig
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
  private[this] var partitions: Option[Int] = Some(CallQueue.DEFAULT_PARTITION_NUMBER)
  private[this] var replications: Option[Short] = Some(CallQueue.DEFAULT_REPLICATION_NUMBER)
  private[this] var pollTimeout: Option[Duration] = Some(CallQueue.DEFAULT_POLL_TIMEOUT)
  private[this] var initializationTimeout: Option[Duration] = Some(CallQueue.DEFAULT_INITIALIZATION_TIMEOUT)
  private[this] var config: Option[OharaConfig] = Some(OharaConfig())

  /**
    * set the kafka brokers information.
    * @param _brokers kafka brokers
    * @return this builder
    */
  def brokers(_brokers: String): CallQueueServerBuilder = {
    this.brokers = Some(_brokers)
    this
  }

  /**
    * set the topic used to send/receive the request/response
    * @param _topicName topic name
    * @return this builder
    */
  def topicName(_topicName: String): CallQueueServerBuilder = {
    this.topicName = Some(_topicName)
    this
  }

  /**
    * set the number of partition of initializing the topic
    * @param _partitions the number of partition
    * @return this builder
    */
  def partitions(_partitions: Int): CallQueueServerBuilder = {
    this.partitions = Some(_partitions)
    this
  }

  /**
    * set the number of replications of initializing the topic
    * @param _replications the number of partition
    * @return this builder
    */
  def replications(_replications: Short): CallQueueServerBuilder = {
    this.replications = Some(_replications)
    this
  }

  /**
    * the time to poll the consumer to receive the response.
    * @param _pollTimeout poll time in millisecond
    * @return this builder
    */
  def pollTimeout(_pollTimeout: Duration): CallQueueServerBuilder = {
    this.pollTimeout = Some(_pollTimeout)
    this
  }

  /**
    * set the timeout of initializing the call queue client
    * @param _initializationTimeout initial timeout
    * @return this builder
    */
  def initializationTimeout(_initializationTimeout: Duration): CallQueueServerBuilder = {
    this.initializationTimeout = Some(_initializationTimeout)
    this
  }

  /**
    * @param _config extra configuration passed to call queue client
    * @return this builder
    */
  def configuration(_config: OharaConfig): CallQueueServerBuilder = {
    this.config = Some(_config)
    this
  }

  /**
    * the group id used to bind the consumer.
    * @param _groupId group id
    * @return this builder
    */
  def groupId(_groupId: String): CallQueueServerBuilder = {
    this.groupId = Some(_groupId)
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
      partitions.get,
      replications.get,
      pollTimeout.get,
      initializationTimeout.get,
      config.get
    )
}
