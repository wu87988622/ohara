package com.island.ohara.configurator

import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.configurator.v0.TopicApi
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.integration.WithBrokerWorker
import com.island.ohara.kafka.{KafkaClient, KafkaUtil}
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestOhara786 extends WithBrokerWorker with Matchers {

  private[this] val configurator =
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .connectClient(ConnectorClient(testUtil.workersConnProps))
      .kafkaClient(KafkaClient.of(testUtil.brokersConnProps))
      .build()

  @Test
  def deleteAnTopicRemovedFromKafka(): Unit = {
    val topicName = methodName

    val topic = Await.result(TopicApi
                               .access()
                               .hostname(configurator.hostname)
                               .port(configurator.port)
                               .add(TopicCreationRequest(topicName, 1, 1)),
                             10 seconds)
    KafkaUtil.deleteTopic(testUtil.brokersConnProps, topic.id)
    // the topic is removed but we don't throw exception.
    Await.result(TopicApi.access().hostname(configurator.hostname).port(configurator.port).delete(topic.id), 10 seconds)
  }
}
