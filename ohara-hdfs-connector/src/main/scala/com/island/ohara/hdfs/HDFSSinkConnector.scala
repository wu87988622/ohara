package com.island.ohara.hdfs

import java.util

import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkTask}
import org.apache.kafka.common.config.ConfigDef
import scala.collection.JavaConverters._

/**
  * This class extends RowSinkConnector abstract.
  */
class HDFSSinkConnector extends RowSinkConnector {

  var props: util.Map[String, String] = _

  override def start(props: util.Map[String, String]): Unit = {
    this.props = props
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = {
    classOf[HDFSSinkTask]
  }

  override def stop(): Unit = {
    //TODO
  }

  override def config(): ConfigDef = {
    new ConfigDef()
  }

  override protected def _taskConfigs(maxTasks: Int): Seq[Map[String, String]] = {
    Seq.fill(maxTasks) { props.asScala.toMap }
  }

  override def version(): String = {
    Version.getVersion()
  }
}
