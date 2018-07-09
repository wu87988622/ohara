package com.island.ohara.configurator

import spray.json.DefaultJsonProtocol

trait ConfiguratorJsonSupport extends DefaultJsonProtocol {
  implicit val getTopicSuccessFormat = jsonFormat4(GetTopicResponse)
  implicit val failureMessageFormat = jsonFormat3(FailureMessage)
  implicit val createTopicFormat = jsonFormat3(CreateTopic)
  implicit val uuidResponseFormat = jsonFormat1(UuidResponse)
}

final case class GetTopicResponse(uuid: String, name: String, numberOfPartitions: Int, numberOfReplications: Int)
final case class FailureMessage(message: String, code: String, stack: String)
final case class CreateTopic(name: String, numberOfPartitions: Int, numberOfReplications: Int)
final case class UuidResponse(uuid: String)
