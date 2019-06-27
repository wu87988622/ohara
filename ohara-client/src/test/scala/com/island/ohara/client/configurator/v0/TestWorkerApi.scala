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

import com.island.ohara.client.configurator.v0.WorkerApi._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.{JsString, _}

class TestWorkerApi extends SmallTest with Matchers {

  @Test
  def testStaleCreationApis(): Unit = {
    val name = CommonUtils.randomString()
    val brokerClusterName = CommonUtils.randomString()
    val nodeName = CommonUtils.randomString()
    val jarId = CommonUtils.randomString()
    val request = WORKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT.read(s"""
                                               |{
                                               |  "name": ${JsString(name).toString()},
                                               |  "brokerClusterName": ${JsString(brokerClusterName).toString()},
                                               |  "nodeNames": ${JsArray(Vector(JsString(nodeName))).toString()},
                                               |  "jars": ${JsArray(Vector(JsString(jarId))).toString()}
                                               |}
                                            """.stripMargin.parseJson)
    request.name shouldBe name
    request.brokerClusterName.get shouldBe brokerClusterName
    request.nodeNames.head shouldBe nodeName
    request.jarIds.head shouldBe jarId
  }

  @Test
  def seeStaleJarNames(): Unit = {
    val workerClusterInfo = WorkerClusterInfo(
      name = CommonUtils.randomString(),
      imageName = CommonUtils.randomString(),
      brokerClusterName = CommonUtils.randomString(),
      clientPort = 10,
      jmxPort = 10,
      groupId = CommonUtils.randomString(),
      statusTopicName = CommonUtils.randomString(),
      statusTopicPartitions = 10,
      statusTopicReplications = 10,
      configTopicName = CommonUtils.randomString(),
      configTopicPartitions = 10,
      configTopicReplications = 10,
      offsetTopicName = CommonUtils.randomString(),
      offsetTopicPartitions = 10,
      offsetTopicReplications = 10,
      jarInfos = Seq.empty,
      connectors = Seq.empty,
      nodeNames = Set.empty,
      deadNodes = Set.empty
    )
    WORKER_CLUSTER_INFO_JSON_FORMAT.write(workerClusterInfo).toString().contains("jarNames") shouldBe true
  }

  @Test
  def testResponseEquals(): Unit = {
    val response = WorkerClusterInfo(
      name = CommonUtils.randomString(),
      imageName = CommonUtils.randomString(),
      brokerClusterName = CommonUtils.randomString(),
      clientPort = 10,
      jmxPort = 10,
      groupId = CommonUtils.randomString(),
      statusTopicName = CommonUtils.randomString(),
      statusTopicPartitions = 10,
      statusTopicReplications = 10,
      configTopicName = CommonUtils.randomString(),
      configTopicPartitions = 10,
      configTopicReplications = 10,
      offsetTopicName = CommonUtils.randomString(),
      offsetTopicPartitions = 10,
      offsetTopicReplications = 10,
      jarInfos = Seq.empty,
      connectors = Seq.empty,
      nodeNames = Set.empty,
      deadNodes = Set.empty
    )

    response shouldBe WORKER_CLUSTER_INFO_JSON_FORMAT.read(WORKER_CLUSTER_INFO_JSON_FORMAT.write(response))
  }

  @Test
  def testCloneNodeNames(): Unit = {
    val newNodeNames = Set(CommonUtils.randomString())
    val workerClusterInfo = WorkerClusterInfo(
      name = CommonUtils.randomString(),
      imageName = CommonUtils.randomString(),
      brokerClusterName = CommonUtils.randomString(),
      clientPort = 10,
      jmxPort = 10,
      groupId = CommonUtils.randomString(),
      statusTopicName = CommonUtils.randomString(),
      statusTopicPartitions = 10,
      statusTopicReplications = 10,
      configTopicName = CommonUtils.randomString(),
      configTopicPartitions = 10,
      configTopicReplications = 10,
      offsetTopicName = CommonUtils.randomString(),
      offsetTopicPartitions = 10,
      offsetTopicReplications = 10,
      jarInfos = Seq.empty,
      connectors = Seq.empty,
      nodeNames = Set.empty,
      deadNodes = Set.empty
    )
    workerClusterInfo.clone(newNodeNames).nodeNames shouldBe newNodeNames
  }

