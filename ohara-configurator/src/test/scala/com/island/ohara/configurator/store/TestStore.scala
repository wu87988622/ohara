package com.island.ohara.configurator.store

import java.util
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import com.island.ohara.config.OharaConfig
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.rule.LargeTest
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.Random

/**
  * @param config configuration for Store implementation
  */
@RunWith(value = classOf[Parameterized])
class TestStore(var config: OharaConfig) extends LargeTest with Matchers {

  val testUtil = createOharaTestUtil()
  val store = Store[String, String](config)
  val elapsedTime = 30 // second
  val readerCount = 10
  val updaterCount = 10
  val removerCount = 10

  def takeBreak() = TimeUnit.MILLISECONDS.sleep(300)

  def needDelete(): Boolean = Random.nextBoolean()

  @Test
  def testUpdate() = {
    clearStore()
    0 until 10 foreach (index => store.update(index.toString, index.toString) shouldBe None)
    0 until 10 foreach (index => store.update(index.toString, index.toString) shouldBe Some(index.toString))
    0 until 10 foreach (index => store.get(index.toString) shouldBe Some(index.toString))
  }

  @Test
  def testRemove(): Unit = {
    0 until 10 foreach (index => store.update(index.toString, index.toString))

    val removed = new ArrayBuffer[String]
    for (index <- 0 until 10) {
      store.get(index.toString) shouldBe Some(index.toString)
      if (index % 2 == 0) {
        store.remove(index.toString)
        removed += index.toString
      }
    }
    removed.foreach(store.get(_) shouldBe None)

    // remove all elements
    clearStore()
  }

  @Test
  def testIterable(): Unit = {
    0 until 10 foreach (index => store.update(index.toString, index.toString))
    store.size shouldBe 10
    store.foreach {
      case (k, v) => k shouldBe v
    }
  }

  /**
    * The write and read ops shouldn't make the data inconsistent. This test will create many threads to change the data stored in Store.
    * And then many readers run for reading and checking the data. Each change to Store is a pair of random and identical key-value.
    * Hence, this test fails if reader find a pair having different key or value.
    */
  @Test
  def testAcid() = {
    val closed = new AtomicBoolean(false)
    val updaters = Seq.fill(readerCount)(createUpdater(closed, store))
    val readers = Seq.fill(updaterCount)(createReader(closed, store))
    val removers = Seq.fill(removerCount)(createRemover(closed, store))
    TimeUnit.SECONDS.sleep(elapsedTime)
    closed.set(true)
    import scala.concurrent.duration._
    def checkResult(futures: Seq[Future[Long]]) = futures.foreach(Await.result(_, 5 second))

    checkResult(updaters)
    checkResult(readers)
    checkResult(removers)
  }

  @After
  def tearDown(): Unit = {
    close(store)
    close(testUtil)
  }

  private[this] def createOharaTestUtil() = if (config.requireBoolean(NEED_OHARA_UTIL)) {
    val util = new OharaTestUtil(3, 3)
    config = config.merge(util.config)
    util
  } else null

  private[this] def createRemover(closed: AtomicBoolean, store: Store[String, String]): Future[Long] = {
    Future[Long] {
      var count = 0L
      while (!closed.get()) {
        val iter = store.iterator
        var done = false
        while (!done && iter.hasNext) {
          val (key, _) = iter.next()
          if (needDelete) {
            count += 1
            store.remove(key)
            done = true
          }
        }
        takeBreak()
      }
      count
    }
  }

  private[this] def createUpdater(closed: AtomicBoolean, store: Store[String, String]): Future[Long] = {
    Future[Long] {
      var count = 0L
      while (!closed.get()) {
        store.update(count.toString, count.toString)
        count += 1
        takeBreak()
      }
      count
    }
  }

  private[this] def createReader(closed: AtomicBoolean, store: Store[String, String]): Future[Long] = {
    Future[Long] {
      var count = 0L
      while (!closed.get()) {
        store.foreach {
          case (k, v) => count += 1; k shouldBe v
        }
        takeBreak()
      }
      count
    }
  }

  private[this] def clearStore() = {
    if (store != null) {
      store.foreach {
        case (key, _) => store.remove(key)
      }

      store.size shouldBe 0
    }
  }
}

object TestStore {

  @Parameters
  def parameters: util.Collection[Array[OharaConfig]] = {
    util.Arrays.asList(Array(configForMemStore), Array(configForTopicStore))
  }

  private[this] def configForMemStore: OharaConfig = {
    val config = OharaConfig()
    config.set(Store.STORE_IMPL, classOf[MemStore[_, _]].getName)
    config.set(Store.KEY_SERIALIZER_IMPL, classOf[StringSerializer].getName)
    config.set(Store.VALUE_SERIALIZER_IMPL, classOf[StringSerializer].getName)
    config.set(NEED_OHARA_UTIL, false.toString)
    config
  }

  private[this] def configForTopicStore: OharaConfig = {
    val config = OharaConfig()
    config.set(Store.STORE_IMPL, classOf[TopicStore[_, _]].getName)
    config.set(Store.KEY_SERIALIZER_IMPL, classOf[StringSerializer].getName)
    config.set(Store.VALUE_SERIALIZER_IMPL, classOf[StringSerializer].getName)
    config.set(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    config.set(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    config.set(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    config.set(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    TopicStore.TOPIC_NAME.set(config, "testacid")
    TopicStore.TOPIC_PARTITION_COUNT.set(config, 1)
    TopicStore.TOPIC_REPLICATION_COUNT.set(config, 1)
    config.set(NEED_OHARA_UTIL, true.toString)
    config
  }
}
