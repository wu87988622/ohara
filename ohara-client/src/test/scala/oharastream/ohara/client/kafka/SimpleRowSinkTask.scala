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

package oharastream.ohara.client.kafka

import java.util

import oharastream.ohara.common.data.{Row, Serializer}
import oharastream.ohara.common.util.Releasable
import oharastream.ohara.kafka.Producer
import oharastream.ohara.kafka.connector.{RowSinkRecord, RowSinkTask, TaskSetting}

import scala.collection.JavaConverters._

class SimpleRowSinkTask extends RowSinkTask {
  private[this] var outputTopic: String                  = _
  private[this] var producer: Producer[Row, Array[Byte]] = _
  override protected def _start(settings: TaskSetting): Unit = {
    outputTopic = settings.stringValue(OUTPUT)
    producer = Producer.builder
      .connectionProps(settings.stringValue(BROKER))
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
  }

  override protected def _stop(): Unit = Releasable.close(producer)

  override protected def _put(records: util.List[RowSinkRecord]): Unit =
    records.asScala.foreach(r => producer.sender().key(r.row()).topicName(outputTopic).send())
}
