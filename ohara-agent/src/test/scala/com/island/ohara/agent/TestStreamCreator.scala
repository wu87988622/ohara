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
import java.util.Objects

import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.{Definition, StreamApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.setting.{SettingDef, TopicKey}
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestStreamCreator extends SmallTest with Matchers {

  private[this] def topicKey(): TopicKey = topicKey(CommonUtils.randomString())
  private[this] def topicKey(name: String): TopicKey = TopicKey.of("group", name)

  private[this] def streamCreator(): StreamCollie.ClusterCreator =
    (clusterName,
     nodeNames,
     imageName,
     brokerClusterName,
     jarInfo,
     jmxPort,
     fromTopics,
     toTopics,
     settings,
     executionContext) => {
      // We only check required variables
      CommonUtils.requireNonEmpty(clusterName)
      CommonUtils.requireNonEmpty(nodeNames.asJava)
      CommonUtils.requireNonEmpty(imageName)
      Objects.requireNonNull(jarInfo)
      CommonUtils.requireConnectionPort(jmxPort)
      CommonUtils.requireNonEmpty(brokerClusterName)
      CommonUtils.requireNonEmpty(fromTopics.asJava)
      CommonUtils.requireNonEmpty(toTopics.asJava)
      Objects.requireNonNull(settings)
      Objects.requireNonNull(executionContext)
      Future.successful {
        StreamClusterInfo(
          settings = settings,
          definition = None,
          deadNodes = Set.empty,
          metrics = Metrics.EMPTY,
          state = None,
          error = None,
          lastModified = CommonUtils.current()
        )
      }
    }

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

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
  def nullJarInfo(): Unit = {
    an[NullPointerException] should be thrownBy streamCreator().jarInfo(null)
  }

  @Test
  def nullSettings(): Unit = an[NullPointerException] should be thrownBy streamCreator().settings(null)

  private[this] def fileInfo: FileInfo = fileInfo(new URL("http://abc/aaa.jar"))

  private[this] def fileInfo(url: URL): FileInfo = FileInfo(
    group = CommonUtils.randomString(),
    name = CommonUtils.randomString(),
    size = 1000,
    url = url,
    lastModified = CommonUtils.current(),
    tags = Map.empty
  )

  @Test
  def testNameLength(): Unit = {
    streamCreator()
      .clusterName(CommonUtils.randomString(10))
      .imageName(CommonUtils.randomString())
      .brokerClusterName(CommonUtils.randomString())
      .fromTopicKey(topicKey())
      .toTopicKey(topicKey())
      .jmxPort(CommonUtils.availablePort())
      .nodeName(CommonUtils.randomString())
      .jarInfo(fileInfo)
      .create()

    an[DeserializationException] should be thrownBy streamCreator()
      .clusterName(CommonUtils.randomString(50))
      .imageName(CommonUtils.randomString())
      .brokerClusterName(CommonUtils.randomString())
      .jarInfo(fileInfo)
      .fromTopicKey(topicKey())
      .toTopicKey(topicKey())
      .jmxPort(CommonUtils.availablePort())
      .nodeName(CommonUtils.randomString())
      .create()
  }

  @Test
  def testCopy(): Unit = {
    val info = StreamClusterInfo(
      settings = StreamApi.access.request
        .nodeNames(Set("n0"))
        .jarInfo(FileInfo(
          group = CommonUtils.randomString(5),
          name = CommonUtils.randomString(5),
          url = new URL("http://localhost:12345/in.jar"),
          size = 1000,
          lastModified = CommonUtils.current(),
          tags = Map.empty
        ))
        .brokerClusterName(CommonUtils.randomString(10))
        .fromTopicKey(topicKey())
        .toTopicKey(topicKey())
        .creation
        .settings,
      definition = Some(Definition("className", Seq(SettingDef.builder().key("key").group("group").build()))),
      deadNodes = Set.empty,
      state = None,
      error = None,
      metrics = Metrics.EMPTY,
      lastModified = CommonUtils.current()
    )

    // pass
    result(streamCreator().copy(info).create())
  }

  @Test
  def testNormalCase(): Unit =
    // could set jmx port
    result(
      streamCreator()
        .clusterName(CommonUtils.randomString(10))
        .imageName(CommonUtils.randomString())
        .brokerClusterName(CommonUtils.randomString())
        .jarInfo(fileInfo)
        .fromTopicKey(topicKey())
        .toTopicKey(topicKey())
        .jmxPort(CommonUtils.availablePort())
        .nodeName(CommonUtils.randomString())
        .create())

  @Test
  def testParseJarKey(): Unit = {
    //a normal url
    val jarInfo = fileInfo(new URL("http://localhost:12345/group/abc.jar"))
    val res = result(
      streamCreator()
        .clusterName(CommonUtils.randomString(10))
        .imageName(CommonUtils.randomString())
        .brokerClusterName(CommonUtils.randomString())
        .nodeName(CommonUtils.randomString())
        .fromTopicKey(topicKey())
        .toTopicKey(topicKey())
        .jmxPort(CommonUtils.availablePort())
        .jarInfo(jarInfo)
        .create())
    res.jarKey.group() shouldBe jarInfo.group
    res.jarKey.name() shouldBe jarInfo.name
  }

  @Test
  def ignoreFromTopic(): Unit = an[IllegalArgumentException] should be thrownBy
    streamCreator()
      .clusterName(CommonUtils.randomString(10))
      .imageName(CommonUtils.randomString())
      .brokerClusterName(CommonUtils.randomString())
      .jarInfo(fileInfo)
      .toTopicKey(topicKey())
      .jmxPort(CommonUtils.availablePort())
      .nodeName(CommonUtils.randomString())
      .create()

  @Test
  def ignoreToTopic(): Unit = an[IllegalArgumentException] should be thrownBy
    streamCreator()
      .clusterName(CommonUtils.randomString(10))
      .imageName(CommonUtils.randomString())
      .brokerClusterName(CommonUtils.randomString())
      .jarInfo(fileInfo)
      .fromTopicKey(topicKey())
      .jmxPort(CommonUtils.availablePort())
      .nodeName(CommonUtils.randomString())
      .create()

  /**
    * the ignored jmx port is replaced by random one.
    */
  @Test
  def ignoreJmxPort(): Unit = CommonUtils.requireConnectionPort(
    result(
      streamCreator()
        .clusterName(CommonUtils.randomString(10))
        .imageName(CommonUtils.randomString())
        .brokerClusterName(CommonUtils.randomString())
        .jarInfo(fileInfo)
        .fromTopicKey(topicKey())
        .toTopicKey(topicKey())
        .nodeName(CommonUtils.randomString())
        .create()).jmxPort)

}
