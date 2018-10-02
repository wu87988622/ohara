package com.island.ohara.connector.jdbc.datatype

import java.sql.ResultSet
import com.island.ohara.client.ConfiguratorJson.RdbColumn
import com.island.ohara.connector.jdbc.util.DateTimeUtils

class RDBDataTypeConverter {
  def converterValue(resultSet: ResultSet, column: RdbColumn): Object = {
    val columnName = column.name
    val typeName = column.typeName

    val value: Object = typeName match {
      case RDBDataTypeConverter.RDB_TYPE_BOOLEAN =>
        java.lang.Boolean.valueOf(resultSet.getBoolean(columnName))

      case RDBDataTypeConverter.RDB_TYPE_BIT =>
        java.lang.Byte.valueOf(resultSet.getByte(columnName))

      case RDBDataTypeConverter.RDB_TYPE_INTEGER =>
        java.lang.Integer.valueOf(resultSet.getInt(columnName))

      case RDBDataTypeConverter.RDB_TYPE_BIGINT =>
        java.lang.Long.valueOf(resultSet.getLong(columnName))

      case RDBDataTypeConverter.RDB_TYPE_FLOAT =>
        java.lang.Float.valueOf(resultSet.getFloat(columnName))

      case RDBDataTypeConverter.RDB_TYPE_DOUBLE =>
        java.lang.Double.valueOf(resultSet.getDouble(columnName))

      case RDBDataTypeConverter.RDB_TYPE_CHAR | RDBDataTypeConverter.RDB_TYPE_VARCHAR |
          RDBDataTypeConverter.RDB_TYPE_LONGVARCHAR =>
        resultSet.getString(columnName)

      case RDBDataTypeConverter.RDB_TYPE_TIMESTAMP =>
        resultSet.getTimestamp(columnName, DateTimeUtils.CALENDAR)

      case RDBDataTypeConverter.RDB_TYPE_DATE =>
        resultSet.getDate(columnName, DateTimeUtils.CALENDAR)

      case RDBDataTypeConverter.RDB_TYPE_TIME =>
        resultSet.getTime(columnName, DateTimeUtils.CALENDAR)

      case _ =>
        throw new RuntimeException(s"Data type '$typeName' not support on column '$columnName'.")
    }
    value
  }
}

object RDBDataTypeConverter {
  val RDB_TYPE_BOOLEAN: String = "BOOLEAN"
  val RDB_TYPE_BIT: String = "BIT"
  val RDB_TYPE_INTEGER: String = "INT"
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
