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

package com.island.ohara.client.configurator.v0

import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DefaultJsonProtocol._
import spray.json._
class TestZookeeperApi extends OharaTest with Matchers {

  private[this] final val access =
    ZookeeperApi.access.hostname(CommonUtils.randomString(5)).port(CommonUtils.availablePort()).request

  @Test
  def testClone(): Unit = {
    val nodeNames = Set(CommonUtils.randomString())
    val zookeeperClusterInfo = ZookeeperClusterInfo(
      settings = access.nodeNames(Set(CommonUtils.randomString())).creation.settings,
      aliveNodes = Set.empty,
      state = None,
      error = None,
      lastModified = CommonUtils.current()
    )
    zookeeperClusterInfo.newNodeNames(nodeNames).nodeNames shouldBe nodeNames
  }

  @Test
  def ignoreNameOnCreation(): Unit = access.nodeName(CommonUtils.randomString(10)).creation.name.length should not be 0

  @Test
  def testTags(): Unit = access
    .nodeName(CommonUtils.randomString(10))
    .tags(Map("a" -> JsNumber(1), "b" -> JsString("2")))
    .creation
    .tags
    .size shouldBe 2

  @Test
  def ignoreNodeNamesOnCreation(): Unit =
    an[DeserializationException] should be thrownBy access.name(CommonUtils.randomString(10)).creation

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy access.name(null)

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy access.name("")

  @Test
  def nullGroup(): Unit = an[NullPointerException] should be thrownBy access.group(null)

  @Test
  def emptyGroup(): Unit = an[IllegalArgumentException] should be thrownBy access.group("")

  @Test
  def nullImageName(): Unit = an[NullPointerException] should be thrownBy access.imageName(null)

  @Test
  def emptyImageName(): Unit = an[IllegalArgumentException] should be thrownBy access.imageName("")

  @Test
  def nullNodeNames(): Unit = {
    an[NullPointerException] should be thrownBy access.nodeNames(null)
    an[IllegalArgumentException] should be thrownBy access.nodeNames(Set.empty)
  }

  @Test
  def emptyNodeNames(): Unit = an[IllegalArgumentException] should be thrownBy access.nodeNames(Set.empty)

  @Test
  def negativeClientPort(): Unit = an[IllegalArgumentException] should be thrownBy access.clientPort(-1)

  @Test
  def negativeElectionPort(): Unit = an[IllegalArgumentException] should be thrownBy access.electionPort(-1)

  @Test
  def negativePeerPort(): Unit = an[IllegalArgumentException] should be thrownBy access.peerPort(-1)

  @Test
  def testCreation(): Unit = {
    val name = CommonUtils.randomString(10)
    val group = CommonUtils.randomString(10)
    val imageName = CommonUtils.randomString()
    val clientPort = CommonUtils.availablePort()
    val peerPort = CommonUtils.availablePort()
    val electionPort = CommonUtils.availablePort()
    val nodeName = CommonUtils.randomString()
    val creation = access
      .name(name)
      .group(group)
      .imageName(imageName)
      .clientPort(clientPort)
      .peerPort(peerPort)
      .electionPort(electionPort)
      .nodeName(nodeName)
      .creation
    creation.name shouldBe name
    creation.group shouldBe group
    creation.imageName shouldBe imageName
    creation.clientPort shouldBe clientPort
    creation.peerPort shouldBe peerPort
    creation.electionPort shouldBe electionPort
    creation.nodeNames.head shouldBe nodeName
  }

  @Test
  def testExtraSettingInCreation(): Unit = {
    val name = CommonUtils.randomString(10)
    val name2 = JsString(CommonUtils.randomString(10))
    val creation = access.name(name).nodeNames(Set("n1")).settings(Map("name" -> name2)).creation

    // settings() has higher priority than name()
    creation.name shouldBe name2.value
  }

