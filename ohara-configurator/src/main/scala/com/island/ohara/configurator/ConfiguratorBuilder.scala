/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.configurator

import java.util.concurrent.ConcurrentHashMap

import akka.http.scaladsl.server
import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, ContainerState}
import com.island.ohara.client.configurator.v0.InfoApi.ConnectorVersion
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0.{ClusterInfo, InfoApi, NodeApi}
import com.island.ohara.client.kafka.TopicAdmin.TopicInfo
import com.island.ohara.client.kafka.WorkerJson.{
  ConnectorConfig,
  ConnectorInfo,
  ConnectorStatus,
  CreateConnectorResponse,
  Plugin,
  TaskStatus
}
import com.island.ohara.client.kafka.{TopicAdmin, WorkerClient}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.data.{ConnectorState, Serializer}
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ConfiguratorBuilder {
  private[this] var hostname: Option[String] = None
  private[this] var port: Option[Int] = None
  private[this] val store: Store = new Store(
    com.island.ohara.configurator.store.Store.inMemory(Serializer.STRING, Configurator.DATA_SERIALIZER))
  private[this] var initializationTimeout: Option[Duration] = Some(10 seconds)
  private[this] var terminationTimeout: Option[Duration] = Some(10 seconds)
  private[this] var extraRoute: Option[server.Route] = None
  private[this] var clusterCollie: Option[ClusterCollie] = None

  @Optional("default is none")
  def extraRoute(extraRoute: server.Route): ConfiguratorBuilder = {
    this.extraRoute = Some(extraRoute)
    this
  }

  /**
    * set advertised hostname which will be exposed by configurator.
    *
    * @param hostname used to build the rest server
    * @return this builder
    */
  @Optional("default is 0.0.0.0")
  def hostname(hostname: String): ConfiguratorBuilder = {
    this.hostname = Some(hostname)
    this
  }

  /**
    * set advertised port which will be exposed by configurator.
    *
    * @param port used to build the rest server
    * @return this builder
    */
  @Optional("default is random port")
  def port(port: Int): ConfiguratorBuilder = {
    this.port = Some(port)
    this
  }

  @Optional("default is 10 seconds")
  def terminationTimeout(terminationTimeout: Duration): ConfiguratorBuilder = {
    this.terminationTimeout = Some(terminationTimeout)
    this
  }

  @Optional("default is 10 seconds")
  def initializationTimeout(initializationTimeout: Duration): ConfiguratorBuilder = {
    this.initializationTimeout = Some(initializationTimeout)
    this
  }

  /**
    * set all client to fake mode with a pre-created broker cluster and worker cluster.
    *
    * @return this builder
    */
  def fake(): ConfiguratorBuilder = fake(1, 1)

  /**
    * set all client to fake mode but broker client and worker client is true that they are connecting to embedded cluster.
    *
    * @return this builder
    */
  def fake(bkConnectionProps: String, wkConnectionProps: String): ConfiguratorBuilder = {
    // we fake nodes for embedded bk and wk
    def nodes(s: String): Seq[String] = s.split(",").map(_.split(":").head)
    (nodes(bkConnectionProps) ++ nodes(wkConnectionProps))
    // DON'T add duplicate nodes!!!
      .toSet[String]
      .map(NodeApi.node(_, 22, "fake", "fake"))
      .foreach(store.add)
    val collie = new FakeClusterCollie(bkConnectionProps, wkConnectionProps)
    val bkCluster = {
      val pair = bkConnectionProps.split(",")
      val host = pair.map(_.split(":").head).head
      val port = pair.map(_.split(":").last).head.toInt
      BrokerClusterInfo(
        name = "embedded_broker_cluster",
        imageName = "None",
        zookeeperClusterName = "None",
        exporterPort = -1,
        clientPort = port,
        nodeNames = Seq(host)
      )
    }
    val connectorVersions =
      Await.result(WorkerClient(wkConnectionProps).plugins(), 10 seconds).map(InfoApi.toConnectorVersion)
    val wkCluster = {
      val pair = wkConnectionProps.split(",")
      val host = pair.map(_.split(":").head).head
      val port = pair.map(_.split(":").last).head.toInt
      WorkerClusterInfo(
        name = "embedded_worker_cluster",
        imageName = "None",
        brokerClusterName = bkCluster.name,
        clientPort = port,
        groupId = "None",
        statusTopicName = "None",
        statusTopicPartitions = 1,
        statusTopicReplications = 1.asInstanceOf[Short],
        configTopicName = "None",
        configTopicPartitions = 1,
        configTopicReplications = 1.asInstanceOf[Short],
        offsetTopicName = "None",
        offsetTopicPartitions = 1,
        offsetTopicReplications = 1.asInstanceOf[Short],
        jarNames = Seq.empty,
        sources = connectorVersions.filter(_.typeName.toLowerCase == "source"),
        sinks = connectorVersions.filter(_.typeName.toLowerCase == "sink"),
        nodeNames = Seq(host)
      )
    }
    collie.brokerCollie().add(Seq.empty, bkCluster)
    collie.workerCollie().add(Seq.empty, wkCluster)
    clusterCollie(collie)
  }

  /**
    * Create a fake collie with specified number of broker/worker cluster.
    * @param numberOfBrokerCluster number of broker cluster
    * @param numberOfWorkerCluster number of worker cluster
    * @return this builder
    */
  def fake(numberOfBrokerCluster: Int, numberOfWorkerCluster: Int): ConfiguratorBuilder = {
    if (numberOfBrokerCluster <= 0)
      throw new IllegalArgumentException(s"numberOfBrokerCluster:$numberOfBrokerCluster should be positive")
    if (numberOfWorkerCluster < 0)
      throw new IllegalArgumentException(s"numberOfWorkerCluster:$numberOfWorkerCluster should be positive")
    val collie = new FakeClusterCollie(null, null)

    // create fake bk clusters
    val brokerClusters: Seq[BrokerClusterInfo] =
      (0 until numberOfBrokerCluster).map(index => brokerClusterInfo(s"fake$index"))
    brokerClusters.foreach(collie.brokerCollie().add(Seq.empty, _))

    (0 until numberOfWorkerCluster).foreach { index =>
      collie
        .workerCollie()
        .add(Seq.empty,
             workerClusterInfo(brokerClusters((Math.random() % brokerClusters.size).asInstanceOf[Int]), s"fake$index"))
    }
    // fake nodes
    brokerClusters
      .flatMap(_.nodeNames)
      // DON'T add duplicate nodes!!!
      .toSet[String]
      .map(NodeApi.node(_, 22, "fake", "fake"))
      .foreach(store.add)
    clusterCollie(collie)
  }

  private[this] def brokerClusterInfo(prefix: String): BrokerClusterInfo = FakeBrokerClusterInfo(
    name = s"${prefix}brokerCluster",
    imageName = s"imageName$prefix",
    zookeeperClusterName = s"zkClusterName$prefix",
    // Assigning a negative value can make test fail quickly.
    exporterPort = -1,
    clientPort = -1,
    nodeNames = (0 to 2).map(_ => CommonUtil.randomString(5))
  )

  private[this] def workerClusterInfo(bkCluster: BrokerClusterInfo, prefix: String): WorkerClusterInfo =
    FakeWorkerClusterInfo(
      name = s"${prefix}workerCluster",
      imageName = s"imageName$prefix",
      brokerClusterName = bkCluster.name,
      // Assigning a negative value can make test fail quickly.
      clientPort = -1,
      groupId = s"groupId$prefix",
      statusTopicName = s"statusTopicName$prefix",
      statusTopicPartitions = 1,
      statusTopicReplications = 1.asInstanceOf[Short],
      configTopicName = s"configTopicName$prefix",
      configTopicPartitions = 1,
      configTopicReplications = 1.asInstanceOf[Short],
      offsetTopicName = s"offsetTopicName$prefix",
      offsetTopicPartitions = 1,
      offsetTopicReplications = 1.asInstanceOf[Short],
      jarNames = Seq.empty,
      sources = Seq.empty,
      sinks = Seq.empty,
      nodeNames = bkCluster.nodeNames
    )

  @Optional("default is implemented by ssh")
  def clusterCollie(clusterCollie: ClusterCollie): ConfiguratorBuilder = {
    this.clusterCollie = Some(clusterCollie)
    this
  }

  private[this] def nodeCollie(): NodeCollie = new NodeCollie {
    override def node(name: String): Future[Node] = store.value[Node](name)
    override def nodes(): Future[Seq[Node]] = store.values[Node]
  }

  def build(): Configurator = {
    new Configurator(hostname, port, initializationTimeout.get, terminationTimeout.get, extraRoute)(
      store = store,
      nodeCollie = nodeCollie(),
      clusterCollie = clusterCollie.getOrElse(ClusterCollie.ssh(nodeCollie()))
    )
  }
}

