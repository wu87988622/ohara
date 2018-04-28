package com.island.ohara.core

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestBuilder extends SmallTest with Matchers {

  @Test
  def testCellBuilder():Unit = {
    val builder = Cell.builder.name("cf")
    val list = List(123, 123L, "123", 123D, 123F, true)
    list.map(_ match {
      case v: Boolean => builder.build(v)
      case v: Short => builder.build(v)
      case v: Int => builder.build(v)
      case v: Long => builder.build(v)
      case v: Float => builder.build(v)
      case v: Double => builder.build(v)
      case v: String => builder.build(v)
      case _ => throw new IllegalArgumentException
    }).foreach((cell: Cell[_]) => {
      cell.value match {
        case v: Boolean => v shouldBe true
        case _ => cell.value.toString.toDouble shouldBe 123.0
      }
    })
  }

  @Test
  def testRowBuilder():Unit = {
    val row = Row.builder
      .append(Cell.builder.name("0").build(1))
      .append(Cell.builder.name("1").build("Abc"))
      .append(Cell.builder.name("2").build(10.123))
      .build()

    row.cellCount shouldBe 3
    row.seekCell(0).name shouldBe "0"
    row.seekCell(0).value shouldBe 1
    row.seekCell(1).name shouldBe "1"
    row.seekCell(1).value shouldBe "Abc"
    row.seekCell(2).name shouldBe "2"
    row.seekCell(2).value shouldBe 10.123
    an[IndexOutOfBoundsException] should be thrownBy row.seekCell(-1)
    an[IndexOutOfBoundsException] should be thrownBy row.seekCell(100)
  }

  @Test
  def testTableBuilder():Unit = {
    val table = Table.builder("test_table")
      .append(Row.builder.append(Cell.builder.name("0").build(1)).build())
      .append(Row.builder.append(Cell.builder.name("1").build("ohara")).build())
      .append(Row.builder.append(Cell.builder.name("2").build(10.134)).build())
      .build()

    table.id shouldBe "test_table"
    table.cellCount shouldBe 3
    table.rowCount shouldBe 3
    table.seekRow(0).seekCell(0).name shouldBe "0"
    table.seekRow(0).seekCell(0).value shouldBe 1
    table.seekRow(1).seekCell(0).name shouldBe "1"
    table.seekRow(1).seekCell(0).value shouldBe "ohara"
    table.seekRow(2).seekCell(0).name shouldBe "2"
    table.seekRow(2).seekCell(0).value shouldBe 10.134
    an[IndexOutOfBoundsException] should be thrownBy table.seekRow(-1)
    an[IndexOutOfBoundsException] should be thrownBy table.seekRow(100)
  }
}
