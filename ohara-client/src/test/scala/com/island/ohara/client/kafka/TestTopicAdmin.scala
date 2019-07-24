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

import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.With3Brokers
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestTopicAdmin extends With3Brokers with Matchers {

  private[this] val topicAdmin = TopicAdmin(testUtil().brokersConnProps())

  @Test
  def createTopic(): Unit = {
    val name = CommonUtils.randomString(10)
    val numberOfPartitions: Int = 1
    val numberOfReplications: Short = 1
    val topic = result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .name(name)
        .create())
    topic.name shouldBe name
    topic.numberOfPartitions shouldBe numberOfPartitions
    topic.numberOfReplications shouldBe numberOfReplications

    result(topicAdmin.topics()).find(_.name == name).get shouldBe topic

    result(topicAdmin.delete(name)) shouldBe true

    result(topicAdmin.topics()).find(_.name == name) shouldBe None
  }

  @Test
  def addPartitions(): Unit = {
    val numberOfPartitions: Int = 1
    val numberOfReplications: Short = 1
    val topic = Await.result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .name(CommonUtils.randomString(10))
        .create(),
      30 seconds
    )
    val topic2 = result(topicAdmin.changePartitions(topic.name, numberOfPartitions + 1))
    topic2 shouldBe topic.copy(numberOfPartitions = numberOfPartitions + 1)
  }

  @Test
  def reducePartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    val topic = Await.result(topicAdmin.creator
                               .numberOfPartitions(numberOfPartitions)
                               .numberOfReplications(numberOfReplications)
                               .name(name)
                               .create(),
                             30 seconds)
    an[IllegalArgumentException] should be thrownBy result(
      topicAdmin.changePartitions(topic.name, numberOfPartitions - 1))
  }

  @Test
  def negativePartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    val topic = Await.result(topicAdmin.creator
                               .numberOfPartitions(numberOfPartitions)
                               .numberOfReplications(numberOfReplications)
                               .name(name)
                               .create(),
                             30 seconds)
    an[IllegalArgumentException] should be thrownBy result(topicAdmin.changePartitions(topic.name, -10))
  }

  @Test
  def keepPartitions(): Unit = {
    val name = CommonUtils.randomString(10)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    val topic = result(
      topicAdmin.creator
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .name(name)
        .create())

    topic shouldBe result(topicAdmin.changePartitions(topic.name, numberOfPartitions))
  }

  @Test
  def changePartitionsOfNonexistentTopic(): Unit =
    an[UnknownTopicOrPartitionException] should be thrownBy result(
      topicAdmin.changePartitions(CommonUtils.randomString(10), 10))

  @Test
  def deleteNonexistentTopic(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    result(topicAdmin.delete(CommonUtils.randomString())) shouldBe false
    val topic = result(topicAdmin.creator.name(CommonUtils.randomString(10)).create())
    // wait the topic to be available
    CommonUtils
      .await(() => result(topicAdmin.topics().map(_.exists(_.name == topic.name))), java.time.Duration.ofSeconds(10))
    result(topicAdmin.delete(topic.name)) shouldBe true
  }

  @After
  def tearDown(): Unit = Releasable.close(topicAdmin)
}
