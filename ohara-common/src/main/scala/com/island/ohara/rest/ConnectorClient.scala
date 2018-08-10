package com.island.ohara.rest
import java.net.HttpRetryException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, RequestEntity, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.io.CloseOnce
import com.island.ohara.rest.ConnectorJson._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.Random

/**
  * a helper class used to send the rest request to kafka worker.
  */
trait ConnectorClient extends CloseOnce {

  def sourceConnectorCreator(): SourceConnectorCreator

  def sinkConnectorCreator(): SinkConnectorCreator

  def delete(name: String): Unit

  def plugins(): Seq[Plugin]

  def activeConnectors(): Seq[String]

  def workersString: String
}

object ConnectorClient {
  private[this] val COUNTER = new AtomicInteger(0)
  import scala.concurrent.duration._
  val TIMEOUT = 10 seconds

  def apply(_workersString: String): ConnectorClient = {
    val workers = _workersString.split(",")
    if (workers.isEmpty) throw new IllegalArgumentException(s"Invalid workers:${_workersString}")
    new ConnectorClient() with SprayJsonSupport {
      private[this] def workerAddress: String = workers(Random.nextInt(workers.size))

      private[this] implicit val actorSystem = ActorSystem(
        s"${classOf[ConnectorClient].getSimpleName}-${COUNTER.getAndIncrement()}-system")

      private[this] implicit val actorMaterializer = ActorMaterializer()

      override def sourceConnectorCreator(): SourceConnectorCreator = (request: ConnectorRequest) => send(request)

      override def sinkConnectorCreator(): SinkConnectorCreator = (request: ConnectorRequest) => send(request)

      private[this] def send(request: ConnectorRequest): ConnectorResponse = Await.result(
        Marshal(request)
          .to[RequestEntity]
          .flatMap(entity => {
            Http()
              .singleRequest(
                HttpRequest(method = HttpMethods.POST, uri = s"http://${workerAddress}/connectors", entity = entity))
              .flatMap(res => Unmarshal(res.entity).to[ConnectorResponse])
          }),
        TIMEOUT
      )

      override def delete(name: String): Unit = Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.DELETE, uri = s"http://${workerAddress}/connectors/$name"))
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
          .singleRequest(HttpRequest(HttpMethods.GET, uri = s"http://${workerAddress}/connector-plugins"))
          .flatMap(unmarshal[Seq[Plugin]](_)),
        TIMEOUT
      )
      override def activeConnectors(): Seq[String] = Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.GET, uri = s"http://${workerAddress}/connectors"))
          .flatMap(unmarshal[Seq[String]](_))
          .recover {
            // retry
            case _: HttpRetryException => {
              TimeUnit.SECONDS.sleep(1)
              activeConnectors()
            }
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
      override def workersString: String = _workersString
    }
  }
}