  @Test
  def ignoreNameOnCreation(): Unit = an[NullPointerException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .nodeName(CommonUtils.randomString(10))
    .creation()

  @Test
  def ignoreNodeNamesOnCreation(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .creation()

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(null)

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name("")

  @Test
  def nullBrokerClusterName(): Unit = an[NullPointerException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .brokerClusterName(null)

  @Test
  def emptyBrokerClusterName(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .brokerClusterName("")

  @Test
  def nullImageName(): Unit = an[NullPointerException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .imageName(null)

  @Test
  def emptyImageName(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .imageName("")

  @Test
  def nullNodeNames(): Unit = an[NullPointerException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .nodeNames(null)

  @Test
  def emptyNodeNames(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .nodeNames(Set.empty)

  @Test
  def negativeClientPort(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .clientPort(-1)

  @Test
  def negativeJmxPort(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .jmxPort(-1)

  @Test
  def nullConfigTopicName(): Unit = an[NullPointerException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .configTopicName(null)

  @Test
  def emptyConfigTopicName(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .configTopicName("")

  @Test
  def negativeNumberOfConfigTopicReplication(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .configTopicReplications(-1)

  @Test
  def nullOffsetTopicName(): Unit = an[NullPointerException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .offsetTopicName(null)

  @Test
  def emptyOffsetTopicName(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .offsetTopicName("")

  @Test
  def negativeNumberOfOffsetTopicPartitions(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .offsetTopicPartitions(-1)

  @Test
  def negativeNumberOfOffsetTopicReplication(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .offsetTopicReplications(-1)

  @Test
  def nullStatusTopicName(): Unit = an[NullPointerException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .statusTopicName(null)

  @Test
  def emptyStatusTopicName(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .statusTopicName("")

  @Test
  def negativeNumberOfStatusTopicPartitions(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .statusTopicPartitions(-1)

  @Test
  def negativeNumberOfStatusTopicReplication(): Unit = an[IllegalArgumentException] should be thrownBy WorkerApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .statusTopicReplications(-1)

  @Test
  def testCreation(): Unit = {
    val name = CommonUtils.randomString()
    val imageName = CommonUtils.randomString()
    val clientPort = CommonUtils.availablePort()
    val jmxPort = CommonUtils.availablePort()
    val brokerClusterName = CommonUtils.randomString()
    val configTopicName = CommonUtils.randomString(10)
    val configTopicReplications: Short = 2
    val offsetTopicName = CommonUtils.randomString(10)
    val offsetTopicPartitions: Int = 2
    val offsetTopicReplications: Short = 2
    val statusTopicName = CommonUtils.randomString(10)
    val statusTopicPartitions: Int = 2
    val statusTopicReplications: Short = 2
    val nodeName = CommonUtils.randomString()
    val creation = WorkerApi
      .access()
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request()
      .name(name)
      .brokerClusterName(brokerClusterName)
      .configTopicName(configTopicName)
      .configTopicReplications(configTopicReplications)
      .offsetTopicName(offsetTopicName)
      .offsetTopicPartitions(offsetTopicPartitions)
      .offsetTopicReplications(offsetTopicReplications)
      .statusTopicName(statusTopicName)
      .statusTopicPartitions(statusTopicPartitions)
      .statusTopicReplications(statusTopicReplications)
      .imageName(imageName)
      .clientPort(clientPort)
      .jmxPort(jmxPort)
      .nodeName(nodeName)
      .creation()
    creation.name shouldBe name
    creation.imageName shouldBe imageName
    creation.clientPort shouldBe clientPort
    creation.jmxPort shouldBe jmxPort
    creation.brokerClusterName.get shouldBe brokerClusterName
    creation.configTopicName shouldBe configTopicName
    creation.configTopicReplications shouldBe configTopicReplications
    creation.offsetTopicName shouldBe offsetTopicName
    creation.offsetTopicPartitions shouldBe offsetTopicPartitions
    creation.offsetTopicReplications shouldBe offsetTopicReplications
    creation.statusTopicName shouldBe statusTopicName
    creation.statusTopicPartitions shouldBe statusTopicPartitions
    creation.statusTopicReplications shouldBe statusTopicReplications
    creation.nodeNames.head shouldBe nodeName
  }

  @Test
  def testJson(): Unit = {
    import spray.json._
    val name = CommonUtils.randomString(10)
    val nodeName = CommonUtils.randomString()
    val creation = WorkerApi.WORKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT.read(s"""
                                                                                 |  {
                                                                                 |    "name": "$name",
                                                                                 |    "nodeNames": ["$nodeName"]
                                                                                 |  }
                                                                     """.stripMargin.parseJson)
    creation.name shouldBe name
    creation.imageName shouldBe WorkerApi.IMAGE_NAME_DEFAULT
    creation.brokerClusterName shouldBe None
    creation.configTopicReplications shouldBe 1
    creation.offsetTopicReplications shouldBe 1
    creation.offsetTopicPartitions shouldBe 1
    creation.statusTopicReplications shouldBe 1
    creation.statusTopicPartitions shouldBe 1
    creation.nodeNames.size shouldBe 1
    creation.nodeNames.head shouldBe nodeName
    creation.jarIds.size shouldBe 0
  }
}
