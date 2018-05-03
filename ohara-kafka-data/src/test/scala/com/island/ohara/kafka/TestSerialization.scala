package com.island.ohara.kafka

import com.island.ohara.core.{Cell, Row, Table}
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestSerialization extends SmallTest with Matchers {

  @Test
  def testSerializeTable(): Unit = {
    val table = Table.builder("my_table").append(Row(Cell.builder.name("cell").build(123))).build()
    val copy = (new TableDeserializer).deserialize("xx", (new TableSerializer).serialize("xx", table))
    copy.id shouldBe table.id
    copy.cellCount shouldBe table.cellCount
    copy.rowCount shouldBe table.rowCount
  }

  @Test
  def testSerializeRow(): Unit = {
    val row = Row(Cell.builder.name("cell").build(123))
    val copy = (new RowDeserializer).deserialize("xx", (new RowSerializer).serialize("topic", row))
    copy.cellCount shouldBe row.cellCount
  }
}
