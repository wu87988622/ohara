package com.island.ohara.connector.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object WebServer {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("ohara-http-connector")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val route =
      path("hello") {
        get {
          val entity = { title: String =>
            HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>$title</h1>")
          }
          complete(entity("Hello akka-http"))
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Ohara http connector at http://localhost:8080/")
    println("Press RETURN to stop...")

    // Use following code if await infinite:
    //    Await.result(system.whenTerminated, Duration.Inf)   // await infinite

    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

}
