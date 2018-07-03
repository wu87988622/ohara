package com.island.ohara.configurator.call

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.kafka.KafkaUtil
import com.island.ohara.rule.MediumTest
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestCallQueueClientBuilder extends MediumTest with Matchers {
  private[this] val testUtil = OharaTestUtil.localBrokers(1)

  @Test
  def testIncompleteArguments(): Unit = {
    var builder = CallQueueClient.builder
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.initializationTimeout(10 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.pollTimeout(1 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.topicName(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.brokers(testUtil.brokersString)
    // we haven't created the topic
    an[IllegalArgumentException] should be thrownBy builder.build()
    KafkaUtil.topicCreator
      .numberOfReplications(1)
      .numberOfPartitions(1)
      .topicName(methodName)
      .brokers(testUtil.brokersString)
      .create()
    builder.build().close()
  }

  @After
  def tearDown(): Unit = {
    testUtil.close()
  }
}
