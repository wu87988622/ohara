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

package com.island.ohara.agent

import java.net.URL

import com.island.ohara.client.configurator.v0.JarApi.{JAR_INFO_JSON_FORMAT, JarInfo}
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.JsArray

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
class TestWorkerCreator extends SmallTest with Matchers {

  private[this] def wkCreator(): WorkerCollie.ClusterCreator = (executionContext,
                                                                clusterName,
                                                                imageName,
                                                                brokerClusterName,
                                                                clientPort,
                                                                jmxPort,
                                                                groupId: String,
                                                                offsetTopicName: String,
                                                                offsetTopicReplications,
                                                                offsetTopicPartitions,
                                                                statusTopicName,
                                                                statusTopicReplications,
                                                                statusTopicPartitions,
                                                                configTopicName,
                                                                configTopicReplications,
                                                                jarInfos,
                                                                nodeNames) => {
    // the inputs have been checked (NullPointerException). Hence, we throw another exception here.
    if (executionContext == null) throw new AssertionError()
    if (clusterName == null || clusterName.isEmpty) throw new AssertionError()
    if (imageName == null || imageName.isEmpty) throw new AssertionError()
    if (brokerClusterName == null || brokerClusterName.isEmpty) throw new AssertionError()
    if (clientPort <= 0) throw new AssertionError()
    if (jmxPort <= 0) throw new AssertionError()
    if (groupId == null || groupId.isEmpty) throw new AssertionError()
    if (offsetTopicName == null || offsetTopicName.isEmpty) throw new AssertionError()
    if (offsetTopicReplications <= 0) throw new AssertionError()
    if (offsetTopicPartitions <= 0) throw new AssertionError()
    if (statusTopicName == null || offsetTopicName.isEmpty) throw new AssertionError()
    if (statusTopicReplications <= 0) throw new AssertionError()
    if (statusTopicPartitions <= 0) throw new AssertionError()
    if (configTopicName == null || offsetTopicName.isEmpty) throw new AssertionError()
    if (configTopicReplications <= 0) throw new AssertionError()
    // it is ok to accept empty url
    if (jarInfos == null) throw new AssertionError()
    if (nodeNames == null || nodeNames.isEmpty) throw new AssertionError()
    Future.successful(
      WorkerClusterInfo(
        name = clusterName,
        imageName = imageName,
        brokerClusterName = brokerClusterName,
        clientPort = clientPort,
        jmxPort = jmxPort,
        groupId = groupId,
        offsetTopicName = offsetTopicName,
        offsetTopicReplications = offsetTopicReplications,
        offsetTopicPartitions = offsetTopicPartitions,
        statusTopicName = statusTopicName,
        statusTopicReplications = statusTopicReplications,
        statusTopicPartitions = statusTopicPartitions,
        configTopicName = configTopicName,
        configTopicReplications = configTopicReplications,
        configTopicPartitions = 1,
        jarInfos = jarInfos,
        connectors = Seq.empty,
        nodeNames = nodeNames
      ))
  }

  @Test
  def nullImage(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().imageName(null)
  }

  @Test
  def emptyImage(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().imageName("")
  }

