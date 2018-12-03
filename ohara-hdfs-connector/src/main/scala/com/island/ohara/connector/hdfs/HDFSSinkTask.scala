package com.island.ohara.connector.hdfs
import java.util

import com.island.ohara.kafka.connector._
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

/**
  *This class extends RowSinkTask abstract
  */
class HDFSSinkTask extends RowSinkTask {

  private[this] lazy val logger = Logger(getClass.getName)

  var hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = _
  var hdfsWriter: DataWriter = _

  override protected def _open(partitions: util.List[TopicPartition]): Unit = {
    logger.info(s"running open function. The partition size is: ${partitions.size}")
    hdfsWriter.createPartitionDataWriters(partitions.asScala)
  }

  override protected def _preCommit(
    offsets: util.Map[TopicPartition, TopicOffset]): util.Map[TopicPartition, TopicOffset] = {
    logger.debug("running flush function.")
    offsets.asScala.foreach { case (p, o) => logger.debug(s"[${p.topic}-${p.partition}] offset: ${o.offset}") }
    offsets
  }

  override protected def _close(partitions: util.List[TopicPartition]): Unit = {
    logger.info("running close function")
    if (partitions != null) {
      hdfsWriter.removePartitionWriters(partitions.asScala)
    }
  }

  override protected def _start(props: TaskConfig): Unit = {
    logger.info("starting HDFS Sink Connector")
    hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(props.options.asScala.toMap)
    hdfsWriter = new DataWriter(hdfsSinkConnectorConfig, rowContext, props.schema.asScala)
  }

  override protected def _stop(): Unit = {
    logger.info("running stop function")
    hdfsWriter.stop()
  }

  override protected def _put(records: util.List[RowSinkRecord]): Unit =
    try {
      hdfsWriter.write(records.asScala)
    } catch {
      case e: Throwable => logger.error("failed to write to HDFS", e)
    }

}
