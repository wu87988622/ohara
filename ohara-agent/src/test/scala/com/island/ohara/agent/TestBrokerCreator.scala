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

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.{BrokerApi, TopicApi}
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DeserializationException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestBrokerCreator extends OharaTest with Matchers {

  private[this] val zkKey: ObjectKey = ObjectKey.of(CommonUtils.randomString(), CommonUtils.randomString())

  private[this] def bkCreator(): BrokerCollie.ClusterCreator =
    (executionContext, creation) => {
      // the inputs have been checked (NullPointerException). Hence, we throw another exception here.
      if (executionContext == null) throw new AssertionError()
      Future.successful(
        BrokerClusterInfo(
          settings = BrokerApi.access.request.settings(creation.settings).creation.settings,
          aliveNodes = creation.nodeNames,
          state = None,
          error = None,
          lastModified = 0,
          topicSettingDefinitions = TopicApi.TOPIC_DEFINITIONS
        ))
    }

  @Test
  def nullImage(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().imageName(null)
  }

  @Test
  def emptyImage(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().imageName("")
  }

  @Test
  def nullClusterName(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().name(null)
  }

  @Test
  def emptyClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().name("")
  }

  @Test
  def nullGroup(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().group(null)
  }

  @Test
  def emptyGroup(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().group("")
  }

  @Test
  def nullZkClusterName(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().zookeeperClusterKey(null)
  }

  @Test
  def negativeClientPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().clientPort(-1)
  }

  @Test
  def negativeJmxPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().jmxPort(-1)
  }

  @Test
  def nullNodes(): Unit = {
    an[NullPointerException] should be thrownBy bkCreator().nodeNames(null)
  }

  @Test
  def emptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy bkCreator().nodeNames(Set.empty)
  }

  @Test
  def testNameLength(): Unit = bkCreator()
    .imageName(CommonUtils.randomString(10))
    .name(CommonUtils.randomString(10))
    .group(CommonUtils.randomString(10))
    .zookeeperClusterKey(zkKey)
    .clientPort(CommonUtils.availablePort())
    .nodeName(CommonUtils.randomString)
    .create()

  @Test
  def testInvalidName(): Unit =
    an[DeserializationException] should be thrownBy bkCreator()
      .name(CommonUtils.randomString(com.island.ohara.client.configurator.v0.LIMIT_OF_KEY_LENGTH))
      .group(CommonUtils.randomString(10))
      .imageName(CommonUtils.randomString(10))
      .nodeName(CommonUtils.randomString)
      .create()

  @Test
  def testMinimumCreator(): Unit = Await.result(
    bkCreator()
      .name(CommonUtils.randomString(10))
      .group(CommonUtils.randomString(10))
      .imageName(CommonUtils.randomString)
      .nodeName(CommonUtils.randomString)
      .zookeeperClusterKey(ObjectKey.of("default", "name"))
      .create(),
    5 seconds
  )

  @Test
  def testCopy(): Unit = {
    val nodeNames = Set(CommonUtils.randomString())
    val brokerClusterInfo = BrokerClusterInfo(
      settings = BrokerApi.access.request
        .name(CommonUtils.randomString(10))
        .imageName(CommonUtils.randomString)
        .zookeeperClusterKey(zkKey)
        .nodeNames(nodeNames)
        .creation
        .settings,
      aliveNodes = nodeNames,
      state = None,
      error = None,
      lastModified = 0,
      topicSettingDefinitions = TopicApi.TOPIC_DEFINITIONS
    )

    // pass
    Await.result(bkCreator().settings(brokerClusterInfo.settings).create(), 30 seconds)
  }
}
