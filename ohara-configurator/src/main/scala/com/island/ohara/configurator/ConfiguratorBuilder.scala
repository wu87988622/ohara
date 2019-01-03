package com.island.ohara.configurator

import java.util.concurrent.ConcurrentHashMap
import java.{time, util}

import akka.http.scaladsl.server
import com.island.ohara.agent._
import com.island.ohara.client.ConfiguratorJson.{
  ClusterDescription,
  ContainerDescription,
  ContainerState,
  Node,
  ZookeeperClusterDescription
}
import com.island.ohara.client.ConnectorJson.{
  ConnectorConfig,
  ConnectorInformation,
  ConnectorStatus,
  CreateConnectorResponse,
  Plugin,
  TaskStatus
}
import com.island.ohara.client.{ConfiguratorJson, ConnectorClient, ConnectorCreator}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.data.connector.ConnectorState
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.kafka._
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future
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
  private[this] var clusterCollie: Option[ClusterCollie] = None

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
  def noCluster(): ConfiguratorBuilder = {
    kafkaClient(new FakeKafkaClient())
    connectClient(new FakeConnectorClient())
    store(com.island.ohara.configurator.store.Store.inMemory(Serializer.STRING, Serializer.OBJECT))
    clusterCollie(new FakeClusterCollie)
  }

  def clusterCollie(clusterCollie: ClusterCollie): ConfiguratorBuilder = {
    this.clusterCollie = Some(clusterCollie)
    this
  }

  def build(): Configurator = {
    val nodeCollie = new NodeCollie {
      override def add(node: Node): Unit = store.get.add(node)
      override def remove(name: String): Node = store.get.remove[Node](name)
      override def update(node: Node): Unit = store.get.update(node)
      override def close(): Unit = {
        // do nothing
      }
      override def iterator: Iterator[Node] = store.get.data[Node]
    }
    new Configurator(hostname.get, port.get, initializationTimeout.get, terminationTimeout.get, extraRoute)(
      ug = uuidGenerator.get,
      store = store.get,
      nodeCollie = nodeCollie,
      clusterCollie = clusterCollie.getOrElse(ClusterCollie.ssh(nodeCollie)),
      kafkaClient = kafkaClient.get,
      connectorClient = connectClient.get
    )
  }
}

/**
  * this class is exposed to Validator...an ugly way (TODO) by chia
  */
private[configurator] class FakeConnectorClient extends ConnectorClient {
  private[this] val cachedConnectors = new ConcurrentHashMap[String, Map[String, String]]()
  private[this] val cachedConnectorsState = new ConcurrentHashMap[String, ConnectorState]()

  override def connectorCreator(): ConnectorCreator = request => {
    if (cachedConnectors.contains(request.name))
      throw new IllegalStateException(s"the connector:${request.name} exists!")
    else {
      cachedConnectors.put(request.name, request.config)
      cachedConnectorsState.put(request.name, ConnectorState.RUNNING)
      CreateConnectorResponse(request.name, request.config, Seq.empty, "source")
    }
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
    cachedConnectorsState.put(name, ConnectorState.PAUSED)
  }

  override def resume(name: String): Unit = {
    checkExist(name)
    cachedConnectorsState.put(name, ConnectorState.RUNNING)
  }

  private[this] def checkExist(name: String): Unit =
    if (!cachedConnectors.containsKey(name)) throw new IllegalArgumentException(s"$name doesn't exist")
}

/**
  * A do-nothing impl from KafkaClient.
  * NOTED: It should be used in testing only.
  */
