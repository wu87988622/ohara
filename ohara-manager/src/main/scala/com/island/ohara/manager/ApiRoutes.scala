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
import com.island.ohara.config.{OharaConfig, OharaJson, OharaProperty}
import com.island.ohara.io.CloseOnce
import com.island.ohara.rest.{RestClient, RestResponse}

import scala.concurrent.duration._

/**
  * an inner rest server used to supply data to WEB UI. this rest server won't contain data. the most data in ohara
  * are stored in ohara configurator. this class plays as a proxy which redirects the request from UI to backend
  * configurator. And then parse and revise the request if UI has something special "requirement" to the response.
  *
  * @param configuratorInfo nullable. Null means you are writing tests and you don't run the configurator.
  */
class ApiRoutes(val system: ActorSystem, configuratorInfo: (String, Int)) extends SprayJsonSupport with CloseOnce {
  import UserLoginActor._
  import spray.json.DefaultJsonProtocol._

  lazy val logger = Logging(system, classOf[ApiRoutes])
  implicit lazy val timeout = Timeout(5.seconds)
  implicit val userJsonFormat = jsonFormat2(User)
  implicit val returnMessageFormat = jsonFormat2(ReturnMessage[Boolean])

  lazy val userLoginActor: ActorRef =
    system.actorOf(UserLoginActor.props, "userLoginActor")

  private[this] val restClient = RestClient()

  /**
    * UI requires a field to represent the status of the rest call.
    * @param response come from ohara configurator
    * @return a new response with extra field "status"
    */
  private[this] def appendStatus(response: RestResponse): String = {
    val data: OharaConfig = OharaConfig(OharaJson(response.body))
    ApiRoutes.status.set(data, if (response.statusCode == 200) true else false)
    data.toJson.toString
  }

  private[this] val schemaRoute: Route = if (configuratorInfo == null) pathPrefix("schemas") { reject } else {
    val basicPath2Configurator = "v0/schemas"
    val addSchema = pathEnd {
      post {
        entity(as[String]) { requestBody =>
          val result =
            restClient.post(configuratorInfo._1, configuratorInfo._2, basicPath2Configurator, OharaJson(requestBody))
          complete(200 -> appendStatus(result))
        }
      }
    }

    val getSchema = path(Segment) { uuid =>
      {
        get {
          val result = restClient.get(configuratorInfo._1, configuratorInfo._2, s"$basicPath2Configurator/${uuid}")
          complete(200 -> appendStatus(result))
        }
      }
    }

    val listSchemas = pathEnd {
      get {
        val result = restClient.get(configuratorInfo._1, configuratorInfo._2, basicPath2Configurator)
        complete(200 -> appendStatus(result))
      }
    }

    val deleteSchema = path(Segment) { uuid =>
      {
        delete {
          val result = restClient.delete(configuratorInfo._1, configuratorInfo._2, s"$basicPath2Configurator/${uuid}")
          complete(200 -> appendStatus(result))
        }
      }
    }

    val updateSchema = path(Segment) { uuid =>
      {
        put {
          entity(as[String]) { requestBody =>
            val result = restClient.put(configuratorInfo._1,
                                        configuratorInfo._2,
                                        s"$basicPath2Configurator/$uuid",
                                        OharaJson(requestBody))
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
      userRoute ~ schemaRoute
    }

  override protected def doClose(): Unit = restClient.close()
}

object ApiRoutes {
  val status: OharaProperty[Boolean] =
    OharaProperty.builder.key("status").description("true if the op succeed. otherwise false").booleanProperty
}
