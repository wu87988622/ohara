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
    cell0.equals(cell0) shouldBe true
    cell0.equals(cell1) shouldBe true
    cell1.equals(cell0) shouldBe true

    cell0.equals(cell2) shouldBe false
    cell2.equals(cell0) shouldBe false

    cell0.equals(cell3) shouldBe false
    cell3.equals(cell0) shouldBe false
  }

  @Test
  def testRowEqual(): Unit = {
    val row0 = Row.builder.append(Cell.builder.name("CELL0").build("123")).build()
    val row1 = Row.builder.append(Cell.builder.name("CELL0").build("123")).build()
    val row2 = Row.builder.append(Cell.builder.name("CELL0").build(123)).build()
    val row3 = Row.builder.append(Cell.builder.name("CELL1").build("123")).build()
    val row4 =
      Row.builder.append(Cell.builder.name("CELL0").build("123")).append(Cell.builder.name("cell1").build(88)).build()
    row0.equals(row0) shouldBe true
    row0.equals(row1) shouldBe true
    row1.equals(row0) shouldBe true

    row0.equals(row2) shouldBe false
    row2.equals(row0) shouldBe false

    row0.equals(row3) shouldBe false
    row3.equals(row0) shouldBe false

    row0.equals(row4) shouldBe false
    row4.equals(row0) shouldBe false
  }
  @Test
  def testDuplicateColumns(): Unit = {
    val cell = Cell.builder.name("CELL0").build("123")
    an[IllegalArgumentException] should be thrownBy Row.builder.append(cell).append(cell).build()
  }

  @Test
  def testBuilderRow(): Unit = {
    val list = List[Any](123.toShort, 123, 123L, "123", 123D, 123F, true)
    val row = Row(list.map(_ match {
      case v: Boolean => Cell.builder.name("boolean").build(v)
      case v: Short   => Cell.builder.name("short").build(v)
      case v: Int     => Cell.builder.name("int").build(v)
      case v: Long    => Cell.builder.name("long").build(v)
      case v: Float   => Cell.builder.name("float").build(v)
      case v: Double  => Cell.builder.name("double").build(v)
      case v: String  => Cell.builder.name("string").build(v)
      case _          => throw new IllegalArgumentException
    }))
    row.size shouldBe list.size
    row.names.size shouldBe list.size
    row seekCell "boolean" shouldBe defined
    row seekCell "short" shouldBe defined
    row seekCell "int" shouldBe defined
    row seekCell "long" shouldBe defined
    row seekCell "float" shouldBe defined
    row seekCell "double" shouldBe defined
    row seekCell "string" shouldBe defined
  }
}
