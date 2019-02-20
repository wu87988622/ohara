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

import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtil
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.Future

class TestWorkerCreator extends SmallTest with Matchers {

  private[this] def wkCreator(): WorkerCollie.ClusterCreator = (clusterName: String,
                                                                imageName: String,
                                                                brokerClusterName: String,
                                                                clientPort: Int,
                                                                groupId: String,
                                                                offsetTopicName: String,
                                                                offsetTopicReplications: Short,
                                                                offsetTopicPartitions: Int,
                                                                statusTopicName: String,
                                                                statusTopicReplications: Short,
                                                                statusTopicPartitions: Int,
                                                                configTopicName: String,
                                                                configTopicReplications: Short,
                                                                jarUrls: Seq[URL],
                                                                nodeNames: Seq[String]) => {
    // the inputs have been checked (NullPointerException). Hence, we throw another exception here.
    if (clusterName == null || clusterName.isEmpty) throw new AssertionError()
    if (imageName == null || imageName.isEmpty) throw new AssertionError()
    if (brokerClusterName == null || brokerClusterName.isEmpty) throw new AssertionError()
    if (clientPort <= 0) throw new AssertionError()
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
    if (jarUrls == null) throw new AssertionError()
    if (nodeNames == null || nodeNames.isEmpty) throw new AssertionError()
    Future.successful(
      WorkerClusterInfo(
        name = clusterName,
        imageName = imageName,
        brokerClusterName = brokerClusterName,
        clientPort = clientPort,
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
        jarNames = jarUrls.map(_.getFile),
        sources = Seq.empty,
        sinks = Seq.empty,
        nodeNames = nodeNames
      ))
  }

  @Test
  def nullImage(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator()
      .clusterName(CommonUtil.randomString(10))
      .brokerClusterName("bk")
      .clientPort(CommonUtil.availablePort())
      .groupId(CommonUtil.randomString(10))
      .offsetTopicName(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq("abc"))
      .create()
  }

  @Test
  def nullClusterName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator()
      .imageName(CommonUtil.randomString(10))
      .brokerClusterName("bk")
      .clientPort(CommonUtil.availablePort())
      .groupId(CommonUtil.randomString(10))
      .offsetTopicName(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq("abc"))
      .create()
  }

  @Test
  def nullClientPort(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(10))
      .brokerClusterName("bk")
      .groupId(CommonUtil.randomString(10))
      .offsetTopicName(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq("abc"))
      .create()
  }

  @Test
  def nullBkClusterName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(10))
      .clientPort(CommonUtil.availablePort())
      .groupId(CommonUtil.randomString(10))
      .offsetTopicName(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq("abc"))
      .create()
  }

  @Test
  def nullGroupId(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(10))
      .brokerClusterName("bk")
      .clientPort(CommonUtil.availablePort())
      .offsetTopicName(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq("abc"))
      .create()
  }

  @Test
  def nullConfigTopicName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(10))
      .brokerClusterName("bk")
      .clientPort(CommonUtil.availablePort())
      .groupId(CommonUtil.randomString(10))
      .offsetTopicName(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq("abc"))
      .create()
  }

  @Test
  def nullStatusTopicName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(10))
      .brokerClusterName("bk")
      .clientPort(CommonUtil.availablePort())
      .groupId(CommonUtil.randomString(10))
      .offsetTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq("abc"))
      .create()
  }

  @Test
  def nullOffsetTopicName(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(10))
      .brokerClusterName("bk")
      .clientPort(CommonUtil.availablePort())
      .groupId(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq("abc"))
      .create()
  }
  @Test
  def nullNodes(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(10))
      .brokerClusterName("bk")
      .clientPort(CommonUtil.availablePort())
      .groupId(CommonUtil.randomString(10))
      .offsetTopicName(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .create()
  }

  @Test
  def emptyNodes(): Unit = {
    an[NullPointerException] should be thrownBy wkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(10))
      .brokerClusterName("bk")
      .clientPort(CommonUtil.availablePort())
      .groupId(CommonUtil.randomString(10))
      .offsetTopicName(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq.empty)
      .create()
  }

  @Test
  def testNameLength(): Unit = {
    // pass
    wkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(10))
      .brokerClusterName("bk")
      .clientPort(CommonUtil.availablePort())
      .groupId(CommonUtil.randomString(10))
      .offsetTopicName(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq("abc"))
      .create()

    an[IllegalArgumentException] should be thrownBy wkCreator()
      .imageName(CommonUtil.randomString(10))
      .clusterName(CommonUtil.randomString(Collie.LIMIT_OF_NAME_LENGTH + 1))
      .brokerClusterName("bk")
      .clientPort(CommonUtil.availablePort())
      .groupId(CommonUtil.randomString(10))
      .offsetTopicName(CommonUtil.randomString(10))
      .statusTopicName(CommonUtil.randomString(10))
      .configTopicName(CommonUtil.randomString(10))
      .nodeNames(Seq("abc"))
      .create()
  }
}
