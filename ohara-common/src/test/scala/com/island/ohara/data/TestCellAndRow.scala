package com.island.ohara.data

import com.island.ohara.rule.SmallTest
import com.island.ohara.serialization.Serializer
import org.junit.Test
import org.scalatest.Matchers

class TestCellAndRow extends SmallTest with Matchers {

  @Test
  def testEmptyRow(): Unit = {
    val copy = Serializer.ROW.from(Serializer.ROW.to(Row.empty))
    copy shouldBe Row.empty
  }

  @Test
  def testInvalidArgument(): Unit = {
    an[NullPointerException] should be thrownBy Cell("", 123)
    an[NullPointerException] should be thrownBy Cell("123", null)
  }

  @Test
  def testCellBuilder(): Unit = {
    val builder = Cell.builder.name("cf")
    val list = List[Any](123, 123L, "123", 123D, 123F, true)
    list
      .map {
        case v: Boolean => builder.build(v)
        case v: Short   => builder.build(v)
        case v: Int     => builder.build(v)
        case v: Long    => builder.build(v)
        case v: Float   => builder.build(v)
        case v: Double  => builder.build(v)
        case v: String  => builder.build(v)
        case _          => throw new IllegalArgumentException
      }
      .foreach((cell: Cell[_]) => {
        cell.value match {
          case v: Boolean => v shouldBe true
          case _          => cell.value.toString.toDouble shouldBe 123.0
        }
      })
  }

  @Test
  def testRowBuilder(): Unit = {
    val row = Row
      .builder()
      .cells(
        Seq(
          Cell("0", 1),
          Cell("1", "Abc"),
          Cell("2", 10.123)
        ))
      .tags(Set[String]("tag0", "tag8"))
      .build()

    row.size shouldBe 3
    row.cell(0).name shouldBe "0"
    row.cell(0).value shouldBe 1
    row.cell(1).name shouldBe "1"
    row.cell(1).value shouldBe "Abc"
    row.cell(2).name shouldBe "2"
    row.cell(2).value shouldBe 10.123
    row.tags.size shouldBe 2
    row.tags.contains("tag0") shouldBe true
    row.tags.contains("tag8") shouldBe true
    row.tags.contains("phantom-tag") shouldBe false
    an[IndexOutOfBoundsException] should be thrownBy row.cell(-1)
    an[IndexOutOfBoundsException] should be thrownBy row.cell(100)
  }

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
    val row0 = Row(Cell("CELL0", "123"))
    val row1 = Row(Cell("CELL0", "123"))
    val row2 = Row(Cell("CELL0", 123))
    val row3 = Row(Cell("CELL1", "123"))
    val row4 = Row(Cell("CELL0", "123"), Cell("cell1", 88))
    row0 shouldBe row0
    row0 shouldBe row1
    row1 shouldBe row0
    row1 shouldBe row1

    row0 should not be row2
    row2 should not be row0

    row0 should not be row3
    row3 should not be row0

    row0 should not be row4
    row4 should not be row0
  }
  @Test
  def testDuplicateColumns(): Unit = {
    an[IllegalArgumentException] should be thrownBy Row(Cell("cf", 123), Cell("cf", 123))
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
    }: _*)
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
