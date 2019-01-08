package com.island.ohara.client.configurator.v0
import spray.json.DefaultJsonProtocol.{jsonFormat3, jsonFormat5, _}
import spray.json.RootJsonFormat

object TopicApi {
  val TOPICS_PREFIX_PATH: String = "topics"
  case class TopicCreationRequest(name: String, numberOfPartitions: Int, numberOfReplications: Short)

  implicit val TOPIC_CREATION_REQUEST_FORMAT: RootJsonFormat[TopicCreationRequest] = jsonFormat3(TopicCreationRequest)

  case class TopicInfo(id: String,
                       name: String,
                       numberOfPartitions: Int,
                       numberOfReplications: Short,
                       lastModified: Long)
      extends Data {
    override def kind: String = "topic"
  }

  implicit val TOPIC_INFO_FORMAT: RootJsonFormat[TopicInfo] = jsonFormat5(TopicInfo)

  def access(): Access[TopicCreationRequest, TopicInfo] =
    new Access[TopicCreationRequest, TopicInfo](TOPICS_PREFIX_PATH)
}
