package com.island.ohara.kafka.connector

import com.island.ohara.core.Table
import com.island.ohara.serialization.TableWriter
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.SourceRecord

import scala.collection.JavaConverters._

/**
  * A wrap to SourceRecord. Currently, only value schema and value are changed.
  */
class TableSourceRecord(sourcePartition: Map[String, _],
                        sourceOffset: Map[String, _],
                        topic: String,
                        partition: Int,
                        keySchema: Schema,
                        key: Any,
                        table: Table,
                        timestamp: Long)
    extends SourceRecord(
      sourcePartition.asJava,
      sourceOffset.asJava,
      topic,
      if (partition < 0) null else partition,
      keySchema,
      key,
      Schema.BYTES_SCHEMA,
      TableWriter.toBytes(table),
      if (timestamp < 0) null else timestamp
    ) {

  def this(sourcePartition: Map[String, _], sourceOffset: Map[String, _], topic: String, table: Table) {
    this(sourcePartition, sourceOffset, topic, -1, null, null, table, -1)
  }

  def this(sourcePartition: Map[String, _],
           sourceOffset: Map[String, _],
           topic: String,
           keySchema: Schema,
           key: Any,
           table: Table) {
    this(sourcePartition, sourceOffset, topic, -1, keySchema, key, table, -1)
  }

  def this(sourcePartition: Map[String, _],
           sourceOffset: Map[String, _],
           topic: String,
           partition: Int,
           keySchema: Schema,
           key: Any,
           table: Table) {
    this(sourcePartition, sourceOffset, topic, partition, keySchema, key, table, -1)
  }

  /**
    * DON'T call this method in TableSourceTask since the object you pass is converted to a specific kafka object.
    *
    * @return a kafka object
    */
  override def value(): AnyRef = super.value()
}
