package com.island.ohara.configurator.store

import com.island.ohara.common.data.Serializer

import scala.concurrent.duration._

/**
  * a helper class to build the Store. Excluding the #brokers and #topicName, other arguments
  * are optional. TODO: introduce a way to highlight the required arguments and optional arguments.
  */
class StoreBuilder {
  private[this] var brokers: Option[String] = None
  private[this] var topicName: Option[String] = None
  private[this] var pollTimeout: Option[Duration] = Some(5 seconds)

  /**
    * set the kafka brokers information.
    * @param brokers kafka brokers
    * @return this builder
    */
  def brokers(brokers: String): StoreBuilder = {
    this.brokers = Some(brokers)
    this
  }

  /**
    * set the topic used to send/receive the request/response
    * @param topicName topic name
    * @return this builder
    */
  def topicName(topicName: String): StoreBuilder = {
    this.topicName = Some(topicName)
    this
  }

  /**
    * the time to poll the consumer to receive the response.
    * @param pollTimeout poll time in millisecond
    * @return this builder
    */
  def pollTimeout(pollTimeout: Duration): StoreBuilder = {
    this.pollTimeout = Some(pollTimeout)
    this
  }

  def build[K, V](implicit keySerializer: Serializer[K], valueSerializer: Serializer[V]): Store[K, V] = TopicStore(
    brokers.get,
    topicName.get,
    pollTimeout.get
  )

}
