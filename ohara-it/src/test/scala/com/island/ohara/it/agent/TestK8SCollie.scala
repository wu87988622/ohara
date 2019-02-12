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

package com.island.ohara.it.agent

import scala.concurrent.duration._
import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, ContainerState}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.it.IntegrationTest
import com.typesafe.scalalogging.Logger
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}

class TestK8SCollie extends IntegrationTest with Matchers {
  private[this] val log = Logger(classOf[TestK8SCollie])
  private[this] val K8S_API_SERVER_URL_KEY: String = "ohara.it.k8s"
  private[this] val K8S_API_NODE_NAME_KEY: String = "ohara.it.k8s.nodename"

  private[this] val API_SERVER_URL: Option[String] = sys.env.get(K8S_API_SERVER_URL_KEY)
  private[this] val NODE_SERVER_NAME: Option[String] = sys.env.get(K8S_API_NODE_NAME_KEY)

  private[this] val nodeCache = new ArrayBuffer[Node]()
  private[this] val nodeCollie: NodeCollie = new NodeCollie {
    override def nodes(): Future[Seq[Node]] = Future.successful(nodeCache)

    override def node(name: String): Future[Node] = Future.successful(
      nodeCache.find(_.name == name).getOrElse(throw new NoSuchElementException(s"expected:$name actual:$nodeCache")))
  }

  private[this] var clusterCollie: ClusterCollie = _
  private[this] var nodeNames: Seq[String] = _

  @Before
  def testBefore(): Unit = {
    val message = s"The k8s is skip test, Please setting $K8S_API_SERVER_URL_KEY and $K8S_API_NODE_NAME_KEY properties"
    if (API_SERVER_URL.isEmpty || NODE_SERVER_NAME.isEmpty) {
      log.info(message)
      skipTest(message)
    }
    log.info(s"Test K8S API Server is: ${API_SERVER_URL.get}, NodeName is: ${NODE_SERVER_NAME.get}")

    implicit val k8sClient: K8SClient = K8SClient(API_SERVER_URL.get)
    clusterCollie = ClusterCollie.k8s(nodeCollie, k8sClient)
    nodeNames = NODE_SERVER_NAME.get.split(",").toSeq
  }

