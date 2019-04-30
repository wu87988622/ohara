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

package com.island.ohara.shabondi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.io.StdIn

object WebServer {

  private[this] val log = Logger(WebServer.getClass)

  implicit val system: ActorSystem = ActorSystem("ohara-shabondi")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private[shabondi] val route =
    path("hello") {
      get {
        val entity = { title: String =>
          HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>$title</h1>")
        }
        complete(entity("Hello akka-http"))
      }
    }

  def main(args: Array[String]): Unit = {
    val interface = "0.0.0.0"
    val bindingFuture = Http().bindAndHandle(route, interface, 8080)
    log.info(s"OharaStream Shabondi at http://$interface:8080/")

    if (args.size > 0 && args(0) == "--return-stop") {
      println("Press RETURN to stop...")
      StdIn.readLine()
      bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
    } else {
      Await.result(system.whenTerminated, Duration.Inf) // await infinite
    }

  }

}
