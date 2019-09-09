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

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.{BrokerApi, TopicApi, ZookeeperApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DeserializationException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestBrokerCreator extends SmallTest with Matchers {
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  private[this] def node(hostname: String): Node = Node(
    hostname = hostname,
    port = Some(22),
    user = Some("user"),
    password = Some("password"),
    services = Seq.empty,
    lastModified = CommonUtils.current(),
    validationReport = None,
    tags = Map.empty
  )
  private[this] def bkCreator(): BrokerCollie.ClusterCreator =
    (executionContext, creation) => {
      // the inputs have been checked (NullPointerException). Hence, we throw another exception here.
      if (executionContext == null) throw new AssertionError()
      Future.successful(
        BrokerClusterInfo(
          settings = BrokerApi.access.request.settings(creation.settings).creation.settings,
          deadNodes = Set.empty,
          state = None,
          error = None,
          lastModified = 0,
          topicSettingDefinitions = TopicApi.TOPIC_DEFINITIONS
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
    .nodeName(CommonUtils.randomString)
    .create()

  @Test
  def testInvalidName(): Unit =
    an[DeserializationException] should be thrownBy bkCreator()
      .clusterName(CommonUtils.randomString(40))
      .imageName(CommonUtils.randomString(10))
      .nodeName(CommonUtils.randomString)
      .create()

  @Test
  def testCopy(): Unit = {
    val nodeNames = Set(CommonUtils.randomString())
    val brokerClusterInfo = BrokerClusterInfo(
      settings = BrokerApi.access.request
        .name(CommonUtils.randomString(10))
        .imageName(CommonUtils.randomString)
        .zookeeperClusterName(CommonUtils.randomString)
        .nodeNames(nodeNames)
        .creation
        .settings,
      deadNodes = Set.empty,
      state = None,
      error = None,
      lastModified = 0,
      topicSettingDefinitions = TopicApi.TOPIC_DEFINITIONS
    )
    Await.result(bkCreator().copy(brokerClusterInfo).create(), 30 seconds) shouldBe brokerClusterInfo
  }

  @Test
  def testBkCreatorZKNotExists(): Unit = {
    val node1Name = "node1"
    val node1 = node(node1Name)
    val brokerCollie = new FakeBrokerCollie(Seq(node1), Seq.empty, Seq.empty)

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
    val node1 = node(node1Name)

    val brokerCollie = new FakeBrokerCollie(Seq(node1), Seq.empty, Seq.empty) //Zk container set empty
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
    val node1 = node(node1Name)
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
        Map(ZookeeperApi.CLIENT_PORT_KEY -> "2181"),
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
        Map(BrokerApi.CLIENT_PORT_KEY -> "9092",
            BrokerApi.ID_KEY -> "1",
            BrokerApi.ZOOKEEPER_CLUSTER_NAME_KEY -> FakeBrokerCollie.zookeeperClusterName),
        "host"
      ))

    val brokerCollie = new FakeBrokerCollie(Seq(node1), zkContainers, bkContainers)

    Await
      .result(
        brokerCollie.creator
          .clusterName("bk1")
          .imageName(BrokerApi.IMAGE_NAME_DEFAULT)
          .zookeeperClusterName(FakeBrokerCollie.zookeeperClusterName)
          .clientPort(9092)
          .exporterPort(9093)
          .jmxPort(9094)
          .nodeName(node1Name) //node1 is running on a bk1
          .create(),
        TIMEOUT
      )
      .nodeNames shouldBe Set(node1Name)
  }

  @Test
  def testCheckBrokerImageValue(): Unit = {
    val node1Name = "node1"
    val node1 = node(node1Name)

    val node2Name = "node2"
    val node2 = node(node2Name)

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
        Map(ZookeeperApi.CLIENT_PORT_KEY -> "2181"),
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
        Map(BrokerApi.ID_KEY -> "0",
            BrokerApi.CLIENT_PORT_KEY -> "9092",
            BrokerApi.ZOOKEEPER_CLUSTER_NAME_KEY -> FakeBrokerCollie.zookeeperClusterName),
        "host"
      ))

    val brokerCollie = new FakeBrokerCollie(Seq(node1, node2), zkContainers, bkContainers)

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
    val node1 = node(node1Name)
    val node2Name = "node2"
    val node2 = node(node2Name)

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
        Map(ZookeeperApi.CLIENT_PORT_KEY -> "2181"),
        "host"
      ))
    val brokerCollie = new FakeBrokerCollie(Seq(node1, node2), containers, Seq.empty)

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
