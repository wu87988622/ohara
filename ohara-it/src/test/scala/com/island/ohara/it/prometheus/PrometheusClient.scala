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

package com.island.ohara.it.prometheus

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.configurator.v0.ErrorApi._
import com.island.ohara.it.prometheus.PrometheusJson.{Config, Targets}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
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
