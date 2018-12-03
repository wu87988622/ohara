package com.island.ohara.configurator

import java.util.concurrent.ConcurrentHashMap

import akka.http.scaladsl.server
import com.island.ohara.client.ConnectorJson.{
  ConnectorConfig,
  ConnectorInformation,
  ConnectorStatus,
  CreateConnectorResponse,
  Plugin,
  State,
  TaskStatus
}
import com.island.ohara.client.{ConnectorClient, ConnectorCreator}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.kafka._
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.duration._

class ConfiguratorBuilder {
  private[this] var uuidGenerator: Option[() => String] = Some(() => CommonUtil.uuid())
  private[this] var hostname: Option[String] = None
  private[this] var port: Option[Int] = None
  private[this] var store: Option[Store] = None
  private[this] var kafkaClient: Option[KafkaClient] = None
  private[this] var connectClient: Option[ConnectorClient] = None
  private[this] var initializationTimeout: Option[Duration] = Some(10 seconds)
  private[this] var terminationTimeout: Option[Duration] = Some(10 seconds)
  private[this] var extraRoute: Option[server.Route] = None

  def extraRoute(extraRoute: server.Route): ConfiguratorBuilder = {
    this.extraRoute = Some(extraRoute)
    this
  }

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
  def store(store: com.island.ohara.configurator.store.Store[String, AnyRef]): ConfiguratorBuilder = {
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

  def build(): Configurator = new Configurator(
    hostname.get,
    port.get,
    initializationTimeout.get,
    terminationTimeout.get,
    extraRoute)(uuidGenerator.get, store.get, kafkaClient.get, connectClient.get)
}

/**
  * this class is exposed to Validator...an ugly way (TODO) by chia
  */
private[configurator] class FakeConnectorClient extends ConnectorClient {
  private[this] val cachedConnectors = new ConcurrentHashMap[String, Map[String, String]]()
  private[this] val cachedConnectorsState = new ConcurrentHashMap[String, State]()

  override def connectorCreator(): ConnectorCreator = request =>
    if (cachedConnectors.contains(request.name))
      throw new IllegalStateException(s"the connector:${request.name} exists!")
    else {
      cachedConnectors.put(request.name, request.config)
      cachedConnectorsState.put(request.name, State.RUNNING)
      CreateConnectorResponse(request.name, request.config, Seq.empty, "source")
  }

  override def delete(name: String): Unit =
    try if (cachedConnectors.remove(name) == null)
      throw new IllegalStateException(s"the connector:$name doesn't exist!")
    finally cachedConnectorsState.remove(name)
  import scala.collection.JavaConverters._
  // TODO; does this work? by chia
  override def plugins(): Seq[Plugin] = cachedConnectors.keys.asScala.map(Plugin(_, "unknown", "unknown")).toSeq
  override protected def doClose(): Unit = {
    cachedConnectors.clear()
    cachedConnectorsState.clear()
  }
  override def activeConnectors(): Seq[String] = cachedConnectors.keys.asScala.toSeq
  override def workers: String = "Unknown"
  override def status(name: String): ConnectorInformation = {
    checkExist(name)
    ConnectorInformation(name, ConnectorStatus(cachedConnectorsState.get(name), "fake id", None), Seq.empty)
  }

  override def config(name: String): ConnectorConfig = {
    val map = cachedConnectors.get(name)
    if (map == null) throw new IllegalArgumentException(s"$name doesn't exist")
    map.toJson.convertTo[ConnectorConfig]
  }

  override def taskStatus(name: String, id: Int): TaskStatus = {
    checkExist(name)
    TaskStatus(0, cachedConnectorsState.get(name), "worker_id", None)
  }
  override def pause(name: String): Unit = {
    checkExist(name)
    cachedConnectorsState.put(name, State.PAUSED)
  }

  override def resume(name: String): Unit = {
    checkExist(name)
    cachedConnectorsState.put(name, State.RUNNING)
  }

  private[this] def checkExist(name: String): Unit =
    if (!cachedConnectors.containsKey(name)) throw new IllegalArgumentException(s"$name doesn't exist")
}

/**
  * A do-nothing impl from KafkaClient.
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

  override def topicCreator(): TopicCreator = request => {
    printDebugMessage()
    cachedTopics.put(
      request.name,
      TopicDescription(request.name, request.numberOfPartitions, request.numberOfReplications, request.options.map {
        case (k, v) => TopicOption(k, v, false, false, false)
      }.toSeq)
    )
  }

  override def addPartitions(topicName: String, numberOfPartitions: Int, timeout: Duration): Unit = {
    printDebugMessage()
    Option(cachedTopics.get(topicName))
      .map(previous => TopicDescription(topicName, numberOfPartitions, previous.numberOfReplications, Seq.empty))
      .getOrElse(throw new IllegalArgumentException(s"the topic:$topicName doesn't exist"))
  }

  private[this] def printDebugMessage(): Unit =
    log.debug("You are using a empty kafka client!!! Please make sure this message only appear in testing")

  override def topicDescription(topicName: String, timeout: Duration): TopicDescription = Option(
    cachedTopics.get(topicName)).get
  override def deleteTopic(topicName: String, timeout: Duration): Unit =
    if (cachedTopics.remove(topicName) == null) throw new IllegalArgumentException(s"$topicName doesn't exist")

  import scala.collection.JavaConverters._
  override def listTopics(timeout: Duration): Seq[String] = cachedTopics.keys().asScala.map(t => t).toList

  override def brokers: String = "Unknown"
  override def consumerBuilder(): ConsumerBuilder = throw new UnsupportedOperationException(
    s"${classOf[FakeKafkaClient].getSimpleName} does not support this operation")
}
