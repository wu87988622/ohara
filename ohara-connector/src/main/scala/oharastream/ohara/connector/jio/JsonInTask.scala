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

package oharastream.ohara.connector.jio

import java.util
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{path, _}
import akka.stream.ActorMaterializer
import oharastream.ohara.common.util.{CommonUtils, Releasable}
import oharastream.ohara.kafka.connector.{RowSourceRecord, RowSourceTask, TaskSetting}

import scala.collection.JavaConverters._
import scala.concurrent.Await
class JsonInTask extends RowSourceTask {
  private[this] implicit var actorSystem: ActorSystem             = _
  private[this] implicit var actorMaterializer: ActorMaterializer = _
  private[this] var httpServer: Http.ServerBinding                = _
  private[this] var blockingQueue: BlockingQueue[JioData]         = _
  private[this] var props: JioProps                               = _

  private[this] var topics: Seq[String] = Seq.empty

  /**
    * TODO: should we handle the BindException?
    * the tasks may be distributed on same node and it means the port will be re-bind. The exception will stop this task.
    */
  override protected def _start(settings: TaskSetting): Unit = {
    topics = settings.topicNames().asScala
    if (topics.isEmpty) throw new IllegalArgumentException(s"no topics!!!!")
    props = JioProps(settings)
    blockingQueue = new ArrayBlockingQueue[JioData](props.bufferSize)

    // used to accept the data
    val route =
      path(props.bindingPath) {
        post {
          entity(as[JioData]) { data =>
            if (blockingQueue.offer(data)) complete(StatusCodes.OK)
            else complete(StatusCodes.TooManyRequests)
          }
        }
      }

    actorSystem = ActorSystem(s"${classOf[JsonInTask].getSimpleName}-system")
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

  override protected def _poll(): util.List[RowSourceRecord] =
    Iterator
      .continually(blockingQueue.poll())
      .takeWhile(_ != null)
      .map(_.row)
      .flatMap { row =>
        topics.map(topic => RowSourceRecord.of(topic, row))
      }
      .toSeq
      .asJava
}
