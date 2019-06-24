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

import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

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
     instances,
     appId,
     brokerProps,
     fromTopics,
     toTopics,
     jmxPort,
     _,
     executionContext) => {
      // We only check required variables
      CommonUtils.requireNonEmpty(clusterName)
      CommonUtils.requireNonEmpty(imageName)
      CommonUtils.requireNonEmpty(jarUrl)
      CommonUtils.requireNonEmpty(appId)
      CommonUtils.requireNonEmpty(brokerProps)
      CommonUtils.requireNonEmpty(fromTopics.asJava)
      CommonUtils.requireNonEmpty(toTopics.asJava)
      Objects.requireNonNull(executionContext)
      Future.successful(
        StreamClusterInfo(
          name = clusterName,
          imageName = imageName,
          jmxPort = jmxPort,
          nodeNames = {
            if (CommonUtils.isEmpty(nodeNames.asJava))
              CommonUtils.requireNonEmpty(Seq.fill(CommonUtils.requirePositiveInt(instances))("fake").asJava).asScala
            else CommonUtils.requireNonEmpty(nodeNames.asJava).asScala
          }.toSet,
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
    an[IllegalArgumentException] should be thrownBy streamCreator().clusterName("!@#$-")
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
    an[IllegalArgumentException] should be thrownBy streamCreator().fromTopics(Seq.empty)
  }

  @Test
  def nullToTopics(): Unit = {
    an[NullPointerException] should be thrownBy streamCreator().toTopics(null)
  }

  @Test
  def emptyToTopics(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator().toTopics(Seq.empty)
  }

  @Test
  def testNormalCase(): Unit = {

    // 0 instances is not allowed
    an[IllegalArgumentException] should be thrownBy awaitResult(
      streamCreator()
        .clusterName(CommonUtils.randomString(Collie.LIMIT_OF_NAME_LENGTH))
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .instances(0)
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Seq("topic1"))
        .toTopics(Seq("topic2"))
        .create())

    // negative instances is not allowed
    an[IllegalArgumentException] should be thrownBy awaitResult(
      streamCreator()
        .clusterName(CommonUtils.randomString(Collie.LIMIT_OF_NAME_LENGTH))
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .instances(-5)
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Seq("topic1"))
        .toTopics(Seq("topic2"))
        .create())

    // could set nodeNames only
    awaitResult(
      streamCreator()
        .clusterName(CommonUtils.randomString(Collie.LIMIT_OF_NAME_LENGTH))
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .nodeNames(Set("bar", "foo", "bez"))
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Seq("topic1"))
        .toTopics(Seq("topic2"))
        .create()).nodeNames.size shouldBe 3

    // nodeNames will override the effect of instances
    awaitResult(
      streamCreator()
        .clusterName(CommonUtils.randomString(Collie.LIMIT_OF_NAME_LENGTH))
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .instances(10)
        .nodeNames(Set("bar", "foo"))
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Seq("topic1"))
        .toTopics(Seq("topic2"))
        .create()).nodeNames.size shouldBe 2

    // could set jmx port
    awaitResult(
      streamCreator()
        .clusterName(CommonUtils.randomString(Collie.LIMIT_OF_NAME_LENGTH))
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .instances(10)
        .nodeNames(Set("bar", "foo"))
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Seq("topic1"))
        .toTopics(Seq("topic2"))
        .jmxPort(1000)
        .create()).jmxPort shouldBe 1000
  }

  @Test
  def testNameLength(): Unit = {
    awaitResult(
      streamCreator()
        .clusterName(CommonUtils.randomString(Collie.LIMIT_OF_NAME_LENGTH))
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .instances(10)
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Seq("topic1"))
        .toTopics(Seq("topic2"))
        .create())
  }

  @Test
  def testInvalidName(): Unit = {
    an[IllegalArgumentException] should be thrownBy streamCreator()
      .clusterName(CommonUtils.randomString(Collie.LIMIT_OF_NAME_LENGTH + 1))
      .imageName(CommonUtils.randomString())
      .jarUrl("jar")
      .instances(10)
      .appId("app")
      .brokerProps("broker")
      .fromTopics(Seq("topic1"))
      .toTopics(Seq("topic2"))
      .create()
  }
}
