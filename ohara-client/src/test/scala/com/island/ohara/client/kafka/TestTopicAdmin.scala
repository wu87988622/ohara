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

import com.island.ohara.common.util.CommonUtil
import com.island.ohara.testing.With3Brokers
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class TestTopicAdmin extends With3Brokers with Matchers {

  @Test
  def createTopic(): Unit = {
    val name = CommonUtil.randomString(10)
    val numberOfPartitions: Int = 1
    val numberOfReplications: Short = 1
    val topicAdmin = TopicAdmin(testUtil().brokersConnProps())
    try {
      val topic = Await.result(topicAdmin
                                 .creator()
                                 .numberOfPartitions(numberOfPartitions)
                                 .numberOfReplications(numberOfReplications)
                                 .name(name)
                                 .create(),
                               30 seconds)
      topic.name shouldBe name
      topic.numberOfPartitions shouldBe numberOfPartitions
      topic.numberOfReplications shouldBe numberOfReplications

      Await.result(topicAdmin.list(), 30 seconds).find(_.name == name).get shouldBe topic

      Await.result(topicAdmin.delete(name), 30 seconds) shouldBe topic

      Await.result(topicAdmin.list(), 30 seconds).find(_.name == name) shouldBe None
    } finally topicAdmin.close()
  }

  @Test
  def addPartitions(): Unit = {
    val numberOfPartitions: Int = 1
    val numberOfReplications: Short = 1
    val topicAdmin = TopicAdmin(testUtil().brokersConnProps())
    try {
      val topic = Await.result(
        topicAdmin
          .creator()
          .numberOfPartitions(numberOfPartitions)
          .numberOfReplications(numberOfReplications)
          .name(CommonUtil.randomString(10))
          .create(),
        30 seconds
      )
      val topic2 = Await.result(topicAdmin.changePartitions(topic.name, numberOfPartitions + 1), 30 seconds)
      topic2 shouldBe topic.copy(numberOfPartitions = numberOfPartitions + 1)
    } finally topicAdmin.close()
  }

  @Test
  def reducePartitions(): Unit = {
    val name = CommonUtil.randomString(10)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    val topicAdmin = TopicAdmin(testUtil().brokersConnProps())
    try {
      val topic = Await.result(topicAdmin
                                 .creator()
                                 .numberOfPartitions(numberOfPartitions)
                                 .numberOfReplications(numberOfReplications)
                                 .name(name)
                                 .create(),
                               30 seconds)
      an[IllegalArgumentException] should be thrownBy Await
        .result(topicAdmin.changePartitions(topic.name, numberOfPartitions - 1), 30 seconds)
    } finally topicAdmin.close()
  }

  @Test
  def negativePartitions(): Unit = {
    val name = CommonUtil.randomString(10)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    val topicAdmin = TopicAdmin(testUtil().brokersConnProps())
    try {
      val topic = Await.result(topicAdmin
                                 .creator()
                                 .numberOfPartitions(numberOfPartitions)
                                 .numberOfReplications(numberOfReplications)
                                 .name(name)
                                 .create(),
                               30 seconds)
      an[IllegalArgumentException] should be thrownBy Await.result(topicAdmin.changePartitions(topic.name, -10),
                                                                   30 seconds)
    } finally topicAdmin.close()
  }

  @Test
  def keepPartitions(): Unit = {
    val name = CommonUtil.randomString(10)
    val numberOfPartitions: Int = 2
    val numberOfReplications: Short = 1
    val topicAdmin = TopicAdmin(testUtil().brokersConnProps())
    try {
      val topic = Await.result(topicAdmin
                                 .creator()
                                 .numberOfPartitions(numberOfPartitions)
                                 .numberOfReplications(numberOfReplications)
                                 .name(name)
                                 .create(),
                               30 seconds)

      topic shouldBe Await.result(topicAdmin.changePartitions(topic.name, numberOfPartitions), 30 seconds)
    } finally topicAdmin.close()
  }
}
