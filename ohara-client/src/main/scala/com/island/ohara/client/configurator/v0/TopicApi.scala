/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
