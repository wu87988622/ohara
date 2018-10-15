package com.island.ohara.connector.jdbc.source
import java.sql.Timestamp
import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.connector.jdbc.util.ColumnInfo
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.io.{CloseOnce, VersionUtil}
import com.island.ohara.kafka.connector.{RowSourceContext, RowSourceRecord, RowSourceTask, TaskConfig}
import com.island.ohara.serialization.DataType
import com.typesafe.scalalogging.Logger

class JDBCSourceTask extends RowSourceTask {

  private[this] lazy val logger = Logger(getClass.getName)

  private[this] var jdbcSourceConnectorConfig: JDBCSourceConnectorConfig = _
  private[this] var dbTableDataProvider: DBTableDataProvider = _
  private[this] var schema: Seq[Column] = _
  private[this] var topics: Seq[String] = _
  private[this] var offsets: Offsets = _

  /**
    * Start the Task. This should handle any configuration parsing and one-time setup of the task.
    *
    * @param config initial configuration
    */
  override protected[source] def _start(config: TaskConfig): Unit = {
    logger.info("starting JDBC Source Connector")
    val props = config.options
    jdbcSourceConnectorConfig = JDBCSourceConnectorConfig(props)

    val dbURL = jdbcSourceConnectorConfig.dbURL
    val dbUserName = jdbcSourceConnectorConfig.dbUserName
    val dbPassword = jdbcSourceConnectorConfig.dbPassword
    val tableName = jdbcSourceConnectorConfig.dbTableName
    dbTableDataProvider = new DBTableDataProvider(dbURL, dbUserName, dbPassword)

    schema = config.schema
    topics = config.topics
    offsets = new Offsets(rowContext, tableName)
  }

  /**
    * Poll this SourceTask for new records. This method should block if no data is currently available.
    *
    * @return a array of RowSourceRecord
    */
  override protected[source] def _poll(): Seq[RowSourceRecord] = {
    val tableName: String = jdbcSourceConnectorConfig.dbTableName
    val timestampColumnName: String = jdbcSourceConnectorConfig.timestampColumnName

    val resultSet: QueryResultIterator =
      dbTableDataProvider.executeQuery(tableName, timestampColumnName, new Timestamp(offsets.readInMemoryOffset()))

    try resultSet
    //Create Ohara Schema
      .map(columns =>
        (if (schema.isEmpty) columns.map(c => Column(c.columnName, DataType.OBJECT, 0)) else schema, columns))
      .flatMap {
        case (newSchema, columns) =>
          val offsetTimestampValue = dbTimestampColumnValue(columns, timestampColumnName)
          offsets.updateInMemOffset(offsetTimestampValue)
          topics.map(
            RowSourceRecord
              .builder()
              .sourcePartition(JDBCSourceTask.partition(tableName))
              //Writer Offset
              .sourceOffset(JDBCSourceTask.offset(offsetTimestampValue))
              //Create Ohara Row
              .row(row(newSchema, columns))
              .build(_))
      }
      .toList
    finally resultSet.close()
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
  override protected def _version: String = VersionUtil.VERSION

  private[source] def row(schema: Seq[Column], columns: Seq[ColumnInfo[_]]): Row = {
    Row
      .builder()
      .cells(
        schema
          .sortBy(_.order)
          .map(s => (s, values(s.name, columns)))
          .map {
            case (schema, value) =>
              Cell(
                schema.newName,
                schema.dataType match {
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
          }
      )
      .build()
  }

  private[this] def values(schemaColumnName: String, dbColumnInfos: Seq[ColumnInfo[_]]): Any = {
    dbColumnInfos.foreach(dbColumn => {
      if (dbColumn.columnName == schemaColumnName) {
        return dbColumn.value
      }
    })
    throw new RuntimeException(s"Database Table not have the $schemaColumnName column")
  }

  private[source] def dbTimestampColumnValue(dbColumnInfo: Seq[ColumnInfo[_]], timestampColumnName: String): Long = {
    dbColumnInfo.foreach(columnInfo => {
      if (columnInfo.columnName == timestampColumnName) {
        return columnInfo.value.asInstanceOf[Timestamp].getTime
      }
    })
    throw new RuntimeException(s"$timestampColumnName not in ${jdbcSourceConnectorConfig.dbTableName} table.")
  }

  private class Offsets(context: RowSourceContext, tableName: String) {
    private[this] val offsets: Map[String, _] = context.offset(JDBCSourceTask.partition(tableName))
    private[this] var cache: Map[String, Long] =
      if (offsets.isEmpty) Map(tableName -> 0)
      else Map(tableName -> offsets(JDBCSourceTask.DB_TABLE_OFFSET_KEY).asInstanceOf[Long])

    private[source] def updateInMemOffset(timestamp: Long): Unit = {
      this.cache = Map(tableName -> timestamp)
    }

    private[source] def readInMemoryOffset(): Long = {
      return this.cache(tableName)
    }
  }
}

object JDBCSourceTask {
  private[source] val DB_TABLE_NAME_KEY = "db.table.name"
  private[source] val DB_TABLE_OFFSET_KEY = "db.table.offset"

  def partition(tableName: String): Map[String, _] = Map(DB_TABLE_NAME_KEY -> tableName)
  def offset(timestamp: Long): Map[String, _] = Map(DB_TABLE_OFFSET_KEY -> timestamp)
}
