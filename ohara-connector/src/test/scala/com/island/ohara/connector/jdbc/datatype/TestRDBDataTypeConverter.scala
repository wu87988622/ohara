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

import java.sql.{ResultSet, Timestamp}

import com.island.ohara.client.configurator.v0.QueryApi.RdbColumn
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.connector.jdbc.util.DateTimeUtils
import org.junit.Test
import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatest.exceptions.TestFailedException
import org.scalatest.mockito.MockitoSugar

class TestRDBDataTypeConverter extends MediumTest with Matchers with MockitoSugar {

  @Test
  def testConverterBooleanValue(): Unit = {
    val resultSet: ResultSet = mock[ResultSet]
    when(resultSet.getBoolean("column1")).thenReturn(true)
    val column = RdbColumn("column1", RDBDataTypeConverter.RDB_TYPE_BOOLEAN, false)
    val rdbDataTypeConverter: RDBDataTypeConverter = new RDBDataTypeConverter()
    val result: Object = rdbDataTypeConverter.converterValue(resultSet, column)
    result shouldBe true
  }

  @Test
  def testConverterBitValue(): Unit = {
    val resultSet: ResultSet = mock[ResultSet]
    val value: Byte = 5
    when(resultSet.getByte("column1")).thenReturn(value)
    val column = RdbColumn("column1", RDBDataTypeConverter.RDB_TYPE_BIT, false)
    val rDBDataTypeConverter: RDBDataTypeConverter = new RDBDataTypeConverter()
    val result: Object = rDBDataTypeConverter.converterValue(resultSet, column)
    result.isInstanceOf[Byte] shouldBe true
    result.isInstanceOf[Object] shouldBe true

    intercept[TestFailedException] {
      result.isInstanceOf[String] shouldBe true
    }.getMessage() shouldBe "false was not equal to true"
  }

  @Test
  def testConverterIntegerValue(): Unit = {
    val resultSet: ResultSet = mock[ResultSet]
    when(resultSet.getInt("column1")).thenReturn(100)
    val column = RdbColumn("column1", RDBDataTypeConverter.RDB_TYPE_INTEGER, false)
    val rdbDataTypeConverter: RDBDataTypeConverter = new RDBDataTypeConverter()
    val result: Object = rdbDataTypeConverter.converterValue(resultSet, column)
    result shouldBe 100
  }

  @Test
  def testConverterChar(): Unit = {
    val resultSet: ResultSet = mock[ResultSet]
    when(resultSet.getString("column1")).thenReturn("h")
    val column = RdbColumn("column1", RDBDataTypeConverter.RDB_TYPE_CHAR, false)
    val rdbDataTypeConverter: RDBDataTypeConverter = new RDBDataTypeConverter()
    val result: Object = rdbDataTypeConverter.converterValue(resultSet, column)
    result shouldBe "h"
  }

  @Test
  def testConveterTimestamp(): Unit = {
    val resultSet: ResultSet = mock[ResultSet]
    when(resultSet.getTimestamp("column1", DateTimeUtils.CALENDAR)).thenReturn(new Timestamp(0L))
    val column = RdbColumn("column1", RDBDataTypeConverter.RDB_TYPE_TIMESTAMP, false)
    val rdbDataTypeConverter: RDBDataTypeConverter = new RDBDataTypeConverter()
    val result: Object = rdbDataTypeConverter.converterValue(resultSet, column)
    result.isInstanceOf[Timestamp] shouldBe true
    result.isInstanceOf[Object] shouldBe true
    result.toString shouldBe "1970-01-01 08:00:00.0"
  }

  @Test
  def testConverterVarchar(): Unit = {
    val resultSet: ResultSet = mock[ResultSet]
    when(resultSet.getString("column1")).thenReturn("hello")
    val column = RdbColumn("column1", RDBDataTypeConverter.RDB_TYPE_VARCHAR, false)
    val rdbDataTypeConverter: RDBDataTypeConverter = new RDBDataTypeConverter()
    val result: Object = rdbDataTypeConverter.converterValue(resultSet, column)
    result shouldBe "hello"
  }
}
