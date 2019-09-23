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

import com.island.ohara.client.configurator.v0.ZookeeperApi
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DeserializationException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestZookeeperCreator extends OharaTest with Matchers {

  private[this] def zkCreator(): ZookeeperCollie.ClusterCreator =
    (executionContext, creation) => {
      if (executionContext == null) throw new AssertionError()
      Future.successful(
        ZookeeperClusterInfo(
          settings = ZookeeperApi.access.request.settings(creation.settings).creation.settings,
          deadNodes = Set.empty,
          state = None,
          error = None,
          lastModified = 0
        ))
    }

  @Test
  def nullImage(): Unit = {
    an[NullPointerException] should be thrownBy zkCreator().imageName(null)
  }

  @Test
  def emptyImage(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().imageName("")
  }

  @Test
  def nullClusterName(): Unit = {
    an[NullPointerException] should be thrownBy zkCreator().name(null)
  }

  @Test
  def emptyClusterName(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().name("")
  }

  @Test
  def nullGroup(): Unit = {
    an[NullPointerException] should be thrownBy zkCreator().group(null)
  }

  @Test
  def emptyGroup(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().group("")
  }

  @Test
  def negativeClientPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().clientPort(-1)
  }

  @Test
  def negativePeerPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().peerPort(-1)
  }

  @Test
  def negativeElectionPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().electionPort(-1)
  }

  @Test
  def nullNodes(): Unit = {
    an[NullPointerException] should be thrownBy zkCreator().nodeNames(null)
  }

  @Test
  def emptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy zkCreator().nodeNames(Set.empty)
  }

  @Test
  def testNameLength(): Unit = zkCreator()
    .name(CommonUtils.randomString(10))
    .group(CommonUtils.randomString(10))
    .imageName(CommonUtils.randomString(10))
    .peerPort(CommonUtils.availablePort())
    .clientPort(CommonUtils.availablePort())
    .electionPort(CommonUtils.availablePort())
    .nodeName(CommonUtils.randomString())
    .create()

  @Test
  def testInvalidName(): Unit =
    an[DeserializationException] should be thrownBy zkCreator()
      .name(CommonUtils.randomString(com.island.ohara.client.configurator.v0.LIMIT_OF_KEY_LENGTH))
      .group(CommonUtils.randomString(10))
      .imageName(CommonUtils.randomString(10))
      .nodeName(CommonUtils.randomString())
      .create()

  @Test
  def testInvalidGroup(): Unit =
    an[DeserializationException] should be thrownBy zkCreator()
      .name(CommonUtils.randomString(10))
      .group(CommonUtils.randomString(com.island.ohara.client.configurator.v0.LIMIT_OF_KEY_LENGTH))
      .imageName(CommonUtils.randomString(10))
      .nodeName(CommonUtils.randomString())
      .create()

  @Test
  def testCopy(): Unit = {
    val nodeNames = Set(CommonUtils.randomString())
    val zookeeperClusterInfo = ZookeeperClusterInfo(
      settings = ZookeeperApi.access.request
        .name(CommonUtils.randomString(10))
        .imageName(CommonUtils.randomString)
        .nodeNames(nodeNames)
        .creation
        .settings,
      deadNodes = Set.empty,
      state = None,
      error = None,
      lastModified = 0
    )

    // pass
    Await.result(zkCreator().settings(zookeeperClusterInfo.settings).create(), 30 seconds)
  }

  @Test
  def testMinimumCreator(): Unit = Await.result(
    zkCreator()
      .name(CommonUtils.randomString(10))
      .group(CommonUtils.randomString(10))
      .imageName(CommonUtils.randomString)
      .nodeName(CommonUtils.randomString)
      .create(),
    5 seconds
  )
}
