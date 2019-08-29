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

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.{DeserializationException, _}

class TestBrokerApi extends SmallTest with Matchers {

  @Test
  def testClone(): Unit = {
    val nodeNames = Set(CommonUtils.randomString())
    val deadNodes = Set(CommonUtils.randomString())
    val error = Some(CommonUtils.randomString())
    val state = Some(CommonUtils.randomString())
    val brokerClusterInfo = BrokerClusterInfo(
      name = CommonUtils.randomString(),
      imageName = CommonUtils.randomString(),
      zookeeperClusterName = CommonUtils.randomString(),
      clientPort = 10,
      exporterPort = 10,
      jmxPort = 10,
      nodeNames = Set.empty,
      deadNodes = Set.empty,
      state = None,
      error = None,
      tags = Map.empty,
      lastModified = CommonUtils.current(),
      topicSettingDefinitions = Seq.empty
    )
    val newOne = brokerClusterInfo.clone(
      nodeNames = nodeNames,
      deadNodes = deadNodes,
      error = error,
      state = state,
      metrics = Metrics.EMPTY,
      tags = Map.empty
    )
    newOne.nodeNames shouldBe nodeNames
    newOne.deadNodes shouldBe deadNodes
    newOne.error shouldBe error
    newOne.state shouldBe state
  }

  @Test
  def ignoreNameOnCreation(): Unit = BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .nodeName(CommonUtils.randomString(10))
    .creation
    .name
    .length should not be 0

  @Test
  def testTags(): Unit = BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .nodeName(CommonUtils.randomString(10))
    .tags(Map("a" -> JsNumber(1), "b" -> JsString("2")))
    .creation
    .tags
    .size shouldBe 2

