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
  private[this] val brokerClusterInfo = result(
    BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head

  private[this] val fileApi = FileInfoApi.access.hostname(configurator.hostname).port(configurator.port)
  private[this] val streamApi = StreamApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 20 seconds)

  private[this] val nodeNames: Set[String] = result(zkApi.list()).head.nodeNames
  private[this] val toTopicKey = TopicKey.of("g", CommonUtils.randomString())
  private[this] val fromTopicKey = TopicKey.of("g", CommonUtils.randomString())
  private[this] var fileInfo: FileInfo = _

  @Before
  def setup(): Unit = {
    val file = CommonUtils.createTempJar("empty_")
    fileInfo = result(fileApi.request.file(file).upload())

    result(topicApi.request.brokerClusterKey(brokerClusterInfo.key).key(toTopicKey).create())
    result(topicApi.start(toTopicKey))
    result(topicApi.request.brokerClusterKey(brokerClusterInfo.key).key(fromTopicKey).create())
    result(topicApi.start(fromTopicKey))

    file.deleteOnExit()
  }
  @Test
  def testCreateOnNonexistentNode(): Unit =
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.request
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeName(CommonUtils.randomString())
        .create())

  @Test
  def testUpdateJarKey(): Unit = {
    val file = CommonUtils.createTempJar("empty_")
    val fileInfo2 = result(fileApi.request.file(file).upload())
    val streamApp = result(
      streamApi.request
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create())
    streamApp.jarKey shouldBe fileInfo.key
    result(streamApi.request.key(streamApp.key).jarKey(fileInfo2.key).update()).jarKey shouldBe fileInfo2.key
  }

  @Test
  def testStreamAppPropertyPage(): Unit = {
    // create default property
    val name = CommonUtils.randomString(10)
    val defaultProps = result(
      streamApi.request
        .name(name)
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create()
    )
    defaultProps.jarKey shouldBe fileInfo.key
    // same name property cannot create again
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.request
        .name(name)
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create()
    )

    // get new streamApp property
    val res1 = result(streamApi.get(defaultProps.key))
    // check initial values
    res1.name shouldBe defaultProps.name
    res1.name shouldBe name
    res1.fromTopicKeys shouldBe Set(fromTopicKey)
    res1.toTopicKeys shouldBe Set(toTopicKey)
    res1.nodeNames shouldBe nodeNames

    // update partial properties
    val to = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString())
    result(topicApi.request.key(to).brokerClusterKey(brokerClusterInfo.key).create())
    val res2 = result(streamApi.request.name(defaultProps.name).toTopicKey(to).nodeNames(nodeNames).update())
    res2.name shouldBe name
    res2.jarKey shouldBe fileInfo.key
    res2.fromTopicKeys shouldBe Set(fromTopicKey)
    res2.toTopicKeys shouldBe Set(to)
    res2.nodeNames.forall(nodeNames.contains) shouldBe true

    // create property with some user defined properties
    val userAppId = CommonUtils.randomString(5)
    val to2 = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString())
    result(topicApi.request.key(to2).brokerClusterKey(brokerClusterInfo.key).create())
    val userProps = result(
      streamApi.request
        .name(userAppId)
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(to2)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames - nodeNames.last)
        .create())
    userProps.name shouldBe userAppId
    userProps.toTopicKeys shouldBe Set(to2)
    userProps.nodeNames.size shouldBe nodeNames.size - 1

    // we create two properties, the list size should be 2
    result(streamApi.list()).size shouldBe 2

    // update properties
    val from3 = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString())
    val to3 = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString())
    result(topicApi.request.key(from3).brokerClusterKey(brokerClusterInfo.key).create())
    result(topicApi.request.key(to3).brokerClusterKey(brokerClusterInfo.key).create())
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

    // create property
    val props = result(
      streamApi.request
        .name(streamAppName)
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create())

    val from = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString())
    val to = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString())

    // run topics
    result(
      topicApi.request
        .key(from)
        .brokerClusterKey(brokerClusterInfo.key)
        .create()
        .flatMap(info => topicApi.start(info.key)))
    result(
      topicApi.request
        .key(to)
        .brokerClusterKey(brokerClusterInfo.key)
        .create()
        .flatMap(info => topicApi.start(info.key)))
    // update properties
    result(streamApi.request.name(streamAppName).fromTopicKey(from).toTopicKey(to).nodeNames(nodeNames).update())

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
    an[RuntimeException] should be thrownBy result(streamApi.request.name(streamAppName).nodeNames(nodeNames).update())

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
    result(
      streamApi.request
        .name(streamAppName)
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .update())

    result(streamApi.request.name(streamAppName).jarKey(fileInfo.key).toTopicKeys(Set.empty).update())

    // delete non-exists object should do nothing
    result(streamApi.delete(ObjectKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))))
  }

  @Test
  def testStreamAppActionPageFailCases(): Unit = {
    val streamAppName = CommonUtils.randomString(5)
    val from = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val to = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))

    result(topicApi.request.key(from).brokerClusterKey(brokerClusterInfo.key).create())
    result(topicApi.request.key(to).brokerClusterKey(brokerClusterInfo.key).create())

    // start action will check all the required parameters
    val stream = result(
      streamApi.request
        .name(streamAppName)
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(to)
        .fromTopicKey(from)
        .nodeNames(nodeNames)
        .create())

    // non-exist topics in broker will cause running fail
    an[IllegalArgumentException] should be thrownBy result(streamApi.start(stream.key))

    // run topics
    result(topicApi.start(to))
    result(topicApi.start(from))

    // after all required parameters are set, it is ok to run
    result(streamApi.start(stream.key))
  }

  @Test
  def duplicateStopStream(): Unit = {
    val stream = result(
      streamApi.request
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create())
    (0 to 10).foreach(_ => result(streamApi.stop(stream.key)))
  }

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
      streamApi.request
        .name(CommonUtils.randomString(10))
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .tags(tags)
        .create())
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
    val streamDesc = result(
      streamApi.request
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create())
    streamDesc.fromTopicKeys should not be Set.empty
    streamDesc.toTopicKeys should not be Set.empty
    val from = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString())
    result(topicApi.request.key(from).brokerClusterKey(brokerClusterInfo.key).create())
    // update from topic
    result(streamApi.request.name(streamDesc.name).fromTopicKey(from).update()).fromTopicKeys shouldBe Set(from)
    streamApi.request.name(streamDesc.name).fromTopicKeys(Set.empty).update()
  }

  @Test
  def testSettingDefault(): Unit = {
    val key = CommonUtils.randomString()
    val value = JsString(CommonUtils.randomString())
    val streamDesc = result(
      streamApi.request
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .setting(key, value)
        .create())
    // the url is not illegal
    streamDesc.settings(key) shouldBe value
  }

  @Test
  def testOnlyAcceptOneTopic(): Unit = {
    val streamDesc = result(
      streamApi.request
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create())
    streamDesc.fromTopicKeys shouldBe Set(fromTopicKey)
    streamDesc.toTopicKeys shouldBe Set(toTopicKey)

    // multiple topics are not allow by now
    val from = TopicKey.of("g", "from")
    val to = TopicKey.of("g", "to")
    // run topics
    result(
      topicApi.request
        .key(from)
        .brokerClusterKey(brokerClusterInfo.key)
        .create()
        .flatMap(info => topicApi.start(info.key)))
    result(
      topicApi.request
        .key(to)
        .brokerClusterKey(brokerClusterInfo.key)
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
    thrown1.getMessage should include("MUST be equal to 1")
  }

  @Test
  def testBrokerClusterKey(): Unit = {
    intercept[DeserializationException] {
      result(
        streamApi.request
          .jarKey(fileInfo.key)
          .fromTopicKey(fromTopicKey)
          .toTopicKey(toTopicKey)
          .nodeNames(nodeNames)
          .create()).brokerClusterKey
    }.getMessage should include("brokerClusterKey")

    val streamDesc = result(
      streamApi.request
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create())
    streamDesc.brokerClusterKey shouldBe brokerClusterInfo.key
    result(streamApi.start(streamDesc.key))

    // fail to update a running streamApp
    an[IllegalArgumentException] should be thrownBy result(streamApi.request.name(streamDesc.name).update())
    result(streamApi.stop(streamDesc.key))
  }

  @Test
  def testCustomTagsShouldExistAfterRunning(): Unit = {
    val nodeNames = result(bkApi.list()).head.nodeNames
    val zk = result(zkApi.request.name(CommonUtils.randomString(5)).nodeNames(nodeNames).create())
    result(zkApi.start(zk.key))
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
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .tags(tags)
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
    val info = result(
      streamApi.request
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create())

    // use same name and group will cause a update request
    result(streamApi.request.name(info.name).group(info.group).nodeNames(nodeNames).update()).nodeNames shouldBe nodeNames

    // use different group will cause a create request
    result(
      streamApi.request
        .name(info.name)
        .group(CommonUtils.randomString(10))
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .update()).jmxPort should not be info.jmxPort
  }

  @Test
  def testNodeNames(): Unit = {
    val info = result(
      streamApi.request
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create())

    // could not use non-exist nodes
    an[IllegalArgumentException] should be thrownBy result(
      streamApi.request.group(info.group).name(info.name).nodeName("fake").update())
  }

  @Test
  def testNameFilter(): Unit = {
    val name = CommonUtils.randomString(10)
    val streamApp = result(
      streamApi.request
        .name(name)
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .toTopicKey(toTopicKey)
        .fromTopicKey(fromTopicKey)
        .nodeNames(nodeNames)
        .create())
    (0 until 3).foreach(
      _ =>
        result(
          streamApi.request
            .jarKey(fileInfo.key)
            .brokerClusterKey(brokerClusterInfo.key)
            .toTopicKey(toTopicKey)
            .fromTopicKey(fromTopicKey)
            .nodeNames(nodeNames)
            .create()))
    result(streamApi.list()).size shouldBe 4
    val streamApps = result(streamApi.query.name(name).execute())
    streamApps.size shouldBe 1
    streamApps.head.key shouldBe streamApp.key
  }

  @Test
  def testGroupFilter(): Unit = {
    val from = result(topicApi.request.brokerClusterKey(brokerClusterInfo.key).create())
    val to = result(topicApi.request.brokerClusterKey(brokerClusterInfo.key).create())
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
        .brokerClusterKey(brokerClusterInfo.key)
        .create())
    (0 until 3).foreach(
      _ =>
        result(
          streamApi.request
            .nodeNames(nodeNames)
            .fromTopicKey(from.key)
            .toTopicKey(to.key)
            .jarKey(fileInfo.key)
            .brokerClusterKey(brokerClusterInfo.key)
            .create()))
    result(streamApi.list()).size shouldBe 4
    val streamApps = result(streamApi.query.group(group).execute())
    streamApps.size shouldBe 1
    streamApps.head.key shouldBe streamApp.key
  }

  @Test
  def testTagsFilter(): Unit = {
    val from = result(topicApi.request.brokerClusterKey(brokerClusterInfo.key).create())
    val to = result(topicApi.request.brokerClusterKey(brokerClusterInfo.key).create())
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
        .brokerClusterKey(brokerClusterInfo.key)
        .create())
    (0 until 3).foreach(
      _ =>
        result(
          streamApi.request
            .nodeNames(nodeNames)
            .fromTopicKey(from.key)
            .toTopicKey(to.key)
            .jarKey(fileInfo.key)
            .brokerClusterKey(brokerClusterInfo.key)
            .create()))
    result(streamApi.list()).size shouldBe 4
    val streamApps = result(streamApi.query.tags(tags).execute())
    streamApps.size shouldBe 1
    streamApps.head.key shouldBe streamApp.key
  }

  @Test
  def testStateFilter(): Unit = {
    val from = result(topicApi.request.brokerClusterKey(brokerClusterInfo.key).create())
    val to = result(topicApi.request.brokerClusterKey(brokerClusterInfo.key).create())
    result(topicApi.start(from.key))
    result(topicApi.start(to.key))
    val streamApp = result(
      streamApi.request
        .nodeNames(nodeNames)
        .fromTopicKey(from.key)
        .toTopicKey(to.key)
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .create())
    (0 until 3).foreach(
      _ =>
        result(
          streamApi.request
            .nodeNames(nodeNames)
            .fromTopicKey(from.key)
            .toTopicKey(to.key)
            .jarKey(fileInfo.key)
            .brokerClusterKey(brokerClusterInfo.key)
            .create()))
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
    val from = result(topicApi.request.brokerClusterKey(brokerClusterInfo.key).create())
    val to = result(topicApi.request.brokerClusterKey(brokerClusterInfo.key).create())
    result(topicApi.start(from.key))
    result(topicApi.start(to.key))
    val streamApp = result(
      streamApi.request
        .nodeName(nodeNames.head)
        .fromTopicKey(from.key)
        .toTopicKey(to.key)
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .create())
    (0 until 3).foreach(
      _ =>
        result(
          streamApi.request
            .nodeNames(nodeNames)
            .fromTopicKey(from.key)
            .toTopicKey(to.key)
            .jarKey(fileInfo.key)
            .brokerClusterKey(brokerClusterInfo.key)
            .create()
            .flatMap(z => streamApi.start(z.key))))
    result(streamApi.list()).size shouldBe 4
    result(streamApi.start(streamApp.key))
    val streamApps = result(streamApi.query.aliveNodes(Set(nodeNames.head)).execute())
    streamApps.size shouldBe 1
    streamApps.head.key shouldBe streamApp.key
    result(streamApi.query.aliveNodes(nodeNames).execute()).size shouldBe 3
  }

  @Test
  def topicMustOnSameBrokerCluster(): Unit = {
    val zk = result(zkApi.request.nodeNames(nodeNames).create())
    result(zkApi.start(zk.key))
    val bk = result(bkApi.request.zookeeperClusterKey(zk.key).nodeNames(nodeNames).create())
    result(bkApi.start(bk.key))

    // put those topics on different broker cluster
    val fromTopic = result(topicApi.request.brokerClusterKey(bk.key).create())
    result(topicApi.start(fromTopic.key))
    val toTopic = result(topicApi.request.brokerClusterKey(bk.key).create())
    result(topicApi.start(toTopic.key))

    val streamApp = result(
      streamApi.request
        .nodeName(nodeNames.head)
        .fromTopicKey(fromTopic.key)
        .toTopicKey(toTopic.key)
        .jarKey(fileInfo.key)
        .brokerClusterKey(brokerClusterInfo.key)
        .create())

    intercept[IllegalArgumentException] {
      result(streamApi.start(streamApp.key))
    }.getMessage should include("another broker cluster")
  }

  @Test
  def testEmptyToTopics(): Unit = result(
    streamApi.request
      .name(CommonUtils.randomString(10))
      .jarKey(fileInfo.key)
      .brokerClusterKey(brokerClusterInfo.key)
      .fromTopicKey(fromTopicKey)
      .nodeNames(nodeNames)
      .create()).toTopicKeys shouldBe Set.empty
  @Test
  def testEmptyFromTopics(): Unit = result(
    streamApi.request
      .name(CommonUtils.randomString(10))
      .jarKey(fileInfo.key)
      .brokerClusterKey(brokerClusterInfo.key)
      .toTopicKey(toTopicKey)
      .nodeNames(nodeNames)
      .create()).fromTopicKeys shouldBe Set.empty

  @Test
  def testInvalidNodeName(): Unit =
    Set(START_COMMAND, STOP_COMMAND, PAUSE_COMMAND, RESUME_COMMAND).foreach { nodeName =>
      intercept[DeserializationException] {
        result(
          streamApi.request.nodeName(nodeName).jarKey(fileInfo.key).brokerClusterKey(brokerClusterInfo.key).create())
      }.getMessage should include(nodeName)
    }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
