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

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.{ObjectKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Before, Test}
import org.scalatest.Matchers
import spray.json.{DeserializationException, JsArray, JsNumber, JsObject, JsString, JsTrue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestStreamRoute extends OharaTest with Matchers {

  // create all fake cluster
  private[this] val configurator = Configurator.builder.fake().build()
  private[this] val topicApi = TopicApi.access.hostname(configurator.hostname).port(configurator.port)
  private[this] val zkApi = ZookeeperApi.access.hostname(configurator.hostname).port(configurator.port)
  private[this] val bkApi = BrokerApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val fileApi = FileInfoApi.access.hostname(configurator.hostname).port(configurator.port)
  private[this] val streamApi = StreamApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 20 seconds)
  private[this] def topicKey(): TopicKey = topicKey(CommonUtils.randomString())
  private[this] def topicKey(name: String): TopicKey = TopicKey.of(GROUP_DEFAULT, name)

  private[this] var nodeNames: Set[String] = _
  private[this] var fileInfo: FileInfo = _

  @Before
  def setup(): Unit = {
    val file = CommonUtils.createTempJar("empty_")
    fileInfo = result(fileApi.request.file(file).upload())
    nodeNames = result(configurator.nodeCollie.nodes()).map(_.name).toSet

    file.deleteOnExit()
  }

  @Test
  def testUpdateJarKey(): Unit = {
    val file = CommonUtils.createTempJar("empty_")
    val fileInfo2 = result(fileApi.request.file(file).upload())
    val streamApp = result(streamApi.request.jarKey(fileInfo.key).create())
    streamApp.jarKey shouldBe fileInfo.key
    result(streamApi.request.key(streamApp.key).jarKey(fileInfo2.key).update()).jarKey shouldBe fileInfo2.key
  }

  @Test
  def testStreamAppPropertyPage(): Unit = {
    // create default property
    val name = CommonUtils.randomString(10)
    val defaultProps = result(
      streamApi.request.name(name).jarKey(fileInfo.key).create()
    )
    defaultProps.jarKey shouldBe fileInfo.key
    // same name property cannot create again
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.request.name(name).jarKey(ObjectKey.of(fileInfo.group, "newJar")).create())

    // get new streamApp property
    val res1 = result(streamApi.get(defaultProps.key))
    // check initial values
    res1.name shouldBe defaultProps.name
    res1.name shouldBe name
    res1.fromTopicKeys shouldBe Set.empty
    res1.toTopicKeys shouldBe Set.empty
    res1.nodeNames.isEmpty shouldBe true

    // update partial properties
    val to = topicKey()
    val res2 = result(streamApi.request.name(defaultProps.name).toTopicKey(to).instances(nodeNames.size).update())
    res2.name shouldBe name
    res2.jarKey shouldBe fileInfo.key
    res2.fromTopicKeys shouldBe Set.empty
    res2.toTopicKeys shouldBe Set(to)
    res2.nodeNames.forall(nodeNames.contains) shouldBe true

    // create property with some user defined properties
    val userAppId = CommonUtils.randomString(5)
    val to2 = topicKey()
    val userProps = result(
      streamApi.request.name(userAppId).jarKey(fileInfo.key).toTopicKey(to2).instances(nodeNames.size - 1).create())
    userProps.name shouldBe userAppId
    userProps.toTopicKeys shouldBe Set(to2)
    userProps.nodeNames.size shouldBe nodeNames.size - 1

    // we create two properties, the list size should be 2
    result(streamApi.list()).size shouldBe 2

    // update properties
    val from3 = topicKey()
    val to3 = topicKey()
    val res3 = result(streamApi.request.name(userAppId).fromTopicKey(from3).toTopicKey(to3).update())
    res3.name shouldBe userAppId
    res3.fromTopicKeys shouldBe Set(from3)
    res3.toTopicKeys shouldBe Set(to3)
    res3.nodeNames.size shouldBe nodeNames.size - 1

    // delete properties
    result(streamApi.delete(defaultProps.key))

    // after delete, the streamApp should not exist
    an[IllegalArgumentException] should be thrownBy result(streamApi.get(defaultProps.key))

    // delete property should not delete actual jar
    result(fileApi.list()).size shouldBe 1
  }

  @Test
  def testStreamAppAction(): Unit = {
    val streamAppName = CommonUtils.randomString(5)
    val from = topicKey()
    val to = topicKey()

    // create property
    val props = result(streamApi.request.name(streamAppName).jarKey(fileInfo.key).create())

    // update properties
    result(streamApi.request.name(streamAppName).fromTopicKey(from).toTopicKey(to).instances(nodeNames.size).update())

    // run topics
    result(
      topicApi.request
        .key(from)
        .brokerClusterKey(result(bkApi.list()).head.key)
        .create()
        .flatMap(info => topicApi.start(info.key)))
    result(
      topicApi.request
        .key(to)
        .brokerClusterKey(result(bkApi.list()).head.key)
        .create()
        .flatMap(info => topicApi.start(info.key)))

    result(streamApi.start(props.key))
    val res1 = result(streamApi.get(props.key))
    res1.name shouldBe props.name
    res1.name shouldBe streamAppName
    res1.fromTopicKeys shouldBe Set(from)
    res1.toTopicKeys shouldBe Set(to)
    res1.jarKey.name shouldBe fileInfo.name
    res1.nodeNames.forall(nodeNames.contains) shouldBe true
    res1.state.get shouldBe ContainerState.RUNNING.name

    // get again
    val running = result(streamApi.get(props.key))
    running.state.get shouldBe ContainerState.RUNNING.name
    running.error.isEmpty shouldBe true

    // get the stream clusters information by clusterCache
    val cluster = result(configurator.serviceCollie.streamCollie.clusters())
    cluster.size shouldBe 1

    // start the same streamApp cluster will get the previous stream cluster
    result(streamApi.start(props.key))
    val prevRes = result(streamApi.get(props.key))
    prevRes.name shouldBe props.name
    prevRes.name shouldBe streamAppName
    prevRes.state.get shouldBe ContainerState.RUNNING.name
    prevRes.error.isDefined shouldBe false

    // running streamApp cannot update state
    an[RuntimeException] should be thrownBy result(streamApi.request.name(streamAppName).instances(10).update())

    // running streamApp cannot delete
    an[RuntimeException] should be thrownBy result(streamApi.delete(props.key))

    result(streamApi.get(props.key)).state should not be None
    result(streamApi.stop(props.key))
    result(streamApi.get(props.key)).state shouldBe None

    // get the stream clusters information again, should be zero
    result(configurator.serviceCollie.streamCollie.clusters()).size shouldBe 0

    // stop the same streamApp cluster will only return the previous object
    result(streamApi.stop(props.key))
    result(streamApi.get(props.key)).state shouldBe None

    // get property will get the latest state (streamApp not exist)
    val latest = result(streamApi.get(props.key))
    latest.state shouldBe None
    latest.error.isDefined shouldBe false

    // after stop, streamApp can be deleted
    result(streamApi.delete(props.key))

    // after delete, streamApp should not exist
    an[IllegalArgumentException] should be thrownBy result(streamApi.get(props.key))
  }

  @Test
  def testStreamAppPropertyPageFailCases(): Unit = {
    val streamAppName = CommonUtils.randomString(10)
    //operations on non-exists property should be fail
    an[NullPointerException] should be thrownBy result(streamApi.request.name("appId").jarKey(null).update())
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.get(ObjectKey.of(CommonUtils.randomString(1), CommonUtils.randomString(1))))

    // we can update the topics to empty (the topic checking is moving to start phase)
    result(streamApi.request.name(streamAppName).jarKey(fileInfo.key).fromTopicKeys(Set.empty).update())

    // we can update the topics to empty (the topic checking is moving to start phase)
    result(streamApi.request.name(streamAppName).jarKey(fileInfo.key).toTopicKeys(Set.empty).update())

    an[IllegalArgumentException] should be thrownBy result(streamApi.request.instances(0).update())

    an[IllegalArgumentException] should be thrownBy result(streamApi.request.instances(-99).update())

    // delete non-exists object should do nothing
    result(streamApi.delete(ObjectKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))))
  }

  @Test
  def testStreamAppActionPageFailCases(): Unit = {
    val streamAppName = CommonUtils.randomString(5)
    val from = topicKey()
    val to = topicKey()

    // start action will check all the required parameters
    val stream = result(streamApi.request.name(streamAppName).jarKey(fileInfo.key).nodeNames(nodeNames).create())
    an[IllegalArgumentException] should be thrownBy result(streamApi.start(stream.key))

    result(streamApi.request.name(streamAppName).fromTopicKey(from).update())
    an[IllegalArgumentException] should be thrownBy result(streamApi.start(stream.key))

    result(streamApi.request.name(streamAppName).toTopicKey(to).update())

    // non-exist topics in broker will cause running fail
    an[IllegalArgumentException] should be thrownBy result(streamApi.start(stream.key))

    // run topics
    result(
      topicApi.request
        .key(from)
        .brokerClusterKey(result(bkApi.list()).head.key)
        .create()
        .flatMap(info => topicApi.start(info.key)))
    result(
      topicApi.request
        .key(to)
        .brokerClusterKey(result(bkApi.list()).head.key)
        .create()
        .flatMap(info => topicApi.start(info.key)))

    // after all required parameters are set, it is ok to run
    result(streamApi.start(stream.key))
  }

  @Test
  def duplicateStopStream(): Unit =
    (0 to 10).foreach(index => result(streamApi.stop(ObjectKey.of(index.toString, index.toString))))

  @Test
  def duplicateDeleteStreamProperty(): Unit =
    (0 to 10).foreach(index => result(streamApi.delete(ObjectKey.of(index.toString, index.toString))))

  @Test
  def updateTags(): Unit = {
    val tags = Map(
      CommonUtils.randomString(10) -> JsString(CommonUtils.randomString(10)),
      CommonUtils.randomString(10) -> JsNumber(CommonUtils.randomInteger())
    )
    val streamDesc = result(
      streamApi.request.name(CommonUtils.randomString(10)).jarKey(fileInfo.key).tags(tags).create())
    streamDesc.tags shouldBe tags
    streamDesc.jarKey shouldBe fileInfo.key

    val tags2 = Map(
      CommonUtils.randomString(10) -> JsString(CommonUtils.randomString(10)),
      CommonUtils.randomString(10) -> JsNumber(CommonUtils.randomInteger())
    )
    val streamDesc2 = result(streamApi.request.name(streamDesc.name).tags(tags2).update())
    streamDesc2.tags shouldBe tags2
    streamDesc2.jarKey shouldBe fileInfo.key

    val streamDesc3 = result(streamApi.request.name(streamDesc.name).update())
    streamDesc3.tags shouldBe tags2

    val streamDesc4 = result(streamApi.request.name(streamDesc.name).tags(Map.empty).update())
    streamDesc4.tags shouldBe Map.empty
  }

  @Test
  def testUpdateTopics(): Unit = {
    val streamDesc = result(streamApi.request.jarKey(fileInfo.key).create())
    streamDesc.fromTopicKeys shouldBe Set.empty
    streamDesc.toTopicKeys shouldBe Set.empty
    val from = topicKey()
    // update from topic
    result(streamApi.request.name(streamDesc.name).fromTopicKey(from).update()).fromTopicKeys shouldBe Set(from)
    // update from topic to empty
    result(streamApi.request.name(streamDesc.name).fromTopicKeys(Set.empty).update()).fromTopicKeys shouldBe Set.empty
    // to topic should still be empty
    result(streamApi.get(streamDesc.key)).toTopicKeys shouldBe Set.empty
  }

  @Test
  def testSettingDefault(): Unit = {
    val key = CommonUtils.randomString()
    val value = JsString(CommonUtils.randomString())
    val streamDesc = result(streamApi.request.jarKey(fileInfo.key).setting(key, value).create())
    // the url is not illegal
    streamDesc.definition should not be None
    streamDesc.settings(key) shouldBe value
  }

  @Test
  def testOnlyAcceptOneTopic(): Unit = {
    val streamDesc = result(streamApi.request.jarKey(fileInfo.key).create())
    streamDesc.fromTopicKeys shouldBe Set.empty
    streamDesc.toTopicKeys shouldBe Set.empty

    // Empty topic is not allow
    an[IllegalArgumentException] should be thrownBy result(streamApi.start(streamDesc.key))

    // multiple topics are not allow by now
    val from = topicKey()
    val to = topicKey()
    // run topics
    result(
      topicApi.request
        .key(from)
        .brokerClusterKey(result(bkApi.list()).head.key)
        .create()
        .flatMap(info => topicApi.start(info.key)))
    result(
      topicApi.request
        .key(to)
        .brokerClusterKey(result(bkApi.list()).head.key)
        .create()
        .flatMap(info => topicApi.start(info.key)))
    val thrown1 = the[IllegalArgumentException] thrownBy result(
      streamApi.request
        .name(streamDesc.name)
        .fromTopicKey(from)
        .toTopicKeys(Set(from, to))
        .update()
        .flatMap(info => streamApi.start(info.key)))
    // "to" field is used multiple topics which is not allow for current version
    thrown1.getMessage should include("We don't allow multiple topics of to field")
  }

  @Test
  def testBrokerClusterKey(): Unit = {
    val nodeNames = result(bkApi.list()).head.nodeNames
    val zk = result(zkApi.request.name(CommonUtils.randomString(5)).nodeNames(nodeNames).create())
    val bk = result(
      bkApi.request.name(CommonUtils.randomString(5)).nodeNames(nodeNames).zookeeperClusterKey(zk.key).create())
    result(bkApi.start(bk.key))
    val from0 = result(topicApi.request.brokerClusterKey(bk.key).create())
    result(topicApi.start(from0.key))
    val to0 = result(topicApi.request.brokerClusterKey(bk.key).create())
    result(topicApi.start(to0.key))
    result(bkApi.start(bk.key))

    // we already have pre-defined broker, lake brokerClusterKey parameter will cause exception
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.request
        .jarKey(fileInfo.key)
        .fromTopicKey(from0.key)
        .toTopicKey(to0.key)
        .instances(1)
        .create()).brokerClusterKey

    val streamDesc = result(
      streamApi.request
        .brokerClusterKey(bk.key)
        .jarKey(fileInfo.key)
        .fromTopicKey(from0.key)
        .toTopicKey(to0.key)
        .instances(1)
        .create())
    streamDesc.brokerClusterKey shouldBe bk.key
    result(streamApi.start(streamDesc.key))

    // fail to update a running streamApp
    an[IllegalArgumentException] should be thrownBy result(streamApi.request.name(streamDesc.name).update())
    result(streamApi.stop(streamDesc.key))
  }

  // TODO remove this test after #2288
  @Test
  def testMixNodeNameAndInstancesInCreation(): Unit = {
    an[IllegalArgumentException] should be thrownBy
      result(streamApi.request.jarKey(fileInfo.key).nodeNames(nodeNames).instances(1).create())

    // pass
    result(streamApi.request.jarKey(fileInfo.key).instances(1).create())

    // pass too
    result(streamApi.request.jarKey(fileInfo.key).nodeNames(nodeNames).create())
  }

  // TODO remove this test after #2288
  @Test
  def testMixNodeNameAndInstancesInUpdate(): Unit = {
    val info = result(streamApi.request.jarKey(fileInfo.key).create())
    // default values
    info.nodeNames shouldBe Set.empty

    // cannot update empty array
    an[DeserializationException] should be thrownBy result(
      streamApi.request.name(info.name).nodeNames(Set.empty).update())
    // non-exist node
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.request.name(info.name).nodeNames(Set("fake")).update())
    // instances bigger than nodes
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.request.name(info.name).instances(nodeNames.size + 1).update())

    // update some nodes normally
    result(streamApi.request.name(info.name).nodeNames(Set(nodeNames.head)).update()).nodeNames shouldBe Set(
      nodeNames.head)
    // update instances normally
    result(streamApi.request.name(info.name).instances(2).update()).nodeNames.size shouldBe 2

    // could not update both nodeNames and instances
    an[IllegalArgumentException] should be thrownBy
      result(streamApi.request.name(info.name).nodeNames(nodeNames).instances(2).update())

    // create another streamApp
    val info2 = result(streamApi.request.jarKey(fileInfo.key).create())
    // update instances normally
    result(streamApi.request.name(info2.name).instances(nodeNames.size).update()).nodeNames.size shouldBe nodeNames.size
    // update nodeNames normally
    result(streamApi.request.name(info.name).nodeNames(Set(nodeNames.last)).update()).nodeNames shouldBe Set(
      nodeNames.last)
  }

  @Test
  def testCustomTagsShouldExistAfterRunning(): Unit = {
    val nodeNames = result(bkApi.list()).head.nodeNames
    val zk = result(zkApi.request.name(CommonUtils.randomString(5)).nodeNames(nodeNames).create())
    val bk = result(
      bkApi.request.name(CommonUtils.randomString(5)).nodeNames(nodeNames).zookeeperClusterKey(zk.key).create())
    result(bkApi.start(bk.key))
    val from0 = result(topicApi.request.brokerClusterKey(bk.key).create())
    result(topicApi.start(from0.key))
    val to0 = result(topicApi.request.brokerClusterKey(bk.key).create())
    result(topicApi.start(to0.key))
    result(bkApi.start(bk.key))

    val tags = Map(
      "aa" -> JsString("bb"),
      "cc" -> JsNumber(123),
      "dd" -> JsArray(JsString("bar"), JsString("foo"))
    )
    val streamDesc = result(
      streamApi.request
        .brokerClusterKey(bk.key)
        .jarKey(fileInfo.key)
        .tags(tags)
        .fromTopicKey(from0.key)
        .toTopicKey(to0.key)
        .instances(1)
        .create())
    streamDesc.tags shouldBe tags

    // after create, tags should exist
    result(streamApi.get(streamDesc.key)).tags shouldBe tags

    // after start, tags should still exist
    result(streamApi.start(streamDesc.key))
    result(streamApi.get(streamDesc.key)).tags shouldBe tags

    // after stop, tags should still exist
    result(streamApi.stop(streamDesc.key))
    result(streamApi.get(streamDesc.key)).tags shouldBe tags
  }

  @Test
  def testUpdateAsCreateRequest(): Unit = {
    val info = result(streamApi.request.jarKey(fileInfo.key).create())

    // use same name and group will cause a update request
    result(streamApi.request.name(info.name).group(info.group).nodeNames(nodeNames).update()).nodeNames shouldBe nodeNames

    // use different group will cause a create request
    result(streamApi.request.name(info.name).group(CommonUtils.randomString(10)).jarKey(fileInfo.key).update()).jmxPort should not be info.jmxPort
  }

  @Test
  def testDuplicatedInstanceFiled(): Unit = {
    val info = result(streamApi.request.jarKey(fileInfo.key).create())
    // duplicated: default nodeNames is empty
    info.nodeNames shouldBe Set.empty
    CommonUtils.requireConnectionPort(info.jmxPort)

    // update with specific instances
    result(streamApi.request.group(info.group).name(info.name).instances(1).update()).nodeNames.size shouldBe 1

    // update with specific nodeNames
    result(streamApi.request.group(info.group).name(info.name).nodeNames(nodeNames).update()).nodeNames shouldBe nodeNames

    // could not update both
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.request.group(info.group).name(info.name).nodeName(nodeNames.head).instances(1).update())

    // could not use instances > nodes.size
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.request.group(info.group).name(info.name).instances(nodeNames.size + 1).update())
  }

  @Test
  def testNodeNames(): Unit = {
    val info = result(streamApi.request.jarKey(fileInfo.key).create())

    // could not use non-exist nodes
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.request.group(info.group).name(info.name).nodeName("fake").update())
  }

  @Test
  def testNameFilter(): Unit = {
    val from = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    val to = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    result(topicApi.start(from.key))
    result(topicApi.start(to.key))
    val name = CommonUtils.randomString(10)
    val streamApp = result(
      streamApi.request
        .name(name)
        .nodeNames(nodeNames)
        .fromTopicKey(from.key)
        .toTopicKey(to.key)
        .jarKey(fileInfo.key)
        .create())
    (0 until 3).foreach(_ =>
      result(
        streamApi.request.nodeNames(nodeNames).fromTopicKey(from.key).toTopicKey(to.key).jarKey(fileInfo.key).create()))
    result(streamApi.list()).size shouldBe 4
    val streamApps = result(streamApi.query.name(name).execute())
    streamApps.size shouldBe 1
    streamApps.head.key shouldBe streamApp.key
  }

  @Test
  def testGroupFilter(): Unit = {
    val from = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    val to = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    result(topicApi.start(from.key))
    result(topicApi.start(to.key))
    val group = CommonUtils.randomString(10)
    val streamApp = result(
      streamApi.request
        .group(group)
        .nodeNames(nodeNames)
        .fromTopicKey(from.key)
        .toTopicKey(to.key)
        .jarKey(fileInfo.key)
        .create())
    (0 until 3).foreach(_ =>
      result(
        streamApi.request.nodeNames(nodeNames).fromTopicKey(from.key).toTopicKey(to.key).jarKey(fileInfo.key).create()))
    result(streamApi.list()).size shouldBe 4
    val streamApps = result(streamApi.query.group(group).execute())
    streamApps.size shouldBe 1
    streamApps.head.key shouldBe streamApp.key
  }

  @Test
  def testTagsFilter(): Unit = {
    val from = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    val to = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    result(topicApi.start(from.key))
    result(topicApi.start(to.key))
    val tags = Map(
      "a" -> JsString("b"),
      "b" -> JsNumber(123),
      "c" -> JsTrue,
      "d" -> JsArray(JsString("B")),
      "e" -> JsObject("a" -> JsNumber(123))
    )
    val streamApp = result(
      streamApi.request
        .tags(tags)
        .nodeNames(nodeNames)
        .fromTopicKey(from.key)
        .toTopicKey(to.key)
        .jarKey(fileInfo.key)
        .create())
    (0 until 3).foreach(_ =>
      result(
        streamApi.request.nodeNames(nodeNames).fromTopicKey(from.key).toTopicKey(to.key).jarKey(fileInfo.key).create()))
    result(streamApi.list()).size shouldBe 4
    val streamApps = result(streamApi.query.tags(tags).execute())
    streamApps.size shouldBe 1
    streamApps.head.key shouldBe streamApp.key
  }

  @Test
  def testStateFilter(): Unit = {
    val from = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    val to = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    result(topicApi.start(from.key))
    result(topicApi.start(to.key))
    val streamApp = result(
      streamApi.request.nodeNames(nodeNames).fromTopicKey(from.key).toTopicKey(to.key).jarKey(fileInfo.key).create())
    (0 until 3).foreach(_ =>
      result(
        streamApi.request.nodeNames(nodeNames).fromTopicKey(from.key).toTopicKey(to.key).jarKey(fileInfo.key).create()))
    result(streamApi.list()).size shouldBe 4
    result(streamApi.start(streamApp.key))
    val streamApps = result(streamApi.query.state("running").execute())
    streamApps.size shouldBe 1
    streamApps.find(_.key == streamApp.key) should not be None

    result(streamApi.query.group(CommonUtils.randomString()).state("running").execute()).size shouldBe 0
    result(streamApi.query.state("none").execute()).size shouldBe 3
  }

  @Test
  def testAliveNodesFilter(): Unit = {
    val from = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    val to = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    result(topicApi.start(from.key))
    result(topicApi.start(to.key))
    val streamApp = result(
      streamApi.request
        .nodeName(nodeNames.head)
        .fromTopicKey(from.key)
        .toTopicKey(to.key)
        .jarKey(fileInfo.key)
        .create())
    (0 until 3).foreach(
      _ =>
        result(
          streamApi.request
            .nodeNames(nodeNames)
            .fromTopicKey(from.key)
            .toTopicKey(to.key)
            .jarKey(fileInfo.key)
            .create()
            .flatMap(z => streamApi.start(z.key))))
    result(streamApi.list()).size shouldBe 4
    result(streamApi.start(streamApp.key))
    val streamApps = result(streamApi.query.aliveNodes(Set(nodeNames.head)).execute())
    streamApps.size shouldBe 1
    streamApps.head.key shouldBe streamApp.key
    result(streamApi.query.aliveNodes(nodeNames).execute()).size shouldBe 3
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
