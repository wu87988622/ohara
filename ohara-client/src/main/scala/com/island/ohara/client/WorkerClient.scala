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

package com.island.ohara.client
import java.net.HttpRetryException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.ConnectorJson._
import com.island.ohara.common.util.ReleaseOnce
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.Random

/**
  * a helper class used to send the rest request to kafka worker.
  */
trait WorkerClient extends ReleaseOnce {

  def connectorCreator(): ConnectorCreator

  def delete(name: String): Unit

  def pause(name: String): Unit

  def resume(name: String): Unit

  def plugins(): Seq[Plugin]

  def activeConnectors(): Seq[String]

  def connectionProps: String

  def status(name: String): ConnectorInformation

  def config(name: String): ConnectorConfig

  def taskStatus(name: String, id: Int): TaskStatus

  /**
    * Check whether a connector name is used in creating connector (even if the connector fails to start, this method
    * still return true)
    * @param name connector name
    * @return true if connector exists
    */
  def exist(name: String): Boolean = activeConnectors().contains(name)

  def nonExist(name: String): Boolean = !exist(name)
}

object WorkerClient {
  private[this] val COUNTER = new AtomicInteger(0)
  import scala.concurrent.duration._
  val TIMEOUT: FiniteDuration = 30 seconds

  def apply(_connectionProps: String): WorkerClient = {
    val workerList = _connectionProps.split(",")
    if (workerList.isEmpty) throw new IllegalArgumentException(s"Invalid workers:${_connectionProps}")
    new WorkerClient() with SprayJsonSupport {
      private[this] val workerAddress: String = workerList(Random.nextInt(workerList.size))

      private[this] implicit val actorSystem: ActorSystem = ActorSystem(
        s"${classOf[WorkerClient].getSimpleName}-${COUNTER.getAndIncrement()}-system")

      private[this] implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

      override def connectorCreator(): ConnectorCreator = request =>
        Await.result(
          Marshal(request)
            .to[RequestEntity]
            .flatMap(
              entity =>
                Http()
                  .singleRequest(
                    HttpRequest(method = HttpMethods.POST, uri = s"http://$workerAddress/connectors", entity = entity))
                  .flatMap(unmarshal[CreateConnectorResponse])),
          TIMEOUT
      )

      override def delete(name: String): Unit = Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.DELETE, uri = s"http://$workerAddress/connectors/$name"))
          .flatMap(
            res =>
              if (res.status.isFailure())
                Unmarshal(res.entity)
                  .to[ErrorResponse]
                  .flatMap(error => Future.failed(new IllegalStateException(error.toString)))
              else Future.successful((): Unit)),
        TIMEOUT
      )

      override protected def doClose(): Unit = Await.result(actorSystem.terminate(), 60 seconds)

      override def plugins(): Seq[Plugin] = Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.GET, uri = s"http://$workerAddress/connector-plugins"))
          .flatMap(unmarshal[Seq[Plugin]]),
        TIMEOUT
      )
      override def activeConnectors(): Seq[String] = Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.GET, uri = s"http://$workerAddress/connectors"))
          .flatMap(unmarshal[Seq[String]])
          .recover {
            // retry
            case _: HttpRetryException =>
              TimeUnit.SECONDS.sleep(1)
              activeConnectors()
          },
        TIMEOUT
      )

      private[this] def unmarshal[T](response: HttpResponse)(implicit um: RootJsonFormat[T]): Future[T] =
        if (response.status.isSuccess()) Unmarshal(response).to[T]
        else
          Unmarshal(response)
            .to[ErrorResponse]
            .flatMap(error => {
              // this is a retriable exception
              if (error.error_code == StatusCodes.Conflict.intValue)
                Future.failed(new HttpRetryException(error.message, error.error_code))
              else {
                // convert the error response to runtime exception
                Future.failed(new IllegalStateException(error.toString))
              }
            })

      override def connectionProps: String = _connectionProps

      override def status(name: String): ConnectorInformation = Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.GET, uri = s"http://$workerAddress/connectors/$name/status"))
          .flatMap(unmarshal[ConnectorInformation]),
        TIMEOUT
      )

      override def config(name: String): ConnectorConfig = Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.GET, uri = s"http://$workerAddress/connectors/$name/config"))
          .flatMap(unmarshal[ConnectorConfig]),
        TIMEOUT
      )

      override def taskStatus(name: String, id: Int): TaskStatus = Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.GET, uri = s"http://$workerAddress/connectors/$name/tasks/$id/status"))
          .flatMap(unmarshal[TaskStatus]),
        TIMEOUT
      )
      override def pause(name: String): Unit = Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.PUT, uri = s"http://$workerAddress/connectors/$name/pause"))
          .flatMap(
            res =>
              if (res.status.isFailure())
                Unmarshal(res.entity)
                  .to[ErrorResponse]
                  .flatMap(error => Future.failed(new IllegalStateException(error.toString)))
              else Future.successful((): Unit)),
        TIMEOUT
      )

      override def resume(name: String): Unit = Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.PUT, uri = s"http://$workerAddress/connectors/$name/resume"))
          .flatMap(
            res =>
              if (res.status.isFailure())
                Unmarshal(res.entity)
                  .to[ErrorResponse]
                  .flatMap(error => Future.failed(new IllegalStateException(error.toString)))
              else Future.successful((): Unit)),
        TIMEOUT
      )
    }
  }
}
