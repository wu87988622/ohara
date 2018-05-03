package com.island.ohara.hdfs

import java.util

import com.island.ohara.kafka.connector.{RowSinkRecord, RowSinkTask}

/**
  *This class extends RowSinkTask abstract
  */
class HDFSSinkTask extends RowSinkTask {

  override protected def _put(records: Array[RowSinkRecord]): Unit = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }

  override def start(props: util.Map[String, String]): Unit = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }

  override def stop(): Unit = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }

  override def version(): String = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }
}