  @Test
  def ignoreNodeNamesOnCreation(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(CommonUtils.randomString())
    .creation

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(null)

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name("")

  @Test
  def nullZookeeperClusterName(): Unit = an[NullPointerException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .zookeeperClusterName(null)

  @Test
  def emptyZookeeperClusterName(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .zookeeperClusterName("")

  @Test
  def nullImageName(): Unit = an[NullPointerException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .imageName(null)

  @Test
  def emptyImageName(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .imageName("")

  @Test
  def nullNodeNames(): Unit = an[NullPointerException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .nodeNames(null)

  @Test
  def emptyNodeNames(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .nodeNames(Set.empty)

  @Test
  def negativeClientPort(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .clientPort(-1)

  @Test
  def negativeJmxPort(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .jmxPort(-1)

  @Test
  def negativeExporterPort(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .exporterPort(-1)

  @Test
  def testCreation(): Unit = {
    val name = CommonUtils.randomString()
    val imageName = CommonUtils.randomString()
    val clientPort = CommonUtils.availablePort()
    val jmxPort = CommonUtils.availablePort()
    val exporterPort = CommonUtils.availablePort()
    val zookeeperClusterName = CommonUtils.randomString()
    val nodeName = CommonUtils.randomString()
    val creation = BrokerApi.access
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request
      .name(name)
      .zookeeperClusterName(zookeeperClusterName)
      .imageName(imageName)
      .clientPort(clientPort)
      .jmxPort(jmxPort)
      .exporterPort(exporterPort)
      .nodeName(nodeName)
      .creation
    creation.name shouldBe name
    creation.imageName shouldBe imageName
    creation.clientPort shouldBe clientPort
    creation.jmxPort shouldBe jmxPort
    creation.exporterPort shouldBe exporterPort
    creation.zookeeperClusterName.get shouldBe zookeeperClusterName
    creation.nodeNames.head shouldBe nodeName
  }

  @Test
  def parseCreation(): Unit = {
    val nodeName = CommonUtils.randomString()
    val creation = BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                       |  {
                                                                                       |    "nodeNames": ["$nodeName"]
                                                                                       |  }
                                                                     """.stripMargin.parseJson)
    creation.group shouldBe BrokerApi.GROUP_DEFAULT
    creation.name.length shouldBe 10
    creation.imageName shouldBe BrokerApi.IMAGE_NAME_DEFAULT
    creation.zookeeperClusterName shouldBe None
    creation.nodeNames.size shouldBe 1
    creation.nodeNames.head shouldBe nodeName
    creation.clientPort should not be 0
    creation.jmxPort should not be 0
    creation.exporterPort should not be 0
    creation.ports.size shouldBe 3

    val name = CommonUtils.randomString(10)
    val zookeeperClusterName = CommonUtils.randomString()
    val clientPort = CommonUtils.availablePort()
    val exporterPort = CommonUtils.availablePort()
    val jmxPort = CommonUtils.availablePort()
    val creation2 = BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                 |  {
                                                                                 |    "name": "$name",
                                                                                 |    "clientPort": $clientPort,
                                                                                 |    "exporterPort": $exporterPort,
                                                                                 |    "jmxPort": $jmxPort,
                                                                                 |    "zookeeperClusterName": "$zookeeperClusterName",
                                                                                 |    "nodeNames": ["$nodeName"]
                                                                                 |  }
                                                                     """.stripMargin.parseJson)
    // node does support custom group
    creation2.group shouldBe BrokerApi.GROUP_DEFAULT
    creation2.name shouldBe name
    creation2.imageName shouldBe BrokerApi.IMAGE_NAME_DEFAULT
    creation2.nodeNames.size shouldBe 1
    creation2.nodeNames.head shouldBe nodeName
    creation2.zookeeperClusterName.get shouldBe zookeeperClusterName
    creation2.clientPort shouldBe clientPort
    creation2.exporterPort shouldBe exporterPort
    creation2.jmxPort shouldBe jmxPort

    val creation3 = BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                |  {
                                                                |    "name": "$name",
                                                                |    "nodeNames": ["$nodeName"]
                                                                |  }
           """.stripMargin.parseJson)

    creation3.name shouldBe name
    creation3.nodeNames.size shouldBe 1
    creation3.nodeNames.head shouldBe nodeName
    creation3.imageName shouldBe BrokerApi.IMAGE_NAME_DEFAULT
    creation3.clientPort should not be 0
    creation3.exporterPort should not be 0
    creation3.jmxPort should not be 0
  }

  @Test
  def testEmptyNodeNames(): Unit =
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
     |  {
     |    "name": "name",
     |    "nodeNames": []
     |  }
      """.stripMargin.parseJson)

  @Test
  def parseNodeNamesOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy BrokerApi.BROKER_UPDATE_JSON_FORMAT.read(s"""
                                                                                                   |  {
                                                                                                   |    "nodeNames": ""
                                                                                                   |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the value of \"nodeNames\" can't be empty string")
  }

  @Test
  def parseZeroClientPort(): Unit =
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "name": "name",
                                                                                                |    "clientPort": 0,
                                                                                                |    "nodeNames": ["n"]
                                                                                                |  }
           """.stripMargin.parseJson)

  @Test
  def parseNegativeClientPort(): Unit =
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "name": "name",
                                                                                                |    "clientPort": -1,
                                                                                                |    "nodeNames": ["n"]
                                                                                                |  }
           """.stripMargin.parseJson)

  @Test
  def parseLargeClientPort(): Unit =
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "name": "name",
                                                                                                |    "clientPort": 999999,
                                                                                                |    "nodeNames": ["n"]
                                                                                                |  }
           """.stripMargin.parseJson)

  @Test
  def parseClientPortOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                     |  {
                                                                                                     |    "clientPort": 0
                                                                                                     |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the connection port must be [1024, 65535)")

    val thrown2 = the[DeserializationException] thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                     |  {
                                                                                                     |    "clientPort": -9
                                                                                                     |  }
           """.stripMargin.parseJson)
    thrown2.getMessage should include("the connection port must be [1024, 65535)")

    val thrown3 = the[DeserializationException] thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                     |  {
                                                                                                     |    "clientPort": 99999
                                                                                                     |  }
           """.stripMargin.parseJson)
    thrown3.getMessage should include("the connection port must be [1024, 65535), but actual port is \"99999\"")
  }

  @Test
  def parseZeroExporterPort(): Unit =
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "name": "name",
                                                                                                  |    "exporterPort": 0,
                                                                                                  |    "nodeNames": ["n"]
                                                                                                  |  }
           """.stripMargin.parseJson)

  @Test
  def parseNegativeExporterPort(): Unit =
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "name": "name",
                                                                                                  |    "exporterPort": -1,
                                                                                                  |    "nodeNames": ["n"]
                                                                                                  |  }
           """.stripMargin.parseJson)

  @Test
  def parseLargeExporterPort(): Unit =
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "name": "name",
                                                                                                  |    "exporterPort": 999999,
                                                                                                  |    "nodeNames": ["n"]
                                                                                                  |  }
           """.stripMargin.parseJson)

  @Test
  def parseExporterPortOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                       |  {
                                                                                                       |    "exporterPort": 0
                                                                                                       |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the connection port must be [1024, 65535)")

    val thrown2 = the[DeserializationException] thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                       |  {
                                                                                                       |    "exporterPort": -9
                                                                                                       |  }
           """.stripMargin.parseJson)
    thrown2.getMessage should include("the connection port must be [1024, 65535)")

    val thrown3 = the[DeserializationException] thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                       |  {
                                                                                                       |    "exporterPort": 99999
                                                                                                       |  }
           """.stripMargin.parseJson)
    thrown3.getMessage should include("the connection port must be [1024, 65535), but actual port is \"99999\"")
  }

  @Test
  def parseZeroJmxPort(): Unit =
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "name": "name",
                                                                                                  |    "jmxPort": 0,
                                                                                                  |    "nodeNames": ["n"]
                                                                                                  |  }
           """.stripMargin.parseJson)

  @Test
  def parseNegativeJmxPort(): Unit =
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "name": "name",
                                                                                                  |    "jmxPort": -1,
                                                                                                  |    "nodeNames": ["n"]
                                                                                                  |  }
           """.stripMargin.parseJson)

  @Test
  def parseLargeJmxPort(): Unit =
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "name": "name",
                                                                                                  |    "jmxPort": 999999,
                                                                                                  |    "nodeNames": ["n"]
                                                                                                  |  }
           """.stripMargin.parseJson)

  @Test
  def parseJmxPortOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                       |  {
                                                                                                       |    "jmxPort": 0
                                                                                                       |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the connection port must be [1024, 65535)")

    val thrown2 = the[DeserializationException] thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                       |  {
                                                                                                       |    "jmxPort": -9
                                                                                                       |  }
           """.stripMargin.parseJson)
    thrown2.getMessage should include("the connection port must be [1024, 65535)")

    val thrown3 = the[DeserializationException] thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                       |  {
                                                                                                       |    "jmxPort": 99999
                                                                                                       |  }
           """.stripMargin.parseJson)
    thrown3.getMessage should include("the connection port must be [1024, 65535), but actual port is \"99999\"")
  }

  @Test
  def testInvalidNodeNames(): Unit = {
    an[DeserializationException] should be thrownBy BrokerApi.access
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request
      .nodeName("start")
      .creation
    an[DeserializationException] should be thrownBy BrokerApi.access
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request
      .nodeName("stop")
      .creation
    an[DeserializationException] should be thrownBy BrokerApi.access
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request
      .nodeName("start")
      .update
    an[DeserializationException] should be thrownBy BrokerApi.access
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request
      .nodeName("stop")
      .update

    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "nodeNames": ["start", "stop"]
                                                                                                  |  }
           """.stripMargin.parseJson)
  }

  @Test
  def testDefaultUpdate(): Unit = {
    val data = BrokerApi.access.hostname(CommonUtils.randomString()).port(CommonUtils.availablePort()).request.update
    data.imageName.isEmpty shouldBe true
    data.zookeeperClusterName.isEmpty shouldBe true
    data.exporterPort.isEmpty shouldBe true
    data.jmxPort.isEmpty shouldBe true
    data.clientPort.isEmpty shouldBe true
    data.nodeNames.isEmpty shouldBe true
  }

  @Test
  def testEmptyString(): Unit = {
    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                                  |  {
                                                                                                                  |    "name": "",
                                                                                                                  |    "nodeNames": ["a0"]
                                                                                                                  |  }
      """.stripMargin.parseJson)

    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                                  |  {
                                                                                                                  |    "name": "name",
                                                                                                                  |    "zookeeperClusterName": "",
                                                                                                                  |    "nodeNames": ["a0"]
                                                                                                                  |  }
      """.stripMargin.parseJson)

    an[DeserializationException] should be thrownBy BrokerApi.BROKER_CREATION_JSON_FORMAT.read(s"""
                                                                                                                  |  {
                                                                                                                  |    "name": "name",
                                                                                                                  |    "imageName": "",
                                                                                                                  |    "nodeNames": ["a0"]
                                                                                                                  |  }
      """.stripMargin.parseJson)
  }
}