/**
  * this class is exposed to Validator...an ugly way (TODO) by chia
  */
private[configurator] class FakeWorkerClient extends WorkerClient {
  private[this] val cachedConnectors = new ConcurrentHashMap[String, Map[String, String]]()
  private[this] val cachedConnectorsState = new ConcurrentHashMap[String, ConnectorState]()

  override def connectorCreator(): WorkerClient.Creator = request => {
    if (cachedConnectors.contains(request.name))
      Future.failed(new IllegalStateException(s"the connector:${request.name} exists!"))
    else {
      cachedConnectors.put(request.name, request.config)
      cachedConnectorsState.put(request.name, ConnectorState.RUNNING)
      Future.successful(CreateConnectorResponse(request.name, request.config, Seq.empty, "source"))
    }
  }

  override def delete(name: String): Future[Unit] = try if (cachedConnectors.remove(name) == null)
    Future.failed(new IllegalStateException(s"the connector:$name doesn't exist!"))
  else Future.successful(())
  finally cachedConnectorsState.remove(name)
  import scala.collection.JavaConverters._
  // TODO; does this work? by chia
  override def plugins(): Future[Seq[Plugin]] =
    Future.successful(cachedConnectors.keys.asScala.map(Plugin(_, "unknown", "unknown")).toSeq)
  override def activeConnectors(): Future[Seq[String]] = Future.successful(cachedConnectors.keys.asScala.toSeq)
  override def connectionProps: String = "Unknown"
  override def status(name: String): Future[ConnectorInfo] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else
      Future.successful(
        ConnectorInfo(name, ConnectorStatus(cachedConnectorsState.get(name), "fake id", None), Seq.empty))

  override def config(name: String): Future[ConnectorConfig] = {
    val map = cachedConnectors.get(name)
    if (map == null) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(map.toJson.convertTo[ConnectorConfig])
  }

  override def taskStatus(name: String, id: Int): Future[TaskStatus] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(TaskStatus(0, cachedConnectorsState.get(name), "worker_id", None))

  override def pause(name: String): Future[Unit] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(cachedConnectorsState.put(name, ConnectorState.PAUSED))

  override def resume(name: String): Future[Unit] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(cachedConnectorsState.put(name, ConnectorState.RUNNING))
}

