package com.island.ohara.connector.jdbc.source

import java.sql.ResultSet

import com.island.ohara.client.configurator.v0.QueryApi.RdbColumn
import com.island.ohara.connector.jdbc.datatype.{RDBDataTypeConverter, RDBDataTypeConverterFactory}
import com.island.ohara.connector.jdbc.util.ColumnInfo

/**
  * This class for converter the ResultSet data
  */
object ResultSetDataConverter {

  /**
    * Converter the ResultSet a record data
    * @param resultSet
    * @param columns
    * @return
    */
  def converterRecord(resultSet: ResultSet, columns: Seq[RdbColumn]): Seq[ColumnInfo[_]] = {
    val rdbDataTypeConverter: RDBDataTypeConverter = RDBDataTypeConverterFactory.dataTypeConverter()
    columns.map(column => {
      val value: Object = rdbDataTypeConverter.converterValue(resultSet, column)
      ColumnInfo(column.name, column.dataType, value)
    })
  }
}
