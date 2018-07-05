package com.island.ohara.manager

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.island.ohara.config.{OharaConfig, OharaJson, OharaProperty}
import com.island.ohara.io.CloseOnce
import com.island.ohara.rest.{BoundRestClient, RestResponse}

import scala.concurrent.duration._

/**
  * an inner rest server used to supply data to WEB UI. this rest server won't contain data. the most data in ohara
  * are stored in ohara configurator. this class plays as a proxy which redirects the request from UI to backend
  * configurator. And then parse and revise the request if UI has something special "requirement" to the response.
  *
  * @param restClient nullable. Null means you are writing tests and you don't run the configurator.
  */
class ApiRoutes(val system: ActorSystem, restClient: BoundRestClient) extends SprayJsonSupport with CloseOnce {

  import UserLoginActor._
  import spray.json.DefaultJsonProtocol._

  private lazy val logger = Logging(system, classOf[ApiRoutes])
  private implicit lazy val timeout = Timeout(5.seconds)
  private implicit val userJsonFormat = jsonFormat2(User)
  private implicit val returnMessageFormat = jsonFormat2(ReturnMessage[Boolean])

  lazy val userLoginActor: ActorRef =
    system.actorOf(UserLoginActor.props, "userLoginActor")

  /**
    * UI requires a field to represent the status of the rest call.
    *
    * @param response come from ohara configurator
    * @return a new response with extra field "status"
    */
  private[this] def appendStatus(response: RestResponse): String = {
    val data: OharaConfig = OharaConfig(OharaJson(response.body))
    ApiRoutes.status.set(data, if (response.statusCode == 200) true else false)
    data.toJson.toString
  }

  private[this] val schemaRoute: Route = if (restClient == null) pathPrefix("schemas") {
    reject
  } else {
    val basicPath2Configurator = "v0/schemas"
    val addSchema = pathEnd {
      post {
        entity(as[String]) { requestBody =>
          val result =
            restClient.post(basicPath2Configurator, OharaJson(requestBody))
          complete(200 -> appendStatus(result))
        }
      }
    }

    val getSchema = path(Segment) { uuid =>
      {
        get {
          val result = restClient.get(s"$basicPath2Configurator/${uuid}")
          complete(200 -> appendStatus(result))
        }
      }
    }

    val listSchemas = pathEnd {
      get {
        val result = restClient.get(basicPath2Configurator)
        complete(200 -> appendStatus(result))
      }
    }

    val deleteSchema = path(Segment) { uuid =>
      {
        delete {
          val result = restClient.delete(s"$basicPath2Configurator/${uuid}")
          complete(200 -> appendStatus(result))
        }
      }
    }

    val updateSchema = path(Segment) { uuid =>
      {
        put {
          entity(as[String]) { requestBody =>
            val result = restClient.put(s"$basicPath2Configurator/$uuid", OharaJson(requestBody))
            complete(200 -> appendStatus(result))
          }
        }
      }
    }

    pathPrefix("schemas") {
      addSchema ~ listSchemas ~ getSchema ~ deleteSchema ~ updateSchema
    }
  }

  private[this] val userRoute: Route = {
    val loginRoute: Route = path("login") {
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

    val logoutRoute: Route = path("logout") {
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

    loginRoute ~ logoutRoute
  }

  lazy val routes: Route =
    pathPrefix("api") {
      // TODO: remove hardcoded host and port
      userRoute ~ schemaRoute ~ new TopicRoutes(new ConfiguratorService(system, "localhost", 9999))(system).routes
    }

  override protected def doClose(): Unit = restClient.close()
}

object ApiRoutes {
  val status: OharaProperty[Boolean] =
    OharaProperty.builder.key("status").description("true if the op succeed. otherwise false").booleanProperty
}
