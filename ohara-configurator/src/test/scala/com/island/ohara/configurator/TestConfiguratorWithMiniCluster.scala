package com.island.ohara.configurator

import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.config.{OharaConfig, OharaJson}
import com.island.ohara.configurator.data.{OharaDataSerializer, OharaTopic}
import com.island.ohara.configurator.kafka.KafkaClient
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce
import com.island.ohara.kafka.KafkaUtil
import com.island.ohara.rest.RestClient
import com.island.ohara.rule.MediumTest
import com.island.ohara.serialization.StringSerializer
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestConfiguratorWithMiniCluster extends MediumTest with Matchers {

  private[this] val util = OharaTestUtil.localBrokers(3)
  private[this] val store = Store
    .builder(StringSerializer, OharaDataSerializer)
    .numberOfPartitions(1)
    .numberOfReplications(1)
    .topicName("TestConfiguratorWithMiniCluster")
    .brokers(util.brokersString)
    .build()
  private[this] val currentUuid = new AtomicInteger(0)
  private[this] val uuidGenerator = () => currentUuid.incrementAndGet().toString
  private[this] val configurator =
    Configurator.builder
      .uuidGenerator(uuidGenerator)
      .hostname("localhost")
      .port(0)
      .store(store)
      .kafkaClient(KafkaClient(util.brokersString))
      .build()
  private[this] val hostName = configurator.hostname
  private[this] val port = configurator.port
  private[this] val restClient = RestClient()
  private[this] val path = s"${Configurator.VERSION}/${Configurator.TOPIC_PATH}"

  @Test
  def testAddTopic(): Unit = {
    val topicName = testName.getMethodName
    val numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    val oharaTopic = OharaTopic.json(topicName, numberOfPartitions, numberOfReplications)
    val rsp = restClient.post(hostName, port, path, oharaTopic)
    rsp.statusCode shouldBe 200
    val uuid = OharaConfig(OharaJson(rsp.body)).requireString("uuid")
    uuid shouldBe currentUuid.get().toString

    // check the information stored in kafka
    val topicInfo = KafkaUtil.topicInfo(util.brokersString, uuid).get
    topicInfo.name shouldBe uuid
    topicInfo.partitions shouldBe numberOfPartitions
    topicInfo.replications shouldBe numberOfReplications

    // check the information stored in ohara
    val storedTopic = store.iterator.next()._2.asInstanceOf[OharaTopic]
    storedTopic.uuid shouldBe uuid
    storedTopic.name shouldBe topicName
    storedTopic.numberOfPartitions shouldBe numberOfPartitions
    storedTopic.numberOfReplications shouldBe numberOfReplications
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(restClient)
    CloseOnce.close(configurator)
    util.close()
  }
}
