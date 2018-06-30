package com.island.ohara.configurator.store

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, TimeUnit}

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.rule.MediumTest
import com.island.ohara.serialization.StringSerializer
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random

class TestTopicStoreAcid extends MediumTest with Matchers {
  val testUtil = OharaTestUtil.localBrokers(3)
  val store = Store
    .builder(StringSerializer, StringSerializer)
    .brokers(testUtil.brokersString)
    .topicName("TestTopicStoreAcid")
    .build()
  val elapsedTime = 30 // second
  val readerCount = 5
  val updaterCount = 5
  val removerCount = 5
  // use custom executor in order to make sure all threads can run parallel
  implicit val executor =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(readerCount + updaterCount + removerCount))
  def takeBreak() = TimeUnit.MILLISECONDS.sleep(300)

  def needDelete(): Boolean = Random.nextBoolean()

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
}
