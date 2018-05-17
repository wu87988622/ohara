package com.island.ohara.configurator.store

import java.util
import java.util.concurrent.TimeUnit

import com.island.ohara.config.OharaConfig
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce.{close, _}
import com.island.ohara.rule.LargeTest
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestTopicOStore extends LargeTest with Matchers {

  val config = configForTopicOStore
  val testUtil = createOharaTestUtil()
  var store = new TopicOStore[String, String](config)

  @Test
  def testRestart(): Unit = {
    store.update("aa", "bb") shouldBe None
    store.close()
    store = new TopicOStore[String, String](config)
    store.get("aa") shouldBe Some("bb")
  }

  @Test
  def testRetention(): Unit = {
    val config = OharaConfig(this.config)
    config.set(TopicOStore.TOPIC_NAME, "testacid2")
    // make small retention so as to trigger log clear
    config.set("log.retention.ms", 1000)
    doClose(new TopicOStore[String, String](config)) { anotherStore =>
      {
        0 until 10 foreach (index => anotherStore.update("key", index.toString))
        // the local cache do the de-duplicate
        anotherStore.size shouldBe 1
      }
    }
    // wait for the log clear
    TimeUnit.SECONDS.sleep(3)
    config.set(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    config.set("log.cleanup.policy", "compact")
    config.set(ConsumerConfig.GROUP_ID_CONFIG, "testRetention")
    val consumer = new KafkaConsumer[String, String](config.toProperties)
    consumer.subscribe(util.Arrays.asList("testacid2"))
    var record: ConsumerRecords[String, String] = null
    var count = 0
    do {
      record = consumer.poll(1000)
      if (record != null) count += 1
    } while (record == null)
    count shouldBe 1
  }

  /**
    * In this test we create extra 10 stores to test the data synchronization. All of them are based on the same kafka topic so any change
    * to one of them should be synced to other stores.
    */
  @Test
  def testMultiStore(): Unit = {
    val numberOfStore = 5
    val stores = 0 until numberOfStore map (_ => new TopicOStore[String, String](config))
    0 until 10 foreach (index => store.update(index.toString, index.toString))

    // make sure all stores have synced the updated data
    testUtil.await(() => stores.filter(_.size == 10).size == numberOfStore, 30 second)

    stores.foreach(s => {
      0 until 10 foreach (index => s.get(index.toString) shouldBe Some(index.toString))
    })

    // remove all data
    val randomStore = stores.iterator.next()
    0 until 10 foreach (index => randomStore.remove(index.toString) shouldBe Some(index.toString))

    // make sure all stores have synced the updated data
    testUtil.await(() => stores.filter(_.isEmpty).size == numberOfStore, 30 second)

    // This store is based on another topic so it should have no data
    val anotherConfig = config.snapshot
    anotherConfig.set(TopicOStore.TOPIC_NAME, "testacidXX")
    val anotherStore = new TopicOStore[String, String](anotherConfig)
    anotherStore.size shouldBe 0
  }

  @After
  def tearDown(): Unit = {
    close(store)
    close(testUtil)
  }

  private[this] def createOharaTestUtil() = {
    val util = new OharaTestUtil(3, 3)
    config.load(util.properties)
    util
  }

  private[this] def configForTopicOStore: OharaConfig = {
    val config = OharaConfig()
    config.set(OStore.OSTORE_IMPL, classOf[TopicOStore[_, _]].getName)
    config.set(OStore.COMPARATOR_IMPL, classOf[StringComparator].getName)
    config.set(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    config.set(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    config.set(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    config.set(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    config.set(TopicOStore.TOPIC_NAME, "testacid")
    config.set(TopicOStore.TOPIC_PARTITION_COUNT.key, 1.toString)
    config.set(TopicOStore.TOPIC_REPLICATION_COUNT.key, 1.toString)
    config.set(NEED_OHARA_UTIL, true.toString)
    config
  }
}
