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

import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, Route}
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

  private val log        = Logging(actorSystem, classOf[SinkRouteHandler])
  private val dataGroups = SinkDataGroups(config)

  private val exceptionHandler = ExceptionHandler {
    case ex: Throwable =>
      log.error(ex, ex.getMessage)
      complete((StatusCodes.InternalServerError, ex.getMessage))
  }

  private def fullyPollQueue(queue: util.Queue[Row]): Seq[Row] = {
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

  def route: Route = handleExceptions(exceptionHandler) {
    (get & path("v0" / "poll")) {
      val group  = dataGroups.defaultGroup
      val result = fullyPollQueue(group.queue).map(row => JsonSupport.toRowData(row))
      complete(result)
    } ~ (get & path("v0" / "poll" / Segment)) { groupId =>
      val group  = dataGroups.createIfAbsent(groupId)
      val result = fullyPollQueue(group.queue).map(row => JsonSupport.toRowData(row))
      complete(result)
    }
  }

  override def close(): Unit = {
    Releasable.close(dataGroups)
  }
}
