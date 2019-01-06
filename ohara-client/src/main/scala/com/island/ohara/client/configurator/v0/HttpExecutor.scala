package com.island.ohara.client.configurator.v0

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.configurator.v0.ErrorApi._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * used to send http request to remote node. The operations implemented by this class includes 1) get, 2) delete, 3) put and 4) post.
  * The method get2 is a variety of get. It sends get request and then assume the response is a array.
  */
private trait HttpExecutor {
  def get[Res](url: String)(implicit rm: RootJsonFormat[Res]): Future[Res]
  def get2[Res](url: String)(implicit rm: RootJsonFormat[Res]): Future[Seq[Res]]
  def delete[Res](url: String)(implicit rm: RootJsonFormat[Res]): Future[Res]
  def post[Res, Req](url: String, request: Req)(implicit rm0: RootJsonFormat[Res],
                                                rm1: RootJsonFormat[Req]): Future[Res]
  def put[Res, Req](url: String, request: Req)(implicit rm0: RootJsonFormat[Res], rm1: RootJsonFormat[Req]): Future[Res]
}

private object HttpExecutor {

  /**
    *  ActorSystem is a heavy component in akka, so we should reuse it as much as possible. We don't need to close it programmatically since
    *  it is a singleton object in whole jvm. And it will be released in closing jvm.
    */
  implicit lazy val SINGLETON: HttpExecutor = new HttpExecutor {
    private[this] implicit val actorSystem: ActorSystem = ActorSystem("Executor-SINGLETON")
    private[this] implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
    private[this] def unmarshal[T](res: HttpResponse)(implicit rm: RootJsonFormat[T]): Future[T] =
      if (res.status.isSuccess()) Unmarshal(res.entity).to[T]
      else
        Unmarshal(res.entity).to[Error].flatMap(error => Future.failed(new IllegalArgumentException(error.message)))
    override def get[Res](url: String)(implicit rm: RootJsonFormat[Res]): Future[Res] =
      Http().singleRequest(HttpRequest(HttpMethods.GET, url)).flatMap(unmarshal[Res])
    override def get2[Res](url: String)(implicit rm: RootJsonFormat[Res]): Future[Seq[Res]] =
      Http().singleRequest(HttpRequest(HttpMethods.GET, url)).flatMap(unmarshal[Seq[Res]])
    override def delete[Res](url: String)(implicit rm: RootJsonFormat[Res]): Future[Res] =
      Http().singleRequest(HttpRequest(HttpMethods.DELETE, url)).flatMap(unmarshal[Res])
    override def post[Res, Req](url: String, request: Req)(implicit rm0: RootJsonFormat[Res],
                                                           rm1: RootJsonFormat[Req]): Future[Res] =
      Marshal(request).to[RequestEntity].flatMap { entity =>
        Http().singleRequest(HttpRequest(HttpMethods.POST, url, entity = entity)).flatMap(unmarshal[Res])
      }
    override def put[Res, Req](url: String, request: Req)(implicit rm0: RootJsonFormat[Res],
                                                          rm1: RootJsonFormat[Req]): Future[Res] =
      Marshal(request).to[RequestEntity].flatMap { entity =>
        Http().singleRequest(HttpRequest(HttpMethods.PUT, url, entity = entity)).flatMap(unmarshal[Res])
      }
  }
}
