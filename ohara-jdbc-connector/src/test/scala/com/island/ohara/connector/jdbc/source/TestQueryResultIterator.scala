package com.island.ohara.connector.jdbc.source

import java.sql.{PreparedStatement, ResultSet}
import com.island.ohara.rule.MediumTest
import org.junit.Test
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

import scala.collection.mutable.ListBuffer

class TestQueryResultIterator extends MediumTest with Matchers with MockitoSugar {

  @Test
  def testOnlyNext(): Unit = {
    val preparedStatement = mock[PreparedStatement]
    val resultSet = mock[ResultSet]
    when(preparedStatement.executeQuery()).thenReturn(resultSet)

    var columnList = new ListBuffer[String]
    columnList += "column1"
    columnList += "column2"
    columnList += "column3"

    var it: Iterator[Seq[Object]] = new QueryResultIterator(preparedStatement, columnList)
    intercept[NoSuchElementException] {
      it.next()
    }.getMessage() shouldBe "Cache no data"
  }

  @Test
  def testHasNextHaveData(): Unit = {
    val preparedStatement = mock[PreparedStatement]
    val resultSet = mock[ResultSet]
    when(preparedStatement.executeQuery()).thenReturn(resultSet)
    when(resultSet.next()).thenReturn(true).thenReturn(false)
    when(resultSet.getString("column1")).thenReturn("value1-1")
    when(resultSet.getString("column2")).thenReturn("value1-2")
    when(resultSet.getString("column3")).thenReturn("value1-3")

    var columnList = new ListBuffer[String]
    columnList += "column1"
    columnList += "column2"
    columnList += "column3"

    var it: Iterator[Seq[Object]] = new QueryResultIterator(preparedStatement, columnList)
    var count: Int = 0
    while (it.hasNext) {
      it.next()
      count = count + 1
    }
    count shouldBe 1
  }

  @Test
  def testHasNextHaveMoreData(): Unit = {
    val preparedStatement = mock[PreparedStatement]
    val resultSet = mock[ResultSet]
    when(preparedStatement.executeQuery()).thenReturn(resultSet)
    when(resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false)
    when(resultSet.getString("column1")).thenReturn("value1-1").thenReturn("value2-1").thenReturn("value3-1")
    when(resultSet.getString("column2")).thenReturn("value1-2").thenReturn("value2-2").thenReturn("value2-3")
    when(resultSet.getString("column3")).thenReturn("value1-3").thenReturn("value2-3").thenReturn("value3-3")

    var columnList = new ListBuffer[String]
    columnList += "column1"
    columnList += "column2"
    columnList += "column3"

    var it: Iterator[Seq[Object]] = new QueryResultIterator(preparedStatement, columnList)
    var count: Int = 0
    while (it.hasNext) {
      it.next()
      count = count + 1
    }
    count shouldBe 3
  }

  @Test
  def testHasNextNoData(): Unit = {
    val preparedStatement = mock[PreparedStatement]
    val resultSet = mock[ResultSet]
    when(preparedStatement.executeQuery()).thenReturn(resultSet)
    when(resultSet.next()).thenReturn(false)

    var columnList = new ListBuffer[String]
    columnList += "column1"
    columnList += "column2"
    columnList += "column3"

    var it: Iterator[Seq[Object]] = new QueryResultIterator(preparedStatement, columnList)
    var count: Int = 0
    while (it.hasNext) {
      it.next()
      count = count + 1
    }
    count shouldBe 0
  }

}
