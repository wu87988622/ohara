package com.island.ohara.configurator.kafka

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.rule.MediumTest
import com.island.ohara.io.CloseOnce._
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestKafkaClient extends MediumTest with Matchers {
  private[this] val util = OharaTestUtil.localBrokers(3)

  @Test
  def testCreate(): Unit = {
    val topicName = "testCreate"
    val numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    doClose(KafkaClient(util.brokersString)) { client =>
      {
        client.topicCreator
          .topicName(topicName)
          .numberOfPartitions(numberOfPartitions)
          .numberOfReplications(numberOfReplications)
          .create()

        val topicInof = client.topicInfo(topicName).get
        topicInof.name shouldBe topicName
        topicInof.numberOfPartitions shouldBe numberOfPartitions
        topicInof.numberOfReplications shouldBe numberOfReplications
      }
    }

  }

  @After
  def tearDown(): Unit = {
    util.close()
  }
}
