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

package com.island.ohara.connector.jio

import java.util
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue, TimeUnit}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{path, _}
import akka.stream.ActorMaterializer
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.connector.jio.JioData._
import com.island.ohara.kafka.connector.{RowSinkRecord, RowSinkTask, TaskSetting}
import spray.json.DefaultJsonProtocol._

import scala.collection.JavaConverters._
import scala.concurrent.Await
class JsonOutTask extends RowSinkTask {
  private[this] implicit var actorSystem: ActorSystem             = _
  private[this] implicit var actorMaterializer: ActorMaterializer = _
  private[this] var httpServer: Http.ServerBinding                = _
  private[this] var blockingQueue: BlockingQueue[JioData]         = _
  private[this] var props: JioProps                               = _

  /**
    * TODO: should we handle the BindException?
    * the tasks may be distributed on same node and it means the port will be re-bind. The exception will stop this task.
    */
  override protected def _start(settings: TaskSetting): Unit = {
    props = JioProps(settings)
    blockingQueue = new ArrayBlockingQueue[JioData](props.bufferSize)

    // used to accept the data
    val route =
      path(props.bindingPath) {
        get {
          complete(blockingQueue.asScala.toList)
        }
      }

    actorSystem = ActorSystem(s"${classOf[JsonOutTask].getSimpleName}-system")
    actorMaterializer = ActorMaterializer()
    httpServer = Await.result(
      Http()(actorSystem).bindAndHandle(
        handler = route ~ path(Remaining)(_ => complete(StatusCodes.NotFound)),
        // we bind the service on all network adapter.
        interface = CommonUtils.anyLocalAddress(),
        port = props.bindingPort
      )(actorMaterializer),
      props.bindingTimeout
    )
  }

  override protected def _stop(): Unit = {
    Releasable.close(() => Await.result(httpServer.terminate(props.closeTimeout), props.closeTimeout))
    Releasable.close(() => Await.result(actorSystem.terminate(), props.closeTimeout))
  }

  override protected def _put(records: util.List[RowSinkRecord]): Unit =
    records.asScala
      .map(_.row())
      .flatMap(
        row =>
          try Some(JioData(row))
          catch {
            case _: Throwable =>
              // unsupported data type is rejected :)
              None
          }
      )
      .foreach(
        d =>
          if (!blockingQueue.offer(d)) {
            // the _put is called by single thread so there is no race condition in write.
            // Hence, we can assume the poll operation must "remove" something from queue, and the following add must be successful.
            blockingQueue.poll(0, TimeUnit.SECONDS)
            blockingQueue.offer(d)
          }
      )
}
