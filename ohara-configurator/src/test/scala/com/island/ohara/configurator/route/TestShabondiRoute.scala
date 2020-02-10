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

import java.time.{Duration => JDuration}

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.{BrokerApi, NodeApi, ShabondiApi, TopicApi}
import com.island.ohara.client.configurator.v0.ShabondiApi.ShabondiClusterCreation
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.{ObjectKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.shabondi.DefaultDefinitions._
import org.junit.{After, Before, Test}
import org.scalatest.Matchers
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class TestShabondiRoute extends OharaTest with Matchers {
  private[this] var configurator: Configurator                     = _
  private[this] var nodeApi: NodeApi.Access                        = _
  private[this] var topicApi: TopicApi.Access                      = _
  private[this] var brokerClusterInfo: BrokerApi.BrokerClusterInfo = _
  private[this] var shabondiApi: ShabondiApi.Access                = _
  private[this] var availableNodeNames: Seq[String]                = _

  private[this] val topicKey  = TopicKey.of("g", CommonUtils.randomString(10))
  private[this] val objectKey = ObjectKey.of("group-1", "name-1")

  private[this] def await[T](f: Future[T]): T = Await.result(f, 20 seconds)

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
    val objectKey              = ObjectKey.of("group-1", "name-1")
    val (clientPort, nodeName) = (CommonUtils.availablePort(), "non-existent-node")
    val clusterInfo: Future[ShabondiApi.ShabondiClusterInfo] =
      shabondiApi.request
        .group(objectKey.group)
        .name(objectKey.name)
        .serverType(SERVER_TYPE_SOURCE)
        .clientPort(clientPort)
        .brokerClusterKey(brokerClusterInfo.key)
        .nodeName(nodeName)
        .create()

    an[IllegalArgumentException] should be thrownBy await(clusterInfo)
  }

  @Test
  def testShabondiSourceCreate(): Unit = {
    val objectKey                                    = ObjectKey.of("group-1", "name-1")
    val (clientPort, nodeName)                       = (CommonUtils.availablePort(), availableNodeNames(0))
    val clusterInfo: ShabondiApi.ShabondiClusterInfo = createSourceShabondi(objectKey, clientPort, nodeName)

    clusterInfo.group should ===(objectKey.group)
    clusterInfo.name should ===(objectKey.name)
    clusterInfo.serverType should ===(SERVER_TYPE_SOURCE)
    clusterInfo.clientPort should ===(clientPort)
    clusterInfo.brokerClusterKey should ===(brokerClusterInfo.key)
    clusterInfo.nodeNames should contain(nodeName)
  }

  @Test
  def testShabondiSourceUpdate(): Unit = {
    val objectKey              = ObjectKey.of("group-1", "name-1")
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    val clusterInfo            = createSourceShabondi(objectKey, clientPort, nodeName)

    clusterInfo.group should ===(objectKey.group)
    clusterInfo.name should ===(objectKey.name)
    clusterInfo.serverType should ===(SERVER_TYPE_SOURCE)
    clusterInfo.clientPort should ===(clientPort)
    clusterInfo.brokerClusterKey should ===(brokerClusterInfo.key)
    clusterInfo.nodeNames should contain(nodeName)
    clusterInfo.sourceToTopics should be(empty)

    val newClientPort = CommonUtils.availablePort()
    val updatedClusterInfo = await(
      shabondiApi.request
        .group(objectKey.group)
        .name(objectKey.name)
        .clientPort(newClientPort)
        .sourceToTopics(Set(topicKey))
        .settings(
          Map(
            SINK_POLL_TIMEOUT_DEF.key   -> JsString(JDuration.ofSeconds(10).toString),
            SINK_GROUP_IDLETIME_DEF.key -> JsString(JDuration.ofMinutes(30).toString)
          )
        )
        .update()
    )
    updatedClusterInfo.clientPort should be(newClientPort)
    updatedClusterInfo.sourceToTopics should be(Set(topicKey))
    updatedClusterInfo.settings should contain(SINK_POLL_TIMEOUT_DEF.key   -> JsString("PT10S"))
    updatedClusterInfo.settings should contain(SINK_GROUP_IDLETIME_DEF.key -> JsString("PT30M"))
  }

  @Test
  def testShabondiSourceStart(): Unit = {
    val objectKey              = ObjectKey.of("group-1", "name-1")
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createSourceShabondi(objectKey, clientPort, nodeName)

    await(
      shabondiApi.request
        .group(objectKey.group)
        .name(objectKey.name)
        .sourceToTopics(Set(topicKey))
        .update()
    )

    await(shabondiApi.start(objectKey))

    val shabondiList = await(shabondiApi.list())
    shabondiList.size should ===(1)
    shabondiList(0).sourceToTopics should ===(Set(topicKey))
    shabondiList(0).state.get should ===(ContainerState.RUNNING.name)
    shabondiList(0).aliveNodes.head should ===(nodeName)
  }

  @Test
  def testShabondiSourceStop(): Unit = {
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createSourceShabondi(objectKey, clientPort, nodeName, Set(topicKey))

    await(shabondiApi.start(objectKey))
    val shabondiList = await(shabondiApi.list())
    shabondiList.size should ===(1)
    shabondiList(0).state.get should ===(ContainerState.RUNNING.name)

    await(shabondiApi.stop(objectKey))
    val shabondiList1 = await(shabondiApi.list())

    shabondiList1.size should ===(1)
    println(shabondiList1)
    shabondiList1(0).state should ===(None)
  }

  @Test
  def testShabondiSourceCanDelete(): Unit = {
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createSourceShabondi(objectKey, clientPort, nodeName, Set(topicKey))

    await(shabondiApi.delete(objectKey))
    val shabondiList1 = await(shabondiApi.list())
    shabondiList1.size should ===(0)
  }

  @Test
  def testShouldThrowExceptionIfShabondiIsRunningWhenSourceDelete(): Unit = {
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createSourceShabondi(objectKey, clientPort, nodeName, Set(topicKey))
    await(shabondiApi.start(objectKey))

    an[IllegalArgumentException] should be thrownBy await(shabondiApi.delete(objectKey))
  }

  @Test
  def testShabondiSourceCanDeleteMultipleTimes(): Unit = {
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createSourceShabondi(objectKey, clientPort, nodeName, Set(topicKey))

    await(shabondiApi.delete(objectKey))
    await(shabondiApi.delete(objectKey))
    await(shabondiApi.delete(objectKey))
  }

  @Test
  def testShabondiSourceCanStopMultipleTimes(): Unit = {
    val (clientPort, nodeName) = (CommonUtils.availablePort(), availableNodeNames(0))
    createSourceShabondi(objectKey, clientPort, nodeName, Set(topicKey))
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
    createSourceShabondi(objectKey, clientPort, nodeName, Set(notStartedTopic))

    an[IllegalArgumentException] should be thrownBy await(shabondiApi.start(objectKey))
  }

  private def createSourceShabondi(
    key: ObjectKey,
    clientPort: Int,
    nodeName: String,
    topicKeys: Set[TopicKey] = Set.empty[TopicKey]
  ): ShabondiApi.ShabondiClusterInfo = {
    await(
      shabondiApi.request
        .group(key.group)
        .name(key.name)
        .serverType(SERVER_TYPE_SOURCE)
        .clientPort(clientPort)
        .brokerClusterKey(brokerClusterInfo.key)
        .nodeName(nodeName)
        .sourceToTopics(topicKeys)
        .create()
    )
  }

  @Test
  def testNecessaryContainInvalidValue(): Unit = {
    val jsonObject = """
        |{
        |  "name": "shabondi00",
        |  "group": "default",
        |  "shabondi.serverType": "aaa",
        |  "shabondi.client.port": 58456,
        |  "brokerClusterKey": {
        |  	"group": "default",
        |  	"name": "bk00"
        |  },
        |  "nodeNames": [
        |    "vito-ohara01"
        |  ]
        |}
        |""".stripMargin.parseJson.asJsObject

    val creation = new ShabondiClusterCreation(jsonObject.fields)
    the[IllegalArgumentException] thrownBy {
      ShabondiRoute.necessaryContains(SERVER_TYPE_DEFINITION, creation.settings)
    } should have message ("Invalid value of shabondi.serverType, must be one of [source, sink]")
  }
}
