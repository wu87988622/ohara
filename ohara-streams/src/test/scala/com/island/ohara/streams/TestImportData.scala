package com.island.ohara.streams

import com.island.ohara.data.Row
import com.island.ohara.integration.With3Brokers
import com.island.ohara.kafka.{Consumer, ConsumerRecord}
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._

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
        .build
      try {
        val messages: Seq[ConsumerRecord[Array[Byte], Row]] = consumer.poll(100 millisecond, 1)
        messages.size should be > 0
      } finally {
        consumer.close()
      }
    }
  }

}
