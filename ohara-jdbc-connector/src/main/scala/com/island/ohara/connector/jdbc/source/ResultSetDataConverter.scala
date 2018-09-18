package com.island.ohara.connector.jdbc.source

import java.sql.ResultSet

import com.island.ohara.client.ConfiguratorJson.RdbColumn
import com.island.ohara.connector.jdbc.datatype.{RDBDataTypeConverter, RDBDataTypeConverterFactory}
import com.island.ohara.connector.jdbc.util.ColumnInfo
import scala.collection.mutable.ListBuffer

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
  def converterRecord(resultSet: ResultSet, columns: Seq[RdbColumn]): Seq[ColumnInfo] = {
    val columnInfos = new ListBuffer[ColumnInfo]
    val rdbDataTypeConverter: RDBDataTypeConverter = RDBDataTypeConverterFactory.dataTypeConverter()
    columns
      .map(column => {
        val value: Object = rdbDataTypeConverter.converterValue(resultSet, column)
        val columnInfo: ColumnInfo = new ColumnInfo(column.name, column.typeName, value)
        columnInfos += columnInfo
      })
      .toSeq
    columnInfos
  }
}
