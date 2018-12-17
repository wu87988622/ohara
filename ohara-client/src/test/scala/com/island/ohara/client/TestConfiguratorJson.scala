package com.island.ohara.client
import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
class TestConfiguratorJson extends SmallTest with Matchers {

  @Test
  def testEmptySchema(): Unit = {
    val schema = Column.toColumns(Column.fromColumns(Seq.empty.asJava))
    schema.size shouldBe 0
  }

  @Test
  def testColumnString(): Unit = {
    val schema =
      Seq(Column.of("a", DataType.STRING, 0),
          Column.of("c", "d", DataType.INT, 1),
          Column.of("e", "f", DataType.DOUBLE, 2))
    schema shouldBe Column.toColumns(Column.fromColumns(schema.asJava)).asScala
  }
}
