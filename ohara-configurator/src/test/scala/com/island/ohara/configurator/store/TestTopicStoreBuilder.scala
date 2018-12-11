package com.island.ohara.configurator.store

import com.island.ohara.common.data.Serializer
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.kafka.KafkaUtil
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestTopicStoreBuilder extends MediumTest with Matchers {

  private[this] val testUtil = OharaTestUtil.broker()

  @Test
  def testIncompleteArguments(): Unit = {
    val topicName = methodName
    KafkaUtil.createTopic(testUtil.brokersConnProps, topicName, 1, 1)
    var builder = Store.builder()
    an[NoSuchElementException] should be thrownBy builder.build(Serializer.STRING, Serializer.STRING)
    builder = builder.pollTimeout(1 seconds)
    an[NoSuchElementException] should be thrownBy builder.build(Serializer.STRING, Serializer.STRING)
    builder = builder.topicName(topicName)
    an[NoSuchElementException] should be thrownBy builder.build(Serializer.STRING, Serializer.STRING)
    builder = builder.brokers(testUtil.brokersConnProps)
    builder.build(Serializer.STRING, Serializer.STRING).close()
  }

  @After
  def tearDown(): Unit = {
    testUtil.close()
  }
}
