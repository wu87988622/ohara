package com.island.ohara.configurator.store

import com.island.ohara.integration.{OharaTestUtil, With3Brokers}
import com.island.ohara.client.util.CloseOnce.close
import com.island.ohara.common.data.Serializer
import org.junit._
import org.scalatest.Matchers

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

class TestTopicStore extends With3Brokers with Matchers {
  private[this] val topicName = random()
  private[this] val store: BlockingStore[String, String] =
    Store
      .builder()
      .brokers(testUtil.brokersConnProps)
      .topicName(topicName)
      .buildBlocking(Serializer.STRING, Serializer.STRING)
  @Test
  def testRestart(): Unit = {
    store._update("aa", "bb", Consistency.STRICT) shouldBe None
    store._get("aa") shouldBe Some("bb")
    store.close()
    val another =
      Store
        .builder()
        .brokers(testUtil.brokersConnProps)
        .topicName(topicName)
        .buildBlocking(Serializer.STRING, Serializer.STRING)
    try {
      OharaTestUtil.await(() => another._get("aa").isDefined, 10 seconds)
      another._get("aa") shouldBe Some("bb")
    } finally another.close()

  }

  /**
    * In this test we create extra 10 stores to test the data synchronization. All from them are based on the same kafka topic so any change
    * to one from them should be synced to other stores.
    */
  @Test
  def testMultiStore(): Unit = {
    val numberOfStore = 5
    val stores = 0 until numberOfStore map (_ =>
      Store
        .builder()
        .brokers(testUtil.brokersConnProps)
        .topicName(topicName)
        .buildBlocking(Serializer.STRING, Serializer.STRING))
    0 until 10 foreach (index => store._update(index.toString, index.toString, Consistency.STRICT))
    store.size shouldBe 10

    // make sure all stores have synced the updated data
    OharaTestUtil.await(() => stores.count(_.size == 10) == numberOfStore, 30 second)

    stores.foreach(s => {
      0 until 10 foreach (index => s._get(index.toString) shouldBe Some(index.toString))
    })

    // remove all data
    val randomStore = stores.iterator.next()
    0 until 10 foreach (index => randomStore._remove(index.toString, Consistency.STRICT) shouldBe Some(index.toString))

    // make sure all stores have synced the updated data
    OharaTestUtil.await(() => stores.count(_.isEmpty) == numberOfStore, 30 second)

    // This store is based on another topic so it should have no data
    val anotherStore =
      Store
        .builder()
        .brokers(testUtil.brokersConnProps)
        .topicName(topicName + "copy")
        .build(Serializer.STRING, Serializer.STRING)
    anotherStore.size shouldBe 0
  }

  @Test
  def testUpdate(): Unit = {
    0 until 10 foreach (index => store._update(index.toString, index.toString, Consistency.STRICT) shouldBe None)
    0 until 10 foreach (index =>
      store._update(index.toString, index.toString, Consistency.STRICT) shouldBe Some(index.toString))
    0 until 10 foreach (index => store._get(index.toString) shouldBe Some(index.toString))
  }

  @Test
  def testRemove(): Unit = {
    0 until 10 foreach (index => store._update(index.toString, index.toString, Consistency.STRICT))

    val removed = new ArrayBuffer[String]
    for (index <- 0 until 10) {
      store._get(index.toString) shouldBe Some(index.toString)
      if (index % 2 == 0) {
        store._remove(index.toString, Consistency.STRICT)
        removed += index.toString
      }
    }
    removed.foreach(store._get(_) shouldBe None)
  }

  @Test
  def testIterable(): Unit = {
    0 until 10 foreach (index => store._update(index.toString, index.toString, Consistency.STRICT))
    store.size shouldBe 10
    store.foreach {
      case (k, v) => k shouldBe v
    }
  }

  @After
  def tearDown(): Unit = {
    close(store)
  }
}
