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

class ConfiguratorService(system: ActorSystem, host: String, port: Int) extends TopicJsonSupport with SprayJsonSupport {
  import ConfiguratorService._
  private implicit val actorSystem = system
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  def createTopic(post: CreateTopic): Future[String] = {
    Marshal(post)
      .to[RequestEntity]
      .flatMap { entity =>
        val request = HttpRequest(method = HttpMethods.POST,
                                  configuratorScheme + "://" + host + ":" + port + configuratorTopicPath,
                                  entity = entity)
        Http().singleRequest(request)
      }
      .flatMap(response => Unmarshal(response.entity).to[String])
      .fallbackTo(Future(""))
  }
}

object ConfiguratorService {

  val configuratorTopicPath = "/v0/topics"
  val configuratorScheme = "http"

}
