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
      jarIds = Seq.empty,
      jarUrls = Seq.empty,
      connectors = Seq.empty,
      nodeNames = Seq.empty
    )
    WORKER_CLUSTER_INFO_JSON_FORMAT.write(workerClusterInfo).toString().contains("jarNames") shouldBe true
  }

  @Test
  def noNull(): Unit = {
    val request = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(),
      imageName = None,
      brokerClusterName = None,
      clientPort = None,
      jmxPort = None,
      groupId = None,
      configTopicName = None,
      configTopicReplications = None,
      offsetTopicName = None,
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      statusTopicName = None,
      statusTopicPartitions = None,
      statusTopicReplications = None,
      jarIds = Seq.empty,
      nodeNames = Seq.empty
    )
    val string = WORKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT.write(request).toString()
    string.toLowerCase.contains("null") shouldBe false
  }

  @Test
  def testRequestEquals(): Unit = {
    val request = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(),
      imageName = None,
      brokerClusterName = None,
      clientPort = None,
      jmxPort = None,
      groupId = None,
      configTopicName = None,
      configTopicReplications = None,
      offsetTopicName = None,
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      statusTopicName = None,
      statusTopicPartitions = None,
      statusTopicReplications = None,
      jarIds = Seq.empty,
      nodeNames = Seq.empty
    )

    request shouldBe WORKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT.read(
      WORKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT.write(request))
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
      jarIds = Seq.empty,
      jarUrls = Seq.empty,
      connectors = Seq.empty,
      nodeNames = Seq.empty
    )

    response shouldBe WORKER_CLUSTER_INFO_JSON_FORMAT.read(WORKER_CLUSTER_INFO_JSON_FORMAT.write(response))
  }
}
