package com.island.ohara.connector.hdfs
import java.util

import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkTask, TaskConfig}

import scala.collection.JavaConverters._

/**
  * This class extends RowSinkConnector abstract.
  */
class HDFSSinkConnector extends RowSinkConnector {

  var props: TaskConfig = _

  override protected[hdfs] def _start(config: TaskConfig): Unit = {
    this.props = config
  }

  override protected def _stop(): Unit = {
    //TODO
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[HDFSSinkTask]

  override protected[hdfs] def _taskConfigs(maxTasks: Int): util.List[TaskConfig] = {
    Seq.fill(maxTasks) { props }.asJava
  }
}