  @Test
  def parseCreation(): Unit = {
    val nodeName = "n1"
    val creation = ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": ["$nodeName"]
      |  }
      """.stripMargin.parseJson)

    creation.group shouldBe GROUP_DEFAULT
    creation.name.length shouldBe LIMIT_OF_KEY_LENGTH / 2
    creation.nodeNames.size shouldBe 1
    creation.nodeNames.head shouldBe nodeName
    creation.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
    creation.clientPort should not be 0
    creation.electionPort should not be 0
    creation.peerPort should not be 0

    val name = CommonUtils.randomString(10)
    val group = CommonUtils.randomString(10)
    val creation2 = ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "group": "$group",
      |    "name": "$name",
      |    "nodeNames": ["$nodeName"]
      |  }
      """.stripMargin.parseJson)
    // group is support in create cluster
    creation2.group shouldBe group
    creation2.name shouldBe name
    creation2.nodeNames.size shouldBe 1
    creation2.nodeNames.head shouldBe nodeName
    creation2.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
    creation2.clientPort should not be 0
    creation2.electionPort should not be 0
    creation2.peerPort should not be 0
  }

  @Test
  def testUpdate(): Unit = {
    val name = CommonUtils.randomString(10)
    val group = CommonUtils.randomString(10)
    val imageName = CommonUtils.randomString()
    val clientPort = CommonUtils.availablePort()
    val nodeName = CommonUtils.randomString()

    val creation = access.name(name).nodeName(nodeName).creation
    creation.name shouldBe name
    // use default values if absent
    creation.group shouldBe GROUP_DEFAULT
    creation.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
    creation.nodeNames shouldBe Set(nodeName)

    // initial a new update request
    val updateAsCreation = ZookeeperApi.access.request
      .name(name)
      // the group here is not as same as before
      // here we use update as creation
      .group(group)
      .imageName(imageName)
      .clientPort(clientPort)
      .updating
    updateAsCreation.imageName shouldBe Some(imageName)
    updateAsCreation.clientPort shouldBe Some(clientPort)
    updateAsCreation.nodeNames should not be Some(Set(nodeName))
  }

  @Test
  def testDefaultName(): Unit = ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": ["n1"]
      |  }
      """.stripMargin.parseJson).name.nonEmpty shouldBe true

  @Test
  def parseNameField(): Unit = {
    val thrown2 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": [
      |      "node"
      |    ],
      |    "name": ""
      |  }
      |  """.stripMargin.parseJson)
    thrown2.getMessage should include("the value of \"name\" can't be empty string")
  }

  @Test
  def parseImageNameField(): Unit = {
    val thrown2 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": [
      |      "node"
      |    ],
      |    "imageName": ""
      |  }
      |  """.stripMargin.parseJson)
    thrown2.getMessage should include("the value of \"imageName\" can't be empty string")
  }

  @Test
  def parseImageNameOnUpdate(): Unit = {
    val thrown = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
          |  {
          |    "imageName": ""
          |  }
           """.stripMargin.parseJson)
    thrown.getMessage should include("the value of \"imageName\" can't be empty string")
  }

  @Test
  def parseEmptyNodeNames(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name",
         |    "nodeNames": []
         |  }
           """.stripMargin.parseJson)

  @Test
  def parseNodeNamesOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
           |  {
           |    "nodeNames": ""
           |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the value of \"nodeNames\" can't be empty string")
  }

  @Test
  def parseZeroClientPort(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name",
         |    "clientPort": 0,
         |    "nodeNames": ["n"]
         |  }
           """.stripMargin.parseJson)

  @Test
  def parseNegativeClientPort(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name",
         |    "clientPort": -1,
         |    "nodeNames": ["n"]
         |  }
           """.stripMargin.parseJson)

  @Test
  def parseLargeClientPort(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name",
         |    "clientPort": 999999,
         |    "nodeNames": ["n"]
         |  }
           """.stripMargin.parseJson)

  @Test
  def parseClientPortOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
           |  {
           |    "clientPort": 0
           |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the connection port must be [1024, 65535)")

    val thrown2 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
           |  {
           |    "clientPort": -9
           |  }
           """.stripMargin.parseJson)
    thrown2.getMessage should include("the connection port must be [1024, 65535)")

    val thrown3 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
           |  {
           |    "clientPort": 99999
           |  }
           """.stripMargin.parseJson)
    thrown3.getMessage should include("the connection port must be [1024, 65535), but actual port is \"99999\"")
  }

  @Test
  def parseZeroElectionPort(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name",
         |    "electionPort": 0,
         |    "nodeNames": ["n"]
         |  }
           """.stripMargin.parseJson)

  @Test
  def parseNegativeElectionPort(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name",
         |    "electionPort": -1,
         |    "nodeNames": ["n"]
         |  }
           """.stripMargin.parseJson)

  @Test
  def parseLargeElectionPort(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name",
         |    "electionPort": 999999,
         |    "nodeNames": ["n"]
         |  }
           """.stripMargin.parseJson)

  @Test
  def parseElectionPortOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
           |  {
           |    "electionPort": 0
           |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the connection port must be [1024, 65535)")

    val thrown2 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
           |  {
           |    "electionPort": -9
           |  }
           """.stripMargin.parseJson)
    thrown2.getMessage should include("the connection port must be [1024, 65535)")

    val thrown3 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
           |  {
           |    "electionPort": 99999
           |  }
           """.stripMargin.parseJson)
    thrown3.getMessage should include("the connection port must be [1024, 65535), but actual port is \"99999\"")
  }

  @Test
  def parseZeroPeerPort(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name",
         |    "peerPort": 0,
         |    "nodeNames": ["n"]
         |  }
           """.stripMargin.parseJson)

  @Test
  def parseNegativePeerPort(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name",
         |    "peerPort": -1,
         |    "nodeNames": ["n"]
         |  }
           """.stripMargin.parseJson)

  @Test
  def parseLargePeerPort(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name",
         |    "peerPort": 999999,
         |    "nodeNames": ["n"]
         |  }
           """.stripMargin.parseJson)

  @Test
  def parsePeerPortOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
           |  {
           |    "peerPort": 0
           |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the connection port must be [1024, 65535)")

    val thrown2 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
           |  {
           |    "peerPort": -9
           |  }
           """.stripMargin.parseJson)
    thrown2.getMessage should include("the connection port must be [1024, 65535)")

    val thrown3 = the[DeserializationException] thrownBy ZookeeperApi.ZOOKEEPER_UPDATING_JSON_FORMAT.read(s"""
           |  {
           |    "peerPort": 99999
           |  }
           """.stripMargin.parseJson)
    thrown3.getMessage should include("the connection port must be [1024, 65535), but actual port is \"99999\"")
  }

  @Test
  def testInvalidNodeNames(): Unit = {
    an[DeserializationException] should be thrownBy access.nodeName("start").creation
    an[DeserializationException] should be thrownBy access.nodeName("stop").creation
    an[DeserializationException] should be thrownBy access.nodeName("start").updating
    an[DeserializationException] should be thrownBy access.nodeName("stop").updating

    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": ["start", "stop"]
      |  }
      """.stripMargin.parseJson)
  }

  @Test
  def testDefaultUpdate(): Unit = {
    val name = CommonUtils.randomString(10)
    val data = access.name(name).updating
    data.imageName.isEmpty shouldBe true
    data.peerPort.isEmpty shouldBe true
    data.electionPort.isEmpty shouldBe true
    data.clientPort.isEmpty shouldBe true
    data.nodeNames.isEmpty shouldBe true
  }

  @Test
  def groupShouldAppearInResponse(): Unit = {
    val name = CommonUtils.randomString(5)
    val res = ZookeeperApi.ZOOKEEPER_CLUSTER_INFO_JSON_FORMAT.write(
      ZookeeperClusterInfo(
        settings = ZookeeperApi.access.request.name(name).nodeNames(Set("n1")).creation.settings,
        aliveNodes = Set.empty,
        state = None,
        error = None,
        lastModified = CommonUtils.current()
      ))
    // serialize to json should see the object key (group, name) in "settings"
    res.asJsObject.fields("settings").asJsObject.fields(NAME_KEY).convertTo[String] shouldBe name
    res.asJsObject.fields("settings").asJsObject.fields(GROUP_KEY).convertTo[String] shouldBe GROUP_DEFAULT

    // // deserialize to info should see the object key (group, name)
    val data = ZookeeperApi.ZOOKEEPER_CLUSTER_INFO_JSON_FORMAT.read(res)
    data.name shouldBe name
    data.group shouldBe GROUP_DEFAULT
  }

  @Test
  def testTagsOnUpdate(): Unit = access.updating.tags shouldBe None

  @Test
  def testOverwriteSettings(): Unit = {
    val r1 =
      access.nodeName("n1").clientPort(12345).peerPort(45678).creation

    val r2 = access.nodeName("n1").clientPort(12345).settings(Map("name" -> JsString("fake"))).creation

    r1.nodeNames shouldBe r2.nodeNames
    r1.clientPort shouldBe r2.clientPort
    // settings will overwrite default value
    r1.name should not be r2.name
  }

  @Test
  def testDeadNodes(): Unit = {
    val cluster = ZookeeperClusterInfo(
      settings = ZookeeperApi.access.request.nodeNames(Set("n0", "n1")).creation.settings,
      aliveNodes = Set("n0"),
      state = Some("running"),
      error = None,
      lastModified = CommonUtils.current()
    )
    cluster.nodeNames shouldBe Set("n0", "n1")
    cluster.deadNodes shouldBe Set("n1")
    cluster.copy(state = None).deadNodes shouldBe Set.empty
  }

  @Test
  def defaultValueShouldBeAppendedToResponse(): Unit = {
    val cluster = ZookeeperClusterInfo(
      settings = ZookeeperApi.access.request.nodeNames(Set("n0", "n1")).creation.settings,
      aliveNodes = Set("n0"),
      state = Some("running"),
      error = None,
      lastModified = CommonUtils.current()
    )

    val string = ZookeeperApi.ZOOKEEPER_CLUSTER_INFO_JSON_FORMAT.write(cluster).toString()

    ZookeeperApi.DEFINITIONS.filter(_.defaultValue() != null).foreach { definition =>
      string should include(definition.key())
      string should include(definition.defaultValue())
    }
  }

  @Test
  def checkNameDefinition(): Unit = ZookeeperApi.DEFINITIONS.find(_.key() == NAME_KEY) should not be None

  @Test
  def checkGroupDefinition(): Unit = ZookeeperApi.DEFINITIONS.find(_.key() == GROUP_KEY) should not be None

  @Test
  def checkNodeNamesDefinition(): Unit = ZookeeperApi.DEFINITIONS.find(_.key() == NODE_NAMES_KEY) should not be None

  @Test
  def checkTagDefinition(): Unit = ZookeeperApi.DEFINITIONS.find(_.key() == TAGS_KEY) should not be None

  @Test
  def checkClientPortDefinition(): Unit = ZookeeperApi.DEFINITIONS.find(_.key() == CLIENT_PORT_KEY) should not be None
}
