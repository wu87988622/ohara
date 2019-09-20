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

package com.island.ohara.it.agent.k8s

import java.util.concurrent.TimeUnit

import com.island.ohara.agent._
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.k8s.{K8SClient, K8sContainerState}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0.{BrokerApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.it.agent.ClusterNameHolder
import com.island.ohara.it.{IntegrationTest, EnvTestingUtils}
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

class TestK8SSimpleCollie extends IntegrationTest with Matchers {
  private[this] val RANDOM_LEN: Int = 7

  private[this] val nodes: Seq[Node] = EnvTestingUtils.k8sNodes()
  private[this] val nodeNames: Set[String] = nodes.map(_.hostname).toSet
  private[this] val nodeCollie: NodeCollie = NodeCollie(nodes)
  private[this] var nameHolder: ClusterNameHolder = _
  private[this] var clusterCollie: ClusterCollie = _
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  @Before
  def before(): Unit = {
    clusterCollie = ClusterCollie.builderOfK8s().nodeCollie(nodeCollie).k8sClient(EnvTestingUtils.k8sClient()).build()
    if (nodes.size < 2) skipTest("TestK8SSimpleCollie requires two nodes at least")
    nameHolder = ClusterNameHolder(nodes, EnvTestingUtils.k8sClient())
  }

  private[this] def waitZookeeperCluster(objectKey: ObjectKey): Unit = {
    await(() => result(clusterCollie.zookeeperCollie.clusters()).exists(_._1.key == objectKey))
    // since we only get "active" containers, all containers belong to the cluster should be running.
    // Currently, both k8s and pure docker have the same context of "RUNNING".
    // It is ok to filter container via RUNNING state.
    await(() => {
      val containers = result(clusterCollie.zookeeperCollie.containers(objectKey))
      containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
    })
  }

  private[this] def waitBrokerCluster(objectKey: ObjectKey): Unit = {
    await(() => result(clusterCollie.brokerCollie.clusters()).exists(_._1.key == objectKey))
    // since we only get "active" containers, all containers belong to the cluster should be running.
    // Currently, both k8s and pure docker have the same context of "RUNNING".
    // It is ok to filter container via RUNNING state.
    await(() => {
      val containers = result(clusterCollie.brokerCollie.containers(objectKey))
      containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
    })
  }

  private[this] def waitWorkerCluster(objectKey: ObjectKey): Unit = {
    await(() => result(clusterCollie.workerCollie.clusters()).exists(_._1.key == objectKey))
    // since we only get "active" containers, all containers belong to the cluster should be running.
    // Currently, both k8s and pure docker have the same context of "RUNNING".
    // It is ok to filter container via RUNNING state.
    await(() => {
      val containers = result(clusterCollie.workerCollie.containers(objectKey))
      containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
    })
  }

  private[this] def node(hostname: String): Node = Node(
    hostname = hostname,
    port = Some(22),
    user = Some("fake"),
    password = Some("fake"),
    services = Seq.empty,
    lastModified = CommonUtils.current(),
    validationReport = None,
    tags = Map.empty
  )

  @Test
  def testZookeeperCollie(): Unit = {
    val clusterName: String = nameHolder.generateClusterName()
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames.last
    firstNode should not be secondNode

    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie

    val clientPort: Int = CommonUtils.availablePort()
    val peerPort: Int = CommonUtils.availablePort()
    val electionPort: Int = CommonUtils.availablePort()

    val zookeeperClusterInfo =
      createZookeeperCollie(zookeeperCollie, clusterName, firstNode, clientPort, peerPort, electionPort)

    intercept[UnsupportedOperationException] {
      result(zookeeperCollie.addNode(zookeeperClusterInfo.key, secondNode))
    }.getMessage shouldBe "zookeeper collie doesn't support to add node from a running cluster"

    zookeeperClusterInfo.name shouldBe clusterName
    zookeeperClusterInfo.nodeNames.size shouldBe 1
    zookeeperClusterInfo.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
    zookeeperClusterInfo.clientPort shouldBe clientPort
    zookeeperClusterInfo.peerPort shouldBe peerPort
    zookeeperClusterInfo.electionPort shouldBe electionPort

  }

  @Test
  def testBrokerCollie(): Unit = {
    val firstNode: String = nodeNames.head

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = nameHolder.generateClusterName()
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    //Create broker cluster service
    val brokerClusterName: String = nameHolder.generateClusterName()
    val brokerClientPort = CommonUtils.availablePort()
    val brokerExporterPort = CommonUtils.availablePort()
    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie
    val brokerClusterInfo: BrokerClusterInfo =
      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Set(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)

    waitBrokerCluster(brokerClusterInfo.key)
    //Check broker info
    brokerClusterInfo.clientPort shouldBe brokerClientPort
    brokerClusterInfo.zookeeperClusterName shouldBe zkClusterName
    brokerClusterInfo.connectionProps shouldBe s"$firstNode:$brokerClientPort"

  }

  @Test
  def testWorkerCollie(): Unit = {
    val firstNode: String = nodeNames.head

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = nameHolder.generateClusterName()
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    //Create broker cluster service
    val brokerClusterName: String = nameHolder.generateClusterName()
    val brokerClientPort = CommonUtils.availablePort()
    val brokerExporterPort = CommonUtils.availablePort()
    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie

    val brokerClusterInfo = createBrokerCollie(brokerCollie,
                                               brokerClusterName,
                                               Set(firstNode),
                                               brokerClientPort,
                                               brokerExporterPort,
                                               zkClusterName)

    waitBrokerCluster(brokerClusterInfo.key)
    //Create worker cluster service
    val workerClusterName: String = nameHolder.generateClusterName()
    val workerCollie: WorkerCollie = clusterCollie.workerCollie
    val workerClientPort: Int = CommonUtils.availablePort()
    val workerClusterInfo: WorkerClusterInfo =
      createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)

    waitWorkerCluster(workerClusterInfo.key)
    workerClusterInfo.brokerClusterName shouldBe brokerClusterName
    workerClusterInfo.clientPort shouldBe workerClientPort
    workerClusterInfo.connectionProps shouldBe s"$firstNode:$workerClientPort"

    // Confirm pod name is a common format
    val wkPodName = Await.result(workerCollie.cluster(workerClusterName), TIMEOUT)._2.head.name
    val wkPodNameFieldSize = wkPodName.split("-").length
    val expectWKPodNameField = Collie.format("k8soccl", workerClusterInfo.group, workerClusterName, "wk").split("-")
    wkPodNameFieldSize shouldBe expectWKPodNameField.size

  }

  @Test
  def testAddBrokerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames.last

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = nameHolder.generateClusterName()
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    //Create broker cluster service
    val brokerClusterName: String = nameHolder.generateClusterName()
    val brokerClientPort = CommonUtils.availablePort()
    val brokerExporterPort = CommonUtils.availablePort()

    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie

    val brokerClusterInfo1: BrokerClusterInfo =
      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Set(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)

    waitBrokerCluster(brokerClusterInfo1.key)
    //Test add broker node
    val brokerClusterInfo2: BrokerClusterInfo = result(brokerCollie.addNode(brokerClusterInfo1.key, secondNode))

    brokerClusterInfo1.connectionProps shouldBe s"$firstNode:$brokerClientPort"
    brokerClusterInfo2.connectionProps should include(s"$secondNode:$brokerClientPort")
    brokerClusterInfo2.connectionProps should include(s"$firstNode:$brokerClientPort")

    brokerClusterInfo1.zookeeperClusterName shouldBe zkClusterName
    brokerClusterInfo2.zookeeperClusterName shouldBe zkClusterName
    brokerClusterInfo1.clientPort shouldBe brokerClientPort
    brokerClusterInfo2.clientPort shouldBe brokerClientPort

  }

  @Test
  def testAddWorkerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames.last

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = nameHolder.generateClusterName()
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    //Create broker cluster service
    val brokerClusterName: String = nameHolder.generateClusterName()
    val brokerClientPort = CommonUtils.availablePort()
    val brokerExporterPort = CommonUtils.availablePort()
    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie

    val brokerClusterInfo1: BrokerClusterInfo =
      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Set(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)

    waitBrokerCluster(brokerClusterInfo1.key)
    //Create worker cluster service
    val workerClusterName: String = nameHolder.generateClusterName()
    val workerCollie: WorkerCollie = clusterCollie.workerCollie
    val workerClientPort: Int = CommonUtils.availablePort()
    val workerClusterInfo1: WorkerClusterInfo =
      createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)
    waitWorkerCluster(workerClusterInfo1.key)
    //Test add worker node
    val workerClusterInfo2: WorkerClusterInfo = result(workerCollie.addNode(workerClusterInfo1.key, secondNode))
    brokerClusterInfo1.connectionProps shouldBe s"$firstNode:$brokerClientPort"
    workerClusterInfo1.connectionProps shouldBe s"$firstNode:$workerClientPort"
    workerClusterInfo2.connectionProps should include(s"$secondNode:$workerClientPort")
    workerClusterInfo2.connectionProps should include(s"$firstNode:$workerClientPort")
  }

  @Test
  def testRemoveBrokerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames.last

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = nameHolder.generateClusterName()
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie
    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie
    val brokerClusterName: String = nameHolder.generateClusterName()
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()
    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)
    //Create broker cluster service
    val brokerClientPort = CommonUtils.availablePort()
    val brokerExporterPort = CommonUtils.availablePort()
    val brokerClusterInfo = createBrokerCollie(brokerCollie,
                                               brokerClusterName,
                                               Set(firstNode),
                                               brokerClientPort,
                                               brokerExporterPort,
                                               zkClusterName)
    waitBrokerCluster(brokerClusterInfo.key)
    val firstContainerName: String = result(brokerCollie.cluster(brokerClusterName))._2.head.hostname

    result(brokerCollie.addNode(brokerClusterInfo.key, secondNode))
    // wait a period for k8s get latest pods information
    TimeUnit.SECONDS.sleep(10)
    result(brokerCollie.cluster(brokerClusterName))._2.size shouldBe 2
    result(brokerCollie.removeNode(brokerClusterInfo.key, firstNode))
    waitBrokerCluster(brokerClusterInfo.key)

    val k8sClient: K8SClient = EnvTestingUtils.k8sClient()
    await(() => !Await.result(k8sClient.containers(), TIMEOUT).exists(c => c.hostname.contains(firstContainerName)))
    result(brokerCollie.cluster(brokerClusterName))._2.size shouldBe 1
  }

  @Test
  def testRemoveWorkerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames.last

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = nameHolder.generateClusterName()
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie
    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie
    val workerCollie: WorkerCollie = clusterCollie.workerCollie
    val workerClusterName: String = nameHolder.generateClusterName()
    val brokerClusterName: String = nameHolder.generateClusterName()
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()
    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)
    //Create broker cluster service
    val brokerClientPort = CommonUtils.availablePort()
    val brokerExporterPort = CommonUtils.availablePort()
    val brokerClusterInfo = createBrokerCollie(brokerCollie,
                                               brokerClusterName,
                                               Set(firstNode),
                                               brokerClientPort,
                                               brokerExporterPort,
                                               zkClusterName)
    waitBrokerCluster(brokerClusterInfo.key)
    //Create worker cluster service
    val workerClientPort: Int = CommonUtils.availablePort()

    val workerClusterInfo =
      createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)
    waitWorkerCluster(workerClusterInfo.key)
    val firstContainerName: String = result(workerCollie.cluster(workerClusterName))._2.head.hostname

    result(workerCollie.addNode(workerClusterInfo.key, secondNode))
    // wait a period for k8s get latest pods information
    TimeUnit.SECONDS.sleep(10)
    result(workerCollie.cluster(workerClusterName))._2.size shouldBe 2
    result(workerCollie.removeNode(workerClusterInfo.key, firstNode))
    waitWorkerCluster(workerClusterInfo.key)

    val k8sClient: K8SClient = EnvTestingUtils.k8sClient()
    await(() => !Await.result(k8sClient.containers(), TIMEOUT).exists(c => c.hostname.contains(firstContainerName)))
    result(workerCollie.cluster(workerClusterName))._2.size shouldBe 1
  }

  @Test
  def testClusters(): Unit = {
    val firstNode: String = nodeNames.head

    //Create zookeeper cluster for start broker service
    val zkClusterName1: String = nameHolder.generateClusterName()
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie
    // for zookeeper, we assign node name to "hostname" field
    result(zookeeperCollie.clusters())
      .flatMap(x => x._2)
      .count(x => x.name.contains(zkClusterName1) && x.hostname.contains(firstNode)) shouldBe 0

    val zkClientPort1: Int = CommonUtils.availablePort()
    val zkPeerPort1: Int = CommonUtils.availablePort()
    val zkElectionPort1: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName1, firstNode, zkClientPort1, zkPeerPort1, zkElectionPort1)
    // for zookeeper, we assign node name to "hostname" field
    result(zookeeperCollie.clusters())
      .flatMap(x => x._2)
      .count(x => x.name.contains(zkClusterName1) && x.hostname.contains(firstNode)) shouldBe 1

  }

  @Test
  def testLog(): Unit = {
    val firstNode: String = nodeNames.head

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = nameHolder.generateClusterName()
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    //Create broker cluster service
    val brokerClusterName: String = nameHolder.generateClusterName()
    val brokerClientPort = CommonUtils.availablePort()
    val brokerExporterPort = CommonUtils.availablePort()
    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie

    val brokerClusterInfo = createBrokerCollie(brokerCollie,
                                               brokerClusterName,
                                               Set(firstNode),
                                               brokerClientPort,
                                               brokerExporterPort,
                                               zkClusterName)

    waitBrokerCluster(brokerClusterInfo.key)
    //Create worker cluster service
    val workerClusterName: String = nameHolder.generateClusterName()
    val workerCollie: WorkerCollie = clusterCollie.workerCollie
    val workerClientPort: Int = CommonUtils.availablePort()
    val workerClusterInfo =
      createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)

    waitWorkerCluster(workerClusterInfo.key)
    val workerContainerHostName: String = result(workerCollie.cluster(workerClusterName))._2.head.hostname

    val k8sClient: K8SClient = EnvTestingUtils.k8sClient()
    await(
      () =>
        Await
          .result(k8sClient.containers(), TIMEOUT)
          .count(c => c.hostname.contains(workerContainerHostName) && c.state == K8sContainerState.RUNNING.name) == 1
    )

    val logMessage: String = "Kafka Connect distributed worker initialization"
    await(() => result(workerCollie.logs(workerClusterInfo.key)).head._2.contains(logMessage))
    val workerlogs: Map[ContainerInfo, String] = result(workerCollie.logs(workerClusterInfo.key))
    workerlogs.size shouldBe 1

    workerlogs.head._2.contains(logMessage) shouldBe true
  }

  @Test
  def testVerifyNode(): Unit = {
    val firstNode: String = nodeNames.head
    val verifyNode: Try[String] =
      Await.result(
        clusterCollie.verifyNode(node(firstNode)),
        30 seconds
      )
    verifyNode.get.contains("node is running.") shouldBe true

    val unknowNode: Try[String] = Await.result(
      clusterCollie.verifyNode(node("unknow-node")),
      30 seconds
    )
    unknowNode.isFailure shouldBe true
  }

  @Test
  def testNameAndGroupLimitInZookeeper(): Unit = {
    // generate the max length name and group (the sum of length is equal to LIMIT_OF_KEY_LENGTH)
    val clusterName: String = CommonUtils.randomString(com.island.ohara.client.configurator.v0.LIMIT_OF_KEY_LENGTH / 2)
    val group: String = CommonUtils.randomString(com.island.ohara.client.configurator.v0.LIMIT_OF_KEY_LENGTH / 2)
    nameHolder.addClusterName(clusterName)

    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames.last
    firstNode should not be secondNode

    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie
    val clientPort: Int = CommonUtils.availablePort()
    val peerPort: Int = CommonUtils.availablePort()
    val electionPort: Int = CommonUtils.availablePort()

    val zookeeperClusterInfo =
      createZookeeperCollie(zookeeperCollie, clusterName, group, nodeNames.head, clientPort, peerPort, electionPort)

    // create zookeeper should be ok
    zookeeperClusterInfo.name shouldBe clusterName
    zookeeperClusterInfo.group shouldBe group
    zookeeperClusterInfo.nodeNames.size shouldBe 1
    zookeeperClusterInfo.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
    zookeeperClusterInfo.clientPort shouldBe clientPort
    zookeeperClusterInfo.peerPort shouldBe peerPort
    zookeeperClusterInfo.electionPort shouldBe electionPort
  }

  private[this] def createZookeeperCollie(zookeeperCollie: ZookeeperCollie,
                                          clusterName: String,
                                          nodeName: String,
                                          clientPort: Int,
                                          peerPort: Int,
                                          electionPort: Int): ZookeeperClusterInfo =
    createZookeeperCollie(zookeeperCollie,
                          clusterName,
                          ZookeeperApi.ZOOKEEPER_GROUP_DEFAULT,
                          nodeName,
                          clientPort,
                          peerPort,
                          electionPort)

  private[this] def createZookeeperCollie(zookeeperCollie: ZookeeperCollie,
                                          clusterName: String,
                                          group: String,
                                          nodeName: String,
                                          clientPort: Int,
                                          peerPort: Int,
                                          electionPort: Int): ZookeeperClusterInfo = {
    val info = result(
      zookeeperCollie.creator
        .imageName(ZookeeperApi.IMAGE_NAME_DEFAULT)
        .group(group)
        .name(clusterName)
        .clientPort(clientPort)
        .peerPort(peerPort)
        .electionPort(electionPort)
        .nodeName(nodeName)
        .create())
    waitZookeeperCluster(info.key)
    info
  }

  private[this] def createBrokerCollie(brokerCollie: BrokerCollie,
                                       cluseterName: String,
                                       nodeNames: Set[String],
                                       clientPort: Int,
                                       exporterPort: Int,
                                       zookeeperClusterName: String): BrokerClusterInfo = {
    result(
      brokerCollie.creator
        .imageName(BrokerApi.IMAGE_NAME_DEFAULT)
        .name(cluseterName)
        .group(BrokerApi.BROKER_GROUP_DEFAULT)
        .clientPort(clientPort)
        .exporterPort(exporterPort)
        .zookeeperClusterName(zookeeperClusterName)
        .nodeNames(nodeNames)
        .jmxPort(CommonUtils.availablePort())
        .create())
  }

  private[this] def createWorkerCollie(workerCollie: WorkerCollie,
                                       clusterName: String,
                                       nodeName: String,
                                       clientPort: Int,
                                       brokerClusterName: String): WorkerClusterInfo = {
    result(
      workerCollie.creator
        .imageName(WorkerApi.IMAGE_NAME_DEFAULT)
        .name(clusterName)
        .group(WorkerApi.WORKER_GROUP_DEFAULT)
        .clientPort(clientPort)
        .brokerClusterName(brokerClusterName)
        .groupId(CommonUtils.randomString(RANDOM_LEN))
        .configTopicName(CommonUtils.randomString(RANDOM_LEN))
        .configTopicReplications(1)
        .statusTopicName(CommonUtils.randomString(RANDOM_LEN))
        .statusTopicPartitions(1)
        .statusTopicReplications(1)
        .offsetTopicName(CommonUtils.randomString(RANDOM_LEN))
        .offsetTopicPartitions(1)
        .offsetTopicReplications(1)
        .jmxPort(CommonUtils.availablePort())
        .nodeName(nodeName)
        .create()
    )
  }

  @After
  final def tearDown(): Unit = {
    Releasable.close(clusterCollie)
    Releasable.close(nameHolder)
  }
}
