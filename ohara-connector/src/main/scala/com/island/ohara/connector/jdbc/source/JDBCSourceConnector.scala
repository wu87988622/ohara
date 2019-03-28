/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.connector.jdbc.source

import com.island.ohara.kafka.connector.json.SettingDefinition
import com.island.ohara.kafka.connector._
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

    val props = taskConfig.raw().asScala.toMap
    val jdbcSourceConnectorConfig: JDBCSourceConnectorConfig = JDBCSourceConnectorConfig(props)

    val dbURL = jdbcSourceConnectorConfig.dbURL
    val dbUserName = jdbcSourceConnectorConfig.dbUserName
    val dbPassword = jdbcSourceConnectorConfig.dbPassword
    val tableName = jdbcSourceConnectorConfig.dbTableName
    val timestampColumnName = jdbcSourceConnectorConfig.timestampColumnName

    val dbTableDataProvider: DBTableDataProvider = new DBTableDataProvider(dbURL, dbUserName, dbPassword)
    try {
      checkTimestampColumnName(timestampColumnName)

      if (!dbTableDataProvider.isTableExists(tableName))
        throw new NoSuchElementException(s"$tableName table is not found.")

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
    * Return the settings for source task.
    *
    * @return a seq from settings
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

  protected[jdbc] def checkTimestampColumnName(timestampColumnName: String): Unit = {
    if (timestampColumnName == null)
      throw new NoSuchElementException(s"Timestamp column is null, Please input timestamp type column name.")

    if (timestampColumnName.isEmpty)
      throw new NoSuchElementException(s"Timestamp column is empty, Please input timestamp type column name.")

    if (!timestampColumnName.matches("^[a-zA-Z]{1}.*"))
      throw new IllegalArgumentException("Your column name input error, Please checkout your column name.")
  }

  override protected def _definitions(): java.util.List[SettingDefinition] = Seq(
    SettingDefinition
      .builder()
      .displayName("jdbc url")
      .documentation("Connection database url")
      .valueType(SettingDefinition.Type.STRING)
      .key(DB_URL)
      .build(),
    SettingDefinition
      .builder()
      .displayName("user name")
      .documentation("Connection database user name")
      .valueType(SettingDefinition.Type.STRING)
      .key(DB_USERNAME)
      .build(),
    SettingDefinition
      .builder()
      .displayName("password")
      .documentation("Connection database user password")
      .valueType(SettingDefinition.Type.PASSWORD)
      .key(DB_PASSWORD)
      .build(),
    SettingDefinition
      .builder()
      .displayName("table name")
      .documentation("write to topic from database table name")
      .valueType(SettingDefinition.Type.STRING)
      .key(DB_TABLENAME)
      .build(),
    SettingDefinition
      .builder()
      .displayName("catalog pattern")
      .documentation("database metadata catalog")
      .valueType(SettingDefinition.Type.STRING)
      .key(DB_CATALOG_PATTERN)
      .optional()
      .build(),
    SettingDefinition
      .builder()
      .displayName("schema pattern")
      .documentation("database metadata schema pattern")
      .valueType(SettingDefinition.Type.STRING)
      .key(DB_SCHEMA_PATTERN)
      .optional()
      .build(),
    SettingDefinition
      .builder()
      .displayName("mode")
      .documentation("Only support timestamp column")
      .valueType(SettingDefinition.Type.STRING)
      .key(MODE)
      .optional(MODE_DEFAULT)
      .build(),
    SettingDefinition
      .builder()
      .displayName("timestamp column name")
      .documentation("Use a timestamp column to detect new and modified rows")
      .valueType(SettingDefinition.Type.STRING)
      .key(TIMESTAMP_COLUMN_NAME)
      .build()
  ).asJava

  override protected def _version: ConnectorVersion = ConnectorVersion.DEFAULT
}

object JDBCSourceConnector {
  val LOG: Logger = LoggerFactory.getLogger(classOf[JDBCSourceConnector])
}
