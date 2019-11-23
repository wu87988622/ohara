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

package com.island.ohara.shabondi

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.island.ohara.common.data.Row
import com.island.ohara.common.setting.TopicKey
import spray.json._

import scala.concurrent.Future

object SourceRoute {
  def apply(config: Config)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) =
    new SourceRoute(config)
}

class SourceRoute(config: Config)(implicit val actorSystem: ActorSystem, implicit val materializer: ActorMaterializer)
    extends Directives {
  import JsonSupport._
  import actorSystem.dispatcher

  private lazy val log = Logging(actorSystem, classOf[SourceRoute])

  private val exceptionHandler = ExceptionHandler {
    case ex: Throwable =>
      log.error(ex, ex.getMessage)
      complete((StatusCodes.InternalServerError, ex.getMessage))
  }

  private val producer = KafkaClient.newProducer(config.brokers)

  def route(topicKeys: Seq[TopicKey]): Route = {
    (post & path("v0")) {
      handleExceptions(exceptionHandler) {
        entity(as[RowData]) { rowData =>
          val row = JsonSupport.toRow(JsObject(rowData))

          complete((StatusCodes.OK, sendRowFuture(topicKeys, row).map(_ => "success")))
        }
      } // handleExceptions
    }
  }

  private[shabondi] def sendRowFuture(topicKeys: Seq[TopicKey], row: Row): Future[Done] = {
    import KafkaClient._
    val source = Source.single(row)
    val flowSendRow = Flow[Row].mapAsync(4) { row =>
      Future.sequence(topicKeys.map { topicKey =>
        val sender = producer.sender().key(row).topicName(topicKey.name())
        sender.send.toScala
      })
    }
    val sink = Sink.ignore
    source.via(flowSendRow).toMat(sink)(Keep.right).run()
  }
}
