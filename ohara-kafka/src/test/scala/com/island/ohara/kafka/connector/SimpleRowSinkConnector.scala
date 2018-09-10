package com.island.ohara.kafka.connector

import Constants._

/**
  * Used for testing.
  */
class SimpleRowSinkConnector extends RowSinkConnector {
  private[this] var config: TaskConfig = _
  override val _version = "100"

  override def _start(props: TaskConfig): Unit = {
    this.config = props
    // check the option
    this.config.options(OUTPUT)
    this.config.options(BROKER)
  }

  override def _taskClass(): Class[_ <: RowSinkTask] = classOf[SimpleRowSinkTask]

  override def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = Seq.fill(maxTasks)(config)

  override def _stop(): Unit = {}
}
