package com.island.ohara.jdbc

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.jdbc.source.JDBCSourceTask
import com.island.ohara.kafka.connector.{RowSourceConnector, RowSourceTask}

/**
  * This class for JDBC Source connector plugin
  */
class JDBCSourceConnector extends RowSourceConnector {

  var props: Map[String, String] = _
  var schema: Seq[Column] = _

  /**
    * Start this Connector. This method will only be called on a clean Connector, i.e. it has
    * either just been instantiated and initialized or _stop() has been invoked.
    *
    * @param props configuration settings
    */
  override protected def _start(props: Map[String, String], schema: Seq[Column]): Unit = {
    this.props = props
    this.schema = schema
  }

  /**
    * Returns the RowSourceTask implementation for this Connector.
    *
    * @return a JDBCSourceTask class
    */
  override protected def _taskClass(): Class[_ <: RowSourceTask] = {
    classOf[JDBCSourceTask]
  }

  /**
    * Return the configs for source task.
    *
    * @return a seq of configs
    */
  override protected def _taskConfigs(maxTasks: Int): Seq[(Map[String, String], Seq[Column])] = {
    //TODO
    Seq((props, schema))
  }

  /**
    * stop this connector
    */
  override protected def _stop(): Unit = {
    //TODO
  }

  /**
    * Get the version of this connector.
    *
    * @return the version, formatted as a String
    */
  override protected def _version: String = Version.getVersion()
}
