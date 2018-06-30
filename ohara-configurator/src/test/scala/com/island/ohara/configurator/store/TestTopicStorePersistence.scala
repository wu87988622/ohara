package com.island.ohara.configurator.store

import java.util
import java.util.Properties

import com.island.ohara.config.UuidUtil
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.KafkaUtil
import com.island.ohara.rule.MediumTest
import com.island.ohara.serialization.StringSerializer
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecords, KafkaConsumer, OffsetResetStrategy}
import org.apache.kafka.common.config.TopicConfig
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.mutable.ArrayBuffer

class TestTopicStorePersistence extends MediumTest with Matchers {

  private[this] val testUtil: OharaTestUtil = OharaTestUtil.localBrokers(3)

  @Test
  def testRetention(): Unit = {
    val topicName = "testRetention"
    val numberOfOtherMessages = 2048
    doClose(
      Store
        .builder(StringSerializer, StringSerializer)
        .brokers(testUtil.brokersString)
        .topicName(topicName)
        // make small retention so as to trigger log clear
        .topicOptions(Map(TopicConfig.FILE_DELETE_DELAY_MS_CONFIG -> 1000.toString,
                          TopicConfig.SEGMENT_BYTES_CONFIG -> 1024.toString))
        .build()) { store =>
      {
        0 until 10 foreach (index => store.update("key", index.toString))
        // the local cache do the de-duplicate
        store.size shouldBe 1
        store.iterator.next()._2 shouldBe 9.toString

        0 until numberOfOtherMessages foreach (index => store.update(index.toString, index.toString))
      }
    }
    def checkContentOfTopic(): (Int, Int) = {
      val config = new Properties()
      config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, testUtil.brokersString)
      config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name.toLowerCase)
      config.put(ConsumerConfig.GROUP_ID_CONFIG, UuidUtil.uuid())
      doClose(
        new KafkaConsumer[String, String](config,
                                          KafkaUtil.wrapDeserializer(StringSerializer),
                                          KafkaUtil.wrapDeserializer(StringSerializer))) { consumer =>
        {
          consumer.subscribe(util.Arrays.asList(topicName))
          val messageBuffer = new ArrayBuffer[String]()
          import scala.collection.JavaConverters._
          var done = false
          while (!done) {
            val records: ConsumerRecords[String, String] = consumer.poll(5 * 1000)
            if (records == null || records.isEmpty) done = true
            else {
              records
                .records(topicName)
                .asScala
                .foreach(record => {
                  messageBuffer += record.key()
                })
            }
          }
          (messageBuffer.filter(_.equals("key")).size, messageBuffer.filterNot(_.equals("key")).size)
        }
      }
    }

    import scala.concurrent.duration._
    OharaTestUtil.await(() => {
      val result = checkContentOfTopic()
      result._1 == 1 && result._2 == numberOfOtherMessages
    }, 60 seconds)
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(testUtil)
  }
}
