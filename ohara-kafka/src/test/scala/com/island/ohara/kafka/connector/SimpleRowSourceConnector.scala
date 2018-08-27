package com.island.ohara.kafka.connector

import com.typesafe.scalalogging.Logger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Used for testing.
  */
class SimpleRowSourceConnector extends RowSourceConnector {
  private[this] lazy val logger = Logger(getClass.getName)
  private[this] var topicName: String = null
  private[this] var pollCountMax: Int = 0

  override val _version = 100.toString

  override def _start(props: Map[String, String]): Unit = {
    topicName = props.get("topic").get
    pollCountMax = props.get(SimpleRowSourceConnector.POLL_COUNT_MAX).map(_.toInt).get
    logger.info(s"start SimpleRowSourceConnector:${topicName} maxPoll:$pollCountMax")
  }

  override def _taskClass(): Class[_ <: RowSourceTask] = classOf[SimpleRowSourceTask]

  override def _taskConfigs(maxTasks: Int): Seq[Map[String, String]] = {
    val list = new ArrayBuffer[Map[String, String]]()
    for (_ <- 0 until maxTasks) {
      val config = new mutable.HashMap[String, String]()
      config.put("topic", topicName)
      config.put(SimpleRowSourceConnector.POLL_COUNT_MAX, pollCountMax.toString)
      list += config.toMap
    }
    logger.info(s"source configs:${list.size} maxTasks:$maxTasks")
    list
  }

  override def _stop(): Unit = {
    logger.info("stop SimpleRowSourceConnector")
  }
}

object SimpleRowSourceConnector {
  val POLL_COUNT_MAX = "poll.count.max"
}
