package com.island.ohara.kafka.connector

import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkConnector

/**
  * A wrap to SinkConnector. Currently, only Task is replaced by ohara object - TableSinkTask
  */
abstract class classTableSinkConnector extends SinkConnector {

  /**
    * Returns the TableSinkTask implementation for this Connector.
    *
    * @return a TableSinkTask class
    */
  protected def _taskClass(): Class[_ <: TableSinkTask]

  /**
    * We take over this method to disable user to use Task.
    */
  final override def taskClass(): Class[_ <: Task] = _taskClass()
}
