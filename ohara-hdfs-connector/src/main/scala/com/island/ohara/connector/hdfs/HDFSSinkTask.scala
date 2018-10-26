package com.island.ohara.connector.hdfs
import com.island.ohara.kafka.connector.{RowSinkRecord, RowSinkTask, TaskConfig, TopicOffset, TopicPartition}
import com.island.ohara.util.VersionUtil
import com.typesafe.scalalogging.Logger

/**
  *This class extends RowSinkTask abstract
  */
class HDFSSinkTask extends RowSinkTask {

  private[this] lazy val logger = Logger(getClass.getName)

  var hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = _
  var hdfsWriter: DataWriter = _

  override def _start(props: TaskConfig): Unit = {
    logger.info("starting HDFS Sink Connector")
    hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(props.options)
    hdfsWriter = new DataWriter(hdfsSinkConnectorConfig, rowContext, props.schema)
  }

  override protected def _open(partitions: Seq[TopicPartition]): Unit = {
    logger.info(s"running open function. The partition size is: ${partitions.size}")
    hdfsWriter.createPartitionDataWriters(partitions)
  }

  override protected def _put(records: Seq[RowSinkRecord]): Unit = try {
    hdfsWriter.write(records)
  } catch {
    case e: Throwable => logger.error("failed to write to HDFS", e)
  }

  override protected def _preCommit(offsets: Map[TopicPartition, TopicOffset]): Map[TopicPartition, TopicOffset] = {
    logger.debug("running flush function.")
    offsets.foreach { case (p, o) => logger.debug(s"[${p.topic}-${p.partition}] offset: ${o.offset}") }
    offsets
  }

  override protected def _close(partitions: Seq[TopicPartition]): Unit = {
    logger.info("running close function")
    if (partitions != null) {
      hdfsWriter.removePartitionWriters(partitions)
    }
  }

  override def _stop(): Unit = {
    logger.info("running stop function")
    hdfsWriter.stop()
  }

  override val _version: String = VersionUtil.VERSION
}
