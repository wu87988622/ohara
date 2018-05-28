package com.island.ohara.kafka

import java.util

import com.island.ohara.core.{Cell, Row}
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.ByteUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.connector.{
  SimpleRowSinkConnector,
  SimpleRowSinkTask,
  SimpleRowSourceConnector,
  SimpleRowSourceTask
}
import com.island.ohara.rule.LargeTest
import com.island.ohara.serialization.RowSerializer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestDataTransmissionOnCluster extends LargeTest with Matchers {

  private[this] val topicName = testName.getMethodName + "-topic"

  @Before
  def setUp(): Unit = {
    SimpleRowSinkTask.reset()
    SimpleRowSourceTask.reset()
  }

  @Test
  def testRowProducer2RowConsumer(): Unit = {
    val row = Row.builder.append(Cell.builder.name("cf0").build(0)).append(Cell.builder.name("cf1").build(1)).build
    doClose(new OharaTestUtil(3)) { testUtil =>
      {
        testUtil.kafkaBrokers.size shouldBe 3
        testUtil.createTopic(topicName)
        val (_, rowQueue) =
          testUtil.run(topicName, true, new ByteArrayDeserializer, KafkaUtil.wrapDeserializer(RowSerializer))
        val totalMessageCount = 100
        doClose(new RowProducer[Array[Byte]](testUtil.producerConfig.toProperties, new ByteArraySerializer)) {
          producer =>
            {
              var count: Int = totalMessageCount
              while (count > 0) {
                producer.send(new ProducerRecord[Array[Byte], Row](topicName, ByteUtil.toBytes("key"), row))
                count -= 1
              }
            }
        }
        testUtil.await(() => rowQueue.size() == totalMessageCount, 1 minute)
        rowQueue.forEach((r: Row) => {
          r.cellCount shouldBe row.cellCount
          r.seekCell(0).name shouldBe "cf0"
          r.seekCell(0).value shouldBe 0
          r.seekCell(1).name shouldBe "cf1"
          r.seekCell(1).value shouldBe 1
        })
      }
    }
  }
  @Test
  def testProducer2SinkConnector(): Unit = {
    val rowCount = 3
    val row = Row.builder.append(Cell.builder.name("cf0").build(10)).append(Cell.builder.name("cf1").build(11)).build
    doClose(new OharaTestUtil(3)) { testUtil =>
      {
        val resp = testUtil
          .sinkConnectorCreator()
          .name("my_sink_connector")
          .connectorClass(classOf[SimpleRowSinkConnector])
          .topic(topicName)
          .taskNumber(1)
          .disableConverter
          .run()
        withClue(s"body:${resp._2}") {
          resp._1 shouldBe 201
        }

        import scala.concurrent.duration._
        testUtil.await(() => SimpleRowSinkTask.runningTaskCount.get() == 1, 30 second)

        doClose(new RowProducer[Array[Byte]](testUtil.producerConfig.toProperties, new ByteArraySerializer)) {
          producer =>
            {
              0 until rowCount foreach (_ =>
                producer.send(new ProducerRecord[Array[Byte], Row](topicName, ByteUtil.toBytes("key"), row)))
              producer.flush()
            }
        }
        testUtil.await(() => SimpleRowSinkTask.receivedRows.size == rowCount, 30 second)
      }
    }
  }

  @Test
  def testSourceConnector2Consumer(): Unit = {
    val pollCountMax = 5
    doClose(new OharaTestUtil(3)) { testUtil =>
      {
        val resp = testUtil
          .sourceConnectorCreator()
          .name("my_sink_connector")
          .connectorClass(classOf[SimpleRowSourceConnector])
          .topic(topicName)
          .taskNumber(1)
          .disableConverter
          .config(Map(SimpleRowSourceConnector.POLL_COUNT_MAX -> pollCountMax.toString))
          .run()
        withClue(s"body:${resp._2}") {
          resp._1 shouldBe 201
        }

        import scala.concurrent.duration._
        testUtil.await(() => SimpleRowSourceTask.runningTaskCount.get() == 1, 30 second)
        doClose(new RowConsumer[Array[Byte]](testUtil.consumerConfig.toProperties, new ByteArrayDeserializer)) {
          consumer =>
            {
              consumer.subscribe(util.Arrays.asList(topicName))
              val list = Iterator
                .continually(consumer.poll(30 * 1000))
                .takeWhile(record => record != null && !record.isEmpty)
                .toList
              // check the number of rows consumer has received
              list.map(_.count()).sum shouldBe pollCountMax * SimpleRowSourceTask.rows.length
            }
        }
      }
    }
  }
}
