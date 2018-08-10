package com.island.ohara.client
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.io.CloseOnce
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

/**
  * Used to communicate with Configurator. The public interface of Configurator is only restful apis. Sometimes the restful
  * apis is unfriendly to use in coding. Hence, this class wraps the restful apis to scala apis. The implementation is based
  * on akka-http and akka-json.
  *
  */
trait ConfiguratorClient extends CloseOnce {

  //------------------------------------------------[storable data]------------------------------------------------//
  def list[T](implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): Seq[T]
  def get[T](uuid: String)(implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): T
  def delete[T](uuid: String)(implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): T
  def add[Req, Res](
    request: Req)(implicit rm0: RootJsonFormat[Req], rm1: RootJsonFormat[Res], cf: DataCommandFormat[Res]): Res
  def update[Req, Res](uuid: String, request: Req)(implicit rm0: RootJsonFormat[Req],
                                                   rm1: RootJsonFormat[Res],
                                                   cf: DataCommandFormat[Res]): Res
  //------------------------------------------------[validation]------------------------------------------------//
  def validate[Req, Res](request: Req)(implicit rm0: RootJsonFormat[Req],
                                       rm1: RootJsonFormat[Res],
                                       cf: ValidationCommandFormat[Req]): Seq[Res]
  //------------------------------------------------[cluster]------------------------------------------------//
  def cluster[Res](implicit rm0: RootJsonFormat[Res], cf: ClusterCommandFormat[Res]): Res
}

object ConfiguratorClient {
  private[this] val COUNTER = new AtomicInteger(0)
  import scala.concurrent.duration._
  private[this] val TIMEOUT = 10 seconds

  def apply(configuratorAddress: String): ConfiguratorClient = new ConfiguratorClient with SprayJsonSupport
  with DefaultJsonProtocol {
    private[this] implicit val actorSystem = ActorSystem(
      s"${classOf[ConfiguratorClient].getSimpleName}-${COUNTER.getAndIncrement()}-system")
    private[this] implicit val actorMaterializer = ActorMaterializer()
    override protected def doClose(): Unit = Await.result(actorSystem.terminate(), 60 seconds)

    override def list[T](implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): Seq[T] = Await.result(
      Http().singleRequest(HttpRequest(HttpMethods.GET, cf.format(configuratorAddress))).flatMap(unmarshal[Seq[T]](_)),
      TIMEOUT)

    override def delete[T](uuid: String)(implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): T = Await.result(
      Http()
        .singleRequest(HttpRequest(HttpMethods.DELETE, cf.format(configuratorAddress, uuid)))
        .flatMap(unmarshal[T](_)),
      TIMEOUT)

    override def get[T](uuid: String)(implicit rm: RootJsonFormat[T], cf: DataCommandFormat[T]): T = Await.result(
      Http().singleRequest(HttpRequest(HttpMethods.GET, cf.format(configuratorAddress, uuid))).flatMap(unmarshal[T](_)),
      TIMEOUT)
    override def add[Req, Res](
      request: Req)(implicit rm0: RootJsonFormat[Req], rm1: RootJsonFormat[Res], cf: DataCommandFormat[Res]): Res =
      Await.result(
        Marshal(request)
          .to[RequestEntity]
          .flatMap(entity => {
            Http()
              .singleRequest(HttpRequest(HttpMethods.POST, cf.format(configuratorAddress), entity = entity))
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
            .singleRequest(HttpRequest(HttpMethods.PUT, cf.format(configuratorAddress, uuid), entity = entity))
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
              .singleRequest(HttpRequest(HttpMethods.PUT, cf.format(configuratorAddress), entity = entity))
              .flatMap(unmarshal[Seq[Res]](_))
          }),
        TIMEOUT
      )

    import com.island.ohara.client.ConfiguratorJson.{ERROR_RESPONSE_JSON_FORMAT, ErrorResponse}
    private[this] def unmarshal[T](res: HttpResponse)(implicit rm: RootJsonFormat[T]): Future[T] =
      if (res.status.isSuccess()) Unmarshal(res.entity).to[T]
      else
        Unmarshal(res.entity)
          .to[ErrorResponse]
          .flatMap(error => Future.failed(new IllegalArgumentException(error.message)))

    // it is unnecessary to use the implicit imports here
    override def cluster[Res](implicit rm0: RootJsonFormat[Res], cf: ClusterCommandFormat[Res]): Res =
      Await.result(
        Http().singleRequest(HttpRequest(HttpMethods.GET, cf.format(configuratorAddress))).flatMap(unmarshal[Res](_)),
        TIMEOUT
      )
  }
}
