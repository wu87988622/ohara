package com.island.ohara.connector.jdbc.util

import java.sql._
import java.util.Calendar
import com.island.ohara.client.ConfiguratorJson.{RdbColumn, RdbTable}
import com.island.ohara.client.DatabaseClient
import com.island.ohara.connector.jdbc.source.QueryResultIterator
import com.island.ohara.io.CloseOnce

/**
  * Connection to database and query data
  *
  * @param url
  * @param userName
  * @param password
  */
class DBTableDataProvider(url: String, userName: String, password: String) extends CloseOnce {

  private[this] val connection: Connection = DriverManager.getConnection(url, userName, password)
  private[this] val client: DatabaseClient = DatabaseClient(url, userName, password)

  protected[util] def executeQuery(tableName: String,
                                   timeStampColumnName: String,
                                   tsOffset: Timestamp): QueryResultIterator = {
    val columnNames = columns(tableName)
    val sql = s"SELECT * FROM $tableName WHERE $timeStampColumnName > ? and $timeStampColumnName < ?"
    val preparedStatement: PreparedStatement = connection.prepareStatement(sql)

    val currentTimestamp: Timestamp = dbCurrentTime(DateTimeUtils.CALENDAR)
    preparedStatement.setTimestamp(1, tsOffset, DateTimeUtils.CALENDAR)
    preparedStatement.setTimestamp(2, currentTimestamp, DateTimeUtils.CALENDAR)
    return new QueryResultIterator(preparedStatement, columnNames)
  }

  protected[util] def columns(tableName: String): Seq[RdbColumn] = {
    val rdbTables: Seq[RdbTable] = client.tables(null, null, tableName)
    rdbTables.head.schema
  }

  protected[util] def dbCurrentTime(cal: Calendar): Timestamp = {
    val dbProduct: String = connection.getMetaData().getDatabaseProductName()
    var query = ""
    var currentTimestamp: Timestamp = new Timestamp(0)
    if (DBTableDataProvider.ORACLE_DB_NAME == dbProduct) {
      query = "select CURRENT_TIMESTAMP from dual";
    } else {
      query = "select CURRENT_TIMESTAMP;";
    }

    val stmt: Statement = connection.createStatement()
    try {
      val resultSet: ResultSet = stmt.executeQuery(query);
      if (resultSet.next()) {
        currentTimestamp = resultSet.getTimestamp(1, cal);
      } else {
        throw new RuntimeException(
          s"Unable to get current time from DB using query $query on database $dbProduct"
        );
      }
    } finally stmt.close()
    currentTimestamp
  }

  /**
    * Do what you want to do when calling closing.
    */
  override def doClose(): Unit = {
    CloseOnce.close(client)
    this.connection.close()
  }
}

object DBTableDataProvider {
  val ORACLE_DB_NAME = "Oracle"
}
