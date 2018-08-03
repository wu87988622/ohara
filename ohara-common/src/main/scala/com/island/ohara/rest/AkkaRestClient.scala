package com.island.ohara.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.island.ohara.config.OharaJson
import com.island.ohara.io.ByteUtil

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

private class AkkaRestClient(as: ActorSystem = null) extends RestClient {
  private[this] val needClose = as == null
  private[this] implicit val actorSystem =
    if (as == null) ActorSystem(s"${classOf[AkkaRestClient].getSimpleName}-system") else as
  private[this] implicit val actorMaterializer = ActorMaterializer()

  override protected def doClose(): Unit = if (needClose) Await.result(actorSystem.terminate(), 60 seconds)

  override def get(host: String, port: Int, path: String, timeout: Duration): RestResponse =
    request(host, port, HttpMethods.GET, path, null, timeout)

  override def delete(host: String, port: Int, path: String, timeout: Duration): RestResponse =
    request(host, port, HttpMethods.DELETE, path, null, timeout)

  override def put(host: String, port: Int, path: String, body: OharaJson, timeout: Duration): RestResponse =
    request(host, port, HttpMethods.PUT, path, body, timeout)

  override def post(host: String, port: Int, path: String, body: OharaJson, timeout: Duration): RestResponse =
    request(host, port, HttpMethods.POST, path, body, timeout)

  private[this] def request(host: String,
                            port: Int,
                            http: HttpMethod,
                            path: String,
                            body: OharaJson,
                            timeout: Duration): RestResponse =
    Await.result(
      Http()
        .singleRequest(
          if (body == null) HttpRequest(http, uri = s"${RestClient.HTTP_SCHEME}://$host:$port/$path")
          else
            HttpRequest(
              http,
              s"${RestClient.HTTP_SCHEME}://$host:$port${if (path.startsWith("/")) path else s"/$path"}",
              entity = HttpEntity(ContentTypes.`application/json`, ByteUtil.toBytes(body.toString))
            ))
        .flatMap(
          res =>
            res.entity
              .toStrict(FiniteDuration(timeout.toMillis, MILLISECONDS))
              .map(strict => RestResponse(res._1.intValue(), strict.data.decodeString("UTF-8")))),
      timeout
    )
}
