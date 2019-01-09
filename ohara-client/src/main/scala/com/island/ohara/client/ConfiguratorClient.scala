package com.island.ohara.client
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.Multipart.FormData.BodyPart.Strict
import akka.http.scaladsl.model.{Multipart, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.util.ReleaseOnce
import spray.json.RootJsonFormat
import com.island.ohara.client.configurator.v0.ErrorApi._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

/**
  * Used to communicate with Configurator. The public interface from Configurator is only restful apis. Sometimes the restful
  * apis is unfriendly to use in coding. Hence, this class wraps the restful apis to scala apis. The implementation is based
  * on akka-http and akka-json.
  *
  */
trait ConfiguratorClient extends ReleaseOnce {

  //------------------------------------------------[DATA]------------------------------------------------//
  def list[T](implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): Seq[T]
  def list[T](uuid: String)(implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): Seq[T]
  def get[T](uuid: String)(implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): T
  def delete[T](uuid: String)(implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): T
  def add[Req, Res](
    request: Req)(implicit rm0: RootJsonFormat[Req], rm1: RootJsonFormat[Res], cf: DataCommandFormat[Res]): Res
  def update[Req, Res](uuid: String, request: Req)(implicit rm0: RootJsonFormat[Req],
                                                   rm1: RootJsonFormat[Res],
                                                   cf: DataCommandFormat[Res]): Res
  //------------------------------------------------[QUERY]------------------------------------------------//
  def query[Req, Res](
    query: Req)(implicit rm0: RootJsonFormat[Req], rm1: RootJsonFormat[Res], cf: QueryCommandFormat[Req]): Res
  //------------------------------------------------[Stream]------------------------------------------------//
  def streamUploadJars[Req, Res](uuid: String, data: Strict*)(implicit rm1: RootJsonFormat[Res],
                                                              cf: DataCommandFormat[Req]): Res
}

object ConfiguratorClient {
  private[this] val COUNTER = new AtomicInteger(0)
  import scala.concurrent.duration._
  private[this] val TIMEOUT = 20 seconds

  def apply(hostname: String, port: Int): ConfiguratorClient = apply(s"$hostname:$port")

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json.DefaultJsonProtocol._
  def apply(connectionProps: String): ConfiguratorClient = new ConfiguratorClient {
    private[this] implicit val actorSystem: ActorSystem = ActorSystem(
      s"${classOf[ConfiguratorClient].getSimpleName}-${COUNTER.getAndIncrement()}-system")
    private[this] implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
    override protected def doClose(): Unit = Await.result(actorSystem.terminate(), 60 seconds)

    override def list[T](implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): Seq[T] = Await.result(
      Http().singleRequest(HttpRequest(HttpMethods.GET, cf.format(connectionProps))).flatMap(unmarshal[Seq[T]](_)),
      TIMEOUT)

    override def list[T](uuid: String)(implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): Seq[T] = Await.result(
      Http()
        .singleRequest(HttpRequest(HttpMethods.GET, cf.format(connectionProps, uuid)))
        .flatMap(unmarshal[Seq[T]](_)),
      TIMEOUT)

    override def delete[T](uuid: String)(implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): T = Await.result(
      Http().singleRequest(HttpRequest(HttpMethods.DELETE, cf.format(connectionProps, uuid))).flatMap(unmarshal[T]),
      TIMEOUT)

    override def get[T](uuid: String)(implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): T = Await.result(
      Http().singleRequest(HttpRequest(HttpMethods.GET, cf.format(connectionProps, uuid))).flatMap(unmarshal[T](_)),
      TIMEOUT)
    override def add[Req, Res](
      request: Req)(implicit rm0: RootJsonFormat[Req], rm1: RootJsonFormat[Res], cf: DataCommandFormat[Res]): Res =
      Await.result(
        Marshal(request)
          .to[RequestEntity]
          .flatMap(entity => {
            Http()
              .singleRequest(HttpRequest(HttpMethods.POST, cf.format(connectionProps), entity = entity))
              .flatMap(unmarshal[Res](_))
          }),
        TIMEOUT
      )

    override def update[Req, Res](uuid: String, request: Req)(implicit rm0: RootJsonFormat[Req],
                                                              rm1: RootJsonFormat[Res],
                                                              cf: DataCommandFormat[Res]): Res = Await.result(
      Marshal(request)
        .to[RequestEntity]
        .flatMap(entity => {
          Http()
            .singleRequest(HttpRequest(HttpMethods.PUT, cf.format(connectionProps, uuid), entity = entity))
            .flatMap(unmarshal[Res](_))
        }),
      TIMEOUT
    )

    private[this] def unmarshal[T](res: HttpResponse)(implicit rm: RootJsonFormat[T]): Future[T] =
      if (res.status.isSuccess()) Unmarshal(res.entity).to[T]
      else
        Unmarshal(res.entity).to[Error].flatMap(error => Future.failed(new IllegalArgumentException(error.message)))

    private[this] def unmarshal2(res: HttpResponse): Future[Unit] =
      if (res.status.isSuccess()) Future.successful(Unit)
      else
        Unmarshal(res.entity).to[Error].flatMap(error => Future.failed(new IllegalArgumentException(error.message)))

    override def query[Req, Res](
      query: Req)(implicit rm0: RootJsonFormat[Req], rm1: RootJsonFormat[Res], cf: QueryCommandFormat[Req]): Res =
      Await.result(
        Marshal(query)
          .to[RequestEntity]
          .flatMap(entity => {
            Http()
              .singleRequest(HttpRequest(HttpMethods.POST, cf.format(connectionProps), entity = entity))
              .flatMap(unmarshal[Res](_))
          }),
        TIMEOUT
      )

    override def streamUploadJars[Req, Res](uuid: String, data: Strict*)(implicit rm1: RootJsonFormat[Res],
                                                                         cf: DataCommandFormat[Req]): Res = {
      val entity = Multipart.FormData(data: _*)
      Await.result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.POST, cf.format(connectionProps, uuid), entity = entity.toEntity))
          .flatMap(unmarshal[Res](_)),
        TIMEOUT
      )
    }
  }
}
