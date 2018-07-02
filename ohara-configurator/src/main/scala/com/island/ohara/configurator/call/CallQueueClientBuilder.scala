package com.island.ohara.configurator.call

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

  /**
    * set the kafka brokers information.
    * @param brokers kafka brokers
    * @return this builder
    */
  def brokers(brokers: String): CallQueueClientBuilder = {
    this.brokers = Some(brokers)
    this
  }

  /**
    * set the topic used to send/receive the request/response
    * @param topicName topic name
    * @return this builder
    */
  def topicName(topicName: String): CallQueueClientBuilder = {
    this.topicName = Some(topicName)
    this
  }

  /**
    * the time to poll the consumer to receive the response.
    * @param pollTimeout poll time in millisecond
    * @return this builder
    */
  def pollTimeout(pollTimeout: Duration): CallQueueClientBuilder = {
    this.pollTimeout = Some(pollTimeout)
    this
  }

  /**
    * set the timeout of initializing the call queue client
    * @param initializationTimeout initial timeout
    * @return this builder
    */
  def initializationTimeout(initializationTimeout: Duration): CallQueueClientBuilder = {
    this.initializationTimeout = Some(initializationTimeout)
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
      expirationCleanupTime.get
    )
}
