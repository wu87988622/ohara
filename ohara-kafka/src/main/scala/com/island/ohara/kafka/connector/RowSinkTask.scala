package com.island.ohara.kafka.connector

import java.util

import com.island.ohara.serialization.RowSerializer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.connect.sink.{SinkRecord, SinkTask, SinkTaskContext}

import scala.collection.JavaConverters._

/**
  * A wrap to Kafka SinkTask. It used to convert the Kafka SinkRecord to ohara RowSinkRecord.
  * Ohara developers should extend this class rather than kafka SinkTask in order to let the conversion from SinkRecord to
  * RowSinkRecord work automatically.
  */
abstract class RowSinkTask extends SinkTask {

  /**
    * Start the Task. This should handle any configuration parsing and one-time setup of the task.
    * @param props initial configuration
    */
  protected def _start(config: TaskConfig): Unit

  /**
    * Perform any cleanup to stop this task. In SinkTasks, this method is invoked only once outstanding calls to other
    * methods have completed (e.g., _put() has returned) and a final flush() and offset
    * commit has completed. Implementations of this method should only need to perform final cleanup operations, such
    * as closing network connections to the sink system.
    */
  protected def _stop(): Unit

  /**
    * Put the table record in the sink. Usually this should send the records to the sink asynchronously
    * and immediately return.
    *
    * @param records table record
    */
  protected def _put(records: Seq[RowSinkRecord]): Unit

  /**
    * Flush all records that have been _put for the specified topic-partitions.
    *
    * @param offsets the current offset state as of the last call to _put,
    *                       provided for convenience but could also be determined by tracking all offsets included in the RowSinkRecords
    *                       passed to _put.
    */
  protected def _flush(offsets: Seq[TopicOffset]): Unit = {
    // do nothing
  }

  /**
    * Get the version of this task. Usually this should be the same as the corresponding Connector class's version.
    *
    * @return the version, formatted as a String
    */
  protected def _version: String

  /**
    * The SinkTask use this method to create writers for newly assigned partitions in case of partition
    * rebalance. This method will be called after partition re-assignment completes and before the SinkTask starts
    * fetching data. Note that any errors raised from this method will cause the task to stop.
    * @param partitions The list of partitions that are now assigned to the task (may include
    *                   partitions previously assigned to the task)
    */
  protected def _open(partitions: Seq[TopicPartition]): Unit = {
    // do nothing
  }

  /**
    * The SinkTask use this method to close writers for partitions that are no
    * longer assigned to the SinkTask. This method will be called before a rebalance operation starts
    * and after the SinkTask stops fetching data. After being closed, Connect will not write
    * any records to the task until a new set of partitions has been opened. Note that any errors raised
    * from this method will cause the task to stop.
    * @param partitions The list of partitions that should be closed
    */
  protected def _close(partitions: Seq[TopicPartition]): Unit = {
    // do nothing
  }

  /**
    * @return RowSinkContext is provided to RowSinkTask to allow them to interact with the underlying runtime.
    */
  protected var rowContext: RowSinkContext = _
  //-------------------------------------------------[WRAPPED]-------------------------------------------------//

  final override def put(records: util.Collection[SinkRecord]): Unit = {
    if (records == null) _put(Array[RowSinkRecord]())
    else
      _put(
        records.asScala
          .map(r => {
            RowSinkRecord(
              topic = r.topic(),
              key = r.key().asInstanceOf[Array[Byte]],
              row = RowSerializer.from(r.value().asInstanceOf[Array[Byte]]),
              partition = r.kafkaPartition(),
              offset = r.kafkaOffset(),
              timestamp = r.timestamp(),
              timestampType = TimestampType(r.timestampType().id, r.timestampType().name)
            )
          })
          .toSeq)
  }

  final override def start(props: util.Map[String, String]): Unit = _start(toTaskConfig(props))

  final override def stop(): Unit = _stop()

  final override def flush(currentOffsets: util.Map[org.apache.kafka.common.TopicPartition, OffsetAndMetadata]): Unit =
    _flush(currentOffsets.asScala.map {
      case (p, o) => TopicOffset(p.topic(), p.partition(), o.metadata(), o.offset())
    }.toSeq)

  final override def version(): String = _version

  final override def open(partitions: util.Collection[org.apache.kafka.common.TopicPartition]): Unit = _open(
    partitions.asScala.map(p => TopicPartition(p.topic(), p.partition())).toSeq)

  final override def close(partitions: util.Collection[org.apache.kafka.common.TopicPartition]): Unit = _close(
    partitions.asScala.map(p => TopicPartition(p.topic(), p.partition())).toSeq)
  //-------------------------------------------------[UN-OVERRIDE]-------------------------------------------------//
  final override def initialize(context: SinkTaskContext): Unit = {
    super.initialize(context)
    rowContext = RowSinkContext(context)
  }

  final override def onPartitionsAssigned(partitions: util.Collection[org.apache.kafka.common.TopicPartition]): Unit =
    super.onPartitionsAssigned(partitions)

  final override def onPartitionsRevoked(partitions: util.Collection[org.apache.kafka.common.TopicPartition]): Unit =
    super.onPartitionsRevoked(partitions)

  final override def preCommit(currentOffsets: util.Map[org.apache.kafka.common.TopicPartition, OffsetAndMetadata])
    : util.Map[org.apache.kafka.common.TopicPartition, OffsetAndMetadata] = super.preCommit(currentOffsets)

}
case class TopicPartition(topic: String, partition: Int)
case class TopicOffset(topic: String, partition: Int, metadata: String, offset: Long)
