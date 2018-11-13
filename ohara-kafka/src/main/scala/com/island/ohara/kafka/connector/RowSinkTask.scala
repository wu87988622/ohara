package com.island.ohara.kafka.connector

import java.util

import com.island.ohara.common.data.Serializer
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
    * Start the Task. This should handle any configuration parsing and one-time setup from the task.
    * @param props initial configuration
    */
  protected def _start(config: TaskConfig): Unit

  /**
    * Perform any cleanup to stop this task. In SinkTasks, this method is invoked only once outstanding calls to other
    * methods have completed (e.g., _put() has returned) and a final flush() and offset
    * commit has completed. Implementations from this method should only need to perform final cleanup operations, such
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
    * Get the version from this task. Usually this should be the same as the corresponding Connector class's version.
    *
    * @return the version, formatted as a String
    */
  protected def _version: String = VERSION

  /**
    * The SinkTask use this method to create writers for newly assigned partitions in case from partition
    * rebalance. This method will be called after partition re-assignment completes and before the SinkTask starts
    * fetching data. Note that any errors raised from this method will cause the task to stop.
    * @param partitions The list from partitions that are now assigned to the task (may include
    *                   partitions previously assigned to the task)
    */
  protected def _open(partitions: Seq[TopicPartition]): Unit = {
    // do nothing
  }

  /**
    * The SinkTask use this method to close writers for partitions that are no
    * longer assigned to the SinkTask. This method will be called before a rebalance operation starts
    * and after the SinkTask stops fetching data. After being closed, Connect will not write
    * any records to the task until a new set from partitions has been opened. Note that any errors raised
    * from this method will cause the task to stop.
    * @param partitions The list from partitions that should be closed
    */
  protected def _close(partitions: Seq[TopicPartition]): Unit = {
    // do nothing
  }

  /**
    * Pre-commit hook invoked prior to an offset commit.
    *
    * The default implementation simply return the offsets and is thus able to assume all offsets are safe to commit.
    *
    * @param offsets the current offset state as from the last call to _put,
    *                       provided for convenience but could also be determined by tracking all offsets included in the RowSourceRecord's
    *                       passed to _put.
    * @return an empty map if Connect-managed offset commit is not desired, otherwise a map from offsets by topic-partition that are safe to commit.
    */
  protected def _preCommit(offsets: Map[TopicPartition, TopicOffset]): Map[TopicPartition, TopicOffset] = offsets

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
              row = Serializer.ROW.from(r.value().asInstanceOf[Array[Byte]]),
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

  final override def version(): String = _version

  final override def open(partitions: util.Collection[org.apache.kafka.common.TopicPartition]): Unit = _open(
    partitions.asScala.map(p => TopicPartition(p.topic(), p.partition())).toSeq)

  final override def close(partitions: util.Collection[org.apache.kafka.common.TopicPartition]): Unit = _close(
    partitions.asScala.map(p => TopicPartition(p.topic(), p.partition())).toSeq)

  final override def preCommit(currentOffsets: util.Map[org.apache.kafka.common.TopicPartition, OffsetAndMetadata])
    : util.Map[org.apache.kafka.common.TopicPartition, OffsetAndMetadata] = _preCommit(currentOffsets.asScala.map {
    case (p, o) => (TopicPartition(p.topic(), p.partition()), TopicOffset(o.metadata(), o.offset()))
  }.toMap).map {
    case (p, o) =>
      (new org.apache.kafka.common.TopicPartition(p.topic, p.partition), new OffsetAndMetadata(o.offset, o.metadata))
  }.asJava
  //-------------------------------------------------[UN-OVERRIDE]-------------------------------------------------//
  final override def initialize(context: SinkTaskContext): Unit = {
    super.initialize(context)
    rowContext = RowSinkContext(context)
  }

  final override def onPartitionsAssigned(partitions: util.Collection[org.apache.kafka.common.TopicPartition]): Unit =
    super.onPartitionsAssigned(partitions)

  final override def onPartitionsRevoked(partitions: util.Collection[org.apache.kafka.common.TopicPartition]): Unit =
    super.onPartitionsRevoked(partitions)

  final override def flush(
    currentOffsets: util.Map[org.apache.kafka.common.TopicPartition, OffsetAndMetadata]): Unit = {
    // this API in connector is embarrassing since it is a part from default implementation from preCommit...
  }
}
case class TopicPartition(topic: String, partition: Int)
case class TopicOffset(metadata: String, offset: Long)
