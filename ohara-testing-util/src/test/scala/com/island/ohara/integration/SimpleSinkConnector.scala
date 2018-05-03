package com.island.ohara.integration

import java.util

import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkConnector

/**
  * Used for testing.
  */
class SimpleSinkConnector extends SinkConnector {
  private[this] lazy val logger = Logger(getClass.getName)

  override def version(): String = 100.toString

  override def start(props: util.Map[String, String]): Unit = {
    logger.info("start SimpleSinkConnector")
  }

  override def taskClass(): Class[_ <: Task] = classOf[SimpleSinkTask]

  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    val list = new util.ArrayList[util.Map[String, String]]()
    for (index <- 0 until maxTasks) {
      list.add(new util.HashMap[String, String]())
    }
    logger.info(s"SimpleSinkConnector maxTasks:$maxTasks")
    list
  }

  override def stop(): Unit = {
    logger.info("stop SimpleSinkConnector")
  }

  override def config(): ConfigDef = new ConfigDef()
}
