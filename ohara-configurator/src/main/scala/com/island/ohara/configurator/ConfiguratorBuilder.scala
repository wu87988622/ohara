package com.island.ohara.configurator

import java.util.concurrent.ConcurrentHashMap
import java.{time, util}

import akka.http.scaladsl.server
import com.island.ohara.agent._
import com.island.ohara.client.ConnectorJson.{
  ConnectorConfig,
  ConnectorInformation,
  ConnectorStatus,
  CreateConnectorResponse,
  Plugin,
  TaskStatus
}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, ContainerState}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.{ConnectorClient, ConnectorCreator}
import com.island.ohara.common.data.{ConnectorState, Serializer}
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.kafka._
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ConfiguratorBuilder {
  private[this] var hostname: Option[String] = None
  private[this] var port: Option[Int] = None
  private[this] val store: Store = new Store(
    com.island.ohara.configurator.store.Store.inMemory(Serializer.STRING, Configurator.DATA_SERIALIZER))
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
    clusterCollie(new FakeClusterCollie)
  }

  /**
    * set a standalone configurator. It assigns only fake kafka client and connector client to configurator. The collie is based on ssh.
    * @return
    */
  def standalone(): ConfiguratorBuilder = {
    kafkaClient(new FakeKafkaClient())
    connectClient(new FakeConnectorClient())
  }

  def clusterCollie(clusterCollie: ClusterCollie): ConfiguratorBuilder = {
    this.clusterCollie = Some(clusterCollie)
    this
  }

  private[this] def nodeCollie(): NodeCollie = new NodeCollie {
    import scala.concurrent.duration._
    override def node(name: String): Node = Await.result(store.value[Node](name), 10 seconds)
    override def iterator: Iterator[Node] =
      Await.result(store.raw(), 10 seconds).filter(_.isInstanceOf[Node]).map(_.asInstanceOf[Node]).iterator
  }
  def build(): Configurator = {
    new Configurator(hostname.get, port.get, initializationTimeout.get, terminationTimeout.get, extraRoute)(
      store = store,
      nodeCollie = nodeCollie(),
      clusterCollie = clusterCollie.getOrElse(ClusterCollie.ssh(nodeCollie())),
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
  private[this] val zkCollie: ZookeeperCollie = new FakeZookeeperCollie
  private[this] val bkCollie: BrokerCollie = new FakeBrokerCollie
  private[this] val wkCollie: WorkerCollie = new FakeWorkerCollie
  override def zookeepersCollie(): ZookeeperCollie = zkCollie

  override def brokerCollie(): BrokerCollie = bkCollie

  override def workerCollie(): WorkerCollie = wkCollie

  override def close(): Unit = {
    // do nothing
  }
}

private[this] abstract class FakeCollie[T <: ClusterInfo] extends Collie[T] {
  protected val clusterCache = new ConcurrentHashMap[String, T]()
  protected val containerCache = new ConcurrentHashMap[String, Seq[ContainerInfo]]()

  protected def generateContainerDescription(nodeName: String,
                                             imageName: String,
                                             state: ContainerState): ContainerInfo = ContainerInfo(
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
  } else Future.failed(new NoSuchElementException(s"$clusterName doesn't exist"))

  override def logs(clusterName: String): Map[ContainerInfo, String] = Map.empty

  override def containers(clusterName: String): Seq[ContainerInfo] = if (exists(clusterName))
    containerCache.get(clusterName)
  else Seq.empty

  import scala.collection.JavaConverters._
  override def iterator: Iterator[T] = clusterCache.values().asScala.toIterator
}

private[this] class FakeZookeeperCollie extends FakeCollie[ZookeeperClusterInfo] with ZookeeperCollie {
  override def creator(): ZookeeperCollie.ClusterCreator =
    (clusterName, imageName, clientPort, peerPort, electionPort, nodeNames) =>
      Future.successful {
        val cluster = ZookeeperClusterInfo(
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

  override def removeNode(clusterName: String, nodeName: String): Future[ZookeeperClusterInfo] =
    Future.failed(
      new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

  override def addNode(clusterName: String, nodeName: String): Future[ZookeeperClusterInfo] =
    Future.failed(
      new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))
}

private[this] class FakeBrokerCollie extends FakeCollie[BrokerClusterInfo] with BrokerCollie {
  override def creator(): BrokerCollie.ClusterCreator =
    (clusterName, imageName, zookeeperClusterName, clientPort, _, nodeNames) =>
      Future.successful {
        val cluster = BrokerClusterInfo(
          name = clusterName,
          imageName = imageName,
          clientPort = clientPort,
          zookeeperClusterName = zookeeperClusterName,
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

  override def removeNode(clusterName: String, nodeName: String): Future[BrokerClusterInfo] = {
    val previous = clusterCache.get(clusterName)
    if (previous == null) Future.failed(new IllegalArgumentException(s"$clusterName doesn't exists!!!"))
    else if (!previous.nodeNames.contains(nodeName))
      Future.failed(new IllegalArgumentException(s"$nodeName doesn't run on $clusterName!!!"))
    else
      Future.successful {
        val newOne = previous.copy(nodeNames = previous.nodeNames.filterNot(_ == nodeName))
        clusterCache.put(clusterName, newOne)
        newOne
      }
  }

  override def addNode(clusterName: String, nodeName: String): Future[BrokerClusterInfo] = {
    val previous = clusterCache.get(clusterName)
    if (previous == null) Future.failed(new IllegalArgumentException(s"$clusterName doesn't exists!!!"))
    else if (previous.nodeNames.contains(nodeName))
      Future.failed(new IllegalArgumentException(s"$nodeName already run on $clusterName!!!"))
    else
      Future.successful {
        val newOne = previous.copy(nodeNames = previous.nodeNames :+ nodeName)
        clusterCache.put(clusterName, newOne)
        newOne
      }
  }
}

private[this] class FakeWorkerCollie extends FakeCollie[WorkerClusterInfo] with WorkerCollie {
  override def creator(): WorkerCollie.ClusterCreator =
    (clusterName,
     imageName,
     brokerClusterName,
     clientPort,
     groupId,
     offsetTopicName,
     offsetTopicReplications,
     offsetTopicPartitions,
     statusTopicName,
     statusTopicReplications,
     statusTopicPartitions,
     configTopicName,
     configTopicReplications,
     nodeNames) =>
      Future.successful {
        val cluster = WorkerClusterInfo(
          name = clusterName,
          imageName = imageName,
          brokerClusterName = brokerClusterName,
          clientPort = clientPort,
          groupId = groupId,
          offsetTopicName = offsetTopicName,
          offsetTopicPartitions = offsetTopicPartitions,
          offsetTopicReplications = offsetTopicReplications,
          configTopicName = configTopicName,
          configTopicPartitions = 1,
          configTopicReplications = configTopicReplications,
          statusTopicName = statusTopicName,
          statusTopicPartitions = statusTopicPartitions,
          statusTopicReplications = statusTopicReplications,
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

  override def removeNode(clusterName: String, nodeName: String): Future[WorkerClusterInfo] = {
    val previous = clusterCache.get(clusterName)
    if (previous == null) Future.failed(new IllegalArgumentException(s"$clusterName doesn't exists!!!"))
    else if (!previous.nodeNames.contains(nodeName))
      Future.failed(new IllegalArgumentException(s"$nodeName doesn't run on $clusterName!!!"))
    else
      Future.successful {
        val newOne = previous.copy(nodeNames = previous.nodeNames.filterNot(_ == nodeName))
        clusterCache.put(clusterName, newOne)
        newOne
      }
  }

  override def addNode(clusterName: String, nodeName: String): Future[WorkerClusterInfo] = {
    val previous = clusterCache.get(clusterName)
    if (previous == null) Future.failed(new IllegalArgumentException(s"$clusterName doesn't exists!!!"))
    else if (previous.nodeNames.contains(nodeName))
      Future.failed(new IllegalArgumentException(s"$nodeName already run on $clusterName!!!"))
    else
      Future.successful {
        val newOne = previous.copy(nodeNames = previous.nodeNames :+ nodeName)
        clusterCache.put(clusterName, newOne)
        newOne
      }
  }
}
