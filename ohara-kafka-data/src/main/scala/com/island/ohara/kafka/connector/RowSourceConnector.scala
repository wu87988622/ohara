package com.island.ohara.kafka.connector

import java.util

import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector

import scala.collection.JavaConverters

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
    * We take over this method to disable user to use Task.
    */
  final override def taskClass(): Class[_ <: Task] = _taskClass()

  /**
    * Return the configs for source task.
    *
    * @return a seq of configs
    */
  protected def _taskConfigs(maxTasks: Int): Seq[Map[String, String]]

  import scala.collection.JavaConverters._

  /**
    * We take over this method to disable user to use java collection.
    */
  final override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] =
    _taskConfigs(maxTasks: Int).map(JavaConverters.mapAsJavaMap(_)).asJava
}
