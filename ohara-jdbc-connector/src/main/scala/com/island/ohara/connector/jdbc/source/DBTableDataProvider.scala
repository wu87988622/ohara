package com.island.ohara.connector.jdbc.source

import java.sql._
import java.util.Calendar

import com.island.ohara.client.ConfiguratorJson.{RdbColumn, RdbTable}
import com.island.ohara.client.DatabaseClient
import com.island.ohara.connector.jdbc.util.DateTimeUtils
import com.island.ohara.io.CloseOnce
import com.island.ohara.io.CloseOnce._

/**
  * Connection to database and query data
  *
  */
class DBTableDataProvider(url: String, userName: String, password: String) extends CloseOnce {
  private[this] val client: DatabaseClient = DatabaseClient(url, userName, password)

  def executeQuery(tableName: String, timeStampColumnName: String, tsOffset: Timestamp): QueryResultIterator = {
    val columnNames = columns(tableName)
    val sql =
      s"""SELECT * FROM \"$tableName\" WHERE \"$timeStampColumnName\" > ? and \"$timeStampColumnName\" < ? ORDER BY \"$timeStampColumnName\""""

    val preparedStatement: PreparedStatement = client.connection.prepareStatement(sql)

    val currentTimestamp: Timestamp = dbCurrentTime(DateTimeUtils.CALENDAR)
    preparedStatement.setTimestamp(1, tsOffset, DateTimeUtils.CALENDAR)
    preparedStatement.setTimestamp(2, currentTimestamp, DateTimeUtils.CALENDAR)
    new QueryResultIterator(preparedStatement, columnNames)
  }

  def columns(tableName: String): Seq[RdbColumn] = {
    val rdbTables: Seq[RdbTable] = client.tables(null, null, tableName)
    rdbTables.head.schema
  }

  def dbCurrentTime(cal: Calendar): Timestamp = {
    val dbProduct: String = client.connection.getMetaData.getDatabaseProductName
    import DBTableDataProvider._
    val query = dbProduct.toLowerCase match {
      case ORACLE_DB_NAME => "SELECT CURRENT_TIMESTAMP FROM dual"
      case _              => "SELECT CURRENT_TIMESTAMP;"
    }
    doClose2(client.connection.createStatement())(stmt => stmt.executeQuery(query)) { (_, resultSet) =>
      if (resultSet.next()) resultSet.getTimestamp(1, cal)
      else
        throw new RuntimeException(
          s"Unable to get current time from DB using query $query on database $dbProduct"
        )
    }
  }

  /**
    * Do what you want to do when calling closing.
    */
  override def doClose(): Unit = {
    CloseOnce.close(client)
  }
}

object DBTableDataProvider {
  val ORACLE_DB_NAME = "oracle"
}
