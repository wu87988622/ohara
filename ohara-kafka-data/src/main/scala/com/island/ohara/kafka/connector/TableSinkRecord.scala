package com.island.ohara.kafka.connector

import com.island.ohara.core.Table
import com.island.ohara.serialization.TableReader
import org.apache.kafka.common.record.TimestampType
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.sink.SinkRecord

/**
  * The methods it have are almost same with SinkRecord.
  * It return Table rather than any object. Also, it doesn't have method to return value schema
  * because the value schema is useless to user.
  *
  * @param sinkRecord a sink record passed by kafka connector
  */
class TableSinkRecord(sinkRecord: SinkRecord) {

  def kafkaOffset: Long = sinkRecord.kafkaOffset()

  def timestampType: TimestampType = sinkRecord.timestampType()

  def topic: String = sinkRecord.topic()

  def kafkaPartition: Option[Int] = if (sinkRecord.kafkaPartition == null) None else Some(sinkRecord.kafkaPartition)

  def key: Any = sinkRecord.key()

  def keySchema: Schema = sinkRecord.keySchema()

  def value: Table = sinkRecord.value match {
    case buf: Array[Byte] => TableReader.toTable(buf)
    case _ => throw new IllegalStateException(s"Why we get a non-supported type:${sinkRecord.value.getClass.getName}")
  }

  def timestamp: Option[Long] = if (sinkRecord.timestamp == null) None else Some(sinkRecord.timestamp)
}
