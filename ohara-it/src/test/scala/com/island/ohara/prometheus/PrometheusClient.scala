package com.island.ohara.prometheus

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.prometheus.PrometheusJson.{Config, Targets}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

/**
  * Prometheus supplies rest api for query .
  * There are no java client for query .
  * @see <a href="https://prometheus.io/docs/prometheus/latest/querying/api/">Prometheus Rest API</a>
  */
trait PrometheusClient {

  ///api/v1/targets  all targets
  def targets(): Targets

  ///api/v1/status/config    prometheus config
  def config(): Config

}

object PrometheusClient {

  private[this] val COUNTER = new AtomicInteger(0)
  private[this] val TIMEOUT = 20 seconds
  private[this] val url_preffix = "/api/"
  private[this] val schema = "http"

  def apply(prometheusURL: String): PrometheusClient = new PrometheusClient with SprayJsonSupport
  with DefaultJsonProtocol {
    private[this] implicit val actorSystem: ActorSystem = ActorSystem(
      s"${classOf[PrometheusClient].getSimpleName}-${COUNTER.getAndIncrement()}-system")
    private[this] implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

    private def send[T](path: String, apiVersion: String = "v1")(implicit rm: RootJsonFormat[T]) = {
      val url = schema + "://" + prometheusURL + url_preffix + apiVersion + path
      Await.result(Http().singleRequest(HttpRequest(HttpMethods.GET, url)).flatMap(unmarshal[T](_)), TIMEOUT)
    }

    import com.island.ohara.client.ConfiguratorJson.{ERROR_JSON_FORMAT, Error}
    private[this] def unmarshal[T](res: HttpResponse)(implicit rm: RootJsonFormat[T]): Future[T] =
      if (res.status.isSuccess()) Unmarshal(res.entity).to[T]
      else
        Unmarshal(res.entity).to[Error].flatMap(error => Future.failed(new IllegalArgumentException(error.message)))

    override def targets(): Targets = {
      send[Targets]("/targets")
    }

    override def config(): Config = {
      send[Config]("/status/config")
    }
  }
}
