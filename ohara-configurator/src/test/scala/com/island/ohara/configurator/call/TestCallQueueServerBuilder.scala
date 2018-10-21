package com.island.ohara.configurator.call

import com.island.ohara.integration.With3Brokers
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestCallQueueServerBuilder extends With3Brokers with Matchers {

  @Test
  def testIncompleteArguments(): Unit = {
    var builder = CallQueueServer.builder()
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.pollTimeout(1 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.responseTopic(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.requestTopic(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.brokers(testUtil.brokersConnProps)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.groupId("xxx")
    builder.build().close()
  }
}
