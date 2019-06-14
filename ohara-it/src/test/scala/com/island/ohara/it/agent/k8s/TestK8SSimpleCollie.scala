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
import com.island.ohara.agent.k8s.{K8SClient, K8SStatusInfo, K8sContainerState}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0.{BrokerApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.it.IntegrationTest
import com.typesafe.scalalogging.Logger
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class TestK8SSimpleCollie extends IntegrationTest with Matchers {
  private[this] val log = Logger(classOf[TestK8SSimpleCollie])
  private[this] val K8S_API_SERVER_URL_KEY: String = "ohara.it.k8s"
  private[this] val K8S_API_NODE_NAME_KEY: String = "ohara.it.k8s.nodename"

  private[this] val API_SERVER_URL: Option[String] = sys.env.get(K8S_API_SERVER_URL_KEY)
  private[this] val NODE_SERVER_NAME: Option[String] = sys.env.get(K8S_API_NODE_NAME_KEY)

  private[this] val RANDOM_LEN: Int = 7

  private[this] val nodeCache = new ArrayBuffer[Node]()
  private[this] val nodeCollie: NodeCollie = NodeCollie(nodeCache)

  private[this] var clusterCollie: ClusterCollie = _
  private[this] var nodeNames: Seq[String] = _
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  @Before
  def testBefore(): Unit = {
    val message = s"The k8s is skip test, Please setting $K8S_API_SERVER_URL_KEY and $K8S_API_NODE_NAME_KEY properties"
    if (API_SERVER_URL.isEmpty || NODE_SERVER_NAME.isEmpty) {
      log.info(message)
      skipTest(message)
    }
    log.info(s"Test K8S API Server is: ${API_SERVER_URL.get}, NodeName is: ${NODE_SERVER_NAME.get}")

    clusterCollie = ClusterCollie.builderOfK8s().nodeCollie(nodeCollie).k8sClient(K8SClient(API_SERVER_URL.get)).build()
    nodeNames = NODE_SERVER_NAME.get.split(",").toSeq
  }

  private[this] def waitZookeeperCluster(clusterName: String): Unit = {
    await(() => result(clusterCollie.zookeeperCollie().clusters).exists(_._1.name == clusterName))
    // since we only get "active" containers, all containers belong to the cluster should be running.
    // Currently, both k8s and pure docker have the same context of "RUNNING".
    // It is ok to filter container via RUNNING state.
    await(() => {
      val containers = result(clusterCollie.zookeeperCollie().containers(clusterName))
      containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
    })
  }

  private[this] def waitBrokerCluster(clusterName: String): Unit = {
    await(() => result(clusterCollie.brokerCollie().clusters).exists(_._1.name == clusterName))
    // since we only get "active" containers, all containers belong to the cluster should be running.
    // Currently, both k8s and pure docker have the same context of "RUNNING".
    // It is ok to filter container via RUNNING state.
    await(() => {
      val containers = result(clusterCollie.brokerCollie().containers(clusterName))
      containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
    })
  }

  private[this] def waitWorkerCluster(clusterName: String): Unit = {
    await(() => result(clusterCollie.workerCollie().clusters).exists(_._1.name == clusterName))
    // since we only get "active" containers, all containers belong to the cluster should be running.
    // Currently, both k8s and pure docker have the same context of "RUNNING".
    // It is ok to filter container via RUNNING state.
    await(() => {
      val containers = result(clusterCollie.workerCollie().containers(clusterName))
      containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
    })
  }

  @Test
  def testZookeeperCollie(): Unit = {
    val clusterName: String = s"cluster${CommonUtils.randomString(RANDOM_LEN)}"
    val firstNode: String = nodeNames.head
    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))

    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()

    val clientPort: Int = CommonUtils.availablePort()
    val peerPort: Int = CommonUtils.availablePort()
    val electionPort: Int = CommonUtils.availablePort()

    val zookeeperClusterInfo =
      createZookeeperCollie(zookeeperCollie, clusterName, firstNode, clientPort, peerPort, electionPort)

    try {
      intercept[UnsupportedOperationException] {
        result(zookeeperCollie.addNode(clusterName, firstNode))
      }.getMessage shouldBe "zookeeper collie doesn't support to add node from a running cluster"

      zookeeperClusterInfo.name shouldBe clusterName
      zookeeperClusterInfo.nodeNames.size shouldBe 1
      zookeeperClusterInfo.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
      zookeeperClusterInfo.clientPort shouldBe clientPort
      zookeeperClusterInfo.peerPort shouldBe peerPort
      zookeeperClusterInfo.electionPort shouldBe electionPort
    } finally {
      //Remove Zookeeper Cluster
      result(zookeeperCollie.remove(clusterName))
    }
  }

  @Test
  def testBrokerCollie(): Unit = {
    val firstNode: String = nodeNames.head
    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtils.randomString(RANDOM_LEN)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    try {
      //Create broker cluster service
      val brokerClusterName: String = s"brokercluster${CommonUtils.randomString(RANDOM_LEN)}"
      val brokerClientPort = CommonUtils.availablePort()
      val brokerExporterPort = CommonUtils.availablePort()
      val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()
      val brokerClusterInfo: BrokerClusterInfo =
        createBrokerCollie(brokerCollie,
                           brokerClusterName,
                           Seq(firstNode),
                           brokerClientPort,
                           brokerExporterPort,
                           zkClusterName)
      try {
        waitBrokerCluster(brokerClusterName)
        //Check broker info
        brokerClusterInfo.clientPort shouldBe brokerClientPort
        brokerClusterInfo.zookeeperClusterName shouldBe zkClusterName
        brokerClusterInfo.connectionProps shouldBe s"$firstNode:$brokerClientPort"
      } finally result(brokerCollie.remove(brokerClusterName))
    } finally result(zookeeperCollie.remove(zkClusterName))

  }

  @Test
  def testWorkerCollie(): Unit = {
    val firstNode: String = nodeNames.head
    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtils.randomString(RANDOM_LEN)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    try {
      //Create broker cluster service
      val brokerClusterName: String = s"brokercluster${CommonUtils.randomString(RANDOM_LEN)}"
      val brokerClientPort = CommonUtils.availablePort()
      val brokerExporterPort = CommonUtils.availablePort()
      val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()

      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Seq(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)

      try {
        waitBrokerCluster(brokerClusterName)
        //Create worker cluster service
        val workerClusterName: String = s"workercluster${CommonUtils.randomString(RANDOM_LEN)}"
        val workerCollie: WorkerCollie = clusterCollie.workerCollie()
        val workerClientPort: Int = CommonUtils.availablePort()
        val workerClusterInfo: WorkerClusterInfo =
          createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)
        try {
          waitWorkerCluster(workerClusterName)
          workerClusterInfo.brokerClusterName shouldBe brokerClusterName
          workerClusterInfo.clientPort shouldBe workerClientPort
          workerClusterInfo.connectionProps shouldBe s"$firstNode:$workerClientPort"

          // Confirm pod name is a common format
          val wkPodName = Await.result(workerCollie.cluster(workerClusterName), TIMEOUT)._2.head.name
          val wkPodNameFieldSize = wkPodName.split("-").size
          val expectWKPodNameField = ContainerCollie.format("k8soccl", workerClusterName, "wk").split("-")
          wkPodNameFieldSize shouldBe expectWKPodNameField.size

        } finally result(workerCollie.remove(workerClusterName))
      } finally result(brokerCollie.remove(brokerClusterName))
    } finally result(zookeeperCollie.remove(zkClusterName))
  }

  @Test
  def testAddBrokerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames.last

    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))
    nodeCache.append(Node(secondNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtils.randomString(RANDOM_LEN)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    try {
      //Create broker cluster service
      val brokerClusterName: String = s"brokercluster${CommonUtils.randomString(RANDOM_LEN)}"
      val brokerClientPort = CommonUtils.availablePort()
      val brokerExporterPort = CommonUtils.availablePort()

      val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()

      val brokerClusterInfo1: BrokerClusterInfo =
        createBrokerCollie(brokerCollie,
                           brokerClusterName,
                           Seq(firstNode),
                           brokerClientPort,
                           brokerExporterPort,
                           zkClusterName)
      try {
        waitBrokerCluster(brokerClusterName)
        //Test add broker node
        val brokerClusterInfo2: BrokerClusterInfo = result(brokerCollie.addNode(brokerClusterName, secondNode))

        brokerClusterInfo1.connectionProps shouldBe s"$firstNode:$brokerClientPort"
        brokerClusterInfo2.connectionProps shouldBe s"$secondNode:$brokerClientPort,$firstNode:$brokerClientPort"

        brokerClusterInfo1.zookeeperClusterName shouldBe zkClusterName
        brokerClusterInfo2.zookeeperClusterName shouldBe zkClusterName
        brokerClusterInfo1.clientPort shouldBe brokerClientPort
        brokerClusterInfo2.clientPort shouldBe brokerClientPort

      } finally result(brokerCollie.remove(brokerClusterName))
    } finally result(zookeeperCollie.remove(zkClusterName))

  }

  @Test
  def testAddWorkerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames(1)

    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))
    nodeCache.append(Node(secondNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtils.randomString(RANDOM_LEN)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    try {
      //Create broker cluster service
      val brokerClusterName: String = s"brokercluster${CommonUtils.randomString(RANDOM_LEN)}"
      val brokerClientPort = CommonUtils.availablePort()
      val brokerExporterPort = CommonUtils.availablePort()
      val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()

      val brokerClusterInfo1: BrokerClusterInfo =
        createBrokerCollie(brokerCollie,
                           brokerClusterName,
                           Seq(firstNode),
                           brokerClientPort,
                           brokerExporterPort,
                           zkClusterName)

      try {
        waitBrokerCluster(brokerClusterName)
        //Create worker cluster service
        val workerClusterName: String = s"workercluster${CommonUtils.randomString(RANDOM_LEN)}"
        val workerCollie: WorkerCollie = clusterCollie.workerCollie()
        val workerClientPort: Int = CommonUtils.availablePort()
        val workerClusterInfo1: WorkerClusterInfo =
          createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)
        try {
          waitWorkerCluster(workerClusterName)
          //Test add worker node
          val workerClusterInfo2: WorkerClusterInfo = result(workerCollie.addNode(workerClusterName, secondNode))
          brokerClusterInfo1.connectionProps shouldBe s"$firstNode:$brokerClientPort"
          workerClusterInfo1.connectionProps shouldBe s"$firstNode:$workerClientPort"
          workerClusterInfo2.connectionProps.contains(s"$secondNode:$workerClientPort,$firstNode:$workerClientPort") shouldBe true
        } finally result(workerCollie.remove(workerClusterName))
      } finally result(brokerCollie.remove(brokerClusterName))
    } finally result(zookeeperCollie.remove(zkClusterName))

  }

  @Test
  def testRemoveBrokerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames(1)

    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))
    nodeCache.append(Node(secondNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtils.randomString(RANDOM_LEN)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()
    val brokerClusterName: String = s"brokercluster${CommonUtils.randomString(RANDOM_LEN)}"
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()
    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)
    try {
      //Create broker cluster service
      val brokerClientPort = CommonUtils.availablePort()
      val brokerExporterPort = CommonUtils.availablePort()
      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Seq(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)
      try {
        waitBrokerCluster(brokerClusterName)
        val firstContainerName: String = result(brokerCollie.cluster(brokerClusterName))._2.head.hostname

        result(brokerCollie.addNode(brokerClusterName, secondNode))
        // wait a period for k8s get latest pods information
        TimeUnit.SECONDS.sleep(10)
        result(brokerCollie.cluster(brokerClusterName))._2.size shouldBe 2
        result(brokerCollie.removeNode(brokerClusterName, firstNode))
        waitBrokerCluster(brokerClusterName)

        val k8sClient: K8SClient = K8SClient(API_SERVER_URL.get)
        await(() => !Await.result(k8sClient.containers, TIMEOUT).exists(c => c.hostname.contains(firstContainerName)))
        result(brokerCollie.cluster(brokerClusterName))._2.size shouldBe 1
      } finally result(brokerCollie.remove(brokerClusterName))
    } finally result(zookeeperCollie.remove(zkClusterName))
  }

  @Test
  def testRemoveWorkerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames(1)

    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))
    nodeCache.append(Node(secondNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtils.randomString(RANDOM_LEN)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()
    val workerCollie: WorkerCollie = clusterCollie.workerCollie()
    val workerClusterName: String = s"workercluster${CommonUtils.randomString(RANDOM_LEN)}"
    val brokerClusterName: String = s"brokercluster${CommonUtils.randomString(RANDOM_LEN)}"
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()
    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)
    try {
      //Create broker cluster service
      val brokerClientPort = CommonUtils.availablePort()
      val brokerExporterPort = CommonUtils.availablePort()
      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Seq(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)
      try {
        waitBrokerCluster(brokerClusterName)
        //Create worker cluster service
        val workerClientPort: Int = CommonUtils.availablePort()

        createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)
        try {
          waitWorkerCluster(workerClusterName)
          val firstContainerName: String = result(workerCollie.cluster(workerClusterName))._2.head.hostname

          result(workerCollie.addNode(workerClusterName, secondNode))
          // wait a period for k8s get latest pods information
          TimeUnit.SECONDS.sleep(10)
          result(workerCollie.cluster(workerClusterName))._2.size shouldBe 2
          result(workerCollie.removeNode(workerClusterName, firstNode))
          waitWorkerCluster(workerClusterName)

          val k8sClient: K8SClient = K8SClient(API_SERVER_URL.get)
          await(() => !Await.result(k8sClient.containers, TIMEOUT).exists(c => c.hostname.contains(firstContainerName)))
          result(workerCollie.cluster(workerClusterName))._2.size shouldBe 1
        } finally result(workerCollie.remove(workerClusterName))
      } finally result(brokerCollie.remove(brokerClusterName))
    } finally result(zookeeperCollie.remove(zkClusterName))
  }

  @Test
  def testClusters(): Unit = {
    val firstNode: String = nodeNames.head
    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName1: String = s"zk${CommonUtils.randomString(RANDOM_LEN)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    result(zookeeperCollie.clusters).flatMap(x => x._2).count(x => x.hostname.contains(zkClusterName1)) shouldBe 0

    try {
      val zkClientPort1: Int = CommonUtils.availablePort()
      val zkPeerPort1: Int = CommonUtils.availablePort()
      val zkElectionPort1: Int = CommonUtils.availablePort()

      createZookeeperCollie(zookeeperCollie, zkClusterName1, firstNode, zkClientPort1, zkPeerPort1, zkElectionPort1)
      result(zookeeperCollie.clusters).flatMap(x => x._2).count(x => x.hostname.contains(zkClusterName1)) shouldBe 1
    } finally result(zookeeperCollie.remove(zkClusterName1))
  }

  @Test
  def testLog(): Unit = {
    val firstNode: String = nodeNames.head
    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "fake", "fake", Seq.empty, CommonUtils.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtils.randomString(RANDOM_LEN)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val zkClientPort: Int = CommonUtils.availablePort()
    val zkPeerPort: Int = CommonUtils.availablePort()
    val zkElectionPort: Int = CommonUtils.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    try {
      //Create broker cluster service
      val brokerClusterName: String = s"brokercluster${CommonUtils.randomString(RANDOM_LEN)}"
      val brokerClientPort = CommonUtils.availablePort()
      val brokerExporterPort = CommonUtils.availablePort()
      val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()

      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Seq(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)

      try {
        waitBrokerCluster(brokerClusterName)
        //Create worker cluster service
        val workerClusterName: String = s"workercluster${CommonUtils.randomString(RANDOM_LEN)}"
        val workerCollie: WorkerCollie = clusterCollie.workerCollie()
        val workerClientPort: Int = CommonUtils.availablePort()
        createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)

        try {
          waitWorkerCluster(workerClusterName)
          val workerContainerHostName: String = result(workerCollie.cluster(workerClusterName))._2.head.hostname

          val k8sClient: K8SClient = K8SClient(API_SERVER_URL.get)
          await(
            () =>
              Await
                .result(k8sClient.containers, TIMEOUT)
                .count(c => c.hostname.contains(workerContainerHostName) && c.state == K8sContainerState.RUNNING.name) == 1
          )

          val logMessage: String = "Kafka Connect distributed worker initialization"
          await(() => result(workerCollie.logs(workerClusterName)).head._2.contains(logMessage))
          val workerlogs: Map[ContainerInfo, String] = result(workerCollie.logs(workerClusterName))
          workerlogs.size shouldBe 1

          workerlogs.head._2.contains(logMessage) shouldBe true
        } finally result(workerCollie.remove(workerClusterName))
      } finally result(brokerCollie.remove(brokerClusterName))
    } finally result(zookeeperCollie.remove(zkClusterName))
  }

  @Test
  def testVerifyNode(): Unit = {
    val firstNode: String = nodeNames.head
    val verifyNode: Try[String] =
      Await.result(clusterCollie.verifyNode(Node(firstNode, 22, "fake", "fake", Seq.empty, CommonUtils.current())),
                   30 seconds)
    verifyNode.get.contains("node is running.") shouldBe true

    val unknowNode: Try[String] = Await.result(
      clusterCollie.verifyNode(Node("unknow-node", 22, "fake", "fake", Seq.empty, CommonUtils.current())),
      30 seconds)
    unknowNode.isFailure shouldBe true
  }

  @Test
  def testMockK8sClientVerifyNode1(): Unit = {
    val fakeK8SClient = new FakeK8SClient(true, Option(K8SStatusInfo(true, "")))
    val clusterCollie: ClusterCollie =
      ClusterCollie.builderOfK8s().nodeCollie(nodeCollie).k8sClient(fakeK8SClient).build()
    val runningNode =
      Await.result(clusterCollie.verifyNode(Node("ohara", 22, "fake", "fake", Seq.empty, CommonUtils.current())),
                   30 seconds)
    runningNode match {
      case Success(value) => value shouldBe "ohara node is running."
      case Failure(e)     => throw new AssertionError()
    }
  }

  @Test
  def testMockK8sClientVerifyNode2(): Unit = {
    val fakeK8SClient = new FakeK8SClient(true, Option(K8SStatusInfo(false, "node failed.")))
    val clusterCollie: ClusterCollie =
      ClusterCollie.builderOfK8s().nodeCollie(nodeCollie).k8sClient(fakeK8SClient).build()
    val runningNode =
      Await.result(clusterCollie.verifyNode(Node("ohara", 22, "fake", "fake", Seq.empty, CommonUtils.current())),
                   30 seconds)
    runningNode match {
      case Success(value) => throw new AssertionError()
      case Failure(e)     => e.getMessage shouldBe "ohara node doesn't running container. cause: node failed."
    }
  }

  @Test
  def testMockK8sClientVerifyNode3(): Unit = {
    val fakeK8SClient = new FakeK8SClient(false, Option(K8SStatusInfo(false, "failed")))
    val clusterCollie: ClusterCollie =
      ClusterCollie.builderOfK8s().nodeCollie(nodeCollie).k8sClient(fakeK8SClient).build()
    val runningNode =
      Await.result(clusterCollie.verifyNode(Node("ohara", 22, "fake", "fake", Seq.empty, CommonUtils.current())),
                   30 seconds)
    runningNode match {
      case Success(value) => throw new AssertionError()
      case Failure(e)     => e.getMessage() shouldBe "ohara node doesn't running container. cause: failed"
    }
  }

  @Test
  def testMockK8SClientVerifyNode4(): Unit = {
    val fakeK8SClient = new FakeK8SClient(false, None)
    val clusterCollie: ClusterCollie =
      ClusterCollie.builderOfK8s().nodeCollie(nodeCollie).k8sClient(fakeK8SClient).build()
    val runningNode =
      Await.result(clusterCollie.verifyNode(Node("ohara", 22, "fake", "fake", Seq.empty, CommonUtils.current())),
                   30 seconds)
    runningNode match {
      case Success(value) => throw new AssertionError()
      case Failure(e) =>
        e.getMessage() shouldBe "ohara node doesn't running container. cause: ohara node doesn't exists."
    }
  }

  private[this] def createZookeeperCollie(zookeeperCollie: ZookeeperCollie,
                                          clusterName: String,
                                          nodeName: String,
                                          clientPort: Int,
                                          peerPort: Int,
                                          electionPort: Int): ZookeeperClusterInfo = {
    val info = result(
      zookeeperCollie
        .creator()
        .imageName(ZookeeperApi.IMAGE_NAME_DEFAULT)
        .clusterName(clusterName)
        .clientPort(clientPort)
        .peerPort(peerPort)
        .electionPort(electionPort)
        .nodeName(nodeName)
        .create())
    waitZookeeperCluster(clusterName)
    info
  }

  private[this] def createBrokerCollie(brokerCollie: BrokerCollie,
                                       cluseterName: String,
                                       nodeName: Seq[String],
                                       clientPort: Int,
                                       exporterPort: Int,
                                       zookeeperClusterName: String): BrokerClusterInfo = {
    result(
      brokerCollie
        .creator()
        .imageName(BrokerApi.IMAGE_NAME_DEFAULT)
        .clusterName(cluseterName)
        .clientPort(clientPort)
        .exporterPort(exporterPort)
        .zookeeperClusterName(zookeeperClusterName)
        .nodeNames(nodeName)
        .jmxPort(CommonUtils.availablePort())
        .create())
  }

  private[this] def createWorkerCollie(workerCollie: WorkerCollie,
                                       clusterName: String,
                                       nodeName: String,
                                       clientPort: Int,
                                       brokerClusterName: String): WorkerClusterInfo = {
    result(
      workerCollie
        .creator()
        .imageName(WorkerApi.IMAGE_NAME_DEFAULT)
        .clusterName(clusterName)
        .clientPort(clientPort)
        .brokerClusterName(brokerClusterName)
        .groupId(CommonUtils.randomString(RANDOM_LEN))
        .configTopicName(CommonUtils.randomString(RANDOM_LEN))
        .statusTopicName(CommonUtils.randomString(RANDOM_LEN))
        .offsetTopicName(CommonUtils.randomString(RANDOM_LEN))
        .jmxPort(CommonUtils.availablePort())
        .nodeName(nodeName)
        .create()
    )
  }

  @After
  final def tearDown(): Unit = Releasable.close(clusterCollie)
}
