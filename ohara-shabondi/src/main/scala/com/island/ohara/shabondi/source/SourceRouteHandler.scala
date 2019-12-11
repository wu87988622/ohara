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

package com.island.ohara.shabondi.source

import java.util.concurrent.{ExecutorService, Executors}

import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.Releasable
import com.island.ohara.kafka.Producer
import com.island.ohara.shabondi._

import scala.concurrent.ExecutionContext

private[shabondi] object SourceRouteHandler {
  def apply(config: Config) = new SourceRouteHandler(config)
}

private[shabondi] class SourceRouteHandler(config: Config) extends RouteHandler {
  import Boot._
  import JsonSupport._

  private val log = Logging(actorSystem, classOf[SourceRouteHandler])

  private val threadPool: ExecutorService = Executors.newFixedThreadPool(4)
  implicit private val ec                 = ExecutionContext.fromExecutorService(threadPool)

  private val exceptionHandler = ExceptionHandler {
    case ex: Throwable =>
      log.error(ex, ex.getMessage)
      complete((StatusCodes.InternalServerError, ex.getMessage))
  }

  private val producer = Producer
    .builder()
    .connectionProps(config.brokers)
    .keySerializer(Serializer.ROW)
    .valueSerializer(Serializer.BYTES)
    .build()

  private val topicKeys = config.sourceToTopics

  override def route(): Route = {
    (post & path("v0")) {
      handleExceptions(exceptionHandler) {
        entity(as[RowData]) { rowData =>
          val row   = JsonSupport.toRow(rowData)
          val graph = StreamGraph.fromSendRow(producer, topicKeys, row)
          complete((StatusCodes.OK, graph.run()))
        }
      } // handleExceptions
    }
  }

  override def close(): Unit = {
    Releasable.close(producer)
    threadPool.shutdown()
  }
}
