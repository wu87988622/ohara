package com.island.ohara.data

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestRow extends SmallTest with Matchers {

  @Test
  def testCellEqual(): Unit = {
    val cell0 = Cell.builder.name("CELL0").build("123")
    val cell1 = Cell.builder.name("CELL0").build("123")
    val cell2 = Cell.builder.name("CELL0").build(123)
    val cell3 = Cell.builder.name("CELLX").build("123")
    cell0 == cell0 shouldBe true
    cell0 == cell1 shouldBe true
    cell1 == cell0 shouldBe true

    //noinspection ComparingUnrelatedTypes
    cell0 == cell2 shouldBe false
    //noinspection ComparingUnrelatedTypes
    cell2 == cell0 shouldBe false

    cell0 == cell3 shouldBe false
    cell3 == cell0 shouldBe false
  }

  @Test
  def testRowEqual(): Unit = {
    val row0 = Row.builder.append(Cell.builder.name("CELL0").build("123")).build()
    val row1 = Row.builder.append(Cell.builder.name("CELL0").build("123")).build()
    val row2 = Row.builder.append(Cell.builder.name("CELL0").build(123)).build()
    val row3 = Row.builder.append(Cell.builder.name("CELL1").build("123")).build()
    val row4 =
      Row.builder.append(Cell.builder.name("CELL0").build("123")).append(Cell.builder.name("cell1").build(88)).build()
    row0 == row0 shouldBe true
    row0 == row1 shouldBe true
    row1 == row0 shouldBe true

    row0 == row2 shouldBe false
    row2 == row0 shouldBe false

    row0 == row3 shouldBe false
    row3 == row0 shouldBe false

    row0 == row4 shouldBe false
    row4 == row0 shouldBe false
  }
  @Test
  def testDuplicateColumns(): Unit = {
    val cell = Cell.builder.name("CELL0").build("123")
    an[IllegalArgumentException] should be thrownBy Row.builder.append(cell).append(cell).build()
  }

  @Test
  def testBuilderRow(): Unit = {
    val list = List[Any](123.toShort, 123, 123L, "123", 123D, 123F, true)
    val row = Row(list.map {
      case v: Boolean => Cell.builder.name("boolean").build(v)
      case v: Short   => Cell.builder.name("short").build(v)
      case v: Int     => Cell.builder.name("int").build(v)
      case v: Long    => Cell.builder.name("long").build(v)
      case v: Float   => Cell.builder.name("float").build(v)
      case v: Double  => Cell.builder.name("double").build(v)
      case v: String  => Cell.builder.name("string").build(v)
      case _          => throw new IllegalArgumentException
    })
    row.size shouldBe list.size
    row.names.size shouldBe list.size
    row.cell("boolean").value shouldBe true
    row.cell("short").value shouldBe 123
    row.cell("int").value shouldBe 123
    row.cell("long").value shouldBe 123
    row.cell("float").value shouldBe 123
    row.cell("double").value shouldBe 123
    row.cell("string").value shouldBe "123"
  }
}
