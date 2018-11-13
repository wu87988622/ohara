package com.island.ohara.connector.perf
import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.common.data.DataType
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.kafka.connector.TaskConfig
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestPerfSourceProps extends SmallTest with Matchers {
  private[this] val props = PerfSourceProps(10, 10 seconds)
  private[this] val topics = Seq("TestPerfSourceProps")
  private[this] val schema = Seq(Column("name", DataType.SHORT, 1))

  @Test
  def testPlainMap(): Unit = {
    val props = PerfSourceProps(123, 10 seconds)
    val copy = PerfSourceProps(props.toMap)
    props shouldBe copy
  }

  @Test
  def testEmptyTopics(): Unit = {
    val source = new PerfSource
    an[IllegalArgumentException] should be thrownBy source._start(
      TaskConfig(methodName, Seq.empty, schema, props.toMap))
  }

  @Test
  def testEmptySchema(): Unit = {
    val source = new PerfSource
    an[IllegalArgumentException] should be thrownBy source._start(
      TaskConfig(methodName, topics, Seq.empty, props.toMap))
  }

  @Test
  def testInvalidProps(): Unit = {
    val source = new PerfSource
    an[IllegalArgumentException] should be thrownBy source._start(
      TaskConfig(methodName, topics, schema, props.copy(batch = -1).toMap))
  }

}
