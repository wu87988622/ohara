package com.island.ohara.connector.hdfs

import com.island.ohara.io.VersionUtil
import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkTask, TaskConfig}

/**
  * This class extends RowSinkConnector abstract.
  */
class HDFSSinkConnector extends RowSinkConnector {

  var props: TaskConfig = _

  override protected[hdfs] def _start(props: TaskConfig): Unit = {
    this.props = props
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = {
    classOf[HDFSSinkTask]
  }

  override def _stop(): Unit = {
    //TODO
  }

  override protected[hdfs] def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = {
    Seq.fill(maxTasks) { props }
  }

  override val _version: String = VersionUtil.VERSION
}
