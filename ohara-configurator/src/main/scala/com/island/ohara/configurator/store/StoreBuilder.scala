package com.island.ohara.configurator.store

import com.island.ohara.config.OharaConfig
import com.island.ohara.serialization.Serializer

import scala.concurrent.duration.Duration

/**
  * a helper class to build the Store. Excluding the #brokers and #topicName, other arguments
  * are optional. TODO: introduce a way to highlight the required arguments and optional arguments.
  */
class StoreBuilder[K, V](val keySerializer: Serializer[K], val valueSerializer: Serializer[V]) {
  private[this] var brokers: Option[String] = None
  private[this] var topicName: Option[String] = None
  private[this] var partitions: Option[Int] = Some(Store.DEFAULT_PARTITION_NUMBER)
  private[this] var replications: Option[Short] = Some(Store.DEFAULT_REPLICATION_NUMBER)
  private[this] var pollTimeout: Option[Duration] = Some(Store.DEFAULT_POLL_TIMEOUT)
  private[this] var initializationTimeout: Option[Duration] = Some(Store.DEFAULT_INITIALIZATION_TIMEOUT)
  private[this] var config: Option[OharaConfig] = Some(OharaConfig())

  /**
    * set the kafka brokers information.
    * @param _brokers kafka brokers
    * @return this builder
    */
  def brokers(_brokers: String): StoreBuilder[K, V] = {
    this.brokers = Some(_brokers)
    this
  }

  /**
    * set the topic used to send/receive the request/response
    * @param _topicName topic name
    * @return this builder
    */
  def topicName(_topicName: String): StoreBuilder[K, V] = {
    this.topicName = Some(_topicName)
    this
  }

  /**
    * set the number of partition of initializing the topic
    * @param _partitions the number of partition
    * @return this builder
    */
  def partitions(_partitions: Int): StoreBuilder[K, V] = {
    this.partitions = Some(_partitions)
    this
  }

  /**
    * set the number of replications of initializing the topic
    * @param _replications the number of partition
    * @return this builder
    */
  def replications(_replications: Short): StoreBuilder[K, V] = {
    this.replications = Some(_replications)
    this
  }

  /**
    * the time to poll the consumer to receive the response.
    * @param _pollTimeout poll time in millisecond
    * @return this builder
    */
  def pollTimeout(_pollTimeout: Duration): StoreBuilder[K, V] = {
    this.pollTimeout = Some(_pollTimeout)
    this
  }

  /**
    * set the timeout of initializing the StoreBuilder
    * @param _initializationTimeout initial timeout
    * @return this builder
    */
  def initializationTimeout(_initializationTimeout: Duration): StoreBuilder[K, V] = {
    this.initializationTimeout = Some(_initializationTimeout)
    this
  }

  /**
    * @param _config extra configuration passed to StoreBuilder
    * @return this builder
    */
  def configuration(_config: OharaConfig): StoreBuilder[K, V] = {
    this.config = Some(_config)
    this
  }

  def build(): Store[K, V] = new TopicStore(
    Option(keySerializer).get,
    Option(valueSerializer).get,
    brokers.get,
    topicName.get,
    partitions.get,
    replications.get,
    pollTimeout.get,
    initializationTimeout.get,
    config.get
  )
}
