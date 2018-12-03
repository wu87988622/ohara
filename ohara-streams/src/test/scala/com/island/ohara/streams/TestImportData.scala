package com.island.ohara.streams

import java.util

import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.integration.With3Brokers
import com.island.ohara.kafka.{Consumer, ConsumerRecord}
import org.junit.Test
import org.scalatest.Matchers

class TestImportData extends With3Brokers with Matchers {

  val TOPIC_CARRIERS = "carriers"
  val TOPIC_PLANE = "plane"
  val TOPIC_AIRPORT = "airport"
  val TOPIC_FLIGHT = "flight"

  @Test
  def testImportAirlineData(): Unit = {

    val bootstrapServers = testUtil.brokersConnProps
    AirlineImporter.importData(bootstrapServers, false)

    Seq(AirlineImporter.TOPIC_AIRPORT,
        AirlineImporter.TOPIC_CARRIERS,
        AirlineImporter.TOPIC_PLANE,
        AirlineImporter.TOPIC_FLIGHT).foreach { topicName =>
      logger.info(s"===== $topicName =====")
      val consumer: Consumer[Array[Byte], Row] = Consumer
        .builder()
        .brokers(bootstrapServers)
        .offsetFromBegin()
        .topicName(topicName)
        .groupId("import-airline")
        .build(Serializer.BYTES, Serializer.ROW)
      try {

        val messages: util.List[ConsumerRecord[Array[Byte], Row]] = consumer.poll(java.time.Duration.ofMillis(100), 1)
        messages.size should be > 0
      } finally {
        consumer.close()
      }
    }
  }

}
