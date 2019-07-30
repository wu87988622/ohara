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
import java.util.Objects

import com.island.ohara.client.Enum
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.TopicKey
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object TopicApi {

  val DEFAULT_NUMBER_OF_PARTITIONS: Int = 1
  val DEFAULT_NUMBER_OF_REPLICATIONS: Short = 1
  val TOPICS_PREFIX_PATH: String = "topics"

  private[this] val CONFIGS_KEY = "configs"
  private[this] val NUMBER_OF_PARTITIONS_KEY = "numberOfPartitions"
  private[this] val NUMBER_OF_REPLICATIONS_KEY = "numberOfReplications"

  case class Update private[TopicApi] (brokerClusterName: Option[String],
                                       numberOfPartitions: Option[Int],
                                       numberOfReplications: Option[Short],
                                       configs: Option[Map[String, String]],
                                       tags: Option[Map[String, JsValue]])
  implicit val TOPIC_UPDATE_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update].format(jsonFormat5(Update)).rejectEmptyString().refine

  case class Creation private[TopicApi] (group: String,
                                         name: String,
                                         brokerClusterName: Option[String],
                                         numberOfPartitions: Int,
                                         numberOfReplications: Short,
                                         configs: Map[String, String],
                                         tags: Map[String, JsValue])
      extends CreationRequest {
    def key: TopicKey = TopicKey.of(group, name)
  }

  implicit val TOPIC_CREATION_FORMAT: OharaJsonFormat[Creation] = JsonRefiner[Creation]
    .format(jsonFormat7(Creation))
    .stringRestriction(Set(Data.GROUP_KEY, Data.NAME_KEY))
    .withNumber()
    .withCharset()
    .withDot()
    .withDash()
    .withUnderLine()
    .toRefiner
    .nullToInt(NUMBER_OF_PARTITIONS_KEY, DEFAULT_NUMBER_OF_REPLICATIONS)
    .nullToInt(NUMBER_OF_REPLICATIONS_KEY, DEFAULT_NUMBER_OF_REPLICATIONS)
    .rejectEmptyString()
    .nullToString(Data.GROUP_KEY, () => Data.GROUP_DEFAULT)
    .nullToString(Data.NAME_KEY, () => CommonUtils.randomString(10))
    .nullToEmptyObject(CONFIGS_KEY)
    .nullToEmptyObject(Data.TAGS_KEY)
    .refine

  import MetricsApi._

  abstract sealed class TopicState(val name: String) extends Serializable
  object TopicState extends Enum[TopicState] {
    case object RUNNING extends TopicState("RUNNING")
  }
  implicit val TOPIC_STATE_FORMAT: RootJsonFormat[TopicState] = new RootJsonFormat[TopicState] {
    override def read(json: JsValue): TopicState = TopicState.forName(json.convertTo[String].toUpperCase)
    override def write(obj: TopicState): JsValue = JsString(obj.name)
  }

  case class TopicInfo(group: String,
                       name: String,
                       numberOfPartitions: Int,
                       numberOfReplications: Short,
                       brokerClusterName: String,
                       metrics: Metrics,
                       state: Option[TopicState],
                       lastModified: Long,
                       configs: Map[String, String],
                       tags: Map[String, JsValue])
      extends Data {
    override def key: TopicKey = TopicKey.of(group, name)
    override def kind: String = "topic"

    /**
      * kafka topic does not support to group topic so we salt the group with name.
      * @return topic name for kafka
      */
    def topicNameOnKafka: String = key.topicNameOnKafka
  }

  implicit val TOPIC_INFO_FORMAT: RootJsonFormat[TopicInfo] = jsonFormat10(TopicInfo)

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {
    @Optional("default group is \"default\"")
    def group(group: String): Request

    @Optional("default name is a random string. But it is required in updating")
    def name(name: String): Request

    @Optional("server will match a broker cluster for you if the bk name is ignored")
    def brokerClusterName(brokerClusterName: String): Request

    @Optional("default value is DEFAULT_NUMBER_OF_PARTITIONS")
    def numberOfPartitions(numberOfPartitions: Int): Request

    @Optional("default value is DEFAULT_NUMBER_OF_REPLICATIONS")
    def numberOfReplications(numberOfReplications: Short): Request

    @Optional("default configs is empty array")
    def configs(configs: Map[String, String]): Request

    @Optional("default value is empty array")
    def tags(tags: Map[String, JsValue]): Request

    private[v0] def creation: Creation

    private[v0] def update: Update

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

  class Access private[v0] extends com.island.ohara.client.configurator.v0.Access[TopicInfo](TOPICS_PREFIX_PATH) {
    def start(key: TopicKey)(implicit executionContext: ExecutionContext): Future[Unit] = put(key, START_COMMAND)
    def stop(key: TopicKey)(implicit executionContext: ExecutionContext): Future[Unit] = put(key, STOP_COMMAND)
    def request: Request = new Request {
      private[this] var group: String = Data.GROUP_DEFAULT
      private[this] var name: String = _
      private[this] var brokerClusterName: Option[String] = None
      private[this] var numberOfPartitions: Option[Int] = None
      private[this] var numberOfReplications: Option[Short] = None
      private[this] var configs: Map[String, String] = _
      private[this] var tags: Map[String, JsValue] = _

      override def group(group: String): Request = {
        this.group = CommonUtils.requireNonEmpty(group)
        this
      }

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

      override def configs(configs: Map[String, String]): Request = {
        this.configs = Objects.requireNonNull(configs)
        this
      }

      override def tags(tags: Map[String, JsValue]): Request = {
        this.tags = Objects.requireNonNull(tags)
        this
      }

      override private[v0] def creation: Creation = Creation(
        group = CommonUtils.requireNonEmpty(group),
        name = if (CommonUtils.isEmpty(name)) CommonUtils.randomString(10) else name,
        brokerClusterName = brokerClusterName,
        numberOfPartitions = numberOfPartitions.getOrElse(DEFAULT_NUMBER_OF_PARTITIONS),
        numberOfReplications = numberOfReplications.getOrElse(DEFAULT_NUMBER_OF_REPLICATIONS),
        configs = if (configs == null) Map.empty else configs,
        tags = if (tags == null) Map.empty else tags
      )

      override private[v0] def update: Update = Update(
        brokerClusterName = brokerClusterName,
        numberOfPartitions = numberOfPartitions,
        numberOfReplications = numberOfReplications,
        configs = Option(configs),
        tags = Option(tags)
      )

      override def create()(implicit executionContext: ExecutionContext): Future[TopicInfo] =
        exec.post[Creation, TopicInfo, ErrorApi.Error](
          _url,
          creation
        )
      override def update()(implicit executionContext: ExecutionContext): Future[TopicInfo] =
        exec.put[Update, TopicInfo, ErrorApi.Error](
          _url(TopicKey.of(group, name)),
          update
        )
    }
  }

  def access: Access = new Access
}
