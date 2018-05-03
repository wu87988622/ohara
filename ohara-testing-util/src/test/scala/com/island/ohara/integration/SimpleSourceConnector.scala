package com.island.ohara.integration

import java.util

import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector

/**
  * Used for testing.
  */
class SimpleSourceConnector extends SourceConnector {
  private[this] lazy val logger = Logger(getClass.getName)
  private[this] var topicName: String = null

  override def version(): String = 100.toString

  override def start(props: util.Map[String, String]): Unit = {
    topicName = props.get("topic")
    logger.info(s"start SimpleSourceConnector:${topicName}")
  }

  override def taskClass(): Class[_ <: Task] = classOf[SimpleSourceTask]

  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    val list = new util.ArrayList[util.Map[String, String]]()
    for (_ <- 0 until maxTasks) {
      val config = new util.HashMap[String, String]()
      config.put("topic", topicName)
      config.put("task.count", maxTasks.toString)
      list.add(config)
    }
    logger.info(s"source configs:${list.size()} maxTasks:$maxTasks")
    list
  }

  override def stop(): Unit = {
    logger.info("stop SimpleSourceConnector")
  }

  override def config(): ConfigDef = {
    new ConfigDef().define("topic", Type.LIST, Importance.HIGH, "The topic to publish data to")
  }
}
