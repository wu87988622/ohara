package com.island.ohara.client
import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.common.data.DataType
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestConfiguratorJson extends SmallTest with Matchers {

  @Test
  def testEmptySchema(): Unit = {
    val schema = Column.toColumns(Column.toString(Seq.empty))
    schema.size shouldBe 0
  }

  @Test
  def testColumnString(): Unit = {
    val schema =
      Seq(Column("a", DataType.STRING, 0), Column("c", "d", DataType.INT, 1), Column("e", "f", DataType.DOUBLE, 2))
    schema shouldBe Column.toColumns(Column.toString(schema))
  }
}