private[this] class FakeTopicAdmin extends TopicAdmin {
  import scala.collection.JavaConverters._

  override val connectionProps: String = "Unknown"

  private[this] val cachedTopics = new ConcurrentHashMap[String, TopicInfo]()

  override def changePartitions(name: String, numberOfPartitions: Int): Future[TopicInfo] =
    Option(cachedTopics.get(name))
      .map(
        previous =>
          Future.successful(
            TopicInfo(
              name,
              numberOfPartitions,
              previous.numberOfReplications
            )))
      .getOrElse(Future.failed(new NoSuchElementException(
        s"the topic:$name doesn't exist. actual:${cachedTopics.keys().asScala.mkString(",")}")))

  override def list(): Future[Seq[TopicAdmin.TopicInfo]] = Future.successful {
    cachedTopics.values().asScala.toSeq
  }

  override def creator(): TopicAdmin.Creator = (name, numberOfPartitions, numberOfReplications, _) =>
    if (cachedTopics.contains(name)) Future.failed(new IllegalArgumentException(s"$name already exists!"))
    else {
      cachedTopics.put(name, TopicInfo(name, numberOfPartitions, numberOfReplications))
      Future.successful(
        TopicInfo(
          name,
          numberOfPartitions,
          numberOfReplications
        ))
  }
  private[this] var _closed = false
  override def close(): Unit = {
    _closed = true
  }

  override def closed(): Boolean = _closed
  override def delete(name: String): Future[TopicInfo] = Option(cachedTopics.remove(name))
    .map(Future.successful)
    .getOrElse(Future.failed(new NoSuchElementException(s"the topic:$name doesn't exist")))
}

/**
  * It doesn't involve any running cluster but save all description in memory
  */
