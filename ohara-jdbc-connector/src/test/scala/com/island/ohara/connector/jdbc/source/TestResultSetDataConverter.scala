package com.island.ohara.connector.jdbc.source

import java.sql.{ResultSet, Timestamp}

import com.island.ohara.client.ConfiguratorJson.RdbColumn
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
    columnList += new RdbColumn("column1", RDBDataTypeConverter.RDB_TYPE_TIMESTAMP, true)
    columnList += new RdbColumn("column2", RDBDataTypeConverter.RDB_TYPE_VARCHAR, false)
    columnList += new RdbColumn("column3", RDBDataTypeConverter.RDB_TYPE_INTEGER, false)

    val result: Seq[ColumnInfo[_]] = ResultSetDataConverter.converterRecord(resultSet, columnList)
    result(0).columnName shouldBe "column1"
    result(0).columnType shouldBe RDBDataTypeConverter.RDB_TYPE_TIMESTAMP
    result(0).value.toString() shouldBe "1970-01-01 08:00:00.0"

    result(1).columnName shouldBe "column2"
    result(1).columnType shouldBe RDBDataTypeConverter.RDB_TYPE_VARCHAR
    result(1).value shouldBe "aaa"

    result(2).columnName shouldBe "column3"
    result(2).columnType shouldBe RDBDataTypeConverter.RDB_TYPE_INTEGER
    result(2).value shouldBe 10
  }
}
