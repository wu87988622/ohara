package com.island.ohara.kafka

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.rule.MediumTest
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestKafkaUtil extends MediumTest with Matchers {
  private[this] val util = OharaTestUtil.localBrokers(3)

  import scala.concurrent.duration._
  private[this] val timeout = 10 seconds

  @Test
  def testAddPartitions(): Unit = {
    val topicName = methodName
    KafkaUtil.createTopic(util.brokersString, topicName, 1, 1)
    KafkaUtil.topicInfo(util.brokersString, topicName, timeout).get.numberOfPartitions shouldBe 1

    KafkaUtil.addPartitions(util.brokersString, topicName, 2, timeout)
    KafkaUtil.topicInfo(util.brokersString, topicName, timeout).get.numberOfPartitions shouldBe 2

    // decrease the number
    an[IllegalArgumentException] should be thrownBy KafkaUtil.addPartitions(util.brokersString, topicName, 1, timeout)
    // alter an nonexistent topic
    an[IllegalArgumentException] should be thrownBy KafkaUtil.addPartitions(util.brokersString, "Xxx", 2, timeout)
  }

  @After
  def tearDown(): Unit = {
    util.close()
  }
}
