package com.island.ohara.kafka

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.ByteUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.connector.{
  SimpleRowSinkConnector,
  SimpleRowSinkTask,
  SimpleRowSourceConnector,
  SimpleRowSourceTask
}
import com.island.ohara.serialization.{DataType, RowSerializer, Serializer}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class TestDataTransmissionOnCluster extends With3Brokers3Workers with Matchers {

  @Before
  def setUp(): Unit = {
    SimpleRowSinkTask.reset()
    SimpleRowSourceTask.reset()
  }

  @Test
  def testRowProducer2RowConsumer(): Unit = {
    val topicName = methodName
    val row = Row.builder
      .append(Cell.builder.name("cf0").build(0))
      .append(Cell.builder.name("cf1").build(1))
      .tags(Set[String]("123", "456"))
      .build()
    testUtil.createTopic(topicName)
    val (_, rowQueue) =
      testUtil.run(topicName, true, new ByteArrayDeserializer, KafkaUtil.wrapDeserializer(RowSerializer))
    val totalMessageCount = 100
    doClose(Producer.builder(Serializer.BYTES, Serializer.ROW).brokers(testUtil.brokers).build()) { producer =>
      {
        var count: Int = totalMessageCount
        while (count > 0) {
          producer.sender().topic(topicName).key(ByteUtil.toBytes("key")).value(row).send()
          count -= 1
        }
      }
    }
    OharaTestUtil.await(() => rowQueue.size() == totalMessageCount, 1 minute)
    rowQueue.forEach((r: Row) => {
      r.size shouldBe row.size
      r.cell(0).name shouldBe "cf0"
      r.cell(0).value shouldBe 0
      r.cell(1).name shouldBe "cf1"
      r.cell(1).value shouldBe 1
      r.tags.size shouldBe 2
      r.tags.contains("123") shouldBe true
      r.tags.contains("456") shouldBe true
    })
  }
  @Test
  def testProducer2SinkConnector(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    val rowCount = 3
    val row = Row.builder.append(Cell.builder.name("cf0").build(10)).append(Cell.builder.name("cf1").build(11)).build()
    testUtil.connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[SimpleRowSinkConnector])
      .topic(topicName)
      .numberOfTasks(1)
      .disableConverter()
      .schema(Seq(Column("cf", DataType.BOOLEAN, 1)))
      .create()

    import scala.concurrent.duration._
    OharaTestUtil.await(() => SimpleRowSinkTask.runningTaskCount.get() == 1, 30 second)

    doClose(Producer.builder(Serializer.BYTES, Serializer.ROW).brokers(testUtil.brokers).build()) { producer =>
      {
        0 until rowCount foreach (_ =>
          producer.sender().topic(topicName).key(ByteUtil.toBytes("key")).value(row).send())
        producer.flush()
      }
    }
    OharaTestUtil.await(() => SimpleRowSinkTask.receivedRows.size == rowCount, 30 second)
  }

  @Test
  def testSourceConnector2Consumer(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    val pollCountMax = 5
    testUtil.connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[SimpleRowSourceConnector])
      .schema(Seq(Column("cf", DataType.BOOLEAN, 1)))
      .topic(topicName)
      .numberOfTasks(1)
      .disableConverter()
      .config(Map(SimpleRowSourceConnector.POLL_COUNT_MAX -> pollCountMax.toString))
      .create()

    import scala.concurrent.duration._
    OharaTestUtil.await(() => SimpleRowSourceTask.runningTaskCount.get() == 1, 30 second)
    doClose(
      Consumer
        .builder(Serializer.BYTES, Serializer.ROW)
        .brokers(testUtil.brokers)
        .topicName(topicName)
        .offsetFromBegin()
        .build()) {
      _.poll(40 seconds, pollCountMax * SimpleRowSourceTask.rows.length).size shouldBe pollCountMax * SimpleRowSourceTask.rows.length
    }
  }

  /**
    * Test case for OHARA-150
    */
  @Test
  def shouldKeepColumnOrderAfterSendToKafka(): Unit = {
    val topicName = methodName
    testUtil.createTopic(topicName)

    val row = Row.builder
      .append(Cell.builder.name("c").build(3))
      .append(Cell.builder.name("b").build(2))
      .append(Cell.builder.name("a").build(1))
      .build()

    doClose(Producer.builder(Serializer.STRING, Serializer.ROW).brokers(testUtil.brokers).build()) { producer =>
      producer.sender().topic(topicName).key(topicName).value(row).send()
      producer.flush()
    }

    val (_, valueQueue) =
      testUtil.run(topicName, true, new StringDeserializer, KafkaUtil.wrapDeserializer(RowSerializer))
    OharaTestUtil.await(() => valueQueue.size() == 1, 10 seconds)
    val fromKafka = valueQueue.take()

    fromKafka.cell(0).name shouldBe "c"
    fromKafka.cell(1).name shouldBe "b"
    fromKafka.cell(2).name shouldBe "a"

    doClose(Producer.builder(Serializer.STRING, Serializer.ROW).brokers(testUtil.brokers).build()) { producer =>
      val meta = Await.result(producer.sender().topic(topicName).key(topicName).value(row).send(), 10 seconds)
      meta.topic shouldBe topicName
    }
  }
}
