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

import java.sql._
import java.util.Calendar

import com.island.ohara.client.DatabaseClient
import com.island.ohara.client.configurator.v0.QueryApi.{RdbColumn, RdbTable}
import com.island.ohara.common.util.ReleaseOnce
import com.island.ohara.connector.jdbc.util.DateTimeUtils

/**
  * Connection to database and query data
  *
  */
class DBTableDataProvider(url: String, userName: String, password: String) extends ReleaseOnce {
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

  def isTableExists(tableName: String): Boolean = client.tables(null, null, tableName).nonEmpty

  def dbCurrentTime(cal: Calendar): Timestamp = {
    val dbProduct: String = client.connection.getMetaData.getDatabaseProductName
    import DBTableDataProvider._
    val query = dbProduct.toLowerCase match {
      case ORACLE_DB_NAME => "SELECT CURRENT_TIMESTAMP FROM dual"
      case _              => "SELECT CURRENT_TIMESTAMP;"
    }
    val stmt = client.connection.createStatement()
    try {
      val rs = stmt.executeQuery(query)
      try if (rs.next()) rs.getTimestamp(1, cal)
      else
        throw new RuntimeException(
          s"Unable to get current time from DB using query $query on database $dbProduct"
        )
      finally rs.close()
    } finally stmt.close()
  }

  /**
    * Do what you want to do when calling closing.
    */
  override def doClose(): Unit = {
    ReleaseOnce.close(client)
  }
}

object DBTableDataProvider {
  val ORACLE_DB_NAME = "oracle"
}
