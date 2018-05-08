package com.island.ohara.kafka.connector

import java.util

import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.config.ConfigDef

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

/**
  * Used for testing.
  */
class SimpleRowSinkConnector extends RowSinkConnector {
  private[this] lazy val logger = Logger(getClass.getName)

  override def version(): String = 100.toString

  override def start(props: util.Map[String, String]): Unit = {
    logger.info("start SimpleSinkConnector")
  }

  override def _taskClass(): Class[_ <: RowSinkTask] = classOf[SimpleRowSinkTask]

  override def _taskConfigs(maxTasks: Int): Seq[Map[String, String]] = {
    val list = new ArrayBuffer[Map[String, String]]()
    for (_ <- 0 until maxTasks) {
      list += new HashMap[String, String]()
    }
    logger.info(s"SimpleRowSinkConnector maxTasks:$maxTasks")
    list
  }

  override def stop(): Unit = {
    logger.info("stop SimpleRowSinkConnector")
  }

  override def config(): ConfigDef = new ConfigDef()
}
