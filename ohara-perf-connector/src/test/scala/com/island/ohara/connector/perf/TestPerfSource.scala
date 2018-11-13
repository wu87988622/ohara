package com.island.ohara.connector.perf
import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.common.data.{Cell, DataType, Serializer}
import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.kafka.Consumer
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestPerfSource extends With3Brokers3Workers with Matchers {

  private[this] val props = PerfSourceProps(
    batch = 5,
    freq = 5 seconds
  )

  private[this] val schema: Seq[Column] = Seq(
    Column("a", DataType.STRING, 1),
    Column("b", DataType.SHORT, 2),
    Column("c", DataType.INT, 3),
    Column("d", DataType.LONG, 4),
    Column("e", DataType.FLOAT, 5),
    Column("f", DataType.DOUBLE, 6),
    Column("g", DataType.BOOLEAN, 7),
    Column("h", DataType.BYTE, 8),
    Column("i", DataType.BYTES, 9)
  )

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[PerfSource])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      .schema(schema)
      .configs(props.toMap)
      .create()

    try {
      PerfUtil.checkConnector(testUtil, connectorName)
      val consumer =
        Consumer
          .builder()
          .brokers(testUtil.brokersConnProps)
          .offsetFromBegin()
          .topicName(topicName)
          .build(Serializer.BYTES, Serializer.ROW)
      try {
        def matchType(lhs: Class[_], dataType: DataType): Unit = {
          dataType match {
            case DataType.STRING  => lhs shouldBe classOf[String]
            case DataType.SHORT   => lhs shouldBe classOf[java.lang.Short]
            case DataType.INT     => lhs shouldBe classOf[java.lang.Integer]
            case DataType.LONG    => lhs shouldBe classOf[java.lang.Long]
            case DataType.FLOAT   => lhs shouldBe classOf[java.lang.Float]
            case DataType.DOUBLE  => lhs shouldBe classOf[java.lang.Double]
            case DataType.BOOLEAN => lhs shouldBe classOf[java.lang.Boolean]
            case DataType.BYTE    => lhs shouldBe classOf[java.lang.Byte]
            case DataType.BYTES   => lhs shouldBe classOf[Array[Byte]]
            case _                => throw new IllegalArgumentException("unsupported type in testing TestPerfSource")
          }
        }
        // it is hard to evaluate number from records in topics so we just fetch some records here.
        val records = consumer.poll(props.freq * 3, props.batch)
        records.size >= props.batch shouldBe true
        records
          .map(_.value.get)
          .foreach(row => {
            row.size shouldBe schema.size
            schema.foreach(c => {
              val cell: Cell[_] = row.cell(c.order - 1)
              cell.name shouldBe c.name
              matchType(cell.value.getClass, c.dataType)
            })

          })
      } finally consumer.close()
    } finally testUtil.connectorClient.delete(connectorName)
  }
}
