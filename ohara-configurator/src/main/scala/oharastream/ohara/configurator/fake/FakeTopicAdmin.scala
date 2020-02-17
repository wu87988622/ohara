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

package oharastream.ohara.configurator.fake

import java.util.Collections
import java.util.concurrent.{CompletableFuture, CompletionStage, ConcurrentHashMap}
import java.{lang, util}

import oharastream.ohara.kafka.{TopicAdmin, TopicCreator, TopicDescription, TopicOption}

private[configurator] class FakeTopicAdmin extends TopicAdmin {
  import scala.collection.JavaConverters._

  override val connectionProps: String = "Unknown"

  private[this] val cachedTopics = new ConcurrentHashMap[String, TopicDescription]()

  override def createPartitions(name: String, numberOfPartitions: Int): CompletionStage[Void] = {
    val previous = cachedTopics.get(name)
    val f        = new CompletableFuture[Void]()
    if (previous == null)
      f.completeExceptionally(
        new NoSuchElementException(
          s"the topic:$name doesn't exist. actual:${cachedTopics.keys().asScala.mkString(",")}"
        )
      )
    else {
      cachedTopics.put(
        name,
        new TopicDescription(
          previous.name,
          previous.partitionInfos(),
          previous.options
        )
      )
    }
    f
  }

  override def topicDescriptions(): CompletionStage[util.List[TopicDescription]] =
    CompletableFuture.completedFuture(new util.ArrayList[TopicDescription](cachedTopics.values()))

  override def topicCreator(): TopicCreator =
    (numberOfPartitions: Int, numberOfReplications: Short, options: util.Map[String, String], name: String) => {
      val f = new CompletableFuture[Void]()
      if (cachedTopics.contains(name))
        f.completeExceptionally(new IllegalArgumentException(s"$name already exists!"))
      else {
        val topicInfo = new TopicDescription(
          name,
          Collections.emptyList(),
          options.asScala
            .map {
              case (key, value) =>
                new TopicOption(
                  key,
                  value,
                  false,
                  false,
                  false
                )
            }
            .toSeq
            .asJava
        )
        if (cachedTopics.putIfAbsent(name, topicInfo) != null)
          throw new RuntimeException(s"the $name already exists in kafka")
        f.complete(null)
      }
      f
    }

  private[this] var _closed = false

  override def close(): Unit = _closed = true

  override def closed(): Boolean = _closed

  override def brokerPorts(): CompletionStage[util.Map[String, Integer]] =
    CompletableFuture.completedFuture(Collections.emptyMap())

  override def exist(name: String): CompletionStage[lang.Boolean] =
    CompletableFuture.completedFuture(cachedTopics.containsKey(name))

  override def deleteTopic(name: String): CompletionStage[lang.Boolean] = {
    val f       = new CompletableFuture[lang.Boolean]()
    val removed = cachedTopics.remove(name)
    if (removed == null) f.complete(false)
    else f.complete(true)
    f
  }
}
