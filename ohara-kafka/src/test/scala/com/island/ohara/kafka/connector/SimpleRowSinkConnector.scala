package com.island.ohara.kafka.connector

import com.typesafe.scalalogging.Logger

/**
  * Used for testing.
  */
class SimpleRowSinkConnector extends RowSinkConnector {
  private[this] lazy val logger = Logger(getClass.getName)
  private[this] var config: TaskConfig = _
  override val _version = "100"

  override def _start(props: TaskConfig): Unit = {
    logger.info("start SimpleSinkConnector")
    this.config = props
  }

  override def _taskClass(): Class[_ <: RowSinkTask] = classOf[SimpleRowSinkTask]

  override def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = {
    logger.info(s"SimpleRowSinkConnector maxTasks:$maxTasks")
    Seq.fill(maxTasks)(config)
  }

  override def _stop(): Unit = {
    logger.info("stop SimpleRowSinkConnector")
  }
}
