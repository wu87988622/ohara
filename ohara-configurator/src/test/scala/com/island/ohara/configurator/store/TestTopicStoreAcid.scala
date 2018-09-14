package com.island.ohara.configurator.store

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, TimeUnit}

import com.island.ohara.integration.With3Brokers
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.KafkaClient
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random

class TestTopicStoreAcid extends With3Brokers with Matchers {
  private[this] val topicName = "TestTopicStoreAcid"
  doClose(KafkaClient(testUtil.brokers))(
    _.topicCreator().numberOfReplications(1).numberOfPartitions(1).compacted().create(topicName))

  private[this] val store =
    Store.builder().brokers(testUtil.brokers).topicName(topicName).buildBlocking[String, String]
  private[this] val elapsedTime = 30 // second
  private[this] val readerCount = 5
  private[this] val updaterCount = 5
  private[this] val removerCount = 5
  // use custom executor in order to make sure all threads can run parallel
  private[this] implicit val executor: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(readerCount + updaterCount + removerCount))
  private[this] def takeBreak(): Unit = TimeUnit.MILLISECONDS.sleep(300)

  private[this] def needDelete(): Boolean = Random.nextBoolean()

  /**
    * The write and read ops shouldn't make the data inconsistent. This test will create many threads to change the data stored in Store.
    * And then many readers run for reading and checking the data. Each change to Store is a pair of random and identical key-value.
    * Hence, this test fails if reader find a pair having different key or value.
    */
  @Test
  def testAcid(): Unit = {
    val closed = new AtomicBoolean(false)
    val updaters = Seq.fill(readerCount)(createUpdater(closed, store))
    val readers = Seq.fill(updaterCount)(createReader(closed, store))
    val removers = Seq.fill(removerCount)(createRemover(closed, store))
    TimeUnit.SECONDS.sleep(elapsedTime)
    closed.set(true)
    import scala.concurrent.duration._
    def checkResult(futures: Seq[Future[Long]]): Unit = futures.foreach(Await.result(_, 5 second))

    checkResult(updaters)
    checkResult(readers)
    checkResult(removers)
  }

  @After
  def tearDown(): Unit = {
    close(store)
  }

  private[this] def createRemover(closed: AtomicBoolean, store: BlockingStore[String, String]): Future[Long] = {
    Future[Long] {
      var count = 0L
      while (!closed.get()) {
        val iter = store.iterator
        var done = false
        while (!done && iter.hasNext) {
          val (key, _) = iter.next()
          if (needDelete()) {
            count += 1
            store._remove(key, Consistency.STRICT)
            done = true
          }
        }
        takeBreak()
      }
      count
    }
  }

  private[this] def createUpdater(closed: AtomicBoolean, store: BlockingStore[String, String]): Future[Long] = {
    Future[Long] {
      var count = 0L
      while (!closed.get()) {
        store._update(count.toString, count.toString, Consistency.STRICT)
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
}
