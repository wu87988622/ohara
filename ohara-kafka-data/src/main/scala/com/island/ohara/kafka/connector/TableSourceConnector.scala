package com.island.ohara.kafka.connector

import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.source.SourceConnector

/**
  * A wrap to SourceConnector. Currently, only Task is replaced by ohara object - TableSourceTask
  */
abstract class TableSourceConnector extends SourceConnector {

  /**
    * Returns the TableSourceTask implementation for this Connector.
    *
    * @return a TableSourceTask class
    */
  protected def _taskClass(): Class[_ <: TableSourceTask]

  /**
    * We take over this method to disable user to use Task.
    */
  final override def taskClass(): Class[_ <: Task] = _taskClass()
}
