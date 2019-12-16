/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.shabondi.sink

import java.util
import java.util.concurrent.{ExecutorService, Executors}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.island.ohara.common.data.Row
import com.island.ohara.common.util.Releasable
import com.island.ohara.shabondi.{BasicShabondiTest, KafkaSupport}
import org.junit.{After, Test}

import scala.compat.java8.DurationConverters
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

final class TestDataGroups extends BasicShabondiTest {
  private val newThreadPool: () => ExecutorService = () =>
    Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("testSinkRoute-%d").build())

  private val countRows: (util.Queue[Row], Long, ExecutionContext) => Future[Long] =
    (queue, executionTime, ec) =>
      Future {
        log.debug("countRows begin...")
        val baseTime = System.currentTimeMillis()
        var count    = 0L
        var running  = true
        while (running) {
          val row = queue.poll()
          if (row != null) count += 1 else Thread.sleep(100)
          running = (System.currentTimeMillis() - baseTime) < executionTime
        }
        log.debug("countRows done")
        count
      }(ec)

  @After
  override def tearDown(): Unit = {
    super.tearDown()
  }

  @Test
  def testDefaultGroup(): Unit = {
    val threadPool: ExecutorService = newThreadPool()
    implicit val ec                 = ExecutionContext.fromExecutorService(threadPool)
    val topicKey1                   = createTopicKey
    val rowCount                    = 999
    val dataGroups                  = new SinkDataGroups(brokerProps, Seq(topicKey1.name), DurationConverters.toJava(10 seconds))
    try {
      KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount)

      val queue  = dataGroups.defaultGroup.queue
      val queue1 = dataGroups.defaultGroup.queue
      queue should ===(queue1)
      dataGroups.size should ===(1)

      val rows = countRows(queue, 10 * 1000, ec)

      Await.result(rows, 30 seconds) should ===(rowCount)
    } finally {
      Releasable.close(dataGroups)
      threadPool.shutdown()
    }
  }

  @Test
  def testMultipleGroup(): Unit = {
    val threadPool: ExecutorService = newThreadPool()
    implicit val ec                 = ExecutionContext.fromExecutorService(threadPool)
    val topicKey1                   = createTopicKey
    val rowCount                    = 999
    val dataGroups                  = new SinkDataGroups(brokerProps, Seq(topicKey1.name), DurationConverters.toJava(10 seconds))
    try {
      KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount)

      val queue  = dataGroups.defaultGroup.queue
      val queue1 = dataGroups.createIfAbsent("group1").queue
      val queue2 = dataGroups.createIfAbsent("group1").queue
      queue1 should ===(queue2)
      dataGroups.size should ===(2)

      val rows  = countRows(queue, 10 * 1000, ec)
      val rows1 = countRows(queue1, 10 * 1000, ec)

      Await.result(rows, 30 seconds) should ===(rowCount)
      Await.result(rows1, 30 seconds) should ===(rowCount)
    } finally {
      Releasable.close(dataGroups)
      threadPool.shutdown()
    }
  }
}
