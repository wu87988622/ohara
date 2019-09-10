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
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.setting.TopicKey
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.Producer
import com.island.ohara.testing.With3Brokers
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.errors.{InvalidPartitionsException, UnknownTopicOrPartitionException}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestTopicAdmin extends With3Brokers with Matchers {

  private[this] val topicAdmin = TopicAdmin(testUtil().brokersConnProps())
  private[this] val GROUP = "topic_group"

  private[this] def waitAndGetTopicInfo(topicKey: TopicKey): TopicInfo = {
    // wait the topic to be available
    CommonUtils.await(
      () =>
        try result(topicAdmin.topics().map(_.exists(_.name == topicKey.topicNameOnKafka())))
        catch {
          // the partition is not ready
          case _: NoSuchElementException           => false
          case _: UnknownTopicOrPartitionException => false
      },
      java.time.Duration.ofSeconds(60)
    )
    result(topicAdmin.topics()).find(_.name == topicKey.topicNameOnKafka()).get
  }

  @Test
  def createTopic(): Unit = {
    val name = CommonUtils.randomString(10)
    val topicKey = TopicKey.of(GROUP, name)
    val numberOfPartitions: Int = 1
    val numberOfReplications: Short = 1
    result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .topicKey(topicKey)
        .create())
    val topic = waitAndGetTopicInfo(topicKey)
    topic.name shouldBe topicKey.topicNameOnKafka()
    topic.numberOfPartitions shouldBe numberOfPartitions
    topic.numberOfReplications shouldBe numberOfReplications

    result(topicAdmin.topics()).find(_.name == topicKey.topicNameOnKafka()).get shouldBe topic

    result(topicAdmin.delete(topicKey)) shouldBe true

    result(topicAdmin.topics()).find(_.name == topicKey.topicNameOnKafka()) shouldBe None
  }

  @Test
  def addPartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val topicKey = TopicKey.of(GROUP, name)
    val numberOfPartitions: Int = 1
    val numberOfReplications: Short = 1
    result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .topicKey(topicKey)
        .create()
    )
    val topic = waitAndGetTopicInfo(topicKey)
    result(topicAdmin.changePartitions(topicKey, numberOfPartitions + 1))
    val topic2 = waitAndGetTopicInfo(topicKey)
    topic2.numberOfPartitions shouldBe topic.numberOfPartitions + 1
  }

  @Test
  def reducePartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val topicKey = TopicKey.of(GROUP, name)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .topicKey(topicKey)
        .create())
    waitAndGetTopicInfo(topicKey)
    an[InvalidPartitionsException] should be thrownBy result(
      topicAdmin.changePartitions(topicKey, numberOfPartitions - 1))
  }

  @Test
  def negativePartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val topicKey = TopicKey.of(GROUP, name)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .topicKey(topicKey)
        .create())
    waitAndGetTopicInfo(topicKey)
    an[InvalidPartitionsException] should be thrownBy result(topicAdmin.changePartitions(topicKey, -10))
  }

  @Test
  def keepPartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val topicKey = TopicKey.of(GROUP, name)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .topicKey(topicKey)
        .create())
    waitAndGetTopicInfo(topicKey)
    an[InvalidPartitionsException] should be thrownBy result(topicAdmin.changePartitions(topicKey, numberOfPartitions))
  }

  @Test
  def changePartitionsOfNonexistentTopic(): Unit =
    an[UnknownTopicOrPartitionException] should be thrownBy result(
      topicAdmin.changePartitions(TopicKey.of(GROUP, CommonUtils.randomString(10)), 10))

  @Test
  def deleteNonexistentTopic(): Unit = {
    val name = CommonUtils.randomString(10)
    val topicKey = TopicKey.of(GROUP, name)
    result(topicAdmin.delete(TopicKey.of(GROUP, CommonUtils.randomString()))) shouldBe false
    result(topicAdmin.creator.topicKey(topicKey).create())
    waitAndGetTopicInfo(topicKey)
    result(topicAdmin.delete(topicKey)) shouldBe true
  }

  @Test
  def testCleanupPolicy(): Unit = {
    val name = CommonUtils.randomString(10)
    val topicKey = TopicKey.of(GROUP, name)
    result(topicAdmin.creator.topicKey(topicKey).cleanupPolicy(CleanupPolicy.DELETE).create())
    val topic = waitAndGetTopicInfo(topicKey)
    topic.configs(TopicConfig.CLEANUP_POLICY_CONFIG) shouldBe CleanupPolicy.DELETE.name
  }

  @Test
  def testCustomConfigs(): Unit = {
    val name = CommonUtils.randomString(10)
    val topicKey = TopicKey.of(GROUP, name)
    val key = TopicConfig.SEGMENT_BYTES_CONFIG
    val value = 1024 * 1024
    result(topicAdmin.creator.topicKey(topicKey).config(key, value.toString).create())
    val topic = waitAndGetTopicInfo(topicKey)
    topic.configs(key) shouldBe value.toString
  }

  @Test
  def testExist(): Unit = {
    result(topicAdmin.exist(TopicKey.of(GROUP, CommonUtils.randomString()))) shouldBe false
    val name = CommonUtils.randomString(10)
    val topicKey = TopicKey.of(GROUP, name)
    result(topicAdmin.creator.topicKey(topicKey).create())
    waitAndGetTopicInfo(topicKey)
    result(topicAdmin.exist(topicKey)) shouldBe true
  }

  @Test
  def testBeginningOffsetAndEndOffset(): Unit = {
    result(topicAdmin.exist(TopicKey.of(GROUP, CommonUtils.randomString()))) shouldBe false
    val topicKey = TopicKey.of(GROUP, CommonUtils.randomString(10))
    result(topicAdmin.creator.topicKey(topicKey).create())
    waitAndGetTopicInfo(topicKey)
    val producer = Producer.builder
      .connectionProps(testUtil().brokersConnProps())
      .keySerializer(Serializer.STRING)
      .valueSerializer(Serializer.STRING)
      .build()
    val count = 10
    try {
      (0 until count).foreach(
        _ =>
          producer
            .sender()
            .topicName(topicKey.topicNameOnKafka())
            .key(CommonUtils.randomString())
            .value(CommonUtils.randomString())
            .send())
      producer.flush()
    } finally Releasable.close(producer)
    val topicInfo = result(topicAdmin.topics()).find(_.name == topicKey.topicNameOnKafka()).get
    topicInfo.partitionInfos.foreach { partition =>
      partition.beginningOffset shouldBe 0
      partition.endOffset shouldBe count
    }
  }

  @After
  def tearDown(): Unit = Releasable.close(topicAdmin)
}
