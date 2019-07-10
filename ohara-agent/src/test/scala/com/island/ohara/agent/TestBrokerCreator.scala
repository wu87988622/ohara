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

package com.island.ohara.agent

import com.island.ohara.client.configurator.v0.{BrokerApi, WorkerApi}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DeserializationException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestBrokerCreator extends SmallTest with Matchers {
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  private[this] def bkCreator(): BrokerCollie.ClusterCreator =
    (executionContext, clusterName, imageName, zookeeperClusterName, clientPort, exporterPort, jmxPort, nodeNames) => {
      // the inputs have been checked (NullPointerException). Hence, we throw another exception here.
      if (executionContext == null) throw new AssertionError()
      if (clusterName == null || clusterName.isEmpty) throw new AssertionError()
      if (imageName == null || imageName.isEmpty) throw new AssertionError()
      if (clientPort <= 0) throw new AssertionError()
      if (exporterPort <= 0) throw new AssertionError()
      if (jmxPort <= 0) throw new AssertionError()
      if (zookeeperClusterName == null || zookeeperClusterName.isEmpty) throw new AssertionError()
      if (nodeNames == null || nodeNames.isEmpty) throw new AssertionError()
      Future.successful(
        BrokerClusterInfo(
          name = clusterName,
          imageName = imageName,
          zookeeperClusterName = zookeeperClusterName,
          clientPort = clientPort,
          exporterPort = exporterPort,
          jmxPort = jmxPort,
          nodeNames = nodeNames,
          deadNodes = Set.empty
        ))
    }

  @Test
  def nullImage(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().imageName(null)
  }

  @Test
  def emptyImage(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().imageName("")
  }

  @Test
  def nullClusterName(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().clusterName(null)
  }

  @Test
  def emptyClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().clusterName("")
  }

  @Test
  def nullZkClusterName(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().zookeeperClusterName(null)
  }

  @Test
  def emptyZkClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().zookeeperClusterName("")
  }

  @Test
  def negativeClientPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().clientPort(-1)
  }

  @Test
  def negativeExporterPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().exporterPort(-1)
  }

  @Test
  def negativeJmxPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().jmxPort(-1)
  }

  @Test
  def nullNodes(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().nodeNames(null)
  }

  @Test
  def emptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().nodeNames(Set.empty)
  }

  @Test
  def testNameLength(): Unit = bkCreator()
    .imageName(CommonUtils.randomString(10))
    .clusterName(CommonUtils.randomString(10))
    .zookeeperClusterName("zk")
    .exporterPort(CommonUtils.availablePort())
    .clientPort(CommonUtils.availablePort())
    .nodeNames(Set("abc"))
    .create()

  @Test
  def testInvalidName(): Unit = an[DeserializationException] should be thrownBy bkCreator().clusterName(
    CommonUtils.randomString(WorkerApi.LIMIT_OF_NAME_LENGTH + 1))

  @Test
  def testCopy(): Unit = {
    val brokerClusterInfo = BrokerClusterInfo(
      name = CommonUtils.randomString(10),
      imageName = CommonUtils.randomString(),
      zookeeperClusterName = CommonUtils.randomString(),
      exporterPort = 10,
      clientPort = 10,
      jmxPort = 10,
      nodeNames = Set(CommonUtils.randomString()),
      deadNodes = Set.empty
    )
    Await.result(bkCreator().copy(brokerClusterInfo).create(), 30 seconds) shouldBe brokerClusterInfo
  }

  @Test
  def testBkCreatorZKNotExists(): Unit = {
    val node1Name = "node1"
    val node1 =
      Node(node1Name, 22, "user1", "123456", services = Seq.empty, lastModified = CommonUtils.current(), Set.empty)
    val brokerCollie = new FakeBrokerCollie(NodeCollie(Seq(node1)), Seq.empty, Seq.empty)

    val bkCreator: Future[BrokerClusterInfo] = brokerCollie.creator
      .clusterName("cluster123")
      .imageName(BrokerApi.IMAGE_NAME_DEFAULT)
      .zookeeperClusterName("zk123456")
      .clientPort(9092)
      .exporterPort(9093)
      .jmxPort(9094)
      .nodeName(node1Name)
      .create()

    an[NoSuchClusterException] should be thrownBy {
      Await.result(bkCreator, TIMEOUT)
    }
  }

  @Test
  def testBkCreatorZKContainerEmpty(): Unit = {
    val node1Name = "node1"
    val node1 =
      Node(node1Name, 22, "user1", "123456", services = Seq.empty, lastModified = CommonUtils.current(), Set.empty)

    val brokerCollie = new FakeBrokerCollie(NodeCollie(Seq(node1)), Seq.empty, Seq.empty) //Zk container set empty
    val bkCreator: Future[BrokerClusterInfo] = brokerCollie.creator
      .clusterName("cluster123")
      .imageName(BrokerApi.IMAGE_NAME_DEFAULT)
      .zookeeperClusterName(FakeBrokerCollie.zookeeperClusterName)
      .clientPort(9092)
      .exporterPort(9093)
      .jmxPort(9094)
      .nodeName(node1Name)
      .create()

    an[IllegalArgumentException] should be thrownBy {
      Await.result(bkCreator, TIMEOUT)
    }
  }

  @Test
  def testNodeIsRunningBroker(): Unit = {
    val node1Name = "node1"
    val node1 =
      Node(node1Name, 22, "user1", "123456", services = Seq.empty, lastModified = CommonUtils.current(), Set.empty)
    val zkContainers = Seq(
      ContainerInfo(
        "node1",
        "00000",
        "zookeeper",
        "2018-05-24 00:00:00",
        "RUNNING",
        "unknown",
        "containername",
        "",
        Seq.empty,
        Map(ZookeeperCollie.CLIENT_PORT_KEY -> "2181"),
        "host"
      ))

    val bkContainers = Seq(
      ContainerInfo(
        "node1",
        "00000",
        BrokerApi.IMAGE_NAME_DEFAULT,
        "2018-05-24 00:00:00",
        "RUNNING",
        "unknown",
        "containername",
        "",
        Seq.empty,
        Map(BrokerCollie.CLIENT_PORT_KEY -> "9092",
            BrokerCollie.ZOOKEEPER_CLUSTER_NAME -> FakeBrokerCollie.zookeeperClusterName),
        "host"
      ))

    val brokerCollie = new FakeBrokerCollie(NodeCollie(Seq(node1)), zkContainers, bkContainers)
    val bkCreator: Future[BrokerClusterInfo] = brokerCollie.creator
      .clusterName("bk1")
      .imageName(BrokerApi.IMAGE_NAME_DEFAULT)
      .zookeeperClusterName(FakeBrokerCollie.zookeeperClusterName)
      .clientPort(9092)
      .exporterPort(9093)
      .jmxPort(9094)
      .nodeName(node1Name) //node1 is running on a bk1
      .create()

    an[IllegalArgumentException] shouldBe thrownBy {
      Await.result(bkCreator, TIMEOUT)
    }
  }

  @Test
  def testCheckBrokerImageValue(): Unit = {
    val node1Name = "node1"
    val node1 =
      Node(node1Name, 22, "user1", "123456", services = Seq.empty, lastModified = CommonUtils.current(), Set.empty)

    val node2Name = "node2"
    val node2 =
      Node(node2Name, 22, "user1", "123456", services = Seq.empty, lastModified = CommonUtils.current(), Set.empty)

    val zkContainers = Seq(
      ContainerInfo(
        "node1",
        "00000",
        "zookeeper",
        "2018-05-24 00:00:00",
        "RUNNING",
        "unknown",
        "containername",
        "",
        Seq.empty,
        Map(ZookeeperCollie.CLIENT_PORT_KEY -> "2181"),
        "host"
      ))

    val bkContainers = Seq(
      ContainerInfo(
        "node1",
        "00000",
        BrokerApi.IMAGE_NAME_DEFAULT,
        "2018-05-24 00:00:00",
        "RUNNING",
        "unknown",
        "containername",
        "",
        Seq.empty,
        Map(BrokerCollie.ID_KEY -> "0",
            BrokerCollie.CLIENT_PORT_KEY -> "9092",
            BrokerCollie.ZOOKEEPER_CLUSTER_NAME -> FakeBrokerCollie.zookeeperClusterName),
        "host"
      ))

    val brokerCollie = new FakeBrokerCollie(NodeCollie(Seq(node1, node2)), zkContainers, bkContainers)

    val bkCreator: Future[BrokerClusterInfo] = brokerCollie.creator
      .clusterName("bk1")
      .zookeeperClusterName(FakeBrokerCollie.zookeeperClusterName)
      .clientPort(9092)
      .jmxPort(9093)
      .exporterPort(9094)
      .imageName("brokerimage") //Docker image setting error
      .nodeName(node2Name)
      .create()

    an[IllegalArgumentException] shouldBe thrownBy {
      Await.result(bkCreator, TIMEOUT)
    }
  }

  @Test
  def testBkCreator(): Unit = {
    val node1Name = "node1"
    val node1 =
      Node(node1Name, 22, "user1", "123456", services = Seq.empty, lastModified = CommonUtils.current(), Set.empty)
    val node2Name = "node2"
    val node2 =
      Node(node2Name, 22, "user1", "123456", services = Seq.empty, lastModified = CommonUtils.current(), Set.empty)

    val containers = Seq(
      ContainerInfo(
        "node1",
        "00000",
        "zookeeper",
        "2018-05-24 00:00:00",
        "RUNNING",
        "unknown",
        "containername",
        "",
        Seq.empty,
        Map(ZookeeperCollie.CLIENT_PORT_KEY -> "2181"),
        "host"
      ))
    val brokerCollie = new FakeBrokerCollie(NodeCollie(Seq(node1, node2)), containers, Seq.empty)

    val bkCreator: Future[BrokerClusterInfo] = brokerCollie.creator
      .clusterName("cluster123")
      .imageName(BrokerApi.IMAGE_NAME_DEFAULT)
      .zookeeperClusterName(FakeBrokerCollie.zookeeperClusterName)
      .clientPort(9092)
      .exporterPort(9093)
      .jmxPort(9094)
      .nodeNames(Set(node1Name, node2Name))
      .create()

    val result: BrokerClusterInfo = Await.result(bkCreator, TIMEOUT)
    result.zookeeperClusterName shouldBe FakeBrokerCollie.zookeeperClusterName
    result.nodeNames.size shouldBe 2
    result.clientPort shouldBe 9092
    result.exporterPort shouldBe 9093
    result.jmxPort shouldBe 9094
    result.connectionProps shouldBe "node1:9092,node2:9092"
    result.ports.size shouldBe 3
  }

}
