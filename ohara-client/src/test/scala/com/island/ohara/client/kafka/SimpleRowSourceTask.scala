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

import java.util
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.common.util.Releasable
import com.island.ohara.kafka.Consumer
import com.island.ohara.kafka.connector.{RowSourceRecord, RowSourceTask, TaskSetting}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class SimpleRowSourceTask extends RowSourceTask {

  private[this] var settings: TaskSetting = _
  private[this] val queue = new LinkedBlockingQueue[RowSourceRecord]
  private[this] val closed = new AtomicBoolean(false)
  private[this] var consumer: Consumer[Row, Array[Byte]] = _
  override protected def _start(settings: TaskSetting): Unit = {
    this.settings = settings
    this.consumer = Consumer
      .builder()
      .connectionProps(settings.stringValue(BROKER))
      .groupId(settings.name)
      .topicName(settings.stringValue(INPUT))
      .offsetFromBegin()
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    Future {
      try while (!closed.get) {
        consumer
          .poll(java.time.Duration.ofSeconds(2))
          .asScala
          .filter(_.key.isPresent)
          .map(_.key.get)
          .flatMap(row =>
            settings.topicNames().asScala.map(topic => RowSourceRecord.builder().row(row).topicName(topic).build()))
          .foreach(r => queue.put(r))
      } finally Releasable.close(consumer)
    }
  }

  override protected def _poll(): util.List[RowSourceRecord] =
    Iterator.continually(queue.poll()).takeWhile(_ != null).toSeq.asJava

  override protected def _stop(): Unit = {
    closed.set(true)
    consumer.wakeup()
  }
}
