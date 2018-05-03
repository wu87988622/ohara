package com.island.ohara.manager.sample

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path

import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout

trait UserRoutes extends SprayJsonSupport {

  import spray.json.DefaultJsonProtocol._
  import UserRegistryActor._

  lazy val log = Logging(system, classOf[UserRoutes])
  implicit def system: ActorSystem
  implicit lazy val timeout = Timeout(5.seconds)
  implicit val userJsonFormat = jsonFormat3(User)
  implicit val usersJsonFormat = jsonFormat1(Users)
  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

  def userRegistryActor: ActorRef

  lazy val userRoutes: Route =
    pathPrefix("users") {
      concat (
        get {
          val users: Future[Users] = (userRegistryActor ? GetUsers).mapTo[Users]
          complete(users)
        },
        post {
          entity(as[User]) { user =>
            val userCreated = (userRegistryActor ? CreateUser(user)).mapTo[ActionPerformed]
            onSuccess(userCreated) { performed =>
              log.info("Created user [{}]: {}", user.name, performed.description)
              complete( (StatusCodes.Created, performed) )
            }
          }
        }
      )  // concat
    }

}
