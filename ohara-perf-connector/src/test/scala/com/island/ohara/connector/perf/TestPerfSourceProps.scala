package com.island.ohara.connector.perf
import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.kafka.connector.TaskConfig
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class TestPerfSourceProps extends SmallTest with Matchers {
  private[this] val props = PerfSourceProps(10, 10 seconds)
  private[this] val topics = Seq("TestPerfSourceProps")
  private[this] val schema = Seq(Column.of("name", DataType.SHORT, 1))

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
      TaskConfig.builder().name(methodName()).schema(schema.asJava).options(props.toMap.asJava).build())
  }

  @Test
  def testEmptySchema(): Unit = {
    val source = new PerfSource
    an[IllegalArgumentException] should be thrownBy source._start(
      TaskConfig.builder().name(methodName()).topics(topics.asJava).options(props.toMap.asJava).build())
  }

  @Test
  def testInvalidProps(): Unit = {
    TaskConfig
      .builder()
      .name(methodName())
      .topics(topics.asJava)
      .schema(schema.asJava)
      .options(props.copy(batch = -1).toMap.asJava)
      .build()
    val source = new PerfSource
    an[IllegalArgumentException] should be thrownBy source._start(
      TaskConfig
        .builder()
        .name(methodName())
        .topics(topics.asJava)
        .schema(schema.asJava)
        .options(props.copy(batch = -1).toMap.asJava)
        .build())
  }
}
