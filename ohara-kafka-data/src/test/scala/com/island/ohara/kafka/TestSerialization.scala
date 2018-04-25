package com.island.ohara.kafka

import com.island.ohara.core.{Cell, Row, Table}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TestSerialization extends FlatSpec with Matchers {

  "The conversion of table between serializer and deserializer" should "work" in {
    val table = Table.builder("my_table")
      .append(Row(Cell.builder.name("cell").build(123)))
      .build()
    val copy = (new TableDeserializer).deserialize("xx", (new TableSerializer).serialize("xx", table))
    copy.id shouldBe table.id
    copy.cellCount shouldBe table.cellCount
    copy.rowCount shouldBe table.rowCount
  }

  "The conversion of row between serializer and deserializer" should "work" in {
    val row = Row(Cell.builder.name("cell").build(123))
    val copy = (new RowDeserializer).deserialize("xx", (new RowSerializer).serialize("topic", row))
    copy.cellCount shouldBe row.cellCount
  }
}
