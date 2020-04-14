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

package oharastream.ohara.shabondi.source

import java.util.concurrent.{ExecutorService, Executors}
import java.util.function.Consumer

import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import oharastream.ohara.common.data.Serializer
import oharastream.ohara.common.util.Releasable
import oharastream.ohara.kafka.Producer
import oharastream.ohara.metrics.basic.Counter
import oharastream.ohara.shabondi.common.{JsonSupport, RouteHandler}

import scala.concurrent.ExecutionContext

private[shabondi] object SourceRouteHandler {
  def apply(config: SourceConfig, materializer: ActorMaterializer) =
    new SourceRouteHandler(config, materializer)
}
private[shabondi] class SourceRouteHandler(
  config: SourceConfig,
  materializer: ActorMaterializer
) extends RouteHandler {
  import oharastream.ohara.shabondi.common.JsonSupport._

  private val actorSystem = materializer.system
  private val log         = Logging(actorSystem, classOf[SourceRouteHandler])

  private val threadPool: ExecutorService = Executors.newFixedThreadPool(4)
  implicit private val ec                 = ExecutionContext.fromExecutorService(threadPool)

  private val totalRowsCounter =
    Counter.builder
      .key(config.objectKey)
      .item("total-rows")
      .unit("row")
      .document("The number of received rows")
      .value(0)
      .register()

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
    implicit val _materializer = materializer
    (post & path("v0")) {
      handleExceptions(exceptionHandler) {
        entity(as[RowData]) { rowData =>
          val row = JsonSupport.toRow(rowData)
          totalRowsCounter.incrementAndGet()
          val graph = StreamGraph.fromSendRow(producer, topicKeys, row)
          complete((StatusCodes.OK, graph.run()))
        }
      } // handleExceptions
    }
  }

  override def close(): Unit = {
    var exception: Throwable = null
    val addSuppressedException: Consumer[Throwable] = (ex: Throwable) => {
      if (exception == null) exception = ex else exception.addSuppressed(ex)
    }
    Releasable.close(producer, addSuppressedException)
    Releasable.close(totalRowsCounter, addSuppressedException)
    if (exception != null) throw exception
    threadPool.shutdown()
  }
}
