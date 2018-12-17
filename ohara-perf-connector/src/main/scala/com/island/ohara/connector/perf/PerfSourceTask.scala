package com.island.ohara.connector.perf
import com.island.ohara.common.data.{Cell, Column, DataType, Row}
import com.island.ohara.common.util.{ByteUtil, CommonUtil}
import com.island.ohara.kafka.connector.{RowSourceRecord, RowSourceTask, TaskConfig}

import scala.collection.JavaConverters._

class PerfSourceTask extends RowSourceTask {
  private[this] var props: PerfSourceProps = _
  private[this] var topics: Seq[String] = _
  private[this] var schema: Seq[Column] = _
  private[this] var lastPoll: Long = -1
  override protected def _start(config: TaskConfig): Unit = {
    this.props = PerfSourceProps(config.options.asScala.toMap)
    this.topics = config.topics.asScala
    this.schema = config.schema.asScala
  }

  override protected def _stop(): Unit = {}

  override protected def _poll(): java.util.List[RowSourceRecord] = {
    val current = CommonUtil.current()
    if (current - lastPoll > props.freq.toMillis) {
      val row: Row = Row.of(
        schema.sortBy(_.order).map { c =>
          Cell.of(
            c.name,
            c.dataType match {
              case DataType.BOOLEAN => false
              case DataType.BYTE    => ByteUtil.toBytes(current).head
              case DataType.BYTES   => ByteUtil.toBytes(current)
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
      val records: Seq[RowSourceRecord] = topics.map(RowSourceRecord.builder().row(row).build(_))
      lastPoll = current
      (0 until props.batch).flatMap(_ => records).asJava
    } else Seq.empty.asJava
  }
}
