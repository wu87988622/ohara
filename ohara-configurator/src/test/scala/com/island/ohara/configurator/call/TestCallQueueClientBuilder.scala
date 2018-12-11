package com.island.ohara.configurator.call

import com.island.ohara.common.rule.MediumTest
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.kafka.KafkaUtil
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestCallQueueClientBuilder extends MediumTest with Matchers {
  private[this] val testUtil = OharaTestUtil.broker()

  @Test
  def testIncompleteArguments(): Unit = {
    var builder = CallQueueClient.builder()
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.pollTimeout(1 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.requestTopic(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.responseTopic(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.brokers(testUtil.brokersConnProps)
    // we haven't created the topic
    an[IllegalArgumentException] should be thrownBy builder.build()
    KafkaUtil.createTopic(testUtil.brokersConnProps, methodName, 1, 1)
    builder.build().close()
  }

  @After
  def tearDown(): Unit = {
    testUtil.close()
  }
}
