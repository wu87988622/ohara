package com.island.ohara.core

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TestTable extends FlatSpec with Matchers {

  "The build of table" should "work" in {
    val table = Table("test_table", for (_ <- 0 until 10) yield
      Row(for (cellIndex <- 0 until 10) yield Cell.builder.name(cellIndex.toString).build(cellIndex)))
    table.id shouldBe "test_table"
    table.cellCount shouldBe 100
    table.rowCount shouldBe 10
    an[IndexOutOfBoundsException] should be thrownBy table.seekRow(-1)
    an[IndexOutOfBoundsException] should be thrownBy table.seekRow(1000)
    for (index <- 0 until 10) {
      (table seekRow index).cellCount shouldBe 10
    }
  }
}
