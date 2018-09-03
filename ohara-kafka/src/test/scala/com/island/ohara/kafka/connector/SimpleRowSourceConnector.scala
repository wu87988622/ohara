package com.island.ohara.kafka.connector

import com.island.ohara.client.ConfiguratorJson.Column
import com.typesafe.scalalogging.Logger

/**
  * Used for testing.
  */
class SimpleRowSourceConnector extends RowSourceConnector {
  private[this] lazy val logger = Logger(getClass.getName)
  private[this] var topicName: String = _
  private[this] var pollCountMax: Int = 0
  private[this] var schema: Seq[Column] = _
  override val _version: String = 100.toString

  override def _start(props: Map[String, String], schema: Seq[Column]): Unit = {
    topicName = props("topic")
    pollCountMax = props.get(SimpleRowSourceConnector.POLL_COUNT_MAX).map(_.toInt).get
    this.schema = schema
    logger.info(s"start SimpleRowSourceConnector:$topicName maxPoll:$pollCountMax")
  }

  override def _taskClass(): Class[_ <: RowSourceTask] = classOf[SimpleRowSourceTask]

  override def _taskConfigs(maxTasks: Int): Seq[(Map[String, String], Seq[Column])] = {
    (0 until maxTasks).map { _ =>
      (Map("topic" -> topicName, SimpleRowSourceConnector.POLL_COUNT_MAX -> pollCountMax.toString), schema)
    }
  }

  override def _stop(): Unit = {
    logger.info("stop SimpleRowSourceConnector")
  }
}

object SimpleRowSourceConnector {
  val POLL_COUNT_MAX = "poll.count.max"
}
