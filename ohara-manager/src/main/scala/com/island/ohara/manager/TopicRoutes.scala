package com.island.ohara.manager
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class TopicRoutes(val configuratorService: ConfiguratorService)(implicit system: ActorSystem)
    extends TopicJsonSupport
    with SprayJsonSupport {
  import TopicRoutes._

  private implicit val executionContent = system.dispatcher
  def routes = pathPrefix("topics") {
    (post & entity(as[CreateTopic])) { post =>
      onComplete(
        if (validateCreate(post)) {
          configuratorService
            .createTopic(post)
            .map(result =>
              if (Option(result).getOrElse(EMPTY_STRING) != EMPTY_UUID) {
                CreateTopicResponse(true, EMPTY_STRING, result)
              } else {
                CreateTopicResponse(false, "Configurator returns empty or null", result)
            })
        } else {
          Future(CreateTopicResponse(false, "User didn't input expected values", EMPTY_UUID))
        }
      ) {
        case Success(result) => complete(result)
        case Failure(e)      => complete(CreateTopicResponse(false, e.toString, EMPTY_UUID))
      }
    }
  }

}

object TopicRoutes {

  private val namePattern = "[a-zA-Z0-9_-]*$".r
  val EMPTY_STRING = ""
  val EMPTY_UUID = EMPTY_STRING

  def validateCreate(post: CreateTopic): Boolean =
    namePattern.pattern.matcher(post.name).matches() &&
      post.numberOfPartitions > 0 && post.numberOfPartitions < Int.MaxValue &&
      post.numberOfReplications > 0 && post.numberOfReplications < Int.MaxValue
}

final case class CreateTopic(name: String, numberOfPartitions: Int, numberOfReplications: Int)
final case class CreateTopicResponse(status: Boolean, message: String, uuid: String)
