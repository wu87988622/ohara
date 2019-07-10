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
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json._
class TestZookeeperApi extends SmallTest with Matchers {

  @Test
  def testCloneNodeNames(): Unit = {
    val newNodeNames = Set(CommonUtils.randomString())
    val zookeeperClusterInfo = ZookeeperClusterInfo(
      name = CommonUtils.randomString(),
      imageName = CommonUtils.randomString(),
      clientPort = 10,
      peerPort = 10,
      electionPort = 10,
      nodeNames = Set.empty,
      deadNodes = Set.empty
    )
    zookeeperClusterInfo.clone(newNodeNames).nodeNames shouldBe newNodeNames
  }

  @Test
  def ignoreNameOnCreation(): Unit = ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .nodeName(CommonUtils.randomString(10))
    .creation
    .name
    .length should not be 0

  @Test
  def ignoreNodeNamesOnCreation(): Unit = an[IllegalArgumentException] should be thrownBy ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(CommonUtils.randomString())
    .creation

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(null)

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name("")

  @Test
  def nullImageName(): Unit = an[NullPointerException] should be thrownBy ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .imageName(null)

  @Test
  def emptyImageName(): Unit = an[IllegalArgumentException] should be thrownBy ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .imageName("")

  @Test
  def nullNodeNames(): Unit = an[NullPointerException] should be thrownBy ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .nodeNames(null)

  @Test
  def emptyNodeNames(): Unit = an[IllegalArgumentException] should be thrownBy ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .nodeNames(Set.empty)

  @Test
  def negativeClientPort(): Unit = an[IllegalArgumentException] should be thrownBy ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .clientPort(-1)

  @Test
  def negativeElectionPort(): Unit = an[IllegalArgumentException] should be thrownBy ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .electionPort(-1)

  @Test
  def negativePeerPort(): Unit = an[IllegalArgumentException] should be thrownBy ZookeeperApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .peerPort(-1)

  @Test
  def testCreation(): Unit = {
    val name = CommonUtils.randomString()
    val imageName = CommonUtils.randomString()
    val clientPort = CommonUtils.availablePort()
    val peerPort = CommonUtils.availablePort()
    val electionPort = CommonUtils.availablePort()
    val nodeName = CommonUtils.randomString()
    val creation = ZookeeperApi.access
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request
      .name(name)
      .imageName(imageName)
      .clientPort(clientPort)
      .peerPort(peerPort)
      .electionPort(electionPort)
      .nodeName(nodeName)
      .creation
    creation.name shouldBe name
    creation.imageName shouldBe imageName
    creation.clientPort shouldBe clientPort
    creation.peerPort shouldBe peerPort
    creation.electionPort shouldBe electionPort
    creation.nodeNames.head shouldBe nodeName
  }

  @Test
  def parseCreation(): Unit = {
    val nodeName = CommonUtils.randomString()
    val creation = ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
                                                                        |  {
                                                                        |    "nodeNames": ["$nodeName"]
                                                                        |  }
           """.stripMargin.parseJson)

    creation.name.length shouldBe 10
    creation.nodeNames.size shouldBe 1
    creation.nodeNames.head shouldBe nodeName
    creation.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
    creation.clientPort should not be 0
    creation.electionPort should not be 0
    creation.peerPort should not be 0

    val name = CommonUtils.randomString(10)
    val creation2 = ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "$name",
         |    "nodeNames": ["$nodeName"]
         |  }
           """.stripMargin.parseJson)

    creation2.name shouldBe name
    creation2.nodeNames.size shouldBe 1
    creation2.nodeNames.head shouldBe nodeName
    creation2.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
    creation2.clientPort should not be 0
    creation2.electionPort should not be 0
    creation2.peerPort should not be 0
  }

  @Test
  def parseEmptyNodeNames(): Unit =
    an[DeserializationException] should be thrownBy ZookeeperApi.ZOOKEEPER_CREATION_JSON_FORMAT.read(s"""
         |  {
         |    "name": "name"
         |  }
           """.stripMargin.parseJson)

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
}
