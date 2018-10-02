package com.island.ohara.kafka.connector

import java.util

import com.island.ohara.serialization.RowSerializer
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.{SourceRecord, SourceTask, SourceTaskContext}

import scala.collection.JavaConverters._

/**
  * A wrap to SourceTask. The users should substitute RowSourceRecord for SourceRecord.
  */
abstract class RowSourceTask extends SourceTask {

  /**
    * Start the Task. This should handle any configuration parsing and one-time setup of the task.
    * @param config initial configuration
    * @param schema the schema should be used in this connector task
    * @param topics the target topics
    */
  protected def _start(config: TaskConfig): Unit

  /**
    * Signal this SourceTask to stop. In SourceTasks, this method only needs to signal to the task that it should stop
    * trying to poll for new data and interrupt any outstanding poll() requests. It is not required that the task has
    * fully stopped. Note that this method necessarily may be invoked from a different thread than _poll() and _commit()
    */
  protected def _stop(): Unit

  /**
    * Poll this SourceTask for new records. This method should block if no data is currently available.
    *
    * @return a array of RowSourceRecord
    */
  protected def _poll(): Seq[RowSourceRecord]

  /**
    * Commit an individual RowSourceRecord when the callback from the producer client is received, or if a record is filtered by a transformation.
    * SourceTasks are not required to implement this functionality; Kafka Connect will record offsets
    * automatically. This hook is provided for systems that also need to store offsets internally
    * in their own system.
    *
    * @param record RowSourceRecord that was successfully sent via the producer.
    */
  protected def _commitRecord(record: RowSourceRecord): Unit = {
    // do nothing
  }

  /**
    * Commit the offsets, up to the offsets that have been returned by _poll(). This
    * method should block until the commit is complete.
    *
    * SourceTasks are not required to implement this functionality; Kafka Connect will record offsets
    * automatically. This hook is provided for systems that also need to store offsets internally
    * in their own system.
    */
  protected def _commit(): Unit = {
    // do nothing
  }

  /**
    * Get the version of this task. Usually this should be the same as the corresponding Connector class's version.
    *
    * @return the version, formatted as a String
    */
  protected def _version: String

  /**
    * @return RowSourceContext is provided to RowSourceTask to allow them to interact with the underlying runtime.
    */
  protected var rowContext: RowSourceContext = _
  //-------------------------------------------------[WRAPPED]-------------------------------------------------//
  final override def poll(): util.List[SourceRecord] = {
    val value = _poll()
    // kafka connector doesn't support the empty list in testing. see https://github.com/apache/kafka/pull/4958
    if (value == null || value.isEmpty) null
    else
      value
        .map(s => {
          new SourceRecord(
            s.sourcePartition.asJava,
            s.sourceOffset.asJava,
            s.topic,
            s.partition.map(new Integer(_)).orNull,
            Schema.BYTES_SCHEMA,
            s.key,
            Schema.BYTES_SCHEMA,
            RowSerializer.to(s.row),
            s.timestamp.map(new java.lang.Long(_)).orNull
          )
        })
        .toList
        .asJava
  }

  final override def start(props: util.Map[String, String]): Unit = _start(toTaskConfig(props))
  final override def stop(): Unit = _stop()

  final override def commit(): Unit = _commit()

  // TODO: We do a extra conversion here (bytes => Row)... by chia
  final override def commitRecord(record: SourceRecord): Unit = _commitRecord(
    RowSourceRecord
      .builder()
      .sourcePartition(if (record.sourcePartition() == null) Map.empty else record.sourcePartition().asScala.toMap)
      .sourceOffset(if (record.sourceOffset() == null) Map.empty else record.sourceOffset().asScala.toMap)
      .row(RowSerializer.from(record.value().asInstanceOf[Array[Byte]]))
      ._timestamp(Option[java.lang.Long](record.timestamp()).map(_.longValue()))
      ._partition(Option[java.lang.Integer](record.kafkaPartition()).map(_.intValue()))
      .build(record.topic()))

  final override def version(): String = _version

  //-------------------------------------------------[UN-OVERRIDE]-------------------------------------------------//
  final override def initialize(context: SourceTaskContext): Unit = {
    super.initialize(context)
    rowContext = RowSourceContext(context)
  }
}
