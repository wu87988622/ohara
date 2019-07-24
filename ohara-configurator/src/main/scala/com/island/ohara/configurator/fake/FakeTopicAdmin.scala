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
import com.island.ohara.client.kafka.TopicAdmin.TopicInfo

import scala.concurrent.Future

private[configurator] class FakeTopicAdmin extends TopicAdmin {
  import scala.collection.JavaConverters._

  override val connectionProps: String = "Unknown"

  private[this] val cachedTopics = new ConcurrentHashMap[String, TopicInfo]()

  override def changePartitions(name: String, numberOfPartitions: Int): Future[Unit] = {
    val previous = cachedTopics.get(name)
    if (previous == null)
      Future.failed(
        new NoSuchElementException(
          s"the topic:$name doesn't exist. actual:${cachedTopics.keys().asScala.mkString(",")}"))
    else {
      cachedTopics.put(name, previous.copy(numberOfPartitions = numberOfPartitions))
      Future.unit
    }
  }

  override def topics(): Future[Seq[TopicAdmin.TopicInfo]] =
    Future.successful {
      cachedTopics.values().asScala.toSeq
    }

  override def creator: TopicAdmin.Creator = (name, numberOfPartitions, numberOfReplications, configs) =>
    if (cachedTopics.contains(name)) Future.failed(new IllegalArgumentException(s"$name already exists!"))
    else {
      val topicInfo = TopicInfo(
        name = name,
        numberOfPartitions = numberOfPartitions,
        numberOfReplications = numberOfReplications,
        configs
      )
      cachedTopics.put(name, topicInfo)
      Future.unit
  }
  private[this] var _closed = false
  override def close(): Unit = {
    _closed = true
  }

  override def closed: Boolean = _closed
  override def delete(name: String): Future[Boolean] = {
    val removed = cachedTopics.remove(name)
    if (removed == null) Future.successful(false)
    else Future.successful(true)
  }
}
