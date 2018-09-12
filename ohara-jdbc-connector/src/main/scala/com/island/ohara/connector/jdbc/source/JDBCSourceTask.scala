package com.island.ohara.connector.jdbc.source
import com.island.ohara.connector.jdbc.Version
import com.island.ohara.connector.jdbc.util.DBTableDataProvider
import com.island.ohara.io.CloseOnce
import com.island.ohara.kafka.connector.{RowSourceRecord, RowSourceTask, TaskConfig}
import com.typesafe.scalalogging.Logger

class JDBCSourceTask extends RowSourceTask {

  private[this] lazy val logger = Logger(getClass.getName)

  private[this] var jdbcSourceConnectorConfig: JDBCSourceConnectorConfig = _
  private[this] var dbTableDataProvider: DBTableDataProvider = _

  /**
    * Start the Task. This should handle any configuration parsing and one-time setup of the task.
    *
    * @param config initial configuration
    */
  override protected def _start(config: TaskConfig): Unit = {
    logger.info("starting JDBC Source Connector")
    val props = config.options
    jdbcSourceConnectorConfig = JDBCSourceConnectorConfig(props)

    val dbURL = jdbcSourceConnectorConfig.dbURL
    val dbUserName = jdbcSourceConnectorConfig.dbUserName
    val dbPassword = jdbcSourceConnectorConfig.dbPassword
    dbTableDataProvider = new DBTableDataProvider(dbURL, dbUserName, dbPassword)
  }

  /**
    * Poll this SourceTask for new records. This method should block if no data is currently available.
    *
    * @return a array of RowSourceRecord
    */
  override protected def _poll(): Seq[RowSourceRecord] = {
    //TODO loadOffset, call dbTableDataProvider#rowList, converter ohara datatype, return Seq[RowSourceRecord]
    //TODO OHARA-413, OHARA-414
    throw new RuntimeException("Not implement to poll database table data")
  }

  /**
    * Signal this SourceTask to stop. In SourceTasks, this method only needs to signal to the task that it should stop
    * trying to poll for new data and interrupt any outstanding poll() requests. It is not required that the task has
    * fully stopped. Note that this method necessarily may be invoked from a different thread than _poll() and _commit()
    */
  override protected def _stop(): Unit = {
    CloseOnce.close(dbTableDataProvider)
  }

  /**
    * Get the version of this task. Usually this should be the same as the corresponding Connector class's version.
    *
    * @return the version, formatted as a String
    */
  override protected def _version: String = Version.getVersion()
}
