package com.island.ohara.connector.jdbc.util

case class ColumnInfo[T](columnName: String, columnType: String, value: T)
