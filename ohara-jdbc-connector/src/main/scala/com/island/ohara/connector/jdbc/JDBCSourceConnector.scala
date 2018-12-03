package com.island.ohara.connector.jdbc
import com.island.ohara.connector.jdbc.source.{DBTableDataProvider, JDBCSourceConnectorConfig, JDBCSourceTask}
import com.island.ohara.kafka.connector.{RowSourceConnector, RowSourceTask, TaskConfig}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

/**
  * This class for JDBC Source connector plugin
  */
class JDBCSourceConnector extends RowSourceConnector {

  private[this] var taskConfig: TaskConfig = _

  /**
    * Start this Connector. This method will only be called on a clean Connector, i.e. it has
    * either just been instantiated and initialized or _stop() has been invoked.
    *
    * @param taskConfig configuration settings
    */
  override protected def _start(taskConfig: TaskConfig): Unit = {
    this.taskConfig = taskConfig

    val props = taskConfig.options.asScala.toMap
    val jdbcSourceConnectorConfig: JDBCSourceConnectorConfig = JDBCSourceConnectorConfig(props)

    val dbURL = jdbcSourceConnectorConfig.dbURL
    val dbUserName = jdbcSourceConnectorConfig.dbUserName
    val dbPassword = jdbcSourceConnectorConfig.dbPassword
    val tableName = jdbcSourceConnectorConfig.dbTableName
    val dbTableDataProvider: DBTableDataProvider = new DBTableDataProvider(dbURL, dbUserName, dbPassword)
    try {
      if (!dbTableDataProvider.isTableExists(tableName))
        throw new NoSuchElementException(s"$tableName table is not found")

    } finally dbTableDataProvider.close()
  }

  /**
    * Returns the RowSourceTask implementation for this Connector.
    *
    * @return a JDBCSourceTask class
    */
  override protected def _taskClass(): Class[_ <: RowSourceTask] = {
    classOf[JDBCSourceTask]
  }

  /**
    * Return the configs for source task.
    *
    * @return a seq from configs
    */
  override protected def _taskConfigs(maxTasks: Int): java.util.List[TaskConfig] = {
    //TODO
    Seq(taskConfig).asJava
  }

  /**
    * stop this connector
    */
  override protected def _stop(): Unit = {
    //TODO
  }
}

object JDBCSourceConnector {
  val LOG: Logger = LoggerFactory.getLogger(classOf[JDBCSourceConnector])
}
