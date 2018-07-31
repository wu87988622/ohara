package com.island.ohara.data

import com.island.ohara.data.Cell
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestBuilder extends SmallTest with Matchers {

  @Test
  def testInvalidArgument(): Unit = {
    an[NullPointerException] should be thrownBy Cell.builder.name("").build(123)
    an[NullPointerException] should be thrownBy Cell.builder.name("123").build(null)
  }

  @Test
  def testCellBuilder(): Unit = {
    val builder = Cell.builder.name("cf")
    val list = List[Any](123, 123L, "123", 123D, 123F, true)
    list
      .map(_ match {
        case v: Boolean => builder.build(v)
        case v: Short   => builder.build(v)
        case v: Int     => builder.build(v)
        case v: Long    => builder.build(v)
        case v: Float   => builder.build(v)
        case v: Double  => builder.build(v)
        case v: String  => builder.build(v)
        case _          => throw new IllegalArgumentException
      })
      .foreach((cell: Cell[_]) => {
        cell.value match {
          case v: Boolean => v shouldBe true
          case _          => cell.value.toString.toDouble shouldBe 123.0
        }
      })
  }

  @Test
  def testRowBuilder(): Unit = {
    val row = Row.builder
      .append(Cell.builder.name("0").build(1))
      .append(Cell.builder.name("1").build("Abc"))
      .append(Cell.builder.name("2").build(10.123))
      .tags(Set[String]("tag0", "tag8"))
      .build()

    row.cellCount shouldBe 3
    row.seekCell(0).name shouldBe "0"
    row.seekCell(0).value shouldBe 1
    row.seekCell(1).name shouldBe "1"
    row.seekCell(1).value shouldBe "Abc"
    row.seekCell(2).name shouldBe "2"
    row.seekCell(2).value shouldBe 10.123
    row.tags.size shouldBe 2
    row.tags.contains("tag0") shouldBe true
    row.tags.contains("tag8") shouldBe true
    row.tags.contains("phantom-tag") shouldBe false
    an[IndexOutOfBoundsException] should be thrownBy row.seekCell(-1)
    an[IndexOutOfBoundsException] should be thrownBy row.seekCell(100)
  }

}
