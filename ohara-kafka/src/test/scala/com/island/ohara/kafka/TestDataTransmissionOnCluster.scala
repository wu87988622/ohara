package com.island.ohara.kafka

import java.util

import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.{OharaTestUtil, With3Blockers3Workers}
import com.island.ohara.io.ByteUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.connector.{
  SimpleRowSinkConnector,
  SimpleRowSinkTask,
  SimpleRowSourceConnector,
  SimpleRowSourceTask
}
import com.island.ohara.serialization.RowSerializer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{
  ByteArrayDeserializer,
  ByteArraySerializer,
  StringDeserializer,
  StringSerializer
}
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestDataTransmissionOnCluster extends With3Blockers3Workers with Matchers {

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
      .build
    testUtil.kafkaBrokers.size shouldBe 3
    testUtil.createTopic(topicName)
    val (_, rowQueue) =
      testUtil.run(topicName, true, new ByteArrayDeserializer, KafkaUtil.wrapDeserializer(RowSerializer))
    val totalMessageCount = 100
    doClose(new RowProducer[Array[Byte]](testUtil.producerConfig.toProperties, new ByteArraySerializer)) { producer =>
      {
        var count: Int = totalMessageCount
        while (count > 0) {
          producer.send(new ProducerRecord[Array[Byte], Row](topicName, ByteUtil.toBytes("key"), row))
          count -= 1
        }
      }
    }
    OharaTestUtil.await(() => rowQueue.size() == totalMessageCount, 1 minute)
    rowQueue.forEach((r: Row) => {
      r.cellCount shouldBe row.cellCount
      r.seekCell(0).name shouldBe "cf0"
      r.seekCell(0).value shouldBe 0
      r.seekCell(1).name shouldBe "cf1"
      r.seekCell(1).value shouldBe 1
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
    val row = Row.builder.append(Cell.builder.name("cf0").build(10)).append(Cell.builder.name("cf1").build(11)).build
    val resp = testUtil
      .sinkConnectorCreator()
      .name(connectorName)
      .connectorClass(classOf[SimpleRowSinkConnector])
      .topic(topicName)
      .taskNumber(1)
      .disableConverter
      .run()
    withClue(s"body:${resp.body}") {
      resp.statusCode shouldBe 201
    }

    import scala.concurrent.duration._
    OharaTestUtil.await(() => SimpleRowSinkTask.runningTaskCount.get() == 1, 30 second)

    doClose(new RowProducer[Array[Byte]](testUtil.producerConfig.toProperties, new ByteArraySerializer)) { producer =>
      {
        0 until rowCount foreach (_ =>
          producer.send(new ProducerRecord[Array[Byte], Row](topicName, ByteUtil.toBytes("key"), row)))
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
    val resp = testUtil
      .sourceConnectorCreator()
      .name(connectorName)
      .connectorClass(classOf[SimpleRowSourceConnector])
      .topic(topicName)
      .taskNumber(1)
      .disableConverter
      .config(Map(SimpleRowSourceConnector.POLL_COUNT_MAX -> pollCountMax.toString))
      .run()
    withClue(s"body:${resp.body}") {
      resp.statusCode shouldBe 201
    }

    import scala.concurrent.duration._
    OharaTestUtil.await(() => SimpleRowSourceTask.runningTaskCount.get() == 1, 30 second)
    doClose(new RowConsumer[Array[Byte]](testUtil.consumerConfig.toProperties, new ByteArrayDeserializer)) { consumer =>
      {
        consumer.subscribe(util.Arrays.asList(topicName))
        val list =
          Iterator.continually(consumer.poll(30 * 1000)).takeWhile(record => record != null && !record.isEmpty).toList
        // check the number of rows consumer has received
        list.map(_.count()).sum shouldBe pollCountMax * SimpleRowSourceTask.rows.length
      }
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

    doClose(
      new KafkaProducer[String, Row](testUtil.producerConfig.toProperties,
                                     new StringSerializer,
                                     KafkaUtil.wrapSerializer(RowSerializer))) { producer =>
      producer.send(new ProducerRecord[String, Row](topicName, topicName, row)).get()
    }

    val (_, valueQueue) =
      testUtil.run(topicName, true, new StringDeserializer, KafkaUtil.wrapDeserializer(RowSerializer))
    OharaTestUtil.await(() => valueQueue.size() == 1, 10 seconds)
    val fromKafka = valueQueue.take()

    fromKafka.seekCell(0).name shouldBe "c"
    fromKafka.seekCell(1).name shouldBe "b"
    fromKafka.seekCell(2).name shouldBe "a"
  }
}
