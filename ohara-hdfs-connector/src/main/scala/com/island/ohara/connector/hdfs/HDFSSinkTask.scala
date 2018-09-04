package com.island.ohara.connector.hdfs

import com.island.ohara.kafka.connector.{RowSinkRecord, RowSinkTask, TopicOffset, TopicPartition}
import com.typesafe.scalalogging.Logger

/**
  *This class extends RowSinkTask abstract
  */
class HDFSSinkTask extends RowSinkTask {

  private[this] lazy val logger = Logger(getClass.getName)

  var hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = _
  var hdfsWriter: DataWriter = _

  override def _start(props: Map[String, String]): Unit = {
    logger.info("starting HDFS Sink Connector")
    hdfsSinkConnectorConfig = new HDFSSinkConnectorConfig(props)
    hdfsWriter = new DataWriter(hdfsSinkConnectorConfig, rowContext)
  }

  override protected def _open(partitions: Seq[TopicPartition]): Unit = {
    logger.info(s"running open function. The partition size is: ${partitions.size}")
    hdfsWriter.createPartitionDataWriters(partitions)
  }

  override protected def _put(records: Seq[RowSinkRecord]): Unit = {
    hdfsWriter.write(records)
  }

  override protected def _flush(offsets: Seq[TopicOffset]): Unit = {
    logger.debug("running flush function.")
    offsets.foreach(offset => {
      logger.debug(s"[${offset.topic}-${offset.partition}] offset: ${offset.offset}")
    })
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

  override val _version: String = Version.getVersion
}
