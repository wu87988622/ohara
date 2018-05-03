package com.island.ohara.kafka.connector

import java.util

import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkConnector

import scala.collection.JavaConverters

/**
  * A wrap to SinkConnector. Currently, only Task is replaced by ohara object - RowSinkTask
  */
abstract class RowSinkConnector extends SinkConnector {

  /**
    * Returns the RowSinkTask implementation for this Connector.
    *
    * @return a RowSinkTask class
    */
  protected def _taskClass(): Class[_ <: RowSinkTask]

  /**
    * We take over this method to disable user to use Task.
    */
  final override def taskClass(): Class[_ <: Task] = _taskClass()

  /**
    * Return the configs for source task.
    *
    * @return a seq of configs
    */
  protected def _taskConfigs(): Seq[Map[String, String]]

  import scala.collection.JavaConverters._

  /**
    * We take over this method to disable user to use java collection.
    */
  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] =
    _taskConfigs().map(JavaConverters.mapAsJavaMap(_)).asJava
}
