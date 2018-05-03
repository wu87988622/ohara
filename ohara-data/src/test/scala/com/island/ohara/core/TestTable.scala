package com.island.ohara.core

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestTable extends SmallTest with Matchers {

  @Test
  def testBuildTable(): Unit = {
    val table = Table(
      "test_table",
      for (_ <- 0 until 10)
        yield Row(for (cellIndex <- 0 until 10) yield Cell.builder.name(cellIndex.toString).build(cellIndex)))
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
