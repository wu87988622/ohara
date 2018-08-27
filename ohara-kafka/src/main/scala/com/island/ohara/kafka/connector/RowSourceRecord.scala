package com.island.ohara.kafka.connector

import java.util.Objects

import com.island.ohara.data.Row

/**
  * A wrap to SourceRecord. Currently, only value schema and value are changed.
  */
case class RowSourceRecord(sourcePartition: Map[String, _],
                           sourceOffset: Map[String, _],
                           topic: String,
                           partition: Option[Int],
                           row: Row,
                           timestamp: Option[Long])

object RowSourceRecord {
  def apply(topic: String, row: Row): RowSourceRecord = builder.topic(topic).row(row).build()
  def builder = new RowSourceRecordBuilder
}
class RowSourceRecordBuilder {
  private[this] var sourcePartition: Map[String, _] = Map.empty
  private[this] var sourceOffset: Map[String, _] = Map.empty
  private[this] var topic: String = _
  private[this] var partition: Option[Int] = None
  private[this] var row: Row = _
  private[this] var timestamp: Option[Long] = None

  def sourcePartition(sourcePartition: Map[String, _]): RowSourceRecordBuilder = {
    this.sourcePartition = Objects.requireNonNull(sourcePartition)
    this
  }

  def sourceOffset(sourceOffset: Map[String, _]): RowSourceRecordBuilder = {
    this.sourceOffset = Objects.requireNonNull(sourceOffset)
    this
  }

  def topic(topic: String): RowSourceRecordBuilder = {
    this.topic = Objects.requireNonNull(topic)
    this
  }

  def partition(partition: Int): RowSourceRecordBuilder = {
    this.partition = Some(partition)
    this
  }

  def row(row: Row): RowSourceRecordBuilder = {
    this.row = Objects.requireNonNull(row)
    this
  }

  def timestamp(timestamp: Long): RowSourceRecordBuilder = {
    this.timestamp = Some(timestamp)
    this
  }

  def build(): RowSourceRecord = RowSourceRecord(sourcePartition,
                                                 sourceOffset,
                                                 Objects.requireNonNull(topic),
                                                 partition,
                                                 Objects.requireNonNull(row),
                                                 timestamp)

}
