package com.island.ohara.client
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.Multipart.FormData.BodyPart.Strict
import akka.http.scaladsl.model.{Multipart, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.util.ReleaseOnce
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

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
  //------------------------------------------------[VALIDATION]------------------------------------------------//
  def validate[Req, Res](request: Req)(implicit rm0: RootJsonFormat[Req],
                                       rm1: RootJsonFormat[Res],
                                       cf: ValidationCommandFormat[Req]): Seq[Res]
  //------------------------------------------------[CLUSTER]------------------------------------------------//
  def cluster[Res](implicit rm0: RootJsonFormat[Res], cf: ClusterCommandFormat[Res]): Res
  //------------------------------------------------[CONTROL]------------------------------------------------//
  def start[T](uuid: String)(implicit cf: ControlCommandFormat[T]): Unit
  def stop[T](uuid: String)(implicit cf: ControlCommandFormat[T]): Unit
  def pause[T](uuid: String)(implicit cf: ControlCommandFormat[T]): Unit
  def resume[T](uuid: String)(implicit cf: ControlCommandFormat[T]): Unit
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
  def apply(connectionProps: String): ConfiguratorClient = new ConfiguratorClient with SprayJsonSupport
  with DefaultJsonProtocol {
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

    override def validate[Req, Res](request: Req)(implicit rm0: RootJsonFormat[Req],
                                                  rm1: RootJsonFormat[Res],
                                                  cf: ValidationCommandFormat[Req]): Seq[Res] =
      Await.result(
        Marshal(request)
          .to[RequestEntity]
          .flatMap(entity => {
            Http()
              .singleRequest(HttpRequest(HttpMethods.PUT, cf.format(connectionProps), entity = entity))
              .flatMap(unmarshal[Seq[Res]])
          }),
        TIMEOUT
      )

    import com.island.ohara.client.ConfiguratorJson.{ERROR_JSON_FORMAT, Error}
    private[this] def unmarshal[T](res: HttpResponse)(implicit rm: RootJsonFormat[T]): Future[T] =
      if (res.status.isSuccess()) Unmarshal(res.entity).to[T]
      else
        Unmarshal(res.entity).to[Error].flatMap(error => Future.failed(new IllegalArgumentException(error.message)))

    private[this] def unmarshal2(res: HttpResponse): Future[Unit] =
      if (res.status.isSuccess()) Future.successful(Unit)
      else
        Unmarshal(res.entity).to[Error].flatMap(error => Future.failed(new IllegalArgumentException(error.message)))

    // it is unnecessary to use the implicit imports here
    override def cluster[Res](implicit rm0: RootJsonFormat[Res], cf: ClusterCommandFormat[Res]): Res =
      Await.result(
        Http().singleRequest(HttpRequest(HttpMethods.GET, cf.format(connectionProps))).flatMap(unmarshal[Res](_)),
        TIMEOUT
      )
    override def start[T](uuid: String)(implicit cf: ControlCommandFormat[T]): Unit =
      Await.result(
        Http().singleRequest(HttpRequest(HttpMethods.PUT, cf.start(connectionProps, uuid))).flatMap(unmarshal2),
        TIMEOUT)

    override def stop[T](uuid: String)(implicit cf: ControlCommandFormat[T]): Unit =
      Await.result(
        Http().singleRequest(HttpRequest(HttpMethods.PUT, cf.stop(connectionProps, uuid))).flatMap(unmarshal2),
        TIMEOUT)

    override def pause[T](uuid: String)(implicit cf: ControlCommandFormat[T]): Unit =
      Await.result(
        Http().singleRequest(HttpRequest(HttpMethods.PUT, cf.pause(connectionProps, uuid))).flatMap(unmarshal2),
        TIMEOUT)

    override def resume[T](uuid: String)(implicit cf: ControlCommandFormat[T]): Unit =
      Await.result(
        Http().singleRequest(HttpRequest(HttpMethods.PUT, cf.resume(connectionProps, uuid))).flatMap(unmarshal2),
        TIMEOUT)

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
