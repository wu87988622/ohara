package com.island.ohara.kafka

import java.util.Properties

import com.island.ohara.data.{Cell, Row}
import com.island.ohara.rule.SmallTest
import com.island.ohara.serialization.RowSerializer
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

class TestConsumerAndProducer extends SmallTest with Matchers with MockitoSugar {

  @Test
  def testCreateRowConsumer(): Unit = {
    val producerProps = new Properties
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9000")
    new RowConsumer[Array[Byte]](producerProps, new ByteArrayDeserializer).close()
  }

  @Test
  def testSerializeRow(): Unit = {
    val row = Row(Cell.builder.name("cell").build(123))
    val copy = (KafkaUtil
      .wrapDeserializer(RowSerializer))
      .deserialize("xx", (KafkaUtil.wrapSerializer(RowSerializer).serialize("topic", row)))
    copy.cellCount shouldBe row.cellCount
  }

  @Test
  def testCreateRowProducer(): Unit = {
    val producerProps = new Properties
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9000")
    new RowProducer[Array[Byte]](producerProps, new ByteArraySerializer).close()
  }

  @Test
  def testSendDataWithRow(): Unit = {
    // assume we get some data from cav file
    val csvData = readDataFromCsv
    // assume we get cf name and type from ohara configurator
    val types = readTypeAndCfFromConfig
    val producer = mock[RowProducer[Array[Byte]]]
    val row = Row(csvData.zipWithIndex.map(_ match {
      case (data, index) =>
        types(index) match {
          case (name, "string")  => Cell.builder.name(name).build(data)
          case (name, "boolean") => Cell.builder.name(name).build(data.toBoolean)
          case (name, "int")     => Cell.builder.name(name).build(data.toInt)
          case _                 => throw new UnsupportedOperationException
        }
    }))
    // just a mock so nothing can happen
    producer.send(new ProducerRecord[Array[Byte], Row]("topic", row))
    row.cellCount shouldBe 3
    row.seekCell("cf0").get.value shouldBe "123"
    row.seekCell("cf1").get.value shouldBe true
    row.seekCell("cf2").get.value shouldBe 10
  }

  private[this] def readDataFromCsv: Array[String] = Array("123", "true", "10")

  private[this] def readTypeAndCfFromConfig: Array[(String, String)] =
    Array(("cf0", "string"), ("cf1", "boolean"), ("cf2", "int"))
}
