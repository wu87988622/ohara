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

import java.util.concurrent.{BlockingQueue, ExecutorService, Executors}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.island.ohara.common.data.Row
import com.island.ohara.common.util.Releasable
import com.island.ohara.shabondi.{BasicShabondiTest, KafkaSupport}
import org.junit.{After, Test}

import scala.compat.java8.DurationConverters
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

final class TestRowQueueProducer extends BasicShabondiTest {
  // Use ThreadFactoryBuilder to set the "thread name"
  val threadPool: ExecutorService =
    Executors.newFixedThreadPool(4, new ThreadFactoryBuilder().setNameFormat("test-pool-%d").build())
  implicit val ec = ExecutionContext.fromExecutorService(threadPool)

  @After
  override def tearDown(): Unit = {
    super.tearDown()
    ec.shutdown()
  }

  private def createRowQueueProducer(topicNames: Seq[String]): RowQueueProducer =
    new RowQueueProducer(brokerProps, topicNames, DurationConverters.toJava(1 seconds))

  @Test
  def testFetchAllRows(): Unit = {
    val topicKey1 = createTopicKey
    val rowCount  = 999
    KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount)

    val rowQueueProducer          = createRowQueueProducer(Seq(topicKey1.name))
    val queue: BlockingQueue[Row] = rowQueueProducer.queue
    try {
      threadPool.execute(rowQueueProducer)

      val countFuture = countRows(queue, 10 * 1000)

      Await.result(countFuture, 30 seconds) should ===(rowCount)
    } finally {
      Releasable.close(rowQueueProducer)
    }
  }

  @Test
  def testPauseAndResume(): Unit = {
    val topicKey1 = createTopicKey

    val rowQueueProducer          = createRowQueueProducer(Seq(topicKey1.name))
    val queue: BlockingQueue[Row] = rowQueueProducer.queue
    try {
      threadPool.execute(rowQueueProducer)

      log.info("[[[ phase 1 ]]] prepareBulkOfRow...")
      val rowCount           = 499
      val countExecutionTime = 5 * 1000
      KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount)

      val countFuture = countRows(queue, countExecutionTime)
      Await.result(countFuture, 30 seconds) should ===(rowCount)

      log.info("[[[ phase 2 ]]] pause producer")
      rowQueueProducer.pause()
      Thread.sleep(countExecutionTime) // make sure the rowQueueProducer's polling already finished

      KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount)

      val count1 = countRows(queue, countExecutionTime)
      Await.result(count1, Duration.Inf) should ===(0)

      log.info("[[[ phase 3 ]]] resume producer")
      rowQueueProducer.resume()

      val count2 = countRows(queue, countExecutionTime)
      Await.result(count2, Duration.Inf) should ===(rowCount)
    } finally {
      Releasable.close(rowQueueProducer)
    }
  }

  /**
    * Continue fetch and count the rows until execution time's up
    */
  private def countRows(queue: BlockingQueue[Row], executionTime: Long): Future[Long] =
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
    }
}
