package com.island.ohara.kafka

import com.island.ohara.common.util.CloseOnce
import com.island.ohara.integration.With3Brokers
import org.apache.kafka.common.config.TopicConfig
import org.junit.{After, Test}
import org.scalatest.Matchers
class TestKafkaClient extends With3Brokers with Matchers {
  import scala.concurrent.duration._
  private[this] val timeout = 10 seconds

  private[this] val client = KafkaClient(testUtil.brokersConnProps)

  @Test
  def testAddPartitions(): Unit = {
    val topicName = methodName
    client.topicCreator().numberOfPartitions(1).numberOfReplications(1).create(topicName)
    client.topicDescription(topicName).numberOfPartitions shouldBe 1

    client.addPartitions(topicName, 2)
    client.topicDescription(topicName).numberOfPartitions shouldBe 2

    // decrease the number
    an[IllegalArgumentException] should be thrownBy client.addPartitions(topicName, 1)
    // alter an nonexistent topic
    an[IllegalArgumentException] should be thrownBy client.addPartitions("Xxx", 2, timeout)
  }

  @Test
  def testCreate(): Unit = {
    val topicName = methodName
    val numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    client
      .topicCreator()
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      .create(topicName)

    val topicInfo = client.topicDescription(topicName)
    topicInfo.name shouldBe topicName
    topicInfo.numberOfPartitions shouldBe numberOfPartitions
    topicInfo.numberOfReplications shouldBe numberOfReplications

    client.deleteTopic(topicName)
    client.exist(topicName) shouldBe false
  }

  @Test
  def testTopicOptions(): Unit = {
    val topicName = methodName
    val numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    val options = Map(
      TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_DELETE
    )
    client
      .topicCreator()
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      .options(options)
      .create(topicName)

    val desc = client.topicDescription(topicName)
    desc.name shouldBe topicName
    desc.numberOfPartitions shouldBe numberOfPartitions
    desc.numberOfReplications shouldBe numberOfReplications
    desc.options
      .filter(_.key == TopicConfig.CLEANUP_POLICY_CONFIG)
      .head
      .value shouldBe TopicConfig.CLEANUP_POLICY_DELETE
  }

  @After
  def cleanup(): Unit = {
    CloseOnce.close(client)
  }
}
