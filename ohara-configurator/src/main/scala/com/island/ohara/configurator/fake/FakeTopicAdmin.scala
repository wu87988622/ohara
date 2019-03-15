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

import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeTopicAdmin extends TopicAdmin {
  import scala.collection.JavaConverters._

  override val connectionProps: String = "Unknown"

  private[this] val cachedTopics = new ConcurrentHashMap[String, TopicInfo]()

  override def changePartitions(name: String, numberOfPartitions: Int)(
    implicit executionContext: ExecutionContext): Future[TopicInfo] =
    Option(cachedTopics.get(name))
      .map(
        previous =>
          Future.successful(
            TopicInfo(
              name,
              numberOfPartitions,
              previous.numberOfReplications
            )))
      .getOrElse(Future.failed(new NoSuchElementException(
        s"the topic:$name doesn't exist. actual:${cachedTopics.keys().asScala.mkString(",")}")))

  override def list(implicit executionContext: ExecutionContext): Future[Seq[TopicAdmin.TopicInfo]] =
    Future.successful {
      cachedTopics.values().asScala.toSeq
    }

  override def creator(): TopicAdmin.Creator = (_, name, numberOfPartitions, numberOfReplications, _) =>
    if (cachedTopics.contains(name)) Future.failed(new IllegalArgumentException(s"$name already exists!"))
    else {
      cachedTopics.put(name, TopicInfo(name, numberOfPartitions, numberOfReplications))
      Future.successful(
        TopicInfo(
          name,
          numberOfPartitions,
          numberOfReplications
        ))
  }
  private[this] var _closed = false
  override def close(): Unit = {
    _closed = true
  }

  override def closed(): Boolean = _closed
  override def delete(name: String)(implicit executionContext: ExecutionContext): Future[TopicInfo] =
    Option(cachedTopics.remove(name))
      .map(Future.successful)
      .getOrElse(Future.failed(new NoSuchElementException(s"the topic:$name doesn't exist")))
}
