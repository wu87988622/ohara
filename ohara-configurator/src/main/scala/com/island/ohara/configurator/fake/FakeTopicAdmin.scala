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

package com.island.ohara.configurator.fake

import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.client.kafka.TopicAdmin
import com.island.ohara.client.kafka.TopicAdmin.KafkaTopicInfo
import com.island.ohara.common.setting.TopicKey

import scala.concurrent.Future

private[configurator] class FakeTopicAdmin extends TopicAdmin {
  import scala.collection.JavaConverters._

  override val connectionProps: String = "Unknown"

  private[this] val cachedTopics = new ConcurrentHashMap[String, KafkaTopicInfo]()

  override def changePartitions(topicKey: TopicKey, numberOfPartitions: Int): Future[Unit] = {
    val previous = cachedTopics.get(topicKey.topicNameOnKafka())
    if (previous == null)
      Future.failed(new NoSuchElementException(
        s"the topic:${topicKey.topicNameOnKafka()} doesn't exist. actual:${cachedTopics.keys().asScala.mkString(",")}"))
    else {
      cachedTopics.put(
        topicKey.topicNameOnKafka(),
        new KafkaTopicInfo(
          name = previous.name,
          numberOfPartitions = numberOfPartitions,
          numberOfReplications = previous.numberOfReplications,
          partitionInfos = previous.partitionInfos,
          configs = previous.configs
        )
      )
      Future.unit
    }
  }

  override def topics(): Future[Seq[TopicAdmin.KafkaTopicInfo]] =
    Future.successful {
      cachedTopics.values().asScala.toSeq
    }

  override def creator: TopicAdmin.Creator = (topicKey, numberOfPartitions, numberOfReplications, configs) =>
    if (cachedTopics.contains(topicKey.topicNameOnKafka()))
      Future.failed(new IllegalArgumentException(s"${topicKey.topicNameOnKafka()} already exists!"))
    else {
      val topicInfo = new KafkaTopicInfo(
        name = topicKey.topicNameOnKafka(),
        numberOfPartitions = numberOfPartitions,
        numberOfReplications = numberOfReplications,
        partitionInfos = Seq.empty,
        configs
      )
      if (cachedTopics.putIfAbsent(topicKey.topicNameOnKafka(), topicInfo) != null)
        throw new RuntimeException(s"the ${topicKey.topicNameOnKafka()} already exists in kafka")
      Future.unit
  }
  private[this] var _closed = false
  override def close(): Unit = {
    _closed = true
  }

  override def closed: Boolean = _closed
  override def delete(topicName: String): Future[Boolean] = {
    val removed = cachedTopics.remove(topicName)
    if (removed == null) Future.successful(false)
    else Future.successful(true)
  }

  override def exist(topicKey: TopicKey): Future[Boolean] =
    Future.successful(cachedTopics.containsKey(topicKey.topicNameOnKafka()))
}
