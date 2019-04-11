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

package com.island.ohara.connector.perf
import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.data.{Cell, Column, DataType, Row}
import com.island.ohara.common.util.{ByteUtils, CommonUtils}
import com.island.ohara.kafka.connector.{RowSourceRecord, RowSourceTask, TaskConfig}

import scala.collection.JavaConverters._

class PerfSourceTask extends RowSourceTask {
  private[this] var props: PerfSourceProps = _
  private[this] var topics: Seq[String] = _
  @VisibleForTesting
  private[perf] var schema: Seq[Column] = _
  private[this] var lastPoll: Long = -1
  @VisibleForTesting
  override protected[perf] def _start(config: TaskConfig): Unit = {
    this.props = PerfSourceProps(config.raw().asScala.toMap)
    this.topics = config.topicNames().asScala
    this.schema = config.columns.asScala
    if (schema.isEmpty) schema = DEFAULT_SCHEMA
  }

  override protected def _stop(): Unit = {}

  override protected def _poll(): java.util.List[RowSourceRecord] = {
    val current = CommonUtils.current()
    if (current - lastPoll > props.freq.toMillis) {
      val row: Row = Row.of(
        schema.sortBy(_.order).map { c =>
          Cell.of(
            c.name,
            c.dataType match {
              case DataType.BOOLEAN => false
              case DataType.BYTE    => ByteUtils.toBytes(current).head
              case DataType.BYTES   => ByteUtils.toBytes(current)
              case DataType.SHORT   => current.toShort
              case DataType.INT     => current.toInt
              case DataType.LONG    => current
              case DataType.FLOAT   => current.toFloat
              case DataType.DOUBLE  => current.toDouble
              case DataType.STRING  => current.toString
              case _                => current
            }
          )
        }: _*
      )
      val records: Seq[RowSourceRecord] = topics.map(RowSourceRecord.builder().row(row).topic(_).build())
      lastPoll = current
      (0 until props.batch).flatMap(_ => records).asJava
    } else Seq.empty.asJava
  }
}
