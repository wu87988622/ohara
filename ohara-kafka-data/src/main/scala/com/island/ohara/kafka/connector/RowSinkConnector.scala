package com.island.ohara.kafka.connector

import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.sink.SinkConnector

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
}
