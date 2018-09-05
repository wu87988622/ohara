package com.island.ohara.configurator

import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.client.ConnectorJson.{
  ConnectorInformation,
  ConnectorStatus,
  CreateConnectorRequest,
  CreateConnectorResponse,
  Plugin,
  Status
}
import com.island.ohara.client.{ConnectorClient, SinkConnectorBuilder, SourceConnectorBuilder}
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.kafka.{ConsumerBuilder, KafkaClient, TopicBuilder, TopicDescription}
import com.island.ohara.serialization.Serializer
import com.typesafe.scalalogging.Logger
import org.eclipse.jetty.util.ConcurrentHashSet

import scala.concurrent.duration.Duration

class ConfiguratorBuilder {
  private[this] var uuidGenerator: Option[() => String] = Some(Configurator.DEFAULT_UUID_GENERATOR)
  private[this] var hostname: Option[String] = None
  private[this] var port: Option[Int] = None
  private[this] var store: Option[Store] = None
  private[this] var kafkaClient: Option[KafkaClient] = None
  private[this] var connectClient: Option[ConnectorClient] = None
  private[this] var initializationTimeout: Option[Duration] = Some(Configurator.DEFAULT_INITIALIZATION_TIMEOUT)
  private[this] var terminationTimeout: Option[Duration] = Some(Configurator.DEFAULT_TERMINATION_TIMEOUT)

  /**
    * set a specified uuid generator.
    *
    * @param generator uuid generator
    * @return this builder
    */
  def uuidGenerator(generator: () => String): ConfiguratorBuilder = {
    uuidGenerator = Some(generator)
    this
  }

  /**
    * set a specified hostname
    *
    * @param hostname used to build the rest server
    * @return this builder
    */
  def hostname(hostname: String): ConfiguratorBuilder = {
    this.hostname = Some(hostname)
    this
  }

  /**
    * set a specified port
    *
    * @param port used to build the rest server
    * @return this builder
    */
  def port(port: Int): ConfiguratorBuilder = {
    this.port = Some(port)
    this
  }

  /**
    * set a specified store used to maintain the ohara data.
    * NOTED: Configurator has responsibility to release this store.
    *
    * @param store used to maintain the ohara data.
    * @return this builder
    */
  def store(store: com.island.ohara.configurator.store.Store[String, Any]): ConfiguratorBuilder = {
    this.store = Some(new Store(store))
    this
  }

  def terminationTimeout(terminationTimeout: Duration): ConfiguratorBuilder = {
    this.terminationTimeout = Some(terminationTimeout)
    this
  }

  def initializationTimeout(initializationTimeout: Duration): ConfiguratorBuilder = {
    this.initializationTimeout = Some(initializationTimeout)
    this
  }

  def kafkaClient(kafkaClient: KafkaClient): ConfiguratorBuilder = {
    this.kafkaClient = Some(kafkaClient)
    this
  }

  def connectClient(connectClient: ConnectorClient): ConfiguratorBuilder = {
    this.connectClient = Some(connectClient)
    this
  }

  /**
    * set a mock kafka client to this configurator. a testing-purpose method.
    *
    * @return this builder
    */
  def noCluster: ConfiguratorBuilder = {
    kafkaClient(new FakeKafkaClient())
    connectClient(new FakeConnectorClient())
    store(com.island.ohara.configurator.store.Store.inMemory(Serializer.STRING, Serializer.OBJECT))
  }

  def build(): Configurator = new Configurator(hostname.get, port.get)(uuidGenerator.get,
                                                                       store.get,
                                                                       kafkaClient.get,
                                                                       connectClient.get,
                                                                       initializationTimeout.get,
                                                                       terminationTimeout.get)
}

/**
  * this class is exposed to Validator...an ugly way (TODO) by chia
  */
private[configurator] class FakeConnectorClient extends ConnectorClient {
  private[this] val cachedConnectors = new ConcurrentHashSet[String]()

  override def sourceConnectorCreator(): SourceConnectorBuilder = (request: CreateConnectorRequest) =>
    if (cachedConnectors.contains(request.name))
      throw new IllegalStateException(s"the connector:${request.name} exists!")
    else {
      cachedConnectors.add(request.name)
      CreateConnectorResponse(request.name, request.config, Seq.empty, "source")
  }

  override def sinkConnectorCreator(): SinkConnectorBuilder = (request: CreateConnectorRequest) =>
    if (cachedConnectors.contains(request.name))
      throw new IllegalStateException(s"the connector:${request.name} exists!")
    else CreateConnectorResponse(request.name, request.config, Seq.empty, "source")
  override def delete(name: String): Unit =
    if (!cachedConnectors.remove(name)) throw new IllegalStateException(s"the connector:$name doesn't exist!")
  import scala.collection.JavaConverters._
  // TODO; does this work? by chia
  override def plugins(): Seq[Plugin] = cachedConnectors.asScala.map(Plugin(_, "unknown", "unknown")).toSeq
  override protected def doClose(): Unit = cachedConnectors.clear()
  override def activeConnectors(): Seq[String] = cachedConnectors.asScala.toSeq
  override def workers: String = "Unknown"
  override def status(name: String): ConnectorInformation = {
    if (cachedConnectors.contains(name)) {
      ConnectorInformation(name, ConnectorStatus(Status.STARTED, "fake id", None), Seq.empty)
    } else throw new IllegalStateException(s"the connector:$name doesn't exist!")

  }
}

/**
  * A do-nothing impl of KafkaClient.
  * NOTED: It should be used in testing only.
  */
private class FakeKafkaClient extends KafkaClient {
  private[this] val log = Logger(KafkaClient.getClass.getName)
  private[this] val cachedTopics = new ConcurrentHashMap[String, TopicDescription]()
  override def exist(topicName: String, timeout: Duration): Boolean = {
    printDebugMessage()
    cachedTopics.contains(topicName)
  }
  override protected def doClose(): Unit = {
    printDebugMessage()
  }

  override def topicCreator: TopicBuilder = new TopicBuilder() {
    override def build(): Unit = {
      printDebugMessage()
      cachedTopics.put(topicName.get, TopicDescription(topicName.get, numberOfPartitions.get, numberOfReplications.get))
    }
  }

  override def addPartition(topicName: String, numberOfPartitions: Int, timeout: Duration): Unit = {
    printDebugMessage()
    Option(cachedTopics.get(topicName))
      .map(previous => TopicDescription(topicName, numberOfPartitions, previous.numberOfReplications))
      .getOrElse(throw new IllegalArgumentException(s"the topic:$topicName doesn't exist"))
  }

  private[this] def printDebugMessage(): Unit =
    log.debug("You are using a empty kafka client!!! Please make sure this message only appear in testing")

  override def topicInfo(topicName: String, timeout: Duration): Option[TopicDescription] = Option(
    cachedTopics.get(topicName))
  override def deleteTopic(topicName: String, timeout: Duration): Unit =
    if (cachedTopics.remove(topicName) == null) throw new IllegalArgumentException(s"$topicName doesn't exist")

  import scala.collection.JavaConverters._
  override def listTopics(timeout: Duration): Seq[String] = cachedTopics.keys().asScala.map(t => t).toList

  override def brokers: String = "Unknown"
  override def consumerBuilder[K, V](keySerializer: Serializer[K],
                                     valueSerializer: Serializer[V]): ConsumerBuilder[K, V] =
    throw new UnsupportedOperationException(
      s"${classOf[FakeKafkaClient].getSimpleName} does not support this operation")
}
