package com.island.ohara.kafka.connector

import org.apache.kafka.connect.connector.Task
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
    * We take over this method to disable user to use Task.
    */
  final override def taskClass(): Class[_ <: Task] = _taskClass()
}
