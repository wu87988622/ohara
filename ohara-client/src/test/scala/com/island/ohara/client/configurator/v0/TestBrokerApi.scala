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
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
class TestBrokerApi extends SmallTest with Matchers {

  @Test
  def testCloneNodeNames(): Unit = {
    val newNodeNames = Set(CommonUtils.randomString())
    val brokerClusterInfo = BrokerClusterInfo(
      name = CommonUtils.randomString(),
      imageName = CommonUtils.randomString(),
      zookeeperClusterName = CommonUtils.randomString(),
      clientPort = 10,
      exporterPort = 10,
      jmxPort = 10,
      nodeNames = Set.empty
    )
    brokerClusterInfo.clone(newNodeNames).nodeNames shouldBe newNodeNames
  }

  @Test
  def ignoreNameOnCreation(): Unit = an[NullPointerException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .nodeName(CommonUtils.randomString(10))
    .creation()

  @Test
  def ignoreNodeNamesOnCreation(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .creation()

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(null)

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name("")

  @Test
  def nullZookeeperClusterName(): Unit = an[NullPointerException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .zookeeperClusterName(null)

  @Test
  def emptyZookeeperClusterName(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .zookeeperClusterName("")

  @Test
  def nullImageName(): Unit = an[NullPointerException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .imageName(null)

  @Test
  def emptyImageName(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .imageName("")

  @Test
  def nullNodeNames(): Unit = an[NullPointerException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .nodeNames(null)

  @Test
  def emptyNodeNames(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .nodeNames(Set.empty)

  @Test
  def negativeClientPort(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .clientPort(-1)

  @Test
  def negativeJmxPort(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .jmxPort(-1)

  @Test
  def negativeExporterPort(): Unit = an[IllegalArgumentException] should be thrownBy BrokerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
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
    val creation = BrokerApi
      .access()
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request()
      .name(name)
      .zookeeperClusterName(zookeeperClusterName)
      .imageName(imageName)
      .clientPort(clientPort)
      .jmxPort(jmxPort)
      .exporterPort(exporterPort)
      .nodeName(nodeName)
      .creation()
    creation.name shouldBe name
    creation.imageName shouldBe imageName
    creation.clientPort shouldBe clientPort
    creation.jmxPort shouldBe jmxPort
    creation.exporterPort shouldBe exporterPort
    creation.zookeeperClusterName.get shouldBe zookeeperClusterName
    creation.nodeNames.head shouldBe nodeName
  }

  @Test
  def testJson(): Unit = {
    import spray.json._
    val name = CommonUtils.randomString(10)
    val nodeName = CommonUtils.randomString()
    val creation = BrokerApi.BROKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT.read(s"""
                                                                                       |  {
                                                                                       |    "name": "$name",
                                                                                       |    "nodeNames": ["$nodeName"]
                                                                                       |  }
                                                                     """.stripMargin.parseJson)
    creation.name shouldBe name
    creation.imageName shouldBe BrokerApi.IMAGE_NAME_DEFAULT
    creation.zookeeperClusterName shouldBe None
    creation.nodeNames.size shouldBe 1
    creation.nodeNames.head shouldBe nodeName
  }
}
