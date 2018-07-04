package com.island.ohara.manager

import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol

trait TopicJsonSupport extends DefaultJsonProtocol {
  implicit val topicCreatedRequest = jsonFormat3(CreateTopic)
  implicit val topicCreatedResponse = jsonFormat3(CreateTopicResponse)
}

trait ConfiguratorJsonSupport extends DefaultJsonProtocol {
  implicit val configuratorSuccess = jsonFormat1(CreateTopicSuccess)
  implicit val configuratorFailure = jsonFormat3(CreateTopicFailure)
}

class ConfiguratorService(system: ActorSystem, host: String, port: Int)
    extends TopicJsonSupport
    with ConfiguratorJsonSupport
    with SprayJsonSupport {
  import ConfiguratorService._
  private implicit val actorSystem = system
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  private type CreateTopicReturn = Either[CreateTopicFailure, CreateTopicSuccess]

  def createTopic(post: CreateTopic): Future[CreateTopicReturn] = {
    Marshal(post)
      .to[RequestEntity]
      .flatMap { entity =>
        val request = HttpRequest(method = HttpMethods.POST,
                                  configuratorScheme + "://" + host + ":" + port + configuratorTopicPath,
                                  entity = entity)
        Http().singleRequest(request)
      }
      .flatMap { response =>
        Unmarshal(response.entity).to[CreateTopicReturn]
      }
  }
}

object ConfiguratorService {

  val configuratorTopicPath = "/v0/topics"
  val configuratorScheme = "http"

}

final case class CreateTopicSuccess(uuid: String)
final case class CreateTopicFailure(message: String, code: String, stack: String)
