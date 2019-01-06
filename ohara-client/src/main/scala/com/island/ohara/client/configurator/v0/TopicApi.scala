package com.island.ohara.client.configurator.v0
import spray.json.DefaultJsonProtocol.{jsonFormat3, jsonFormat5, _}
import spray.json.RootJsonFormat

object TopicApi {
  val TOPICS_PREFIX_PATH: String = "topics"
  case class TopicCreationRequest(name: String, numberOfPartitions: Int, numberOfReplications: Short)

  implicit val TOPIC_CREATION_REQUEST_FORMAT: RootJsonFormat[TopicCreationRequest] = jsonFormat3(TopicCreationRequest)

  case class TopicDescription(id: String,
                              name: String,
                              numberOfPartitions: Int,
                              numberOfReplications: Short,
                              lastModified: Long)
      extends Data {
    override def kind: String = "topic"
  }

  implicit val TOPIC_DESCRIPTION_FORMAT: RootJsonFormat[TopicDescription] = jsonFormat5(TopicDescription)

  def access(): Access[TopicCreationRequest, TopicDescription] =
    new Access[TopicCreationRequest, TopicDescription](TOPICS_PREFIX_PATH)
}
