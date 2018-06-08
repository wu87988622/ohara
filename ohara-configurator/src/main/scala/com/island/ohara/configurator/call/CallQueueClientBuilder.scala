package com.island.ohara.configurator.call

import com.island.ohara.config.OharaConfig
import com.island.ohara.configurator.data.OharaData

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
  * a helper class to build the call queue client. Excluding the #brokers and #topicName, other arguments
  * are optional. TODO: introduce a way to highlight the required arguments and optional arguments.
  */
class CallQueueClientBuilder private[call] {

  private[this] var brokers: Option[String] = None
  private[this] var topicName: Option[String] = None
  private[this] var pollTimeout: Option[Duration] = Some(CallQueue.DEFAULT_POLL_TIMEOUT)
  private[this] var initializationTimeout: Option[Duration] = Some(CallQueue.DEFAULT_INITIALIZATION_TIMEOUT)
  private[this] var expirationCleanupTime: Option[Duration] = Some(CallQueue.DEFAULT_EXPIRATION_CLEANUP_TIME)
  private[this] var config: Option[OharaConfig] = Some(OharaConfig())

  /**
    * set the kafka brokers information.
    * @param _brokers kafka brokers
    * @return this builder
    */
  def brokers(_brokers: String): CallQueueClientBuilder = {
    this.brokers = Some(_brokers)
    this
  }

  /**
    * set the topic used to send/receive the request/response
    * @param _topicName topic name
    * @return this builder
    */
  def topicName(_topicName: String): CallQueueClientBuilder = {
    this.topicName = Some(_topicName)
    this
  }

  /**
    * the time to poll the consumer to receive the response.
    * @param _pollTimeout poll time in millisecond
    * @return this builder
    */
  def pollTimeout(_pollTimeout: Duration): CallQueueClientBuilder = {
    this.pollTimeout = Some(_pollTimeout)
    this
  }

  /**
    * set the timeout of initializing the call queue client
    * @param _initializationTimeout initial timeout
    * @return this builder
    */
  def initializationTimeout(_initializationTimeout: Duration): CallQueueClientBuilder = {
    this.initializationTimeout = Some(_initializationTimeout)
    this
  }

  /**
    * set the time to clear the expired request.
    * @param expirationCleanupTime time to clear the expired request. in milliseconds
    * @return this builder
    */
  def expirationCleanupTime(expirationCleanupTime: Duration): CallQueueClientBuilder = {
    this.expirationCleanupTime = Some(expirationCleanupTime)
    this
  }

  /**
    * @param _config extra configuration passed to call queue client
    * @return this builder
    */
  def configuration(_config: OharaConfig): CallQueueClientBuilder = {
    this.config = Some(_config)
    this
  }

  /**
    * construct the call queue client
    * @tparam REQUEST the type of request
    * @tparam RESPONSE the type of response
    * @return a call queue client implementation
    */
  def build[REQUEST <: OharaData, RESPONSE <: OharaData: ClassTag](): CallQueueClient[REQUEST, RESPONSE] =
    new CallQueueClientImpl[REQUEST, RESPONSE](
      brokers.get,
      topicName.get,
      pollTimeout.get,
      initializationTimeout.get,
      expirationCleanupTime.get,
      config.get
    )
}
