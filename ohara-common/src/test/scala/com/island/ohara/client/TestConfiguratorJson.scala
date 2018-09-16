package com.island.ohara.client
import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestConfiguratorJson extends SmallTest with Matchers {

  @Test
  def testEmptySchema(): Unit = {
    val schema = Column.toColumns(Column.toString(Seq.empty))
    schema.size shouldBe 0
  }
}