private[this] class FakeClusterCollie(bkConnectionProps: String, wkConnectionProps: String) extends ClusterCollie {
  private[this] val zkCollie: FakeZookeeperCollie = new FakeZookeeperCollie
  private[this] val bkCollie: FakeBrokerCollie = new FakeBrokerCollie(bkConnectionProps)
  private[this] val wkCollie: FakeWorkerCollie = new FakeWorkerCollie(wkConnectionProps)

  override def zookeeperCollie(): FakeZookeeperCollie = zkCollie

  override def brokerCollie(): FakeBrokerCollie = bkCollie

  override def workerCollie(): FakeWorkerCollie = wkCollie

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
  override def exist(clusterName: String): Future[Boolean] =
    Future.successful(clusterCache.containsKey(clusterName))

  override def remove(clusterName: String): Future[T] = exist(clusterName).flatMap(if (_) Future.successful {
    val cluster = clusterCache.remove(clusterName)
    containerCache.remove(clusterName)
    cluster
  } else Future.failed(new NoSuchClusterException(s"$clusterName doesn't exist")))

  override def logs(clusterName: String): Future[Map[ContainerInfo, String]] = Future.successful(Map.empty)

  override def containers(clusterName: String): Future[Seq[ContainerInfo]] =
    exist(clusterName).map(if (_) containerCache.get(clusterName) else Seq.empty)

  import scala.collection.JavaConverters._
  override def clusters(): Future[Map[T, Seq[ContainerInfo]]] = Future.successful(
    clusterCache
      .values()
      .asScala
      .map { cluster =>
        cluster -> containerCache.get(cluster.name)
      }
      .toMap)

  def add(containers: Seq[ContainerInfo], cluster: T): Unit = {
    clusterCache.put(cluster.name, cluster)
    if (containers.nonEmpty) containerCache.put(cluster.name, containers)
  }
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

private[this] class FakeBrokerCollie(bkConnectionProps: String)
    extends FakeCollie[BrokerClusterInfo]
    with BrokerCollie {

  /**
    * cache all topics info in-memory so we should keep instance for each fake cluster.
    */
  private[this] val fakeAdminCache = new ConcurrentHashMap[BrokerClusterInfo, FakeTopicAdmin]
  override def creator(): BrokerCollie.ClusterCreator =
    (clusterName, imageName, zookeeperClusterName, clientPort, exporterPort, nodeNames) =>
      Future.successful {
        val cluster = FakeBrokerClusterInfo(
          name = clusterName,
          imageName = imageName,
          clientPort = clientPort,
          exporterPort = exporterPort,
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

  override def removeNode(clusterName: String, nodeName: String): Future[FakeBrokerClusterInfo] = {
    val previous = clusterCache.get(clusterName)
    if (previous == null) Future.failed(new NoSuchClusterException(s"$clusterName doesn't exists!!!"))
    else if (!previous.nodeNames.contains(nodeName))
      Future.failed(new IllegalArgumentException(s"$nodeName doesn't run on $clusterName!!!"))
    else
      Future.successful {
        val newOne = FakeBrokerClusterInfo(
          name = previous.name,
          imageName = previous.imageName,
          zookeeperClusterName = previous.zookeeperClusterName,
          exporterPort = previous.exporterPort,
          clientPort = previous.clientPort,
          nodeNames = previous.nodeNames.filterNot(_ == nodeName)
        )
        clusterCache.put(clusterName, newOne)
        newOne
      }
  }

  override def addNode(clusterName: String, nodeName: String): Future[FakeBrokerClusterInfo] = {
    val previous = clusterCache.get(clusterName)
    if (previous == null) Future.failed(new NoSuchClusterException(s"$clusterName doesn't exists!!!"))
    else if (previous.nodeNames.contains(nodeName))
      Future.failed(new IllegalArgumentException(s"$nodeName already run on $clusterName!!!"))
    else
      Future.successful {
        val newOne = FakeBrokerClusterInfo(
          name = previous.name,
          imageName = previous.imageName,
          zookeeperClusterName = previous.zookeeperClusterName,
          clientPort = previous.clientPort,
          exporterPort = previous.exporterPort,
          nodeNames = previous.nodeNames :+ nodeName
        )
        clusterCache.put(clusterName, newOne)
        newOne
      }
  }
  override def topicAdmin(cluster: BrokerClusterInfo): TopicAdmin =
    // < 0 means it is a fake cluster
    if (cluster.isInstanceOf[FakeBrokerClusterInfo]) {
      val fake = new FakeTopicAdmin
      val r = fakeAdminCache.putIfAbsent(cluster, fake)
      if (r == null) fake else r
    } else TopicAdmin(bkConnectionProps)
}

case class FakeBrokerClusterInfo(name: String,
                                 imageName: String,
                                 zookeeperClusterName: String,
                                 exporterPort: Int,
                                 clientPort: Int,
                                 nodeNames: Seq[String])
    extends BrokerClusterInfo

case class FakeWorkerClusterInfo(name: String,
                                 imageName: String,
                                 brokerClusterName: String,
                                 clientPort: Int,
                                 groupId: String,
                                 statusTopicName: String,
                                 statusTopicPartitions: Int,
                                 statusTopicReplications: Short,
                                 configTopicName: String,
                                 configTopicPartitions: Int,
                                 configTopicReplications: Short,
                                 offsetTopicName: String,
                                 offsetTopicPartitions: Int,
                                 offsetTopicReplications: Short,
                                 jarNames: Seq[String],
                                 sources: Seq[ConnectorVersion],
                                 sinks: Seq[ConnectorVersion],
                                 nodeNames: Seq[String])
    extends WorkerClusterInfo

private[this] class FakeWorkerCollie(wkConnectionProps: String)
    extends FakeCollie[WorkerClusterInfo]
    with WorkerCollie {

  /**
    * cache all connectors info in-memory so we should keep instance for each fake cluster.
    */
  private[this] val fakeClientCache = new ConcurrentHashMap[WorkerClusterInfo, FakeWorkerClient]
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
     jarUrls,
     nodeNames) =>
      Future.successful {
        val cluster = FakeWorkerClusterInfo(
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
          jarNames = Seq.empty,
          sources = Seq.empty,
          sinks = Seq.empty,
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

  override def removeNode(clusterName: String, nodeName: String): Future[FakeWorkerClusterInfo] = {
    val previous = clusterCache.get(clusterName)
    if (previous == null) Future.failed(new NoSuchClusterException(s"$clusterName doesn't exists!!!"))
    else if (!previous.nodeNames.contains(nodeName))
      Future.failed(new IllegalArgumentException(s"$nodeName doesn't run on $clusterName!!!"))
    else
      Future.successful {
        val newOne = FakeWorkerClusterInfo(
          name = previous.name,
          imageName = previous.imageName,
          brokerClusterName = previous.brokerClusterName,
          clientPort = previous.clientPort,
          groupId = previous.groupId,
          statusTopicName = previous.statusTopicName,
          statusTopicPartitions = previous.statusTopicPartitions,
          statusTopicReplications = previous.statusTopicReplications,
          configTopicName = previous.configTopicName,
          configTopicPartitions = previous.configTopicPartitions,
          configTopicReplications = previous.configTopicReplications,
          offsetTopicName = previous.offsetTopicName,
          offsetTopicPartitions = previous.offsetTopicPartitions,
          offsetTopicReplications = previous.offsetTopicReplications,
          jarNames = previous.jarNames,
          sources = Seq.empty,
          sinks = Seq.empty,
          nodeNames = previous.nodeNames.filterNot(_ == nodeName)
        )
        clusterCache.put(clusterName, newOne)
        newOne
      }
  }

  override def addNode(clusterName: String, nodeName: String): Future[FakeWorkerClusterInfo] = {
    val previous = clusterCache.get(clusterName)
    if (previous == null) Future.failed(new NoSuchClusterException(s"$clusterName doesn't exists!!!"))
    else if (previous.nodeNames.contains(nodeName))
      Future.failed(new IllegalArgumentException(s"$nodeName already run on $clusterName!!!"))
    else
      Future.successful {
        val newOne = FakeWorkerClusterInfo(
          name = previous.name,
          imageName = previous.imageName,
          brokerClusterName = previous.brokerClusterName,
          clientPort = previous.clientPort,
          groupId = previous.groupId,
          statusTopicName = previous.statusTopicName,
          statusTopicPartitions = previous.statusTopicPartitions,
          statusTopicReplications = previous.statusTopicReplications,
          configTopicName = previous.configTopicName,
          configTopicPartitions = previous.configTopicPartitions,
          configTopicReplications = previous.configTopicReplications,
          offsetTopicName = previous.offsetTopicName,
          offsetTopicPartitions = previous.offsetTopicPartitions,
          offsetTopicReplications = previous.offsetTopicReplications,
          jarNames = previous.jarNames,
          sources = Seq.empty,
          sinks = Seq.empty,
          nodeNames = previous.nodeNames :+ nodeName
        )
        clusterCache.put(clusterName, newOne)
        newOne
      }
  }
  override def workerClient(cluster: WorkerClusterInfo): WorkerClient =
    // < 0 means it is a fake cluster
    if (cluster.isInstanceOf[FakeWorkerClusterInfo]) {
      val fake = new FakeWorkerClient
      val r = fakeClientCache.putIfAbsent(cluster, fake)
      if (r == null) fake else r
    } else WorkerClient(wkConnectionProps)
}
