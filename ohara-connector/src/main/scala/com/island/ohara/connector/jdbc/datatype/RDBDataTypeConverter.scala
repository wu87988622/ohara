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

package com.island.ohara.connector.jdbc.datatype

import java.sql.ResultSet

import com.island.ohara.client.configurator.v0.QueryApi.RdbColumn
import com.island.ohara.connector.jdbc.util.DateTimeUtils

class RDBDataTypeConverter {
  def converterValue(resultSet: ResultSet, column: RdbColumn): Object = {
    val columnName = column.name
    val typeName = column.dataType

    // the type name from postgresql is lower case...
    import RDBDataTypeConverter._
    typeName.toUpperCase match {
      case RDB_TYPE_BOOLEAN =>
        java.lang.Boolean.valueOf(resultSet.getBoolean(columnName))

      case RDB_TYPE_BIT =>
        java.lang.Byte.valueOf(resultSet.getByte(columnName))

      case RDB_TYPE_INTEGER | RDB_TYPE_INTEGER_2 =>
        java.lang.Integer.valueOf(resultSet.getInt(columnName))

      case RDB_TYPE_BIGINT =>
        java.lang.Long.valueOf(resultSet.getLong(columnName))

      case RDB_TYPE_FLOAT =>
        java.lang.Float.valueOf(resultSet.getFloat(columnName))

      case RDB_TYPE_DOUBLE =>
        java.lang.Double.valueOf(resultSet.getDouble(columnName))

      case RDB_TYPE_CHAR | RDB_TYPE_VARCHAR | RDB_TYPE_LONGVARCHAR =>
        resultSet.getString(columnName)

      case RDB_TYPE_TIMESTAMP =>
        resultSet.getTimestamp(columnName, DateTimeUtils.CALENDAR)

      case RDB_TYPE_DATE =>
        resultSet.getDate(columnName, DateTimeUtils.CALENDAR)

      case RDB_TYPE_TIME =>
        resultSet.getTime(columnName, DateTimeUtils.CALENDAR)

      case _ =>
        throw new RuntimeException(s"Data type '$typeName' not support on column '$columnName'.")
    }
  }
}

object RDBDataTypeConverter {
  val RDB_TYPE_BOOLEAN: String = "BOOLEAN"
  val RDB_TYPE_BIT: String = "BIT"
  val RDB_TYPE_INTEGER: String = "INT"
  // a name from postgresql
  val RDB_TYPE_INTEGER_2: String = "INT4"
  val RDB_TYPE_BIGINT: String = "BIGINT"
  val RDB_TYPE_FLOAT: String = "FLOAT"
  val RDB_TYPE_DOUBLE: String = "DOUBLE"
  val RDB_TYPE_CHAR: String = "CHAR"
  val RDB_TYPE_VARCHAR: String = "VARCHAR"
  val RDB_TYPE_LONGVARCHAR: String = "LONGVARCHAR"
  val RDB_TYPE_TIMESTAMP: String = "TIMESTAMP"
  val RDB_TYPE_DATE: String = "DATE"
  val RDB_TYPE_TIME: String = "TIME"
}
