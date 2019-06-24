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
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object TopicApi {

  val DEFAULT_NUMBER_OF_PARTITIONS: Int = 1
  val DEFAULT_NUMBER_OF_REPLICATIONS: Short = 1
  val TOPICS_PREFIX_PATH: String = "topics"
  case class Update private[TopicApi] (brokerClusterName: Option[String],
                                       numberOfPartitions: Option[Int],
                                       numberOfReplications: Option[Short])
  implicit val TOPIC_UPDATE_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update].format(jsonFormat3(Update)).rejectEmptyString().refine

  case class Creation private[TopicApi] (name: String,
                                         brokerClusterName: Option[String],
                                         numberOfPartitions: Int,
                                         numberOfReplications: Short)
      extends CreationRequest

  implicit val TOPIC_CREATION_FORMAT: RootJsonFormat[Creation] = JsonRefiner[Creation]
    .format(jsonFormat4(Creation))
    .defaultInt("numberOfPartitions", DEFAULT_NUMBER_OF_REPLICATIONS)
    .defaultInt("numberOfReplications", DEFAULT_NUMBER_OF_REPLICATIONS)
    .rejectEmptyString()
    .refine

  import MetricsApi._

  case class TopicInfo(name: String,
                       numberOfPartitions: Int,
                       numberOfReplications: Short,
                       brokerClusterName: String,
                       metrics: Metrics,
                       lastModified: Long)
      extends Data {
    override def id: String = name
    override def kind: String = "topic"
  }

  implicit val TOPIC_INFO_FORMAT: RootJsonFormat[TopicInfo] = new RootJsonFormat[TopicInfo] {
    private[this] val format = jsonFormat6(TopicInfo)
    override def read(json: JsValue): TopicInfo = format.read(json)
    override def write(obj: TopicInfo): JsValue = JsObject(
      // TODO: remove the id
      format.write(obj).asJsObject.fields ++ Map("id" -> JsString(obj.id)))
  }

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {
    def name(name: String): Request
    @Optional("server will match a broker cluster for you if the bk name is ignored")
    def brokerClusterName(brokerClusterName: String): Request
    @Optional("default value is DEFAULT_NUMBER_OF_PARTITIONS")
    def numberOfPartitions(numberOfPartitions: Int): Request
    @Optional("default value is DEFAULT_NUMBER_OF_REPLICATIONS")
    def numberOfReplications(numberOfReplications: Short): Request

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[TopicInfo]

    /**
      * generate the PUT request
      * @param executionContext thread pool
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[TopicInfo]
  }

  class Access private[v0] extends Access2[TopicInfo](TOPICS_PREFIX_PATH) {
    def request: Request = new Request {
      private[this] var name: String = _
      private[this] var brokerClusterName: Option[String] = None
      private[this] var numberOfPartitions: Option[Int] = None
      private[this] var numberOfReplications: Option[Short] = None
      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }

      override def brokerClusterName(brokerClusterName: String): Request = {
        this.brokerClusterName = Some(CommonUtils.requireNonEmpty(brokerClusterName))
        this
      }

      override def numberOfPartitions(numberOfPartitions: Int): Request = {
        this.numberOfPartitions = Some(CommonUtils.requirePositiveInt(numberOfPartitions))
        this
      }

      override def numberOfReplications(numberOfReplications: Short): Request = {
        this.numberOfReplications = Some(CommonUtils.requirePositiveShort(numberOfReplications))
        this
      }

      override def create()(implicit executionContext: ExecutionContext): Future[TopicInfo] =
        exec.post[Creation, TopicInfo, ErrorApi.Error](
          _url,
          Creation(
            name = CommonUtils.requireNonEmpty(name),
            brokerClusterName = brokerClusterName,
            numberOfPartitions = numberOfPartitions.getOrElse(DEFAULT_NUMBER_OF_PARTITIONS),
            numberOfReplications = numberOfReplications.getOrElse(DEFAULT_NUMBER_OF_REPLICATIONS)
          )
        )
      override def update()(implicit executionContext: ExecutionContext): Future[TopicInfo] =
        exec.put[Update, TopicInfo, ErrorApi.Error](
          s"${_url}/${CommonUtils.requireNonEmpty(name)}",
          Update(
            brokerClusterName = brokerClusterName,
            numberOfPartitions = numberOfPartitions,
            numberOfReplications = numberOfReplications
          )
        )
    }
  }

  def access: Access = new Access
}
