package com.island.ohara.configurator

import com.island.ohara.client.ConfiguratorJson.{TopicInfo, TopicInfoRequest}
import com.island.ohara.client.{ConfiguratorClient, ConnectorClient}
import com.island.ohara.common.data.Serializer
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.WithBrokerWorker
import com.island.ohara.kafka.{KafkaClient, KafkaUtil}
import org.junit.Test
import org.scalatest.Matchers

class TestOhara786 extends WithBrokerWorker with Matchers {

  private[this] val configurator =
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .store(Store.inMemory(Serializer.STRING, Serializer.OBJECT))
      .connectClient(ConnectorClient(testUtil.workersConnProps))
      .kafkaClient(KafkaClient.of(testUtil.brokersConnProps))
      .build()

  @Test
  def deleteAnTopicRemovedFromKafka(): Unit = {
    val topicName = methodName

    val client = ConfiguratorClient(configurator.connectionProps)
    try {
      val info = client.add[TopicInfoRequest, TopicInfo](
        TopicInfoRequest(
          name = topicName,
          numberOfPartitions = 1,
          numberOfReplications = 1
        ))
      KafkaUtil.deleteTopic(testUtil.brokersConnProps, info.id)
      client.delete[TopicInfo](info.id)
    } finally client.close()
  }
}
