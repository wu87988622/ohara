package com.island.ohara.kafka

import com.island.ohara.core.{Cell, Row, Table}
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.ByteUtil
import com.island.ohara.io.CloseOnce._
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class TestPubAndSub extends FlatSpec with Matchers {

  private[this] lazy val logger = Logger(getClass.getName)

  "Transferring data from RowProducer to RowConsumer" should "work" in {
    val row = Row.builder.append(Cell.builder.name("cf0").build(0))
      .append(Cell.builder.name("cf1").build(1)).build
    doClose(new OharaTestUtil(3)) {
      testUtil => {
        testUtil.kafkaBrokers.size shouldBe 3
        testUtil.createTopic("my_topic")
        val (_, rowQueue) = testUtil.run("my_topic", new ByteArrayDeserializer, new RowDeserializer)
        val totalMessageCount = 100
        doClose(new RowProducer[Array[Byte]](testUtil.properties, new ByteArraySerializer)) {
          producer => {
            var count: Int = totalMessageCount
            while (count > 0) {
              producer.send(new ProducerRecord[Array[Byte], Row]("my_topic", ByteUtil.toBytes("key"), row))
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

  "Transferring data from TableProducer to TableConsumer" should "work" in {
    val table = Table("my_table", Row.builder.append(Cell.builder.name("cf0").build(0))
      .append(Cell.builder.name("cf1").build(1)).build)
    doClose(new OharaTestUtil(3)) {
      testUtil => {
        testUtil.kafkaBrokers.size shouldBe 3
        testUtil.createTopic("my_topic")
        val (_, rowQueue) = testUtil.run("my_topic", new ByteArrayDeserializer, new TableDeserializer)
        val totalMessageCount = 100
        doClose(new TableProducer[Array[Byte]](testUtil.properties, new ByteArraySerializer)) {
          producer => {
            var count: Int = totalMessageCount
            while (count > 0) {
              producer.send(new ProducerRecord[Array[Byte], Table]("my_topic", ByteUtil.toBytes("key"), table))
              count -= 1
            }
          }
        }
        testUtil.await(() => rowQueue.size() == totalMessageCount, 1 minute)
        rowQueue.forEach((t: Table) => {
          t.id shouldBe "my_table"
          t.rowCount shouldBe 1
          val r = t.iterator.next()
          r.cellCount shouldBe table.iterator.next().cellCount
          r.seekCell(0).name shouldBe "cf0"
          r.seekCell(0).value shouldBe 0
          r.seekCell(1).name shouldBe "cf1"
          r.seekCell(1).value shouldBe 1
        })
      }
    }
  }
}
