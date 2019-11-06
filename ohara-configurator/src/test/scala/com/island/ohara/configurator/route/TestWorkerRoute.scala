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

package com.island.ohara.configurator.route

import com.island.ohara.client.configurator.v0.{BrokerApi, ConnectorApi, NodeApi, TopicApi, WorkerApi}
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.fake.FakeWorkerCollie
import org.junit.{After, Before, Test}
import org.scalatest.Matchers
import spray.json.{DeserializationException, JsArray, JsNumber, JsObject, JsString, JsTrue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestWorkerRoute extends OharaTest with Matchers {
  private[this] val numberOfCluster = 1
  private[this] val configurator = Configurator.builder.fake(numberOfCluster, 0).build()

  /**
    * a fake cluster has 3 fake node.
    */
  private[this] val numberOfDefaultNodes = 3 * numberOfCluster
  private[this] val workerApi = WorkerApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val brokerClusterKey =
    Await.result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list(), 10 seconds).head.key

  private[this] val nodeNames: Set[String] = Set("n0", "n1")

  private[this] def result[T](f: Future[T]): T = Await.result(f, Duration("20 seconds"))

  @Before
  def setup(): Unit = {
    val nodeAccess = NodeApi.access.hostname(configurator.hostname).port(configurator.port)

    nodeNames.isEmpty shouldBe false

    nodeNames.foreach { n =>
      result(nodeAccess.request.hostname(n).port(22).user("user").password("pwd").create())
    }

    result(nodeAccess.list()).size shouldBe (nodeNames.size + numberOfDefaultNodes)
  }

  @Test
  def repeatedlyDelete(): Unit = {
    (0 to 10).foreach { index =>
      result(workerApi.delete(ObjectKey.of(index.toString, index.toString)))
      result(workerApi.removeNode(ObjectKey.of(index.toString, index.toString), index.toString))
    }
  }

  @Test
  def testDefaultBk(): Unit =
    result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()).brokerClusterKey shouldBe brokerClusterKey

  @Test
  def createWithIncorrectBk(): Unit =
    an[IllegalArgumentException] should be thrownBy result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .nodeNames(nodeNames)
        .brokerClusterKey(ObjectKey.of("default", CommonUtils.randomString()))
        .create())

  @Test
  def testAllSetting(): Unit = {
    val name = CommonUtils.randomString(10)
    val clientPort = CommonUtils.availablePort()
    val jmxPort = CommonUtils.availablePort()
    val groupId = CommonUtils.randomString(10)
    val configTopicName = CommonUtils.randomString(10)
    val configTopicReplications: Short = 2
    val offsetTopicName = CommonUtils.randomString(10)
    val offsetTopicPartitions = 2
    val offsetTopicReplications: Short = 2
    val statusTopicName = CommonUtils.randomString(10)
    val statusTopicPartitions = 2
    val statusTopicReplications: Short = 2

    val wkCluster = result(
      workerApi.request
        .name(name)
        .clientPort(clientPort)
        .jmxPort(jmxPort)
        .groupId(groupId)
        .brokerClusterKey(brokerClusterKey)
        .configTopicName(configTopicName)
        .configTopicReplications(configTopicReplications)
        .offsetTopicName(offsetTopicName)
        .offsetTopicPartitions(offsetTopicPartitions)
        .offsetTopicReplications(offsetTopicReplications)
        .statusTopicName(statusTopicName)
        .statusTopicPartitions(statusTopicPartitions)
        .statusTopicReplications(statusTopicReplications)
        .nodeNames(nodeNames)
        .create())
    wkCluster.jmxPort shouldBe jmxPort
    wkCluster.clientPort shouldBe clientPort
    wkCluster.groupId shouldBe groupId
    wkCluster.configTopicName shouldBe configTopicName
    wkCluster.configTopicReplications shouldBe configTopicReplications
    wkCluster.offsetTopicName shouldBe offsetTopicName
    wkCluster.offsetTopicPartitions shouldBe offsetTopicPartitions
    wkCluster.offsetTopicReplications shouldBe offsetTopicReplications
    wkCluster.statusTopicName shouldBe statusTopicName
    wkCluster.statusTopicPartitions shouldBe statusTopicPartitions
    wkCluster.statusTopicReplications shouldBe statusTopicReplications
  }

  @Test
  def testCreateOnNonexistentNode(): Unit =
    an[IllegalArgumentException] should be thrownBy result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .nodeName(CommonUtils.randomString())
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )

  @Test
  def testImageName(): Unit = {
    result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
    ).imageName shouldBe WorkerApi.IMAGE_NAME_DEFAULT

    //  the available images in fake mode is only IMAGE_NAME_DEFAULT
    an[IllegalArgumentException] should be thrownBy result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .nodeNames(nodeNames)
        .imageName(CommonUtils.randomString(10))
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )
  }
  @Test
  def testList(): Unit = {
    val count = 5
    (0 until count).foreach { _ =>
      result(
        workerApi.request
          .name(CommonUtils.randomString(10))
          .nodeNames(nodeNames)
          .brokerClusterKey(brokerClusterKey)
          .create()
      )
    }
    result(workerApi.list()).size shouldBe count
  }

  @Test
  def testRemove(): Unit = {
    val cluster = result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
    )
    result(workerApi.list()).size shouldBe 1
    result(workerApi.delete(cluster.key))
    result(workerApi.list()).size shouldBe 0
  }

  @Test
  def testAddNode(): Unit = {
    val cluster = result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .nodeName(nodeNames.head)
        .brokerClusterKey(brokerClusterKey)
        .create()
    )
    result(workerApi.start(cluster.key))
    result(workerApi.addNode(cluster.key, nodeNames.last).flatMap(_ => workerApi.get(cluster.key))).nodeNames shouldBe cluster.nodeNames ++ Set(
      nodeNames.last)
  }

  @Test
  def testRemoveNode(): Unit = {
    val cluster = result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
    )
    result(workerApi.start(cluster.key))
    cluster.nodeNames shouldBe nodeNames
    result(workerApi.removeNode(cluster.key, nodeNames.last))
    result(workerApi.get(cluster.key)).nodeNames shouldBe nodeNames - nodeNames.last
  }

  @Test
  def testInvalidClusterName(): Unit = an[DeserializationException] should be thrownBy result(
    workerApi.request.name("123123.").nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create()
  )

  @Test
  def createWorkerClusterWithSameName(): Unit = {
    val name = CommonUtils.randomString(10)

    // pass
    result(
      workerApi.request.name(name).nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create()
    )

    // we don't need to create another bk cluster since it is feasible to create multi wk cluster on same broker cluster
    an[IllegalArgumentException] should be thrownBy result(
      workerApi.request.name(name).nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create()
    )
  }

  @Test
  def clientPortConflict(): Unit = {
    val clientPort = CommonUtils.availablePort()
    // pass
    result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .clientPort(clientPort)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )

    an[IllegalArgumentException] should be thrownBy result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .clientPort(clientPort)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )
  }

  @Test
  def jmxPortConflict(): Unit = {
    val jmxPort = CommonUtils.availablePort()
    // pass
    result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .jmxPort(jmxPort)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )

    an[IllegalArgumentException] should be thrownBy result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .jmxPort(jmxPort)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )
  }

  @Test
  def duplicateGroupId(): Unit = {
    val groupId = CommonUtils.randomString(10)
    // pass
    result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .groupId(groupId)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )

    an[IllegalArgumentException] should be thrownBy result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .groupId(groupId)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )
  }

  @Test
  def duplicateConfigTopic(): Unit = {
    val configTopicName = CommonUtils.randomString(10)
    // pass
    result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .configTopicName(configTopicName)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )

    an[IllegalArgumentException] should be thrownBy result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .configTopicName(configTopicName)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )
  }

  @Test
  def duplicateOffsetTopic(): Unit = {
    val offsetTopicName = CommonUtils.randomString(10)
    // pass
    result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .offsetTopicName(offsetTopicName)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )

    an[IllegalArgumentException] should be thrownBy result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .offsetTopicName(offsetTopicName)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )
  }

  @Test
  def duplicateStatusTopic(): Unit = {
    val statusTopicName = CommonUtils.randomString(10)
    // pass
    result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .statusTopicName(statusTopicName)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )

    an[IllegalArgumentException] should be thrownBy result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .statusTopicName(statusTopicName)
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
        .flatMap(wk => workerApi.start(wk.key))
    )
  }

  @Test
  def testForceDelete(): Unit = {
    val initialCount = configurator.serviceCollie.workerCollie.asInstanceOf[FakeWorkerCollie].forceRemoveCount

    // graceful delete
    val wk0 = result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create()
    )
    result(workerApi.start(wk0.key))
    result(workerApi.stop(wk0.key))
    result(workerApi.delete(wk0.key))
    configurator.serviceCollie.workerCollie.asInstanceOf[FakeWorkerCollie].forceRemoveCount shouldBe initialCount

    // force delete
    val wk1 = result(
      workerApi.request
        .name(CommonUtils.randomString(10))
        .nodeNames(nodeNames)
        .brokerClusterKey(brokerClusterKey)
        .create())
    result(workerApi.start(wk1.key))
    result(workerApi.forceStop(wk1.key))
    result(workerApi.delete(wk1.key))
    configurator.serviceCollie.workerCollie.asInstanceOf[FakeWorkerCollie].forceRemoveCount shouldBe initialCount + 1
  }

  @Test
  def testCustomTagsShouldExistAfterRunning(): Unit = {
    val tags = Map(
      "aa" -> JsString("bb"),
      "cc" -> JsNumber(123),
      "dd" -> JsArray(JsString("bar"), JsString("foo"))
    )
    val wk = result(workerApi.request.tags(tags).nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create())
    wk.tags shouldBe tags

    // after create, tags should exist
    val res = result(workerApi.get(wk.key))
    res.tags shouldBe tags

    // after start, tags should still exist
    result(workerApi.start(wk.key))
    val res1 = result(workerApi.get(wk.key))
    res1.tags shouldBe tags

    // after stop, tags should still exist
    result(workerApi.stop(wk.key))
    val res2 = result(workerApi.get(wk.key))
    res2.tags shouldBe tags
  }

  @Test
  def testGroup(): Unit = {
    val group = CommonUtils.randomString(10)
    // different name but same group
    result(workerApi.request.group(group).nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create()).group shouldBe group
    result(workerApi.request.group(group).nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create()).group shouldBe group

    result(workerApi.list()).size shouldBe 2

    // same name but different group
    val name = CommonUtils.randomString(10)
    val bk1 = result(workerApi.request.name(name).nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create())
    bk1.name shouldBe name
    bk1.group should not be group
    val bk2 = result(
      workerApi.request.name(name).group(group).nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create())
    bk2.name shouldBe name
    bk2.group shouldBe group

    result(workerApi.list()).size shouldBe 4
  }

  @Test
  def testNameFilter(): Unit = {
    val name = CommonUtils.randomString(10)
    val worker = result(workerApi.request.name(name).nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create())
    (0 until 3).foreach(_ => result(workerApi.request.nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create()))
    result(workerApi.list()).size shouldBe 4
    val workers = result(workerApi.query.name(name).execute())
    workers.size shouldBe 1
    workers.head.key shouldBe worker.key
  }

  @Test
  def testGroupFilter(): Unit = {
    val group = CommonUtils.randomString(10)
    val worker = result(workerApi.request.group(group).nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create())
    (0 until 3).foreach(_ => result(workerApi.request.nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create()))
    result(workerApi.list()).size shouldBe 4
    val workers = result(workerApi.query.group(group).execute())
    workers.size shouldBe 1
    workers.head.key shouldBe worker.key
  }

  @Test
  def testTagsFilter(): Unit = {
    val tags = Map(
      "a" -> JsString("b"),
      "b" -> JsNumber(123),
      "c" -> JsTrue,
      "d" -> JsArray(JsString("B")),
      "e" -> JsObject("a" -> JsNumber(123))
    )
    val worker = result(workerApi.request.tags(tags).nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create())
    (0 until 3).foreach(_ => result(workerApi.request.nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create()))
    result(workerApi.list()).size shouldBe 4
    val workers = result(workerApi.query.tags(tags).execute())
    workers.size shouldBe 1
    workers.head.key shouldBe worker.key
  }

  @Test
  def testStateFilter(): Unit = {
    val worker = result(workerApi.request.nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create())
    (0 until 3).foreach(_ => result(workerApi.request.nodeNames(nodeNames).brokerClusterKey(brokerClusterKey).create()))
    result(workerApi.list()).size shouldBe 4
    result(workerApi.start(worker.key))
    val workers = result(workerApi.query.state("running").execute())
    workers.size shouldBe 1
    workers.find(_.key == worker.key) should not be None

    result(workerApi.query.group(CommonUtils.randomString()).state("running").execute()).size shouldBe 0
    result(workerApi.query.state("none").execute()).size shouldBe 3
  }

  @Test
  def testAliveNodesFilter(): Unit = {
    val worker = result(workerApi.request.nodeName(nodeNames.head).brokerClusterKey(brokerClusterKey).create())
    (0 until 3).foreach(
      _ =>
        result(
          workerApi.request
            .nodeNames(nodeNames)
            .brokerClusterKey(brokerClusterKey)
            .create()
            .flatMap(z => workerApi.start(z.key))))
    result(workerApi.list()).size shouldBe 4
    result(workerApi.start(worker.key))
    val workers = result(workerApi.query.aliveNodes(Set(nodeNames.head)).execute())
    workers.size shouldBe 1
    workers.head.key shouldBe worker.key
    result(workerApi.query.aliveNodes(nodeNames).execute()).size shouldBe 3
  }

  @Test
  def removeWorkerClusterUsedByConnector(): Unit = {
    def connectorApi =
      ConnectorApi.access.hostname(configurator.hostname).port(configurator.port)
    def topicApi =
      TopicApi.access.hostname(configurator.hostname).port(configurator.port)

    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).brokerClusterKey(brokerClusterKey).create())
    result(topicApi.start(topic.key))

    val worker = result(workerApi.request.nodeName(nodeNames.head).brokerClusterKey(brokerClusterKey).create())
    val connector = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .topicKey(topic.key)
        .workerClusterKey(worker.key)
        .create())

    intercept[IllegalArgumentException] {
      result(workerApi.delete(worker.key))
    }.getMessage should include(connector.key.toString)

    result(workerApi.start(worker.key))
    result(connectorApi.start(connector.key))

    intercept[IllegalArgumentException] {
      result(workerApi.stop(worker.key))
    }.getMessage should include(connector.key.toString)
  }

  @Test
  def testInvalidNodeName(): Unit =
    Set(START_COMMAND, STOP_COMMAND, PAUSE_COMMAND, RESUME_COMMAND).foreach { nodeName =>
      intercept[DeserializationException] {
        result(workerApi.request.nodeName(nodeName).brokerClusterKey(brokerClusterKey).create())
      }.getMessage should include(nodeName)
    }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
