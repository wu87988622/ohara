package com.island.ohara.kafka.connector

import com.typesafe.scalalogging.Logger

/**
  * Used for testing.
  */
class SimpleRowSourceConnector extends RowSourceConnector {
  private[this] lazy val logger = Logger(getClass.getName)
  private[this] var config: TaskConfig = _
  override val _version: String = 100.toString

  override def _start(config: TaskConfig): Unit = {
    this.config = config
  }

  override def _taskClass(): Class[_ <: RowSourceTask] = classOf[SimpleRowSourceTask]

  override def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = Seq.fill(maxTasks)(config)

  override def _stop(): Unit = {
    logger.info("stop SimpleRowSourceConnector")
  }
}

object SimpleRowSourceConnector {
  val POLL_COUNT_MAX = "poll.count.max"
}
