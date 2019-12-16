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

import java.util.concurrent.{ExecutorService, Executors}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.island.ohara.common.data.Row
import com.island.ohara.common.util.Releasable
import com.island.ohara.shabondi._
import org.junit.Test

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

final class TestSinkRoute extends BasicShabondiTest {
  import DefaultDefinitions._
  import com.island.ohara.shabondi.JsonSupport._
  import com.island.ohara.shabondi.ShabondiRouteTestSupport._

  private val newThreadPool: () => ExecutorService = () =>
    Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("testSinkRoute-%d").build())

  private def pollRowsRequest(
    webServer: WebServer,
    dataGroup: String,
    timeout: Long,
    ec: ExecutionContext
  ): Future[Seq[Row]] =
    Future {
      val resultRows = ArrayBuffer.empty[Row]
      val request = dataGroup match {
        case ""   => Get(uri = "/v0/poll")
        case name => Get(uri = s"/v0/poll/$name")
      }
      var idx: Int = 0
      val baseTime = System.currentTimeMillis()
      while ((System.currentTimeMillis() - baseTime) < timeout) {
        idx += 1
        request ~> webServer.routes ~> check {
          val result = entityAs[Seq[RowData]].map(JsonSupport.toRow)
          resultRows ++= result
        }
        Thread.sleep(100)
      }
      resultRows
    }(ec)

  @Test
  def testDefaultGroup(): Unit = {
    val threadPool: ExecutorService = newThreadPool()
    implicit val ec                 = ExecutionContext.fromExecutorService(threadPool)

    val topicKey1 = createTopicKey
    val config    = defaultTestConfig(SERVER_TYPE_SINK, sinkFromTopics = Seq(topicKey1))
    val webServer = new WebServer(config)
    webServer.routes // create route handle first.

    try {
      val clientFetch: Future[Seq[Row]]  = pollRowsRequest(webServer, "", 10 * 1000, ec)
      val clientFetch1: Future[Seq[Row]] = pollRowsRequest(webServer, "", 10 * 1000, ec)

      val rowCount1 = 50
      KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount1, FiniteDuration(10, SECONDS))
      log.info("produce {} rows", rowCount1); Thread.sleep(1000)

      val rowCount2 = 80
      KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount2, FiniteDuration(10, SECONDS))
      log.info("produce {} rows", rowCount2); Thread.sleep(1000)

      val rowCount3 = 40
      KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount3, FiniteDuration(10, SECONDS))
      log.info("produce {} rows", rowCount3); Thread.sleep(1000)

      val rows  = Await.result(clientFetch, Duration.Inf)
      val rows1 = Await.result(clientFetch1, Duration.Inf)
      (rows.size + rows1.size) should ===(rowCount1 + rowCount2 + rowCount3)
    } finally {
      Releasable.close(webServer)
      ec.shutdown()
    }
  }
  @Test
  def testMultipleGroup(): Unit = {
    val threadPool: ExecutorService = newThreadPool()
    implicit val ec                 = ExecutionContext.fromExecutorService(threadPool)

    val topicKey1 = createTopicKey
    val config    = defaultTestConfig(SERVER_TYPE_SINK, sinkFromTopics = Seq(topicKey1))
    val webServer = new WebServer(config)
    webServer.routes // create route handle first.

    try {
      // two data group concurrent request
      val clientFetch: Future[Seq[Row]]  = pollRowsRequest(webServer, "", 10 * 1000, ec)
      val clientFetch1: Future[Seq[Row]] = pollRowsRequest(webServer, "group1", 10 * 1000, ec)
      val clientFetch2: Future[Seq[Row]] = pollRowsRequest(webServer, "", 10 * 1000, ec)
      val clientFetch3: Future[Seq[Row]] = pollRowsRequest(webServer, "group1", 10 * 1000, ec)

      val rowCount1 = 150
      KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount1, FiniteDuration(10, SECONDS))
      log.info("produce {} rows", rowCount1); Thread.sleep(1000)

      val rowCount2 = 180
      KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount2, FiniteDuration(10, SECONDS))
      log.info("produce {} rows", rowCount2); Thread.sleep(1000)

      val rowCount3 = 140
      KafkaSupport.prepareBulkOfRow(brokerProps, topicKey1.name, rowCount3, FiniteDuration(10, SECONDS))
      log.info("produce {} rows", rowCount3); Thread.sleep(1000)

      val totalCount = rowCount1 + rowCount2 + rowCount3

      val rows  = Await.result(clientFetch, Duration.Inf)
      val rows1 = Await.result(clientFetch1, Duration.Inf)
      val rows2 = Await.result(clientFetch2, Duration.Inf)
      val rows3 = Await.result(clientFetch3, Duration.Inf)

      (rows.size + rows2.size) should ===(totalCount)
      (rows1.size + rows3.size) should ===(totalCount)
    } finally {
      Releasable.close(webServer)
      ec.shutdown()
    }
  }
}
