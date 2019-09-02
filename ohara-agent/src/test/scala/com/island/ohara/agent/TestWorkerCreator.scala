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

import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DeserializationException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestWorkerCreator extends SmallTest with Matchers {

  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  private[this] def wkCreator(): WorkerCollie.ClusterCreator = (executionContext,
                                                                clusterName,
                                                                imageName,
                                                                brokerClusterName,
                                                                clientPort,
                                                                jmxPort,
                                                                groupId: String,
                                                                offsetTopicName: String,
                                                                offsetTopicReplications,
                                                                offsetTopicPartitions,
                                                                statusTopicName,
                                                                statusTopicReplications,
                                                                statusTopicPartitions,
                                                                configTopicName,
                                                                configTopicReplications,
                                                                jarInfos,
                                                                settings,
                                                                nodeNames) => {
    // the inputs have been checked (NullPointerException). Hence, we throw another exception here.
    if (executionContext == null) throw new AssertionError()
    if (clusterName == null || clusterName.isEmpty) throw new AssertionError()
    if (imageName == null || imageName.isEmpty) throw new AssertionError()
    if (brokerClusterName == null || brokerClusterName.isEmpty) throw new AssertionError()
    if (clientPort <= 0) throw new AssertionError()
    if (jmxPort <= 0) throw new AssertionError()
    if (groupId == null || groupId.isEmpty) throw new AssertionError()
    if (offsetTopicName == null || offsetTopicName.isEmpty) throw new AssertionError()
    if (offsetTopicReplications <= 0) throw new AssertionError()
    if (offsetTopicPartitions <= 0) throw new AssertionError()
    if (statusTopicName == null || offsetTopicName.isEmpty) throw new AssertionError()
    if (statusTopicReplications <= 0) throw new AssertionError()
    if (statusTopicPartitions <= 0) throw new AssertionError()
    if (configTopicName == null || offsetTopicName.isEmpty) throw new AssertionError()
    if (configTopicReplications <= 0) throw new AssertionError()
    // it is ok to accept empty url
    if (jarInfos == null) throw new AssertionError()
    if (settings == null || settings.isEmpty) throw new AssertionError()
    if (nodeNames == null || nodeNames.isEmpty) throw new AssertionError()
    Future.successful(
      WorkerClusterInfo(
        settings = settings,
        connectors = Seq.empty,
        deadNodes = Set.empty,
        state = None,
        error = None,
        lastModified = 0
      ))
  }

  @Test
  def nullImage(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().imageName(null)
  }

  @Test
  def emptyImage(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().imageName("")
  }

  @Test
  def nullClusterName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().clusterName(null)
  }

  @Test
  def emptyClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().clusterName("")
  }

  @Test
  def negativeClientPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().clientPort(-1)
  }

  @Test
  def negativeJmxPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().jmxPort(-1)
  }

  @Test
  def nullBkClusterName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().brokerClusterName(null)
  }

  @Test
  def emptyBkClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().brokerClusterName("")
  }

  @Test
  def nullGroupId(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().groupId(null)
  }

  @Test
  def emptyGroupId(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().groupId("")
  }

  @Test
  def nullConfigTopicName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().configTopicName(null)
  }

  @Test
  def emptyConfigTopicName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().configTopicName("")
  }

  @Test
  def negativeConfigTopicReplications(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().configTopicReplications(-1)
  }

  @Test
  def nullStatusTopicName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().statusTopicName(null)
  }

  @Test
  def emptyStatusTopicName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().statusTopicName("")
  }
  @Test
  def negativeStatusTopicPartitions(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().statusTopicPartitions(-1)
  }
  @Test
  def negativeStatusTopicReplications(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().statusTopicReplications(-1)
  }

  @Test
  def nullOffsetTopicName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().offsetTopicName(null)
  }

  @Test
  def emptyOffsetTopicName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().offsetTopicName("")
  }
  @Test
  def negativeOffsetTopicPartitions(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().offsetTopicPartitions(-1)
  }
  @Test
  def negativeOffsetTopicReplications(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().offsetTopicReplications(-1)
  }

  @Test
  def nullNodes(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().nodeNames(null)
  }

  @Test
  def emptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().nodeNames(Set.empty)
  }

  @Test
  def testNameLength(): Unit = wkCreator()
    .imageName(CommonUtils.randomString(10))
    .clusterName(CommonUtils.randomString(10))
    .brokerClusterName("bk")
    .clientPort(CommonUtils.availablePort())
    .jmxPort(8084)
    .groupId(CommonUtils.randomString(10))
    .configTopicName(CommonUtils.randomString(10))
    .configTopicReplications(1)
    .statusTopicName(CommonUtils.randomString(10))
    .statusTopicPartitions(1)
    .statusTopicReplications(1)
    .offsetTopicName(CommonUtils.randomString(10))
    .offsetTopicPartitions(1)
    .offsetTopicReplications(1)
    .nodeName(CommonUtils.randomString())
    .create()

  @Test
  def testInvalidName(): Unit =
    an[DeserializationException] should be thrownBy wkCreator().clusterName(CommonUtils.randomString(40))

  @Test
  def testCopy(): Unit = {
    val nodeNames = Set(CommonUtils.randomString())
    val workerClusterInfo = WorkerClusterInfo(
      settings =
        WorkerApi.access.request.brokerClusterName(CommonUtils.randomString(5)).nodeNames(nodeNames).creation.settings,
      connectors = Seq.empty,
      deadNodes = Set.empty,
      state = None,
      error = None,
      lastModified = 0
    )
    Await.result(wkCreator().copy(workerClusterInfo).create(), 30 seconds) shouldBe workerClusterInfo
  }

  @Test
  def testWkCreator(): Unit = {
    val node1Name = "node1"
    val node1 = Node(
      hostname = node1Name,
      port = Some(22),
      user = Some("fake"),
      password = Some("fake"),
      services = Seq.empty,
      lastModified = CommonUtils.current(),
      validationReport = None,
      tags = Map.empty
    )
    val node2Name = "node2"
    val node2 = Node(
      hostname = node2Name,
      port = Some(22),
      user = Some("fake"),
      password = Some("fake"),
      services = Seq.empty,
      lastModified = CommonUtils.current(),
      validationReport = None,
      tags = Map.empty
    )

    val bkName = CommonUtils.randomString(5)
    val fakeWorkerCollie = new FakeWorkerCollie(
      Seq(node1, node2),
      Map(
        bkName -> Seq(
          ContainerInfo(
            "node1",
            "aaaa",
            "broker",
            "2019-05-28 00:00:00",
            "running",
            "unknown",
            "ohara-xxx-bk-0000",
            "unknown",
            Seq.empty,
            Map(BrokerCollie.CLIENT_PORT_KEY -> "9092"),
            "ohara-xxx-bk-0000"
          )
        ))
    )
    val workerClusterInfo: Future[WorkerClusterInfo] = fakeWorkerCollie.creator
      .imageName(WorkerApi.IMAGE_NAME_DEFAULT)
      .clusterName("wk1")
      .clientPort(8083)
      .jmxPort(8084)
      .brokerClusterName(bkName)
      .groupId(CommonUtils.randomString(10))
      .configTopicName(CommonUtils.randomString(10))
      .configTopicReplications(1)
      .statusTopicName(CommonUtils.randomString(10))
      .statusTopicPartitions(1)
      .statusTopicReplications(1)
      .offsetTopicName(CommonUtils.randomString(10))
      .offsetTopicPartitions(1)
      .offsetTopicReplications(1)
      .nodeName(node2Name)
      .create()

    val result: WorkerClusterInfo = Await.result(workerClusterInfo, TIMEOUT)
    result.brokerClusterName shouldBe bkName
    result.clientPort shouldBe 8083
    result.nodeNames.size shouldBe 1
    result.connectionProps shouldBe s"$node2Name:8083"
  }

  @Test
  def testExistWorkerNode(): Unit = {
    val node1Name = "node1" // node1 has running worker for fake
    val node1 = Node(
      hostname = node1Name,
      port = Some(22),
      user = Some("fake"),
      password = Some("fake"),
      services = Seq.empty,
      lastModified = CommonUtils.current(),
      validationReport = None,
      tags = Map.empty
    )

    val wkName = CommonUtils.randomString(5)
    val fakeWorkerCollie = new FakeWorkerCollie(Seq(node1), Map.empty, wkName)
    val workerClusterInfo: Future[WorkerClusterInfo] = fakeWorkerCollie.creator
      .imageName(WorkerApi.IMAGE_NAME_DEFAULT)
      .clusterName(wkName)
      .clientPort(8083)
      .jmxPort(8084)
      .brokerClusterName("bk1")
      .groupId(CommonUtils.randomString(10))
      .configTopicName(CommonUtils.randomString(10))
      .configTopicReplications(1)
      .statusTopicName(CommonUtils.randomString(10))
      .statusTopicPartitions(1)
      .statusTopicReplications(1)
      .offsetTopicName(CommonUtils.randomString(10))
      .offsetTopicPartitions(1)
      .offsetTopicReplications(1)
      .nodeName(node1Name)
      .create()

    an[IllegalArgumentException] shouldBe thrownBy {
      Await.result(workerClusterInfo, TIMEOUT)
    }
  }

  @Test
  def testBrokerClusterNotExists(): Unit = {
    val node1Name = "node1"
    val node1 = Node(
      hostname = node1Name,
      port = Some(22),
      user = Some("fake"),
      password = Some("fake"),
      services = Seq.empty,
      lastModified = CommonUtils.current(),
      validationReport = None,
      tags = Map.empty
    )
    val node2Name = "node2"
    val node2 = Node(
      hostname = node2Name,
      port = Some(22),
      user = Some("fake"),
      password = Some("fake"),
      services = Seq.empty,
      lastModified = CommonUtils.current(),
      validationReport = None,
      tags = Map.empty
    )

    val bkName = CommonUtils.randomString(5)
    val fakeWorkerCollie = new FakeWorkerCollie(
      Seq(node1, node2),
      Map(
        bkName -> Seq(
          ContainerInfo(
            "node1",
            "aaaa",
            "broker",
            "2019-05-28 00:00:00",
            "running",
            "unknown",
            "ohara-xxx-bk-0000",
            "unknown",
            Seq.empty,
            Map(BrokerCollie.CLIENT_PORT_KEY -> "9092"),
            "ohara-xxx-bk-0000"
          )
        ))
    )

    Await.result(
      fakeWorkerCollie.creator
        .imageName(WorkerApi.IMAGE_NAME_DEFAULT)
        .clusterName("wk1")
        .clientPort(8083)
        .jmxPort(8084)
        .brokerClusterName(bkName)
        .groupId(CommonUtils.randomString(10))
        .configTopicName(CommonUtils.randomString(10))
        .configTopicReplications(1)
        .statusTopicName(CommonUtils.randomString(10))
        .statusTopicPartitions(1)
        .statusTopicReplications(1)
        .offsetTopicName(CommonUtils.randomString(10))
        .offsetTopicPartitions(1)
        .offsetTopicReplications(1)
        .nodeName(node2Name)
        .create(),
      TIMEOUT
    )

    an[NoSuchClusterException] should be thrownBy Await.result(
      fakeWorkerCollie.creator
        .imageName(WorkerApi.IMAGE_NAME_DEFAULT)
        .clusterName("wk1")
        .clientPort(8083)
        .jmxPort(8084)
        .brokerClusterName(CommonUtils.randomString()) // bk2 not exists
        .groupId(CommonUtils.randomString(10))
        .configTopicName(CommonUtils.randomString(10))
        .configTopicReplications(1)
        .statusTopicName(CommonUtils.randomString(10))
        .statusTopicPartitions(1)
        .statusTopicReplications(1)
        .offsetTopicName(CommonUtils.randomString(10))
        .offsetTopicPartitions(1)
        .offsetTopicReplications(1)
        .nodeName(node2Name)
        .create(),
      TIMEOUT
    )
  }
}
