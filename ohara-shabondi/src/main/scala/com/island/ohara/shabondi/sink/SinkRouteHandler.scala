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

import java.util.concurrent.{BlockingQueue, Executors}

import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.island.ohara.common.data.Row
import com.island.ohara.common.util.Releasable
import com.island.ohara.shabondi._

import scala.collection.mutable.ArrayBuffer

private[shabondi] object SinkRouteHandler {
  def apply(config: Config) =
    new SinkRouteHandler(config)
}

private[shabondi] class SinkRouteHandler(config: Config) extends RouteHandler {
  import Boot._
  import JsonSupport._

  private val log = Logging(actorSystem, classOf[SinkRouteHandler])

  // TODO:  We should provide a
  private val threadPool =
    Executors.newFixedThreadPool(4, new ThreadFactoryBuilder().setNameFormat("shabondi-sinkpool-%d").build())
  private val sinkRowQueueProducer = RowQueueProducer(config)
  private val queue                = sinkRowQueueProducer.queue

  threadPool.execute(sinkRowQueueProducer)

  private val exceptionHandler = ExceptionHandler {
    case ex: Throwable =>
      log.error(ex, ex.getMessage)
      complete((StatusCodes.InternalServerError, ex.getMessage))
  }

  private def fullyPollQueue(queue: BlockingQueue[Row]): Seq[Row] = {
    if (queue.isEmpty)
      Seq.empty[Row]
    else {
      val buffer    = ArrayBuffer.empty[Row]
      var item: Row = queue.poll()
      while (item != null) {
        buffer += item
        item = queue.poll()
      }
      buffer
    }
  }

  def route: Route = {
    (post & path("v0" / "poll")) {
      handleExceptions(exceptionHandler) {
        val result = fullyPollQueue(queue).map(row => JsonSupport.toRowData(row))
        complete(result)
      }
    }
  }

  override def close(): Unit = {
    Releasable.close(sinkRowQueueProducer)
    threadPool.shutdown()
  }
}
