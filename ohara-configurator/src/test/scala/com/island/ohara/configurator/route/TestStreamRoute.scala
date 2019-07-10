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
import com.island.ohara.client.configurator.v0.JarApi.JarKey
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class TestStreamRoute extends SmallTest with Matchers {

  // create all fake cluster
  private[this] val configurator = Configurator.builder().fake().build()
  private[this] val wkApi = WorkerApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val accessJar = JarApi.access.hostname(configurator.hostname).port(configurator.port)
  private[this] val accessStream = StreamApi.access.hostname(configurator.hostname).port(configurator.port)

  @Test
  def testStreamAppPropertyPage(): Unit = {
    val file = CommonUtils.createTempJar("empty_")

    val jar = result(accessJar.request.upload(file))

    // create default property
    val name = CommonUtils.randomString(10)
    val defaultProps = result(
      accessStream.request.name(name).jar(JarKey(jar.group, jar.name)).create()
    )
    // same name property cannot create again
    an[IllegalArgumentException] should be thrownBy result(
      accessStream.request.name(name).jar(JarKey(jar.group, "newJar")).create())

    // get new streamApp property
    val res1 = result(accessStream.get(defaultProps.name))
    // check initial values
    res1.name shouldBe defaultProps.name
    res1.name shouldBe name
    res1.from.size shouldBe 0
    res1.to.size shouldBe 0
    res1.instances shouldBe 1

    // update partial properties
    val res2 = result(accessStream.request.name(defaultProps.name).to(Set("to1", "to2")).instances(10).update())
    res2.name shouldBe name
    res2.from.size shouldBe 0
    res2.to.size shouldBe 2
    res2.to shouldBe Set("to1", "to2")
    res2.instances shouldBe 10

    // create property with some user defined properties
    val userAppId = CommonUtils.randomString(5)
    val userProps = result(
      accessStream.request.name(userAppId).jar(JarKey(jar.group, jar.name)).to(Set("to")).instances(99).create())
    userProps.name shouldBe userAppId
    userProps.to shouldBe Set("to")
    userProps.instances shouldBe 99

    // we create two properties, the list size should be 2
    result(accessStream.list()).size shouldBe 2

    // update properties
    val res3 = result(accessStream.request.name(userAppId).from(Set("from")).to(Set("to")).instances(1).update())
    res3.name shouldBe userAppId
    res3.from.size shouldBe 1
    res3.to.size shouldBe 1
    res3.instances shouldBe 1

    // delete properties
    result(accessStream.delete(defaultProps.name))

    // after delete, the streamApp should not exist
    an[IllegalArgumentException] should be thrownBy result(accessStream.get(defaultProps.name))

    // delete property should not delete actual jar
    result(accessJar.request.list()).size shouldBe 1

    file.deleteOnExit()
  }

  @Test
  def testStreamAppAction(): Unit = {
    val file = CommonUtils.createTempJar("empty_")
    val instances = 3
    val streamAppName = CommonUtils.randomString(5)
    // we should have only one worker cluster
    val wkName = result(wkApi.list).head.name

    // upload jar
    val streamJar = result(accessJar.request.group(wkName).upload(file))

    // create property
    val props = result(accessStream.request.name(streamAppName).jar(JarKey(streamJar.group, streamJar.name)).create())

    // update properties
    result(
      accessStream.request.name(streamAppName).from(Set("fromTopic")).to(Set("toTopic")).instances(instances).update())

    val res1 = result(accessStream.start(props.name))
    res1.name shouldBe props.name
    res1.name shouldBe streamAppName
    res1.from shouldBe Set("fromTopic")
    res1.to shouldBe Set("toTopic")
    res1.jar.name shouldBe streamJar.name
    res1.instances shouldBe instances
    res1.state.get shouldBe ContainerState.RUNNING.name

    val running = result(accessStream.get(props.name))
    running.state.get shouldBe ContainerState.RUNNING.name
    running.error.isEmpty shouldBe true

    // get the stream clusters information by clusterCache
    val cluster = result(configurator.clusterCollie.streamCollie.clusters())
    cluster.size shouldBe 1
    // jmx port should be positive
    cluster.head._1.jmxPort should not be 0

    // create the same streamApp cluster will get the previous stream cluster
    val prevRes = result(accessStream.start(props.name))
    prevRes.name shouldBe props.name
    prevRes.name shouldBe streamAppName
    prevRes.state.get shouldBe ContainerState.RUNNING.name
    prevRes.error.isDefined shouldBe false

    // running streamApp cannot update state
    an[RuntimeException] should be thrownBy result(accessStream.request.name(streamAppName).instances(10).update())

    // running streamApp cannot delete
    an[RuntimeException] should be thrownBy result(accessStream.delete(props.name))

    val res2 = result(accessStream.stop(props.name))
    res2.state shouldBe None
    res2.error shouldBe None

    // get the stream clusters information again, should be zero
    result(configurator.clusterCollie.streamCollie.clusters()).size shouldBe 0

    // stop the same streamApp cluster will only return the previous object
    val res3 = result(accessStream.stop(props.name))
    res3.state shouldBe None
    res3.error shouldBe None

    // get property will get the latest state (streamApp not exist)
    val latest = result(accessStream.get(props.name))
    latest.state shouldBe None
    latest.error.isDefined shouldBe false

    // after stop, streamApp can be deleted
    result(accessStream.delete(props.name))

    // after delete, streamApp should not exist
    an[IllegalArgumentException] should be thrownBy result(accessStream.get(props.name))

    file.deleteOnExit()
  }

  @Test
  def testStreamAppPropertyPageFailCases(): Unit = {
    val streamAppName = CommonUtils.requireNumberAndCharset(CommonUtils.randomString(5))
    //operations on non-exists property should be fail
    an[NullPointerException] should be thrownBy result(accessStream.request.name("appId").jar(null).update())
    an[IllegalArgumentException] should be thrownBy result(accessStream.get("non_exist_id"))

    an[IllegalArgumentException] should be thrownBy result(
      accessStream.request.name(streamAppName).from(Set.empty).update())

    an[IllegalArgumentException] should be thrownBy result(
      accessStream.request.name(streamAppName).to(Set.empty).update())

    an[IllegalArgumentException] should be thrownBy result(
      accessStream.request.name(streamAppName).instances(0).update())

    an[IllegalArgumentException] should be thrownBy result(
      accessStream.request.name(streamAppName).instances(-99).update())

    // delete non-exists object should do nothing
    result(accessStream.delete("non_exist_id"))
  }

  @Test
  def testStreamAppActionPageFailCases(): Unit = {
    val file = CommonUtils.createTempJar("empty_")
    val streamAppName = CommonUtils.randomString(5)
    val wkName = result(wkApi.list()).head.name

    // upload jar
    val streamJar = result(accessJar.request.group(wkName).upload(file))

    // start action will check all the required parameters
    result(accessStream.request.name(streamAppName).jar(JarKey(streamJar.group, streamJar.name)).create())
    an[IllegalArgumentException] should be thrownBy result(accessStream.start(streamAppName))

    result(accessStream.request.name(streamAppName).from(Set("from")).update())
    an[IllegalArgumentException] should be thrownBy result(accessStream.start(streamAppName))

    result(accessStream.request.name(streamAppName).to(Set("to")).update())
    // after all required parameters are set, it is ok to run
    result(accessStream.start(streamAppName))

    file.deleteOnExit()
  }

  @Test
  def duplicateDeleteStream(): Unit =
    (0 to 10).foreach(_ =>
      result(accessJar.request.group(CommonUtils.randomString()).delete(CommonUtils.randomString(5))))

  @Test
  def duplicateDeleteStreamProperty(): Unit =
    (0 to 10).foreach(_ => result(accessStream.delete(CommonUtils.randomString(5))))

  @Test
  def updateTags(): Unit = {
    val file = CommonUtils.createTempJar("empty_")

    val jar = result(accessJar.request.upload(file))

    val tags = Set(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val streamDesc = result(
      accessStream.request.name(CommonUtils.randomString(10)).jar(JarKey(jar.group, jar.name)).tags(tags).create())
    streamDesc.tags shouldBe tags

    val tags2 = Set(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val streamDesc2 = result(accessStream.request.name(streamDesc.name).tags(tags2).update())
    streamDesc2.tags shouldBe tags2

    val streamDesc3 = result(accessStream.request.name(streamDesc.name).update())
    streamDesc3.tags shouldBe tags2

    val streamDesc4 = result(accessStream.request.name(streamDesc.name).tags(Set.empty).update())
    streamDesc4.tags shouldBe Set.empty
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
