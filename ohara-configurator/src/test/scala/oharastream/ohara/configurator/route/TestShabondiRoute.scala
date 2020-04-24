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

package oharastream.ohara.configurator.route

import java.time.{Duration => JDuration}

import oharastream.ohara.client.configurator.v0.{BrokerApi, ClusterState, NodeApi, ShabondiApi, TopicApi}
import oharastream.ohara.common.rule.OharaTest
import oharastream.ohara.common.setting.{ObjectKey, TopicKey}
import oharastream.ohara.common.util.{CommonUtils, Releasable}
import oharastream.ohara.configurator.Configurator
import oharastream.ohara.shabondi.ShabondiDefinitions._
import oharastream.ohara.shabondi.ShabondiType
import org.junit.{After, Before, Test}
import org.scalatest.Matchers
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestShabondiRoute extends OharaTest with Matchers {
  private[this] var configurator: Configurator                     = _
  private[this] var nodeApi: NodeApi.Access                        = _
  private[this] var topicApi: TopicApi.Access                      = _
  private[this] var brokerClusterInfo: BrokerApi.BrokerClusterInfo = _
  private[this] var shabondiApi: ShabondiApi.Access                = _
  private[this] var availableNodeNames: Seq[String]                = _

  private[this] val topicKey  = TopicKey.of("g", CommonUtils.randomString(10))
  private[this] val objectKey = ObjectKey.of("group", "name")

  private[this] def await[T](f: Future[T]): T = Await.result(f, 20 seconds)

  private[this] def awaitTrue(f: () => Boolean, swallowException: Boolean = false): Unit =
    CommonUtils.await(
      () =>
        try f()
        catch {
          case _: Throwable if swallowException =>
            false
        },
      JDuration.ofSeconds(20)
    )

  @Before
  def setup(): Unit = {
    configurator = Configurator.builder.fake().build()
    nodeApi = NodeApi.access.hostname(configurator.hostname).port(configurator.port)
    topicApi = TopicApi.access.hostname(configurator.hostname).port(configurator.port)
    brokerClusterInfo = await(
      BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()
    ).head

    availableNodeNames = await(nodeApi.list()).map(_.hostname)

    shabondiApi = ShabondiApi.access.hostname(configurator.hostname).port(configurator.port)

    await(topicApi.request.brokerClusterKey(brokerClusterInfo.key).key(topicKey).create())
    await(topicApi.start(topicKey))
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)

  @Test
  def testShouldThrowExceptionWhenCreateOnNonExistentNode(): Unit = {
    val objectKey              = ObjectKey.of("group", "name")
    val (clientPort, nodeName) = (CommonUtils.availablePort(), "non-existent-node")
    val clusterInfo: Future[ShabondiApi.ShabondiClusterInfo] =
      shabondiApi.request
        .group(objectKey.group)
        .name(objectKey.name)
        .shabondiClass(ShabondiType.Source.className)
        .clientPort(clientPort)
        .brokerClusterKey(brokerClusterInfo.key)
        .nodeName(nodeName)
        .create()

    an[IllegalArgumentException] should be thrownBy await(clusterInfo)
  }

  @Test
  def testShouldThrowExceptionWithInvalidClassName(): Unit = {
    val objectKey              = ObjectKey.of("group", "name")
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    val clusterInfo: Future[ShabondiApi.ShabondiClusterInfo] =
      shabondiApi.request
        .group(objectKey.group)
        .name(objectKey.name)
        .shabondiClass("oharastream.ohara.shabondi.Source")
        .clientPort(clientPort)
        .brokerClusterKey(brokerClusterInfo.key)
        .nodeName(nodeName)
        .create()

    an[IllegalArgumentException] should be thrownBy await(clusterInfo)
  }
  @Test
  def testSourceCreate(): Unit = {
    val objectKey                                    = ObjectKey.of("group", "name")
    val (clientPort, nodeName)                       = (CommonUtils.availablePort(), availableNodeNames(0))
    val clusterInfo: ShabondiApi.ShabondiClusterInfo = createShabondiSource(objectKey, clientPort, Set(nodeName))

    clusterInfo.group should ===(objectKey.group)
    clusterInfo.name should ===(objectKey.name)
    clusterInfo.shabondiClass should ===(ShabondiType.Source.className)
    clusterInfo.clientPort should ===(clientPort)
    clusterInfo.brokerClusterKey should ===(brokerClusterInfo.key)
    clusterInfo.nodeNames should contain(nodeName)
  }

  @Test
  def testShabondiSourceUpdate(): Unit = {
    val objectKey              = ObjectKey.of("group", "name")
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    val clusterInfo            = createShabondiSource(objectKey, clientPort, Set(nodeName))

    clusterInfo.group should ===(objectKey.group)
    clusterInfo.name should ===(objectKey.name)
    clusterInfo.shabondiClass should ===(ShabondiType.Source.className)
    clusterInfo.clientPort should ===(clientPort)
    clusterInfo.brokerClusterKey should ===(brokerClusterInfo.key)
    clusterInfo.nodeNames should contain(nodeName)
    clusterInfo.sourceToTopics should be(empty)
    clusterInfo.imageName should ===(IMAGE_NAME_DEFAULT)

    val newClientPort = CommonUtils.availablePort()
    val updatedClusterInfo = await(
      shabondiApi.request
        .group(objectKey.group)
        .name(objectKey.name)
        .clientPort(newClientPort)
        .sourceToTopics(Set(topicKey))
        .imageName("ohara/shabondi")
        .settings(
          Map(
            SINK_POLL_TIMEOUT_DEFINITION.key -> JsString(JDuration.ofSeconds(10).toString),
            SINK_GROUP_IDLETIME.key          -> JsString(JDuration.ofMinutes(30).toString)
          )
        )
        .update()
    )
    updatedClusterInfo.clientPort should ===(newClientPort)
    updatedClusterInfo.sourceToTopics should ===(Set(topicKey))
    updatedClusterInfo.imageName should ===("ohara/shabondi")
    updatedClusterInfo.settings should contain(SINK_POLL_TIMEOUT_DEFINITION.key -> JsString("PT10S"))
    updatedClusterInfo.settings should contain(SINK_GROUP_IDLETIME.key          -> JsString("PT30M"))
  }

  @Test
  def testSourceStart(): Unit = {
    val objectKey              = ObjectKey.of("group", "name")
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createShabondiSource(objectKey, clientPort, Set(nodeName))

    await(
      shabondiApi.request
        .group(objectKey.group)
        .name(objectKey.name)
        .sourceToTopics(Set(topicKey))
        .update()
    )

    await(shabondiApi.start(objectKey))

    awaitTrue(() => {
      val shabondiList = await(shabondiApi.list())
      shabondiList.size shouldBe 1
      shabondiList.head.sourceToTopics shouldBe Set(topicKey)
      shabondiList.head.state.get shouldBe ClusterState.RUNNING
      shabondiList.head.aliveNodes.head shouldBe nodeName
      shabondiList.head.meters.nonEmpty
    })
  }

  @Test
  def testSourceStartAndStop(): Unit = {
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createShabondiSource(objectKey, clientPort, Set(nodeName), Set(topicKey))

    await(shabondiApi.start(objectKey))
    awaitTrue(() => {
      val shabondiList = await(shabondiApi.list())
      shabondiList.size shouldBe 1
      shabondiList.head.sourceToTopics shouldBe Set(topicKey)
      shabondiList.head.state.get shouldBe ClusterState.RUNNING
      shabondiList.head.aliveNodes.head shouldBe nodeName
      shabondiList.head.meters.nonEmpty
    })

    await(shabondiApi.stop(objectKey))

    awaitTrue(() => {
      val shabondiList1 = await(shabondiApi.list())
      shabondiList1.size should ===(1)
      shabondiList1.head.state should ===(None)
      shabondiList1.head.meters.isEmpty
    })
  }

  @Test
  def testShabondiSourceCanDelete(): Unit = {
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createShabondiSource(objectKey, clientPort, Set(nodeName), Set(topicKey))

    await(shabondiApi.delete(objectKey))
    val shabondiList1 = await(shabondiApi.list())
    shabondiList1.size should ===(0)
  }

  @Test
  def testSourceDeleteWhenRunning(): Unit = {
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createShabondiSource(objectKey, clientPort, Set(nodeName), Set(topicKey))
    await(shabondiApi.start(objectKey))

    an[IllegalArgumentException] should be thrownBy await(shabondiApi.delete(objectKey))
  }

  @Test
  def testShabondiSourceCanDeleteMultipleTimes(): Unit = {
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createShabondiSource(objectKey, clientPort, Set(nodeName), Set(topicKey))

    await(shabondiApi.delete(objectKey))
    await(shabondiApi.delete(objectKey))
    await(shabondiApi.delete(objectKey))
  }

  @Test
  def testShabondiSourceCanStopMultipleTimes(): Unit = {
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createShabondiSource(objectKey, clientPort, Set(nodeName), Set(topicKey))
    await(shabondiApi.start(objectKey))

    await(shabondiApi.stop(objectKey))
    await(shabondiApi.stop(objectKey))
    await(shabondiApi.stop(objectKey))
  }

  @Test
  def testShouldThrowExceptionIfTopicNotExistWhenSourceStart(): Unit = {
    val notStartedTopic = TopicKey.of("g1", "t1")
    await(topicApi.request.brokerClusterKey(brokerClusterInfo.key).key(notStartedTopic).create())

    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createShabondiSource(objectKey, clientPort, Set(nodeName), Set(notStartedTopic))

    an[IllegalArgumentException] should be thrownBy await(shabondiApi.start(objectKey))
  }

  @Test
  def testShouldThrowExceptionIfMultipleNodeNamesWhenSourceStart(): Unit = {
    val notStartedTopic = TopicKey.of("g1", "t1")
    await(topicApi.request.brokerClusterKey(brokerClusterInfo.key).key(notStartedTopic).create())

    createShabondiSource(objectKey, CommonUtils.availablePort(), availableNodeNames.toSet, Set(notStartedTopic))

    an[IllegalArgumentException] should be thrownBy await(shabondiApi.start(objectKey))
  }

  private def createShabondiSource(
    key: ObjectKey,
    clientPort: Int,
    nodeNames: Set[String],
    topicKeys: Set[TopicKey] = Set.empty[TopicKey]
  ): ShabondiApi.ShabondiClusterInfo = {
    await(
      shabondiApi.request
        .group(key.group)
        .name(key.name)
        .shabondiClass(ShabondiType.Source.className)
        .clientPort(clientPort)
        .brokerClusterKey(brokerClusterInfo.key)
        .nodeNames(nodeNames)
        .sourceToTopics(topicKeys)
        .create()
    )
  }
}
