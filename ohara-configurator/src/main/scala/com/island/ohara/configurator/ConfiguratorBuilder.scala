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

import java.util
import java.util.concurrent.ConcurrentHashMap

import akka.http.scaladsl.server
import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, ContainerState}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.kafka.WorkerJson.{
  ConnectorConfig,
  ConnectorInformation,
  ConnectorStatus,
  CreateConnectorResponse,
  Plugin,
  TaskStatus
}
import com.island.ohara.client.kafka.{ConnectorCreator, WorkerClient}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.data.{ConnectorState, Serializer}
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.kafka._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

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
    val collie = new FakeClusterCollie(bkConnectionProps, wkConnectionProps)
    val bkCluster = {
      val pair = bkConnectionProps.split(",")
      val host = pair.map(_.split(":").head).head
      val port = pair.map(_.split(":").last).head.toInt
      BrokerClusterInfo(
        name = "embedded_broker_cluster",
        imageName = "None",
        zookeeperClusterName = "None",
        clientPort = port,
        nodeNames = Seq(host)
      )
    }
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
    clusterCollie(collie)
  }

  private[this] def brokerClusterInfo(prefix: String): BrokerClusterInfo = BrokerClusterInfo(
    name = s"${prefix}brokerCluster",
    imageName = s"imageName$prefix",
    zookeeperClusterName = s"zkClusterName$prefix",
    // Assigning a negative value can make test fail quickly.
    clientPort = -1,
    nodeNames = Seq.empty
  )

  private[this] def workerClusterInfo(bkCluster: BrokerClusterInfo, prefix: String): WorkerClusterInfo =
    WorkerClusterInfo(
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
      nodeNames = Seq.empty
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
  override def activeConnectors(): Seq[String] = cachedConnectors.keys.asScala.toSeq
  override def connectionProps: String = "Unknown"
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
  * A do-nothing impl from BrokerClient.
  * NOTED: It should be used in testing only.
  */
private[configurator] class FakeBrokerClient extends BrokerClient {

  import scala.collection.JavaConverters._

  private[this] val cachedTopics = new ConcurrentHashMap[String, TopicDescription]()

  override def topicCreator(): TopicCreator = new TopicCreator() {
    override def create(name: String): Unit =
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
            .asJava,
          false
        )
      )
  }

  override def exist(topicName: String): Boolean = cachedTopics.contains(topicName)

  override def addPartitions(topicName: String, numberOfPartitions: Int): Unit = Option(cachedTopics.get(topicName))
    .map(
      previous =>
        new TopicDescription(
          topicName,
          numberOfPartitions,
          previous.numberOfReplications(),
          Seq.empty.asJava,
          false
      ))
    .getOrElse(throw new IllegalArgumentException(
      s"the topic:$topicName doesn't exist. actual:${cachedTopics.keys().asScala.mkString(",")}"))

  override def deleteTopic(topicName: String): Unit =
    if (cachedTopics.remove(topicName) == null) throw new IllegalArgumentException(s"$topicName doesn't exist")

  override def connectionProps(): String = "Unknown"

  override def close(): Unit = {
    // do nothing
  }

  override def topicDescriptions(): util.List[TopicDescription] =
    new util.ArrayList[TopicDescription](cachedTopics.values())
}

/**
  * It doesn't involve any running cluster but save all description in memory
  */
private[configurator] class FakeClusterCollie(bkConnectionProps: String, wkConnectionProps: String)
    extends ClusterCollie {
  private[this] val zkCollie: FakeZookeeperCollie = new FakeZookeeperCollie
  private[this] val bkCollie: FakeBrokerCollie = new FakeBrokerCollie(bkConnectionProps)
  private[this] val wkCollie: FakeWorkerCollie = new FakeWorkerCollie(wkConnectionProps)

  override def zookeepersCollie(): FakeZookeeperCollie = zkCollie

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
  override def exists(clusterName: String): Future[Boolean] =
    Future.successful(clusterCache.containsKey(clusterName))

  override def remove(clusterName: String): Future[T] = exists(clusterName).flatMap(if (_) Future.successful {
    val cluster = clusterCache.remove(clusterName)
    containerCache.remove(clusterName)
    cluster
  } else Future.failed(new NoSuchElementException(s"$clusterName doesn't exist")))

  override def logs(clusterName: String): Future[Map[ContainerInfo, String]] = Future.successful(Map.empty)

  override def containers(clusterName: String): Future[Seq[ContainerInfo]] =
    exists(clusterName).map(if (_) containerCache.get(clusterName) else Seq.empty)

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
    * fake broker client cache all topics info in-memory so we should keep singleton.
    */
  private[this] val fakeBrokerClient = new FakeBrokerClient
  override def createClient(clusterName: String): Future[(BrokerClusterInfo, BrokerClient)] =
    Future.successful(
      if (bkConnectionProps == null) (Some(clusterCache.get(clusterName)).get, fakeBrokerClient)
      // embedded mode should have single one cluster ...
      else (clusterCache.values().iterator().next(), BrokerClient.of(bkConnectionProps)))
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

private[this] class FakeWorkerCollie(wkConnectionProps: String)
    extends FakeCollie[WorkerClusterInfo]
    with WorkerCollie {
  private[this] val fakeWorkerClient = new FakeWorkerClient
  override def createClient(
    clusterName: String
  ): Future[(WorkerClusterInfo, WorkerClient)] =
    Future.successful(
      if (wkConnectionProps == null) (Some(clusterCache.get(clusterName)).get, fakeWorkerClient)
      // embedded mode should have single one cluster ...
      else (clusterCache.values().iterator().next(), WorkerClient(wkConnectionProps)))
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
          jarNames = Seq.empty,
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
