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

import java.util.Objects

import com.island.ohara.client.configurator.v0.StreamApi
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DeserializationException

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class TestStreamCreator extends SmallTest with Matchers {

  private[this] def streamCreator(): StreamCollie.ClusterCreator =
    (clusterName,
     nodeNames,
     imageName,
     jarUrl,
     appId,
     brokerProps,
     fromTopics,
     toTopics,
     jmxPort,
     enableExactlyOnce,
     executionContext) => {
      // We only check required variables
      CommonUtils.requireNonEmpty(clusterName)
      CommonUtils.requireNonEmpty(nodeNames.asJava)
      CommonUtils.requireNonEmpty(imageName)
      CommonUtils.requireNonEmpty(jarUrl)
      CommonUtils.requireNonEmpty(appId)
      CommonUtils.requireNonEmpty(brokerProps)
      CommonUtils.requireNonEmpty(fromTopics.asJava)
      CommonUtils.requireNonEmpty(toTopics.asJava)
      CommonUtils.requireConnectionPort(jmxPort)
      Objects.requireNonNull(enableExactlyOnce)
      Objects.requireNonNull(executionContext)
      Future.successful(
        StreamClusterInfo(
          name = clusterName,
          imageName = imageName,
          jmxPort = jmxPort,
          nodeNames = nodeNames,
          deadNodes = Set.empty,
          state = None
        ))
    }

  private[this] def awaitResult[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def nullClusterName(): Unit = {
    an[NullPointerException] should be thrownBy streamCreator().clusterName(null)
  }

  @Test
  def IllegalClusterName(): Unit = {
    an[DeserializationException] should be thrownBy streamCreator().clusterName("!@#$-")
  }

  @Test
  def nullNodeName(): Unit = {
    an[NullPointerException] should be thrownBy streamCreator().nodeNames(null)
  }

  @Test
  def emptyNodeName(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator().nodeNames(Set.empty)
  }

  @Test
  def nullImage(): Unit = {
    an[NullPointerException] should be thrownBy streamCreator().imageName(null)
  }

  @Test
  def emptyImage(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator().imageName("")
  }

  @Test
  def nullJarUrl(): Unit = {
    an[NullPointerException] should be thrownBy streamCreator().jarUrl(null)
  }

  @Test
  def emptyJarUrl(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator().jarUrl("")
  }

  @Test
  def nullAppId(): Unit = {
    an[NullPointerException] should be thrownBy streamCreator().appId(null)
  }

  @Test
  def emptyAppId(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator().appId("")
  }

  @Test
  def nullBrokerProps(): Unit = {
    an[NullPointerException] should be thrownBy streamCreator().brokerProps(null)
  }

  @Test
  def emptyBrokerProps(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator().brokerProps("")
  }

  @Test
  def nullFromTopics(): Unit = {
    an[NullPointerException] should be thrownBy streamCreator().fromTopics(null)
  }

  @Test
  def emptyFromTopics(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator().fromTopics(Set.empty)
  }

  @Test
  def nullToTopics(): Unit = {
    an[NullPointerException] should be thrownBy streamCreator().toTopics(null)
  }

  @Test
  def emptyToTopics(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator().toTopics(Set.empty)
  }

  @Test
  def zeroJmxPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator().jmxPort(0)
  }

  @Test
  def negativeJmxPort(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator().jmxPort(-99)
  }

  @Test
  def testNameLength(): Unit = {
    streamCreator()
      .clusterName(CommonUtils.randomString(StreamApi.LIMIT_OF_NAME_LENGTH))
      .imageName(CommonUtils.randomString())
      .jarUrl("jar")
      .nodeNames(Set("bar", "foo", "bez"))
      .appId("app")
      .brokerProps("broker")
      .fromTopics(Set("topic1"))
      .toTopics(Set("topic2"))
      .create()

    an[DeserializationException] should be thrownBy streamCreator()
      .clusterName(CommonUtils.randomString(StreamApi.LIMIT_OF_NAME_LENGTH + 1))
      .imageName(CommonUtils.randomString())
      .jarUrl("jar")
      .nodeNames(Set("bar", "foo", "bez"))
      .appId("app")
      .brokerProps("broker")
      .fromTopics(Set("topic1"))
      .toTopics(Set("topic2"))
      .create()
  }

  @Test
  def testCopy(): Unit = {
    val info = StreamClusterInfo(
      name = CommonUtils.randomString(10),
      imageName = CommonUtils.randomString(),
      jmxPort = 1,
      nodeNames = Set(CommonUtils.randomString()),
      deadNodes = Set.empty,
      state = None
    )

    // copy in stream is not support yet
    an[NullPointerException] should be thrownBy awaitResult(streamCreator().copy(info).create())
  }

  @Test
  def testNormalCase(): Unit = {

    // could set jmx port
    awaitResult(
      streamCreator()
        .clusterName(CommonUtils.randomString(StreamApi.LIMIT_OF_NAME_LENGTH))
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .nodeNames(Set("bar", "foo"))
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Set("topic1"))
        .toTopics(Set("topic2"))
        .jmxPort(1000)
        .create()).jmxPort shouldBe 1000
  }
}
