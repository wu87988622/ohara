package com.island.ohara.manager

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._

class ApiRoutes(val system: ActorSystem) extends SprayJsonSupport {
  import UserLoginActor._
  import spray.json.DefaultJsonProtocol._

  lazy val logger = Logging(system, classOf[ApiRoutes])
  implicit lazy val timeout = Timeout(5.seconds)
  implicit val userJsonFormat = jsonFormat2(User)
  implicit val returnMessageFormat = jsonFormat2(ReturnMessage[Boolean])

  lazy val userLoginActor: ActorRef =
    system.actorOf(UserLoginActor.props, "userLoginActor")

  lazy val routes =
    pathPrefix("api") {
      loginRoute ~
        logoutRoute
    }

  private def loginRoute: Route = path("login") {
    post {
      entity(as[User]) { user =>
        logger.info("login: " + user.name)
        val result = (userLoginActor ? Login(user.name, user.password.getOrElse(""))).mapTo[ReturnMessage[Boolean]]
        onSuccess(result) { returnMessage =>
          logger.info(returnMessage.message)
          complete(returnMessage)
        }
      }
    }
  }

  private def logoutRoute: Route = path("logout") {
    post {
      entity(as[String]) { name =>
        logger.info(s"logout: $name")
        val result = (userLoginActor ? Logout(name)).mapTo[ReturnMessage[Boolean]]
        onSuccess(result) { returnMessage =>
          logger.info(returnMessage.message)
          complete(returnMessage)
        }
      }
    }
  }

}
