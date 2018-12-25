package com.island.ohara.configurator
import com.island.ohara.common.data.Serializer
import com.island.ohara.integration.WithBrokerWorker
import com.island.ohara.kafka.KafkaUtil
import org.junit.Test
import org.scalatest.Matchers

class TestOhara773 extends WithBrokerWorker with Matchers {

  @Test
  def shouldNotCreateTopicIfItExists(): Unit = {
    val topicName = methodName
    val store = com.island.ohara.configurator.store.Store
      .builder()
      .brokers(testUtil().brokersConnProps())
      .topicName(topicName)
      .build(Serializer.STRING, Serializer.OBJECT)
    try KafkaUtil.exist(testUtil.brokersConnProps, topicName) shouldBe true
    finally store.close()
  }
}
