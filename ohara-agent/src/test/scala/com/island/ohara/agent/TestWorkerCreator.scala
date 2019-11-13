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

import com.island.ohara.client.configurator.v0.WorkerApi
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers._
import spray.json.DeserializationException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestWorkerCreator extends OharaTest {

  private[this] def wkCreator(): WorkerCollie.ClusterCreator = (executionContext, creation) => {
    // the inputs have been checked (NullPointerException). Hence, we throw another exception here.
    if (executionContext == null) throw new AssertionError()
    Future.successful(
      WorkerClusterInfo(
        settings = creation.settings,
        aliveNodes = Set.empty,
        state = None,
        error = None,
        lastModified = 0
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
    an[NullPointerException] should be thrownBy wkCreator().name(null)
  }

  @Test
  def emptyClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().name("")
  }

  @Test
  def nullGroup(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().group(null)
  }

  @Test
  def emptyGroup(): Unit = {
    an[IllegalArgumentException] should be thrownBy wkCreator().group("")
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
  def nullBkClusterKey(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator().brokerClusterKey(null)
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
    an[IllegalArgumentException] should be thrownBy wkCreator().nodeNames(Set.empty)
  }

  @Test
  def testNameLength(): Unit = wkCreator()
    .imageName(CommonUtils.randomString(10))
    .name(CommonUtils.randomString(10))
    .group(CommonUtils.randomString(10))
    .brokerClusterKey(ObjectKey.of("default", "bk"))
    .clientPort(CommonUtils.availablePort())
    .jmxPort(8084)
    .groupId(CommonUtils.randomString(10))
    .configTopicName(CommonUtils.randomString(10))
    .configTopicReplications(1)
    .statusTopicName(CommonUtils.randomString(10))
    .statusTopicPartitions(1)
    .statusTopicReplications(1)
    .offsetTopicName(CommonUtils.randomString(10))
    .offsetTopicPartitions(1)
    .offsetTopicReplications(1)
    .nodeName(CommonUtils.randomString())
    .create()

  @Test
  def testInvalidName(): Unit =
    an[DeserializationException] should be thrownBy wkCreator()
      .name(CommonUtils.randomString(com.island.ohara.client.configurator.v0.LIMIT_OF_KEY_LENGTH))
      .group(CommonUtils.randomString(10))
      .imageName(CommonUtils.randomString(10))
      .nodeName(CommonUtils.randomString())
      .create()

  @Test
  def testMinimumCreator(): Unit = Await.result(
    wkCreator()
      .name(CommonUtils.randomString(10))
      .group(CommonUtils.randomString(10))
      .imageName(CommonUtils.randomString)
      .nodeName(CommonUtils.randomString)
      .brokerClusterKey(ObjectKey.of("g", "n"))
      .create(),
    5 seconds
  )

  @Test
  def testCopy(): Unit = {
    val nodeNames = Set(CommonUtils.randomString())
    val workerClusterInfo = WorkerClusterInfo(
      settings =
        WorkerApi.access.request.brokerClusterKey(ObjectKey.of("default", "bk")).nodeNames(nodeNames).creation.settings,
      aliveNodes = nodeNames,
      state = None,
      error = None,
      lastModified = 0
    )

    // pass
    Await.result(wkCreator().settings(workerClusterInfo.settings).create(), 30 seconds)
  }
}
