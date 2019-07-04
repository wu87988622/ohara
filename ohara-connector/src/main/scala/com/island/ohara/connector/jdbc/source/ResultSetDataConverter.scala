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
      if (value == null)
        ColumnInfo(column.name, column.dataType, None)
      else ColumnInfo(column.name, column.dataType, value)
    })
  }
}
