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

package oharastream.ohara.connector.perf
import java.util.Collections

import oharastream.ohara.common.annotations.VisibleForTesting
import oharastream.ohara.common.data.{Cell, Column, DataType, Row, Serializer}
import oharastream.ohara.common.setting.TopicKey
import oharastream.ohara.common.util.{ByteUtils, CommonUtils}
import oharastream.ohara.kafka.connector.{RowSourceRecord, RowSourceTask, TaskSetting}

import scala.jdk.CollectionConverters._

class PerfSourceTask extends RowSourceTask {
  private[this] var props: PerfSourceProps = _
  private[this] var topics: Set[TopicKey]  = _
  @VisibleForTesting
  private[perf] var schema: Seq[Column] = _
  private[this] var lastPoll: Long      = -1

  /**
    * this is what we push to topics. We don't generate it repeatedly to avoid extra cost in testing.
    */
  private[this] var records: java.util.List[RowSourceRecord] = java.util.List.of()
  private[this] var rowBytes: Array[Byte]                    = _

  override protected def run(settings: TaskSetting): Unit = {
    this.props = PerfSourceProps(settings)
    this.topics = settings.topicKeys().asScala.toSet
    this.schema = settings.columns.asScala.toSeq
    if (schema.isEmpty) schema = DEFAULT_SCHEMA
    val row = Row.of(
      schema.sortBy(_.order).map { c =>
        Cell.of(
          c.newName,
          c.dataType match {
            case DataType.BOOLEAN => false
            case DataType.BYTE    => ByteUtils.toBytes(CommonUtils.current()).head
            case DataType.BYTES   => new Array[Byte](props.cellSize)
            case DataType.SHORT   => CommonUtils.current().toShort
            case DataType.INT     => CommonUtils.current().toInt
            case DataType.LONG    => CommonUtils.current()
            case DataType.FLOAT   => CommonUtils.current().toFloat
            case DataType.DOUBLE  => CommonUtils.current().toDouble
            case DataType.STRING  => CommonUtils.randomString(props.cellSize)
            case _                => CommonUtils.current()
          }
        )
      }: _*
    )
    this.rowBytes = Serializer.ROW.to(row)
    records = Collections.unmodifiableList(
      (0 until props.batch).flatMap(_ => topics.map(RowSourceRecord.builder().row(row).topicKey(_).build())).asJava
    )
  }

  override protected def toBytes(record: RowSourceRecord): Array[Byte] = rowBytes

  override protected def terminate(): Unit = {}

  override protected def pollRecords(): java.util.List[RowSourceRecord] = {
    val current = CommonUtils.current()
    if (current - lastPoll > props.freq.toMillis) {
      lastPoll = current
      records
    } else java.util.List.of()
  }
}