private[this] class FakeKafkaClient extends KafkaClient {

  import scala.collection.JavaConverters._

  private[this] val log = Logger(classOf[FakeKafkaClient].getName)
  private[this] val cachedTopics = new ConcurrentHashMap[String, TopicDescription]()

  override def topicCreator(): TopicCreator = new TopicCreator() {
    override def create(name: String): Unit = {
      printDebugMessage()
      cachedTopics.put(
        name,
        new TopicDescription(
          name,
          numberOfPartitions,
          numberOfReplications,
          options.asScala
            .map {
              case (k, v) => new TopicOption(k, v, false, false, false)
            }
            .toSeq
            .asJava
        )
      )
    }
  }

  private[this] def printDebugMessage(): Unit =
    log.debug("You are using a empty kafka client!!! Please make sure this message only appear in testing")

  override def exist(topicName: String, timeout: time.Duration): Boolean = {
    printDebugMessage()
    cachedTopics.contains(topicName)
  }

  override def topicDescription(topicName: String, timeout: time.Duration): TopicDescription = Option(
    cachedTopics.get(topicName)).get

  override def addPartitions(topicName: String, numberOfPartitions: Int, timeout: time.Duration): Unit = {
    printDebugMessage()
    Option(cachedTopics.get(topicName))
      .map(previous =>
        new TopicDescription(topicName, numberOfPartitions, previous.numberOfReplications, Seq.empty.asJava))
      .getOrElse(throw new IllegalArgumentException(s"the topic:$topicName doesn't exist"))
  }

  override def deleteTopic(topicName: String, timeout: time.Duration): Unit =
    if (cachedTopics.remove(topicName) == null) throw new IllegalArgumentException(s"$topicName doesn't exist")

  override def listTopics(timeout: time.Duration): util.List[String] = {
    cachedTopics.keys().asScala.map(t => t).toList.asJava
  }

  override def brokers(): String = "Unknown"

  override def consumerBuilder(): ConsumerBuilder = throw new UnsupportedOperationException(
    s"${classOf[FakeKafkaClient].getSimpleName} does not support this operation")

  override def close(): Unit = printDebugMessage()
}

/**
  * It doesn't involve any running cluster but save all description in memory
  */
private[this] class FakeClusterCollie extends ClusterCollie {

  override def zookeepersCollie(): ZookeeperCollie = new FakeZookeeperCollie

  override def brokerCollie(): BrokerCollie = throw new UnsupportedOperationException("TODO: see OHARA-1063")

  override def workerCollie(): WorkerCollie = throw new UnsupportedOperationException("TODO: see OHARA-1064")

  override def close(): Unit = {
    // do nothing
  }
}

private[this] abstract class FakeCollie[T <: ClusterDescription] extends Collie[T] {
  protected val clusterCache = new ConcurrentHashMap[String, T]()
  protected val containerCache = new ConcurrentHashMap[String, Seq[ContainerDescription]]()

  protected def generateContainerDescription(nodeName: String,
                                             imageName: String,
                                             state: ContainerState): ContainerDescription = ContainerDescription(
    nodeName = nodeName,
    id = CommonUtil.randomString(10),
    imageName = imageName,
    created = "unknown",
    state = state,
    name = CommonUtil.randomString(10),
    size = "unknown",
    portMappings = Seq.empty,
    environments = Map.empty,
    hostname = CommonUtil.randomString(10)
  )
  override def exists(clusterName: String): Boolean =
    clusterCache.containsKey(clusterName) && containerCache.containsKey(clusterName)

  override def remove(clusterName: String): Future[T] = if (exists(clusterName)) {
    val cluster = clusterCache.remove(clusterName)
    containerCache.remove(clusterName)
    Future.successful(cluster)
  } else Future.failed(new IllegalArgumentException(s"$clusterName doesn't exist"))

  override def logs(clusterName: String): Map[ConfiguratorJson.ContainerDescription, String] = Map.empty

  override def containers(clusterName: String): Seq[ConfiguratorJson.ContainerDescription] = if (exists(clusterName))
    containerCache.get(clusterName)
  else Seq.empty

  import scala.collection.JavaConverters._
  override def iterator: Iterator[T] = clusterCache.values().asScala.toIterator
}

private[this] class FakeZookeeperCollie extends FakeCollie[ZookeeperClusterDescription] with ZookeeperCollie {
  override def creator(): ZookeeperCollie.ClusterCreator =
    (clusterName, imageName, clientPort, peerPort, electionPort, nodeNames) =>
      if (exists(clusterName)) Future.failed(new IllegalArgumentException(s"$clusterName exists!!!"))
      else
        Future.successful {
          val cluster = ZookeeperClusterDescription(
            name = clusterName,
            imageName = imageName,
            clientPort = clientPort,
            peerPort = peerPort,
            electionPort = electionPort,
            nodeNames = nodeNames
          )
          clusterCache.put(clusterName, cluster)
          containerCache.put(clusterName,
                             nodeNames.map(
                               nodeName =>
                                 generateContainerDescription(
                                   nodeName = nodeName,
                                   imageName = imageName,
                                   state = ContainerState.RUNNING
                               )))
          cluster
      }

  override def removeNode(clusterName: String, nodeName: String): Future[ZookeeperClusterDescription] =
    Future.failed(
      new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

  override def addNode(clusterName: String, nodeName: String): Future[ZookeeperClusterDescription] =
    Future.failed(
      new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))
}
