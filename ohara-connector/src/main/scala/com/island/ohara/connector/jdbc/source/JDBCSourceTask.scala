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
import java.sql.Timestamp
import com.island.ohara.common.data.{Cell, Column, DataType, Row}
import com.island.ohara.common.util.{Releasable, VersionUtils}
import com.island.ohara.connector.jdbc.util.ColumnInfo
import com.island.ohara.kafka.connector._
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

class JDBCSourceTask extends RowSourceTask {

  private[this] lazy val logger = Logger(getClass.getName)

  private[this] var jdbcSourceConnectorConfig: JDBCSourceConnectorConfig = _
  private[this] var dbTableDataProvider: DBTableDataProvider = _
  private[this] var schema: Seq[Column] = _
  private[this] var topics: Seq[String] = _
  private[this] var offsets: Offsets = _

  /**
    * Start the Task. This should handle any configuration parsing and one-time setup from the task.
    *
    * @param settings initial configuration
    */
  override protected[source] def _start(settings: TaskSetting): Unit = {
    logger.info("starting JDBC Source Connector")
    jdbcSourceConnectorConfig = JDBCSourceConnectorConfig(settings)

    dbTableDataProvider = new DBTableDataProvider(jdbcSourceConnectorConfig)

    schema = settings.columns.asScala
    topics = settings.topicNames().asScala

    val tableName = jdbcSourceConnectorConfig.dbTableName
    offsets = new Offsets(rowContext, tableName)
  }

  /**
    * Poll this SourceTask for new records. This method should block if no data is currently available.
    *
    * @return a array from RowSourceRecord
    */
  override protected[source] def _poll(): java.util.List[RowSourceRecord] = try {
    val tableName: String = jdbcSourceConnectorConfig.dbTableName
    val timestampColumnName: String = jdbcSourceConnectorConfig.timestampColumnName
    val flushDataSize: Int = jdbcSourceConnectorConfig.jdbcFlushDataSize
    var offsetTimestampValue = offsets.readInMemoryOffset()

    val resultSet: QueryResultIterator =
      dbTableDataProvider.executeQuery(tableName, timestampColumnName, Timestamp.valueOf(offsets.readInMemoryOffset()))

    val rowSourceRecords: Iterator[RowSourceRecord] = resultSet
      .take(flushDataSize)
      .flatMap(columns => {
        val newSchema =
          if (schema.isEmpty)
            columns.map(c => Column.builder().name(c.columnName).dataType(DataType.OBJECT).order(0).build())
          else schema

        offsetTimestampValue = dbTimestampColumnValue(columns, timestampColumnName)
        offsets.updateInMemOffset(offsetTimestampValue)
        topics.map(
          RowSourceRecord
            .builder()
            .sourcePartition(JDBCSourceTask.partition(tableName).asJava)
            //Writer Offset
            .sourceOffset(JDBCSourceTask.offset(offsetTimestampValue).asJava)
            //Create Ohara Row
            .row(row(newSchema, columns))
            .topicName(_)
            .build())
      })

    val result = if (rowSourceRecords.isEmpty) {
      dbTableDataProvider.queryFlag(true)
      Seq.empty
    } else rowSourceRecords
    result.toList.asJava
  } catch {
    case e: Throwable =>
      logger.error(e.getMessage, e)
      Seq.empty.asJava
  }

  /**
    * Signal this SourceTask to stop. In SourceTasks, this method only needs to signal to the task that it should stop
    * trying to poll for new data and interrupt any outstanding poll() requests. It is not required that the task has
    * fully stopped. Note that this method necessarily may be invoked from a different thread than _poll() and _commit()
    */
  override protected def _stop(): Unit = Releasable.close(dbTableDataProvider)

  /**
    * Get the version from this task. Usually this should be the same as the corresponding Connector class's version.
    *
    * @return the version, formatted as a String
    */
  override protected def _version: String = VersionUtils.VERSION

  private[source] def row(schema: Seq[Column], columns: Seq[ColumnInfo[_]]): Row = {
    Row.of(
      schema
        .sortBy(_.order)
        .map(s => (s, values(s.name, columns)))
        .map {
          case (s, value) =>
            Cell.of(
              s.newName,
              s.dataType match {
                case DataType.BOOLEAN                 => value.asInstanceOf[Boolean]
                case DataType.SHORT                   => value.asInstanceOf[Short]
                case DataType.INT                     => value.asInstanceOf[Int]
                case DataType.LONG                    => value.asInstanceOf[Long]
                case DataType.FLOAT                   => value.asInstanceOf[Float]
                case DataType.DOUBLE                  => value.asInstanceOf[Double]
                case DataType.BYTE                    => value.asInstanceOf[Byte]
                case DataType.STRING                  => value.asInstanceOf[String]
                case DataType.BYTES | DataType.OBJECT => value
                case _                                => throw new IllegalArgumentException("Unsupported type...")
              }
            )
        }: _*)
  }

  private[this] def values(schemaColumnName: String, dbColumnInfos: Seq[ColumnInfo[_]]): Any = {
    dbColumnInfos.foreach(dbColumn => {
      if (dbColumn.columnName == schemaColumnName) {
        return dbColumn.value
      }
    })
    throw new RuntimeException(s"Database Table not have the $schemaColumnName column")
  }

  private[source] def dbTimestampColumnValue(dbColumnInfo: Seq[ColumnInfo[_]], timestampColumnName: String): String =
    dbColumnInfo
      .find(_.columnName == timestampColumnName)
      .map(_.value.asInstanceOf[Timestamp].toString)
      .getOrElse(
        throw new RuntimeException(s"$timestampColumnName not in ${jdbcSourceConnectorConfig.dbTableName} table."))

  private class Offsets(context: RowSourceContext, tableName: String) {
    private[this] val offsets: Map[String, _] = context.offset(JDBCSourceTask.partition(tableName).asJava).asScala.toMap
    private[this] var cache: Map[String, String] =
      if (offsets.isEmpty) Map(tableName -> new Timestamp(0).toString())
      else Map(tableName -> offsets(JDBCSourceTask.DB_TABLE_OFFSET_KEY).asInstanceOf[String])

    private[source] def updateInMemOffset(timestamp: String): Unit = {
      this.cache = Map(tableName -> timestamp)
    }

    private[source] def readInMemoryOffset(): String = this.cache(tableName)
  }
}

object JDBCSourceTask {
  private[source] val DB_TABLE_NAME_KEY = "db.table.name"
  private[source] val DB_TABLE_OFFSET_KEY = "db.table.offset"

  def partition(tableName: String): Map[String, _] = Map(DB_TABLE_NAME_KEY -> tableName)
  def offset(timestamp: String): Map[String, _] = Map(DB_TABLE_OFFSET_KEY -> timestamp)
}
