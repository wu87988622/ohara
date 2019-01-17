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

import java.sql.{ResultSet, Timestamp}

import com.island.ohara.client.configurator.v0.QueryApi.RdbColumn
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.connector.jdbc.datatype.RDBDataTypeConverter
import com.island.ohara.connector.jdbc.util.{ColumnInfo, DateTimeUtils}
import org.junit.Test
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

import scala.collection.mutable.ListBuffer

class TestResultSetDataConverter extends MediumTest with Matchers with MockitoSugar {

  @Test
  def testConverterRecord(): Unit = {
    val resultSet: ResultSet = mock[ResultSet]
    when(resultSet.getTimestamp("column1", DateTimeUtils.CALENDAR)).thenReturn(new Timestamp(0L))
    when(resultSet.getString("column2")).thenReturn("aaa")
    when(resultSet.getInt("column3")).thenReturn(10)

    val columnList = new ListBuffer[RdbColumn]
    columnList += RdbColumn("column1", RDBDataTypeConverter.RDB_TYPE_TIMESTAMP, true)
    columnList += RdbColumn("column2", RDBDataTypeConverter.RDB_TYPE_VARCHAR, false)
    columnList += RdbColumn("column3", RDBDataTypeConverter.RDB_TYPE_INTEGER, false)

    val result: Seq[ColumnInfo[_]] = ResultSetDataConverter.converterRecord(resultSet, columnList)
    result.head.columnName shouldBe "column1"
    result.head.columnType shouldBe RDBDataTypeConverter.RDB_TYPE_TIMESTAMP
    result.head.value.toString shouldBe "1970-01-01 08:00:00.0"

    result(1).columnName shouldBe "column2"
    result(1).columnType shouldBe RDBDataTypeConverter.RDB_TYPE_VARCHAR
    result(1).value shouldBe "aaa"

    result(2).columnName shouldBe "column3"
    result(2).columnType shouldBe RDBDataTypeConverter.RDB_TYPE_INTEGER
    result(2).value shouldBe 10
  }
}
