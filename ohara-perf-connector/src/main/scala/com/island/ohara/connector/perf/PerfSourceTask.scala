package com.island.ohara.connector.perf
import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.io.ByteUtil
import com.island.ohara.kafka.connector.{RowSourceRecord, RowSourceTask, TaskConfig}
import com.island.ohara.serialization.DataType
import com.island.ohara.util.SystemUtil

class PerfSourceTask extends RowSourceTask {
  private[this] var props: PerfSourceProps = _
  private[this] var topics: Seq[String] = _
  private[this] var schema: Seq[Column] = _
  private[this] var lastPoll: Long = -1
  override protected def _start(config: TaskConfig): Unit = {
    this.props = PerfSourceProps(config.options)
    this.topics = config.topics
    this.schema = config.schema
  }

  override protected def _stop(): Unit = {}

  override protected def _poll(): Seq[RowSourceRecord] = {
    val current = SystemUtil.current()
    if (current - lastPoll > props.freq.toMillis) {
      val row: Row = Row(
        schema.sortBy(_.order).map { c =>
          Cell(
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
      (0 until props.batch).flatMap(_ => records)
    } else Seq.empty
  }
}
