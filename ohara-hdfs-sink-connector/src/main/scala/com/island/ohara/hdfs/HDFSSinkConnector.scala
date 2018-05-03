package com.island.ohara.hdfs

import java.util

import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkTask}
import org.apache.kafka.common.config.ConfigDef

/**
  * This class extends RowSinkConnector abstract.
  */
class HDFSSinkConnector extends RowSinkConnector {

  override protected def _taskClass(): Class[_ <: RowSinkTask] = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }

  override def start(props: util.Map[String, String]): Unit = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }

  override def version(): String = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }

  override def stop(): Unit = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }

  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }

  override def config(): ConfigDef = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }

  override protected def _taskConfigs(): Seq[Map[String, String]] = {
    //TODO
    throw new UnsupportedOperationException("This method doesn't implement at present");
  }
}
