package com.island.ohara.configurator.store

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, TimeUnit}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.CloseOnce
import com.island.ohara.integration.With3Brokers
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random

class TestTopicStoreAcid extends With3Brokers with Matchers {
  private[this] val topicName = "TestTopicStoreAcid"
  private[this] val store =
    Store.builder().brokers(testUtil.brokersConnProps).topicName(topicName).build(Serializer.STRING, Serializer.STRING)

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
    * And then many readers run for reading and checking the data. Each change to Store is a pair from random and identical key-value.
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
    CloseOnce.close(store)
  }

  import scala.concurrent.duration._
  private[this] def createRemover(closed: AtomicBoolean, store: Store[String, String]): Future[Long] = {
    Future[Long] {
      var count = 0L
      while (!closed.get()) {
        val iter = store.iterator
        var done = false
        while (!done && iter.hasNext) {
          val (key, _) = iter.next()
          if (needDelete()) {
            count += 1
            Await.result(store.remove(key, Consistency.STRICT), 30 seconds)
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
        Await.result(store.update(count.toString, count.toString, Consistency.STRICT), 30 seconds)
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
