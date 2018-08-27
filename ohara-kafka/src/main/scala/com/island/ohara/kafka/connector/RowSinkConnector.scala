package com.island.ohara.kafka.connector

import java.util

import org.apache.kafka.common.config.{Config, ConfigDef}
import org.apache.kafka.connect.connector.{ConnectorContext, Task}
import org.apache.kafka.connect.sink.SinkConnector

import scala.collection.JavaConverters
import scala.collection.JavaConverters._

/**
  * A wrap to SinkConnector. Currently, only Task is replaced by ohara object - RowSinkTask
  */
abstract class RowSinkConnector extends SinkConnector {

  /**
    * Start this Connector. This method will only be called on a clean Connector, i.e. it has
    * either just been instantiated and initialized or _stop() has been invoked.
    *
    * @param config configuration settings
    */
  protected def _start(config: Map[String, String]): Unit

  /**
    * stop this connector
    */
  protected def _stop(): Unit

  /**
    * Returns the RowSinkTask implementation for this Connector.
    *
    * @return a RowSinkTask class
    */
  protected def _taskClass(): Class[_ <: RowSinkTask]

  /**
    * Return the configs for source task.
    *
    * @return a seq of configs
    */
  protected def _taskConfigs(maxTasks: Int): Seq[Map[String, String]]

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
  /**
    * We take over this method to disable user to use java collection.
    */
  final override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] =
    _taskConfigs(maxTasks).map(JavaConverters.mapAsJavaMap(_)).asJava

  final override def taskClass(): Class[_ <: Task] = _taskClass()

  final override def start(props: util.Map[String, String]): Unit = _start(props.asScala.toMap)

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
