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
  private[this] val currentUuid = new AtomicInteger(0)
  private[this] val uuidGenerator = () => currentUuid.incrementAndGet().toString
  private[this] val configurator =
    Configurator.builder
      .uuidGenerator(uuidGenerator)
      .hostname("localhost")
      .port(0)
      .store(
        Store
          .builder(StringSerializer, OharaDataSerializer)
          .numberOfPartitions(1)
          .numberOfReplications(1)
          .topicName("TestConfiguratorWithMiniCluster")
          .brokers(util.brokersString)
          .build())
      .kafkaClient(KafkaClient(util.brokersString))
      .build()
  private[this] val hostName = configurator.hostname
  private[this] val port = configurator.port
  private[this] val restClient = RestClient()
  private[this] val path = s"${Configurator.VERSION}/${Configurator.TOPIC_PATH}"

  import scala.concurrent.duration._
  private[this] val timeout = 10 seconds

  @Test
  def testAddTopic(): Unit = {
    var topicName = methodName
    var numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    var oharaTopic = OharaTopic.json(topicName, numberOfPartitions, numberOfReplications)
    var rsp = restClient.post(hostName, port, path, oharaTopic)
    rsp.statusCode shouldBe 200
    val uuid = OharaConfig(OharaJson(rsp.body)).requireString("uuid")
    uuid shouldBe currentUuid.get().toString

    // check the information stored in kafka
    var topicInfo = KafkaUtil.topicInfo(util.brokersString, uuid, timeout).get
    topicInfo.name shouldBe uuid
    topicInfo.partitions shouldBe numberOfPartitions
    topicInfo.replications shouldBe numberOfReplications

    // check the information stored in ohara
    var storedTopic: OharaTopic = configurator.store.iterator.next()._2.asInstanceOf[OharaTopic]
    storedTopic.uuid shouldBe uuid
    storedTopic.name shouldBe topicName
    storedTopic.numberOfPartitions shouldBe numberOfPartitions
    storedTopic.numberOfReplications shouldBe numberOfReplications

    numberOfPartitions = 3
    topicName = s"${methodName}123"
    oharaTopic = OharaTopic.json(topicName, numberOfPartitions, numberOfReplications)
    rsp = restClient.put(hostName, port, s"$path/$uuid", oharaTopic)
    rsp.statusCode shouldBe 200
    OharaConfig(OharaJson(rsp.body)).requireString("uuid") shouldBe currentUuid.get().toString

    // check the information stored in kafka
    topicInfo = KafkaUtil.topicInfo(util.brokersString, uuid, timeout).get

    topicInfo.name shouldBe uuid
    topicInfo.partitions shouldBe numberOfPartitions
    topicInfo.replications shouldBe numberOfReplications

    // check the information stored in ohara
    storedTopic = configurator.store.iterator.next()._2.asInstanceOf[OharaTopic]
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