  @Test
  def testZookeeperCollie(): Unit = {
    val clusterName: String = s"cluster${CommonUtil.randomString(10)}"
    val firstNode: String = nodeNames.head
    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "", "", Seq.empty, CommonUtil.current()))

    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()

    val clientPort: Int = CommonUtil.availablePort()
    val peerPort: Int = CommonUtil.availablePort()
    val electionPort: Int = CommonUtil.availablePort()

    val zookeeperClusterInfo =
      createZookeeperCollie(zookeeperCollie, clusterName, firstNode, clientPort, peerPort, electionPort)

    try {
      intercept[UnsupportedOperationException] {
        result(zookeeperCollie.addNode(clusterName, firstNode))
      }.getMessage() shouldBe "zookeeper collie doesn't support to add node from a running cluster"

      zookeeperClusterInfo.name shouldBe clusterName
      zookeeperClusterInfo.nodeNames.size shouldBe 1
      zookeeperClusterInfo.imageName shouldBe ZookeeperCollie.IMAGE_NAME_DEFAULT
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
    nodeCache.append(Node(firstNode, 22, "", "", Seq.empty, CommonUtil.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtil.randomString(10)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val zkClientPort: Int = CommonUtil.availablePort()
    val zkPeerPort: Int = CommonUtil.availablePort()
    val zkElectionPort: Int = CommonUtil.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    //Create broker cluster service
    val brokerClusterName: String = s"brokercluster${CommonUtil.randomString(10)}"
    val brokerClientPort = CommonUtil.availablePort()
    val brokerExporterPort = CommonUtil.availablePort()

    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()
    val brokerClusterInfo: BrokerClusterInfo =
      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Seq(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)
    try {
      //Check broker info
      brokerClusterInfo.clientPort shouldBe brokerClientPort
      brokerClusterInfo.zookeeperClusterName shouldBe zkClusterName
      brokerClusterInfo.connectionProps shouldBe s"$firstNode:$brokerClientPort"
    } finally {
      //Remove zookeeper and broker container brokerCollie
      try {
        result(brokerCollie.remove(brokerClusterName))
      } finally {
        result(zookeeperCollie.remove(zkClusterName))
      }
    }
  }

  @Test
  def testWorkerCollie(): Unit = {
    val firstNode: String = nodeNames.head
    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "", "", Seq.empty, CommonUtil.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtil.randomString(10)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val zkClientPort: Int = CommonUtil.availablePort()
    val zkPeerPort: Int = CommonUtil.availablePort()
    val zkElectionPort: Int = CommonUtil.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    //Create broker cluster service
    val brokerClusterName: String = s"brokercluster${CommonUtil.randomString(10)}"
    val brokerClientPort = CommonUtil.availablePort()
    val brokerExporterPort = CommonUtil.availablePort()

    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()

    createBrokerCollie(brokerCollie,
                       brokerClusterName,
                       Seq(firstNode),
                       brokerClientPort,
                       brokerExporterPort,
                       zkClusterName)

    //Create worker cluster service
    val workerClusterName: String = s"workercluster${CommonUtil.randomString(10)}"
    val workerCollie: WorkerCollie = clusterCollie.workerCollie()
    val workerClientPort: Int = CommonUtil.availablePort()
    val workerClusterInfo: WorkerClusterInfo =
      createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)

    try {
      workerClusterInfo.brokerClusterName shouldBe brokerClusterName
      workerClusterInfo.clientPort shouldBe workerClientPort
      workerClusterInfo.connectionProps shouldBe s"$firstNode:$workerClientPort"
    } finally {
      //Remove worker, broker and zookeeper container
      try {
        result(workerCollie.remove(workerClusterName))
      } finally {
        try {
          result(brokerCollie.remove(brokerClusterName))
        } finally {
          result(zookeeperCollie.remove(zkClusterName))
        }
      }
    }

  }

  @Test
  def testAddBrokerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames(1)

    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "", "", Seq.empty, CommonUtil.current()))
    nodeCache.append(Node(secondNode, 22, "", "", Seq.empty, CommonUtil.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtil.randomString(10)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val zkClientPort: Int = CommonUtil.availablePort()
    val zkPeerPort: Int = CommonUtil.availablePort()
    val zkElectionPort: Int = CommonUtil.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    //Create broker cluster service
    val brokerClusterName: String = s"brokercluster${CommonUtil.randomString(10)}"
    val brokerClientPort = CommonUtil.availablePort()
    val brokerExporterPort = CommonUtil.availablePort()

    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()

    val brokerClusterInfo1: BrokerClusterInfo =
      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Seq(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)

    //Test add broker node
    val brokerClusterInfo2: BrokerClusterInfo =
      result(brokerCollie.addNode(brokerClusterName, secondNode))

    try {
      brokerClusterInfo1.connectionProps shouldBe s"$firstNode:$brokerClientPort"
      brokerClusterInfo2.connectionProps shouldBe s"$secondNode:$brokerClientPort,$firstNode:$brokerClientPort"

      brokerClusterInfo1.zookeeperClusterName shouldBe zkClusterName
      brokerClusterInfo2.zookeeperClusterName shouldBe zkClusterName
      brokerClusterInfo1.clientPort shouldBe brokerClientPort
      brokerClusterInfo2.clientPort shouldBe brokerClientPort

    } finally {
      try {
        result(brokerCollie.remove(brokerClusterName))
      } finally {
        result(zookeeperCollie.remove(zkClusterName))
      }
    }
  }

  @Test
  def testAddWorkerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames(1)

    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "", "", Seq.empty, CommonUtil.current()))
    nodeCache.append(Node(secondNode, 22, "", "", Seq.empty, CommonUtil.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtil.randomString(10)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val zkClientPort: Int = CommonUtil.availablePort()
    val zkPeerPort: Int = CommonUtil.availablePort()
    val zkElectionPort: Int = CommonUtil.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    //Create broker cluster service
    val brokerClusterName: String = s"brokercluster${CommonUtil.randomString(10)}"
    val brokerClientPort = CommonUtil.availablePort()
    val brokerExporterPort = CommonUtil.availablePort()

    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()

    val brokerClusterInfo1: BrokerClusterInfo =
      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Seq(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)

    //Create worker cluster service
    val workerClusterName: String = s"workercluster${CommonUtil.randomString(10)}"
    val workerCollie: WorkerCollie = clusterCollie.workerCollie()
    val workerClientPort: Int = CommonUtil.availablePort()
    val workerClusterInfo1: WorkerClusterInfo =
      createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)

    //Test add worker node
    val workerClusterInfo2: WorkerClusterInfo = result(workerCollie.addNode(workerClusterName, secondNode))

    try {
      brokerClusterInfo1.connectionProps shouldBe s"$firstNode:$brokerClientPort"
      workerClusterInfo1.connectionProps shouldBe s"$firstNode:$workerClientPort"
      workerClusterInfo2.connectionProps.contains(s"$secondNode:$workerClientPort,$firstNode:$workerClientPort") shouldBe true
    } finally {
      try {
        result(workerCollie.remove(workerClusterName))
      } finally {
        try {
          result(brokerCollie.remove(brokerClusterName))
        } finally {
          result(zookeeperCollie.remove(zkClusterName))
        }
      }
    }
  }

  @Test
  def testRemoveBrokerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames(1)

    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "", "", Seq.empty, CommonUtil.current()))
    nodeCache.append(Node(secondNode, 22, "", "", Seq.empty, CommonUtil.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtil.randomString(10)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()
    val brokerClusterName: String = s"brokercluster${CommonUtil.randomString(10)}"
    try {
      val zkClientPort: Int = CommonUtil.availablePort()
      val zkPeerPort: Int = CommonUtil.availablePort()
      val zkElectionPort: Int = CommonUtil.availablePort()

      createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

      //Create broker cluster service

      val brokerClientPort = CommonUtil.availablePort()
      val brokerExporterPort = CommonUtil.availablePort()

      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Seq(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)

      val firstContainerName: String = result(brokerCollie.cluster(brokerClusterName))._2.head.hostname

      result(brokerCollie.addNode(brokerClusterName, secondNode))
      result(brokerCollie.cluster(brokerClusterName))._2.size shouldBe 2
      result(brokerCollie.removeNode(brokerClusterName, firstNode))

      val k8sClient: K8SClient = K8SClient(API_SERVER_URL.get)
      var isContainerDelete: Boolean = false
      while (!isContainerDelete) {
        if (k8sClient.containers().filter(c => c.hostname.contains(firstContainerName)).size == 0) {
          isContainerDelete = true
        }
      }
      result(brokerCollie.cluster(brokerClusterName))._2.size shouldBe 1

    } finally {
      try {
        result(brokerCollie.remove(brokerClusterName))
      } finally {
        result(zookeeperCollie.remove(zkClusterName))
      }
    }
  }

  @Test
  def testRemoveWorkerNode(): Unit = {
    val firstNode: String = nodeNames.head
    val secondNode: String = nodeNames(1)

    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "", "", Seq.empty, CommonUtil.current()))
    nodeCache.append(Node(secondNode, 22, "", "", Seq.empty, CommonUtil.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtil.randomString(10)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()
    val workerCollie: WorkerCollie = clusterCollie.workerCollie()
    val workerClusterName: String = s"workercluster${CommonUtil.randomString(10)}"
    val brokerClusterName: String = s"brokercluster${CommonUtil.randomString(10)}"

    try {
      val zkClientPort: Int = CommonUtil.availablePort()
      val zkPeerPort: Int = CommonUtil.availablePort()
      val zkElectionPort: Int = CommonUtil.availablePort()

      createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

      //Create broker cluster service
      val brokerClientPort = CommonUtil.availablePort()
      val brokerExporterPort = CommonUtil.availablePort()

      createBrokerCollie(brokerCollie,
                         brokerClusterName,
                         Seq(firstNode),
                         brokerClientPort,
                         brokerExporterPort,
                         zkClusterName)

      //Create worker cluster service
      val workerClientPort: Int = CommonUtil.availablePort()

      createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)

      val firstContainerName: String = result(workerCollie.cluster(workerClusterName))._2.head.hostname

      result(workerCollie.addNode(workerClusterName, secondNode))
      result(workerCollie.cluster(workerClusterName))._2.size shouldBe 2
      result(workerCollie.removeNode(workerClusterName, firstNode))

      val k8sClient: K8SClient = K8SClient(API_SERVER_URL.get)
      var isContainerDelete: Boolean = false
      while (!isContainerDelete) {
        if (k8sClient.containers().filter(c => c.hostname.contains(firstContainerName)).size == 0) {
          isContainerDelete = true
        }
      }
      result(workerCollie.cluster(workerClusterName))._2.size shouldBe 1
    } finally {
      try {
        result(workerCollie.remove(workerClusterName))
      } finally {
        try {
          result(brokerCollie.remove(brokerClusterName))
        } finally {
          result(zookeeperCollie.remove(zkClusterName))
        }
      }
    }
  }

  @Test
  def testClusters(): Unit = {
    val firstNode: String = nodeNames.head
    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "", "", Seq.empty, CommonUtil.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName1: String = s"zk${CommonUtil.randomString(10)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    result(zookeeperCollie.clusters())
      .flatMap(x => x._2)
      .filter(x => x.hostname.contains(zkClusterName1))
      .size shouldBe 0

    try {
      val zkClientPort1: Int = CommonUtil.availablePort()
      val zkPeerPort1: Int = CommonUtil.availablePort()
      val zkElectionPort1: Int = CommonUtil.availablePort()

      createZookeeperCollie(zookeeperCollie, zkClusterName1, firstNode, zkClientPort1, zkPeerPort1, zkElectionPort1)
      result(zookeeperCollie.clusters())
        .flatMap(x => x._2)
        .filter(x => x.hostname.contains(zkClusterName1))
        .size shouldBe 1
    } finally {
      result(zookeeperCollie.remove(zkClusterName1))
    }
  }

  @Test
  def testLog(): Unit = {
    val firstNode: String = nodeNames.head
    nodeCache.clear()
    nodeCache.append(Node(firstNode, 22, "", "", Seq.empty, CommonUtil.current()))

    //Create zookeeper cluster for start broker service
    val zkClusterName: String = s"zkcluster${CommonUtil.randomString(10)}"
    val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeeperCollie()
    val zkClientPort: Int = CommonUtil.availablePort()
    val zkPeerPort: Int = CommonUtil.availablePort()
    val zkElectionPort: Int = CommonUtil.availablePort()

    createZookeeperCollie(zookeeperCollie, zkClusterName, firstNode, zkClientPort, zkPeerPort, zkElectionPort)

    //Create broker cluster service
    val brokerClusterName: String = s"brokercluster${CommonUtil.randomString(10)}"
    val brokerClientPort = CommonUtil.availablePort()
    val brokerExporterPort = CommonUtil.availablePort()

    val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()

    createBrokerCollie(brokerCollie,
                       brokerClusterName,
                       Seq(firstNode),
                       brokerClientPort,
                       brokerExporterPort,
                       zkClusterName)

    //Create worker cluster service
    val workerClusterName: String = s"workercluster${CommonUtil.randomString(10)}"
    val workerCollie: WorkerCollie = clusterCollie.workerCollie()
    val workerClientPort: Int = CommonUtil.availablePort()

    try {
      createWorkerCollie(workerCollie, workerClusterName, firstNode, workerClientPort, brokerClusterName)

      val workerContainerHostName: String = result(workerCollie.cluster(workerClusterName))._2.head.hostname

      //Wait worker container started
      var isContainerRunning: Boolean = false
      val k8sClient: K8SClient = K8SClient(API_SERVER_URL.get)
      while (!isContainerRunning) {
        if (k8sClient
              .containers()
              .filter(c => c.hostname.contains(workerContainerHostName) && c.state == ContainerState.RUNNING)
              .size == 1) {
          isContainerRunning = true
        }
      }

      var haveLogMessage: Boolean = false
      val logMessage: String = "Kafka Connect distributed worker initializing ..."
      while (!haveLogMessage) {
        if (result(workerCollie.logs(workerClusterName)).head._2.contains(logMessage)) {
          haveLogMessage = true
        }
      }
      val workerlogs: Map[ContainerInfo, String] = result(workerCollie.logs(workerClusterName))
      workerlogs.size shouldBe 1

      workerlogs.head._2.contains(logMessage) shouldBe true
    } finally {
      //Remove worker, broker and zookeeper container
      try {
        result(workerCollie.remove(workerClusterName))
      } finally {
        try {
          result(brokerCollie.remove(brokerClusterName))
        } finally {
          result(zookeeperCollie.remove(zkClusterName))
        }
      }
    }
  }

  private[this] def createZookeeperCollie(zookeeperCollie: ZookeeperCollie,
                                          clusterName: String,
                                          nodeName: String,
                                          clientPort: Int,
                                          peerPort: Int,
                                          electionPort: Int): ZookeeperClusterInfo = {
    result(
      zookeeperCollie
        .creator()
        .clusterName(clusterName)
        .clientPort(clientPort)
        .peerPort(peerPort)
        .electionPort(electionPort)
        .nodeName(nodeName)
        .create())
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
        .clusterName(cluseterName)
        .clientPort(clientPort)
        .exporterPort(exporterPort)
        .zookeeperClusterName(zookeeperClusterName)
        .nodeNames(nodeName)
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
        .clusterName(clusterName)
        .clientPort(clientPort)
        .brokerClusterName(brokerClusterName)
        .nodeName(nodeName)
        .create()
    )
  }

  private[this] def result[T](f: Future[T]): T = Await.result(f, 60 seconds)
}
