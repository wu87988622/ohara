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

package com.island.ohara.client.kafka

import com.island.ohara.client.kafka.TopicAdmin.TopicInfo
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.With3Brokers
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.errors.{InvalidPartitionsException, UnknownTopicOrPartitionException}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestTopicAdmin extends With3Brokers with Matchers {

  private[this] val topicAdmin = TopicAdmin(testUtil().brokersConnProps())

  private[this] def waitAndGetTopicInfo(name: String): TopicInfo = {
    // wait the topic to be available
    CommonUtils.await(() => result(topicAdmin.topics().map(_.exists(_.name == name))), java.time.Duration.ofSeconds(10))
    result(topicAdmin.topics()).find(_.name == name).get
  }

  @Test
  def createTopic(): Unit = {
    val name = CommonUtils.randomString(10)
    val numberOfPartitions: Int = 1
    val numberOfReplications: Short = 1
    result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .name(name)
        .create())
    val topic = waitAndGetTopicInfo(name)
    topic.name shouldBe name
    topic.numberOfPartitions shouldBe numberOfPartitions
    topic.numberOfReplications shouldBe numberOfReplications

    result(topicAdmin.topics()).find(_.name == name).get shouldBe topic

    result(topicAdmin.delete(name)) shouldBe true

    result(topicAdmin.topics()).find(_.name == name) shouldBe None
  }

  @Test
  def addPartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val numberOfPartitions: Int = 1
    val numberOfReplications: Short = 1
    result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .name(name)
        .create()
    )
    val topic = waitAndGetTopicInfo(name)
    result(topicAdmin.changePartitions(topic.name, numberOfPartitions + 1))
    val topic2 = waitAndGetTopicInfo(name)
    topic2 shouldBe topic.copy(numberOfPartitions = numberOfPartitions + 1)
  }

  @Test
  def reducePartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .name(name)
        .create())
    val topic = waitAndGetTopicInfo(name)
    an[InvalidPartitionsException] should be thrownBy result(
      topicAdmin.changePartitions(topic.name, numberOfPartitions - 1))
  }

  @Test
  def negativePartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .name(name)
        .create())
    val topic = waitAndGetTopicInfo(name)
    an[InvalidPartitionsException] should be thrownBy result(topicAdmin.changePartitions(topic.name, -10))
  }

  @Test
  def keepPartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .name(name)
        .create())
    val topic = waitAndGetTopicInfo(name)
    an[InvalidPartitionsException] should be thrownBy result(
      topicAdmin.changePartitions(topic.name, numberOfPartitions))
  }

  @Test
  def changePartitionsOfNonexistentTopic(): Unit =
    an[UnknownTopicOrPartitionException] should be thrownBy result(
      topicAdmin.changePartitions(CommonUtils.randomString(10), 10))

  @Test
  def deleteNonexistentTopic(): Unit = {
    val name = CommonUtils.randomString(10)
    result(topicAdmin.delete(CommonUtils.randomString())) shouldBe false
    result(topicAdmin.creator.name(name).create())
    waitAndGetTopicInfo(name)
    result(topicAdmin.delete(name)) shouldBe true
  }

  @Test
  def testCleanupPolicy(): Unit = {
    val name = CommonUtils.randomString(10)
    result(topicAdmin.creator.name(name).cleanupPolicy(CleanupPolicy.DELETE).create())
    val topic = waitAndGetTopicInfo(name)
    topic.configs(TopicConfig.CLEANUP_POLICY_CONFIG) shouldBe CleanupPolicy.DELETE.name
  }

  @Test
  def testCustomConfigs(): Unit = {
    val name = CommonUtils.randomString(10)
    val key = TopicConfig.SEGMENT_BYTES_CONFIG
    val value = 1024 * 1024
    result(topicAdmin.creator.name(name).config(key, value.toString).create())
    val topic = waitAndGetTopicInfo(name)
    topic.configs(key) shouldBe value.toString
  }

  @After
  def tearDown(): Unit = Releasable.close(topicAdmin)
}
