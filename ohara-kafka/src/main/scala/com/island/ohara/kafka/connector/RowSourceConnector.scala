package com.island.ohara.kafka.connector

import java.util

import com.island.ohara.client.ConfiguratorJson.Column
import org.apache.kafka.common.config.{Config, ConfigDef}
import org.apache.kafka.connect.connector.{ConnectorContext, Task}
import org.apache.kafka.connect.source.SourceConnector

/**
  * A wrap to SourceConnector. Currently, only Task is replaced by ohara object - RowSourceTask
  */
abstract class RowSourceConnector extends SourceConnector {

  /**
    * Returns the RowSourceTask implementation for this Connector.
    *
    * @return a RowSourceTask class
    */
  protected def _taskClass(): Class[_ <: RowSourceTask]

  /**
    * Return the configs for source task.
    *
    * @return a seq of configs
    */
  protected def _taskConfigs(maxTasks: Int): Seq[(Map[String, String], Seq[Column])]

  /**
    * Start this Connector. This method will only be called on a clean Connector, i.e. it has
    * either just been instantiated and initialized or _stop() has been invoked.
    *
    * @param config configuration settings
    * @param schema the schema should be used in this connector
    */
  protected def _start(config: Map[String, String], schema: Seq[Column]): Unit

  /**
    * stop this connector
    */
  protected def _stop(): Unit

  /**
    * Define the configuration for the connector.
    * TODO: wrap ConfigDef ... by chia
    * @return The ConfigDef for this connector.
    */
  protected def _config: ConfigDef = new ConfigDef()

  /**
    * Get the version of this connector.
    *
    * @return the version, formatted as a String
    */
  protected def _version: String

  //-------------------------------------------------[WRAPPED]-------------------------------------------------//
  import scala.collection.JavaConverters._

  final override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    _taskConfigs(maxTasks: Int).map {
      case (config, columns) => {
        if (config.contains(Column.COLUMN_KEY))
          throw new IllegalArgumentException(s"DON'T touch ${Column.COLUMN_KEY} manually")
        val copy = config + (Column.COLUMN_KEY -> Column.toString(columns))
        copy.asJava
      }
    }.asJava
  }

  final override def taskClass(): Class[_ <: Task] = _taskClass()

  final override def start(props: util.Map[String, String]): Unit = {
    val config = props.asScala
    val columns = Column.toColumns(
      config
        .remove(Column.COLUMN_KEY)
        .getOrElse(throw new IllegalArgumentException(s"${Column.COLUMN_KEY} doesn't exist!!!")))
    _start(config.toMap, columns)
  }

  final override def stop(): Unit = _stop()

  final override def config(): ConfigDef = _config

  final override def version(): String = _version

  //-------------------------------------------------[UN-OVERRIDE]-------------------------------------------------//
  final override def initialize(ctx: ConnectorContext): Unit =
    super.initialize(ctx)

  final override def initialize(ctx: ConnectorContext, taskConfigs: util.List[util.Map[String, String]]): Unit =
    super.initialize(ctx, taskConfigs)

  final override def reconfigure(props: util.Map[String, String]): Unit =
    super.reconfigure(props)

  final override def validate(connectorConfigs: util.Map[String, String]): Config = super.validate(connectorConfigs)
}
