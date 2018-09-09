package com.island.ohara.configurator.store

import com.island.ohara.serialization.Serializer

import scala.concurrent.duration.Duration

/**
  * a helper class to build the Store. Excluding the #brokers and #topicName, other arguments
  * are optional. TODO: introduce a way to highlight the required arguments and optional arguments.
  */
class StoreBuilder {
  private[this] var brokers: Option[String] = None
  private[this] var topicName: Option[String] = None
  private[this] var numberOfPartitions: Option[Int] = Some(Store.DEFAULT_NUMBER_OF_PARTITIONS)
  private[this] var numberOfReplications: Option[Short] = Some(Store.DEFAULT_NUMBER_OF_REPLICATIONS)
  private[this] var pollTimeout: Option[Duration] = Some(Store.DEFAULT_POLL_TIMEOUT)
  private[this] var initializationTimeout: Option[Duration] = Some(Store.DEFAULT_INITIALIZATION_TIMEOUT)
  private[this] var topicOptions: Option[Map[String, String]] = Some(Map[String, String]())

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
    * set the number of partition of initializing the topic
    * @param numberOfPartitions the number of partition
    * @return this builder
    */
  def numberOfPartitions(numberOfPartitions: Int): StoreBuilder = {
    this.numberOfPartitions = Some(numberOfPartitions)
    this
  }

  /**
    * set the number of replications of initializing the topic
    * @param numberOfReplications the number of partition
    * @return this builder
    */
  def numberOfReplications(numberOfReplications: Short): StoreBuilder = {
    this.numberOfReplications = Some(numberOfReplications)
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

  /**
    * set the timeout of initializing the StoreBuilder
    * @param initializationTimeout initial timeout
    * @return this builder
    */
  def initializationTimeout(initializationTimeout: Duration): StoreBuilder = {
    this.initializationTimeout = Some(initializationTimeout)
    this
  }

  /**
    * @param topicOptions extra configuration passed to StoreBuilder to build the topic
    * @return this builder
    */
  def topicOptions(topicOptions: Map[String, String]): StoreBuilder = {
    this.topicOptions = Some(topicOptions)
    this
  }

  def build[K, V](implicit keySerializer: Serializer[K], valueSerializer: Serializer[V]): Store[K, V] = new TopicStore(
    brokers.get,
    topicName.get,
    numberOfPartitions.get,
    numberOfReplications.get,
    pollTimeout.get,
    initializationTimeout.get,
    topicOptions.get
  )
}
