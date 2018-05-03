package com.island.ohara.kafka

import java.util.Properties

import com.island.ohara.core.{Cell, Row, Table}
import com.island.ohara.rule.SmallTest
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

class TestProducer extends SmallTest with Matchers with MockitoSugar {

  @Test
  def testCreateTableProducer(): Unit = {
    val producerProps = new Properties
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9000")
    new TableProducer[Array[Byte]](producerProps, new ByteArraySerializer).close()
  }

  @Test
  def testSendDataWithTable(): Unit = {
    // assume we get some data from cav file
    val csvData = readDataFromCsv
    // assume we get cf name and type from ohara configurator
    val types = readTypeAndCfFromConfig
    val producer = mock[TableProducer[Array[Byte]]]
    val table = Table
      .builder("my_table")
      .append(Row(csvData.zipWithIndex.map(_ match {
        case (data, index) =>
          types(index) match {
            case (name, "string")  => Cell.builder.name(name).build(data)
            case (name, "boolean") => Cell.builder.name(name).build(data.toBoolean)
            case (name, "int")     => Cell.builder.name(name).build(data.toInt)
            case _                 => throw new UnsupportedOperationException
          }
      })))
      .build()
    // just a mock so nothing can happen
    producer.send(new ProducerRecord[Array[Byte], Table]("topic", table))
    table.rowCount shouldBe 1
    table.cellCount shouldBe 3
    table.seekCell("cf0").next().value shouldBe "123"
    table.seekCell("cf1").next().value shouldBe true
    table.seekCell("cf2").next().value shouldBe 10
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
