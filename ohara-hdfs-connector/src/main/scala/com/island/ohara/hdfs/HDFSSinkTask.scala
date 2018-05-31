package com.island.ohara.hdfs

import java.util
import com.island.ohara.kafka.connector.{RowSinkRecord, RowSinkTask}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._

/**
  *This class extends RowSinkTask abstract
  */
class HDFSSinkTask extends RowSinkTask {

  private[this] lazy val logger = Logger(getClass().getName())

  var hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = _
  var hdfsWriter: DataWriter = _

  override def start(props: util.Map[String, String]): Unit = {
    logger.info("starting HDFS Sink Connector")
    hdfsSinkConnectorConfig = new HDFSSinkConnectorConfig(props)
    hdfsWriter = new DataWriter(hdfsSinkConnectorConfig, context)
  }

  override protected def open(partitions: util.Collection[TopicPartition]): Unit = {
    logger.info(s"running open function. The partition size is: ${partitions.size()}")
    hdfsWriter.createPartitionDataWriters(partitions.asScala.toList)
  }

  override protected def _put(records: Array[RowSinkRecord]): Unit = {
    hdfsWriter.write(records)
  }

  override protected def flush(offsets: util.Map[TopicPartition, OffsetAndMetadata]): Unit = {
    //TODO The OHARA-98 is going to implement
  }

  override protected def close(partitions: util.Collection[TopicPartition]): Unit = {
    logger.info("running close function")
    if (partitions != null) {
      hdfsWriter.removePartitionWriters(partitions.asScala.toList)
    }
  }

  override def stop(): Unit = {
    logger.info("running stop function")
    hdfsWriter.stop()
  }

  override def version(): String = {
    Version.getVersion()
  }
}
