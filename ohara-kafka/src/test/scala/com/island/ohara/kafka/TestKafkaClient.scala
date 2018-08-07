package com.island.ohara.kafka

import com.island.ohara.configurator.kafka.KafkaClient
import com.island.ohara.integration.With3Blockers3Workers
import com.island.ohara.io.CloseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers
class TestKafkaClient extends With3Blockers3Workers with Matchers {
  import scala.concurrent.duration._
  private[this] val timeout = 10 seconds

  private[this] val client = KafkaClient(testUtil.brokersString)

  @Test
  def testAddPartitions(): Unit = {
    val topicName = methodName
    KafkaUtil.createTopic(testUtil.brokersString, topicName, 1, 1)
    KafkaUtil.topicInfo(testUtil.brokersString, topicName, timeout).get.numberOfPartitions shouldBe 1

    KafkaUtil.addPartitions(testUtil.brokersString, topicName, 2, timeout)
    KafkaUtil.topicInfo(testUtil.brokersString, topicName, timeout).get.numberOfPartitions shouldBe 2

    // decrease the number
    an[IllegalArgumentException] should be thrownBy KafkaUtil
      .addPartitions(testUtil.brokersString, topicName, 1, timeout)
    // alter an nonexistent topic
    an[IllegalArgumentException] should be thrownBy KafkaUtil.addPartitions(testUtil.brokersString, "Xxx", 2, timeout)
  }

  @Test
  def testCreate(): Unit = {
    val topicName = methodName
    val numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    client.topicCreator
      .topicName(topicName)
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      .create()

    val topicInfo = client.topicInfo(topicName).get
    topicInfo.name shouldBe topicName
    topicInfo.numberOfPartitions shouldBe numberOfPartitions
    topicInfo.numberOfReplications shouldBe numberOfReplications

    client.deleteTopic(topicName)
    client.exist(topicName) shouldBe false
    client.topicInfo(topicName).isEmpty shouldBe true
  }

  @After
  def cleanup(): Unit = {
    CloseOnce.close(client)
  }
}
