package com.island.ohara.hdfs

import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkTask}

/**
  * This class extends RowSinkConnector abstract.
  */
class HDFSSinkConnector extends RowSinkConnector {

  var props: Map[String, String] = _

  override def _start(props: Map[String, String]): Unit = {
    this.props = props
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = {
    classOf[HDFSSinkTask]
  }

  override def _stop(): Unit = {
    //TODO
  }

  override protected def _taskConfigs(maxTasks: Int): Seq[Map[String, String]] = {
    Seq.fill(maxTasks) { props }
  }

  override val _version = Version.getVersion()
}
