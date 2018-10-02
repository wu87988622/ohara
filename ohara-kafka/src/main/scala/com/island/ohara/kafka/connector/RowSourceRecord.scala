package com.island.ohara.kafka.connector

import java.util.Objects

import com.island.ohara.data.Row
import com.island.ohara.serialization.Serializer

/**
  * A wrap to SourceRecord. Currently, only value schema and value are changed.
  */
case class RowSourceRecord(sourcePartition: Map[String, _],
                           sourceOffset: Map[String, _],
                           topic: String,
                           partition: Option[Int],
                           row: Row,
                           timestamp: Option[Long]) {

  /**
    * TODO: we don't discuss the key of row yet...
    * @return byte array of row
    */
  def key: Array[Byte] = Serializer.ROW.to(row)
}

object RowSourceRecord {
  def apply(topic: String, row: Row): RowSourceRecord = builder().row(row).build(topic)
  def builder() = new RowSourceRecordBuilder
}

class RowSourceRecordBuilder {
  private[this] var sourcePartition: Map[String, _] = Map.empty
  private[this] var sourceOffset: Map[String, _] = Map.empty
  private[this] var partition: Option[Int] = None
  private[this] var row: Row = Row.empty
  private[this] var timestamp: Option[Long] = None

  def sourcePartition(sourcePartition: Map[String, _]): RowSourceRecordBuilder = {
    this.sourcePartition = Objects.requireNonNull(sourcePartition)
    this
  }

  def sourceOffset(sourceOffset: Map[String, _]): RowSourceRecordBuilder = {
    this.sourceOffset = Objects.requireNonNull(sourceOffset)
    this
  }

  def partition(partition: Int): RowSourceRecordBuilder = {
    this.partition = Some(partition)
    this
  }

  /**
    * this is a helper method for RowSourceTask
    */
  private[connector] def _partition(partition: Option[Int]): RowSourceRecordBuilder = {
    this.partition = partition
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

  /**
    * this is a helper method for RowSourceTask
    */
  private[connector] def _timestamp(timestamp: Option[Long]): RowSourceRecordBuilder = {
    this.timestamp = timestamp
    this
  }

  def build(topic: String): RowSourceRecord = RowSourceRecord(sourcePartition,
                                                              sourceOffset,
                                                              Objects.requireNonNull(topic),
                                                              partition,
                                                              Objects.requireNonNull(row),
                                                              timestamp)

}