  @Test
  def nullClusterName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().clusterName(null)
  }

  @Test
  def emptyClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().clusterName("")
  }

  @Test
  def negativeClientPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().clientPort(-1)
  }

  @Test
  def negativeJmxPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().jmxPort(-1)
  }

  @Test
  def nullBkClusterName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().brokerClusterName(null)
  }

  @Test
  def emptyBkClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().brokerClusterName("")
  }

  @Test
  def nullGroupId(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().groupId(null)
  }

  @Test
  def emptyGroupId(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().groupId("")
  }

  @Test
  def nullConfigTopicName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().configTopicName(null)
  }

  @Test
  def emptyConfigTopicName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().configTopicName("")
  }

  @Test
  def negativeConfigTopicReplications(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().configTopicReplications(-1)
  }

  @Test
  def nullStatusTopicName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().statusTopicName(null)
  }

  @Test
  def emptyStatusTopicName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().statusTopicName("")
  }
  @Test
  def negativeStatusTopicPartitions(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().statusTopicPartitions(-1)
  }
  @Test
  def negativeStatusTopicReplications(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().statusTopicReplications(-1)
  }

  @Test
  def nullOffsetTopicName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().offsetTopicName(null)
  }

  @Test
  def emptyOffsetTopicName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().offsetTopicName("")
  }
  @Test
  def negativeOffsetTopicPartitions(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().offsetTopicPartitions(-1)
  }
  @Test
  def negativeOffsetTopicReplications(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().offsetTopicReplications(-1)
  }

  @Test
  def nullNodes(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().nodeNames(null)
  }

  @Test
  def emptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().nodeNames(Seq.empty)
  }

  @Test
  def testNameLength(): Unit = wkCreator()
    .imageName(CommonUtils.randomString(10))
    .clusterName(CommonUtils.randomString(10))
    .brokerClusterName("bk")
    .clientPort(CommonUtils.availablePort())
    .groupId(CommonUtils.randomString(10))
    .offsetTopicName(CommonUtils.randomString(10))
    .statusTopicName(CommonUtils.randomString(10))
    .configTopicName(CommonUtils.randomString(10))
    .nodeNames(Seq("abc"))
    .create()

  @Test
  def testInvalidName(): Unit = an[IllegalArgumentException] should be thrownBy wkCreator()
    .imageName(CommonUtils.randomString(10))
    .clusterName(CommonUtils.randomString(Collie.LIMIT_OF_NAME_LENGTH + 1))
    .brokerClusterName("bk")
    .clientPort(CommonUtils.availablePort())
    .groupId(CommonUtils.randomString(10))
    .offsetTopicName(CommonUtils.randomString(10))
    .statusTopicName(CommonUtils.randomString(10))
    .configTopicName(CommonUtils.randomString(10))
    .nodeNames(Seq("abc"))
    .create()

  @Test
  def testCopy(): Unit = {
    val workerClusterInfo = WorkerClusterInfo(
      name = CommonUtils.randomString(10),
      imageName = CommonUtils.randomString(),
      brokerClusterName = CommonUtils.randomString(),
      clientPort = 10,
      jmxPort = 10,
      groupId = CommonUtils.randomString(),
      statusTopicName = CommonUtils.randomString(),
      statusTopicPartitions = 10,
      statusTopicReplications = 10,
      configTopicName = CommonUtils.randomString(),
      configTopicPartitions = 1,
      configTopicReplications = 10,
      offsetTopicName = CommonUtils.randomString(),
      offsetTopicPartitions = 10,
      offsetTopicReplications = 10,
      jarInfos = Seq.empty,
      connectors = Seq.empty,
      nodeNames = Seq(CommonUtils.randomString())
    )
    Await.result(wkCreator().copy(workerClusterInfo).create(), 30 seconds) shouldBe workerClusterInfo
  }

  @Test
  def testPassIncorrectTypeToCopy(): Unit =
    an[IllegalArgumentException] should be thrownBy wkCreator().copy(FakeClusterInfo(CommonUtils.randomString()))

  @Test
  def testJarInfo(): Unit = WorkerCollie.toMap(Seq.empty) shouldBe Map.empty

  @Test
  def testJarInfo2(): Unit = {
    val jarInfos = Seq(
      JarInfo(
        id = CommonUtils.randomString(),
        name = CommonUtils.randomString(),
        group = CommonUtils.randomString(),
        size = 100,
        url = new URL("http://localhost:12345/aa.jar"),
        lastModified = CommonUtils.current()
      ),
      JarInfo(
        id = CommonUtils.randomString(),
        name = CommonUtils.randomString(),
        group = CommonUtils.randomString(),
        size = 100,
        url = new URL("http://localhost:12345/aa.jar"),
        lastModified = CommonUtils.current()
      )
    )
    WorkerCollie.toJarInfos(WorkerCollie.toString(jarInfos)) shouldBe jarInfos
  }

  @Test
  def testJarInfo3(): Unit = {
    val jarInfos = Seq(
      JarInfo(
        id = CommonUtils.randomString(),
        name = CommonUtils.randomString(),
        group = CommonUtils.randomString(),
        size = 100,
        url = new URL("http://localhost:12345/aa.jar"),
        lastModified = CommonUtils.current()
      ),
      JarInfo(
        id = CommonUtils.randomString(),
        name = CommonUtils.randomString(),
        group = CommonUtils.randomString(),
        size = 100,
        url = new URL("http://localhost:12345/aa.jar"),
        lastModified = CommonUtils.current()
      )
    )
    WorkerCollie.toString(jarInfos).contains("\\\"") shouldBe true

    WorkerCollie.toJarInfos(WorkerCollie.toString(jarInfos)) shouldBe jarInfos

    WorkerCollie.toJarInfos(JsArray(jarInfos.map(JAR_INFO_JSON_FORMAT.write).toVector).toString) shouldBe jarInfos

  }
}
