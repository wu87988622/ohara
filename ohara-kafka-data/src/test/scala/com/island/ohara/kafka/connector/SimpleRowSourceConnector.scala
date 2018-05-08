package com.island.ohara.kafka.connector

import java.util

import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Used for testing.
  */
class SimpleRowSourceConnector extends RowSourceConnector {
  private[this] lazy val logger = Logger(getClass.getName)
  private[this] var topicName: String = null

  override def version(): String = 100.toString

  override def start(props: util.Map[String, String]): Unit = {
    topicName = props.get("topic")
    logger.info(s"start SimpleRowSourceConnector:${topicName}")
  }

  override def _taskClass(): Class[_ <: RowSourceTask] = classOf[SimpleRowSourceTask]

  override def _taskConfigs(maxTasks: Int): Seq[Map[String, String]] = {
    val list = new ArrayBuffer[Map[String, String]]()
    for (_ <- 0 until maxTasks) {
      val config = new mutable.HashMap[String, String]()
      config.put("topic", topicName)
      config.put("task.count", maxTasks.toString)
      list += config.toMap
    }
    logger.info(s"source configs:${list.size} maxTasks:$maxTasks")
    list
  }

  override def stop(): Unit = {
    logger.info("stop SimpleRowSourceConnector")
  }

  override def config(): ConfigDef = {
    new ConfigDef().define("topic", Type.LIST, Importance.HIGH, "The topic to publish data to")
  }
}
