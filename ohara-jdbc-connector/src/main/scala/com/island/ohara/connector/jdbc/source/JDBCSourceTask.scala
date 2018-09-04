package com.island.ohara.connector.jdbc.source
import com.island.ohara.connector.jdbc.Version
import com.island.ohara.kafka.connector.{RowSourceRecord, RowSourceTask, TaskConfig}

class JDBCSourceTask extends RowSourceTask {

  /**
    * Start the Task. This should handle any configuration parsing and one-time setup of the task.
    *
    * @param props initial configuration
    */
  override protected def _start(config: TaskConfig): Unit = {
    //TODO setting props for JDBC Source Connector
  }

  /**
    * Poll this SourceTask for new records. This method should block if no data is currently available.
    *
    * @return a array of RowSourceRecord
    */
  override protected def _poll(): Array[RowSourceRecord] = {
    //TODO
    throw new RuntimeException("Not implement to poll database table data")
  }

  /**
    * Signal this SourceTask to stop. In SourceTasks, this method only needs to signal to the task that it should stop
    * trying to poll for new data and interrupt any outstanding poll() requests. It is not required that the task has
    * fully stopped. Note that this method necessarily may be invoked from a different thread than _poll() and _commit()
    */
  override protected def _stop(): Unit = {
    //TODO
  }

  /**
    * Get the version of this task. Usually this should be the same as the corresponding Connector class's version.
    *
    * @return the version, formatted as a String
    */
  override protected def _version: String = Version.getVersion()
}
