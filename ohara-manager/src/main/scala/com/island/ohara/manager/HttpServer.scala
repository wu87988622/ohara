package com.island.ohara.manager

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import com.island.ohara.manager.sample.{UserRegistryActor, UserRoutes}

import scala.io.StdIn

object HttpServer extends UserRoutes {

  implicit val system = ActorSystem("ohara-manager")
  val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props, "userRegistryActor")
  implicit val executionContext = system.dispatcher

  def main(args: Array[String]): Unit = {
    implicit val materializer = ActorMaterializer()

    val route =
      path("hello") {
        get {
          val entity = { title: String =>
            HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>$title</h1>")
          }
          complete(entity("Hello akka-http"))
        }
      } ~ userRoutes

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Ohara-manager online at http://localhost:8080/")

    //Await.result(system.whenTerminated, Duration.Inf)   // await infinite

    println("Press RETURN to stop...")
    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

}
