package com.island.ohara.hdfs

import java.util
import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.kafka.connector.{RowSinkRecord, RowSinkTask}

/**
  *This class extends RowSinkTask abstract
  */
class HDFSSinkTask extends RowSinkTask {
  var hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = _

  override def start(props: util.Map[String, String]): Unit = {
    hdfsSinkConnectorConfig = new HDFSSinkConnectorConfig(props)
  }

  override protected def _put(records: Array[RowSinkRecord]): Unit = {
    //TODO
  }

  override def stop(): Unit = {
    //TODO
  }

  override def version(): String = {
    Version.getVersion()
  }
}
