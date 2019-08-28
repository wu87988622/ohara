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
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.client.Enum
import com.island.ohara.client.configurator.Data
import com.island.ohara.client.kafka.TopicAdmin.PartitionInfo
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.setting.{SettingDef, TopicKey}
import com.island.ohara.common.setting.SettingDef.Type
import com.island.ohara.common.util.CommonUtils
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.record.Records
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object TopicApi {

  /**
    * The default value of group for this API.
    */
  val GROUP_DEFAULT: String = com.island.ohara.client.configurator.v0.GROUP_DEFAULT
  val DEFAULT_NUMBER_OF_PARTITIONS: Int = 1
  val DEFAULT_NUMBER_OF_REPLICATIONS: Short = 1
  val TOPICS_PREFIX_PATH: String = "topics"

  val NUMBER_OF_PARTITIONS_KEY = "numberOfPartitions"
  val NUMBER_OF_REPLICATIONS_KEY = "numberOfReplications"
  val BROKER_CLUSTER_NAME_KEY = "brokerClusterName"
  val TAGS_KEY = "tags"

  /**
    * the config with this group is mapped to kafka's custom config. Kafka divide configs into two parts.
    * 1) required configs (number of partitions and number of replications)
    * 2) custom configs (those config must be able to convert to string)
    *
    * Furthermore, kafka forbids us to put required configs to custom configs. Hence, we have to mark the custom config
    * in order to filter the custom from settings (see Creation).
    */
  private[this] val GROUP_TO_EXTRA_CONFIG = "extra"

  /**
    * list the custom configs of topic. It is useful to developers who long for controlling the topic totally.
    */
  val TOPIC_DEFINITIONS: Seq[SettingDef] = {
    val coreGroup = "core"
    val count = new AtomicInteger(0)
    def toDefWithDefault(key: String, group: String, doc: String, valueType: Type, default: Any): SettingDef =
      toDef(key, group, doc, valueType, Some(default))
    def toDefWithoutDefault(key: String, group: String, doc: String, valueType: Type): SettingDef =
      toDef(key, group, doc, valueType, None)
    def toDef(key: String, group: String, doc: String, valueType: Type, default: Option[Any]): SettingDef = {
      val builder = SettingDef
        .builder()
        .key(key)
        .displayName(key)
        .documentation(doc)
        .group(group)
        .orderInGroup(count.getAndIncrement())
        .valueType(valueType)
      if (default.isDefined) builder.optional(default.get.toString) else builder.optional()
      builder.build()
    }
    Seq(
      //-----------[kafka prerequisite]-----------
      toDefWithDefault(key = GROUP_KEY,
                       group = coreGroup,
                       doc = "the group of this topic",
                       valueType = Type.STRING,
                       default = GROUP_DEFAULT),
      toDefWithoutDefault(key = NAME_KEY, group = coreGroup, doc = "the name of this topic", valueType = Type.STRING),
      toDefWithDefault(key = NUMBER_OF_PARTITIONS_KEY,
                       group = coreGroup,
                       doc = "the number of partitions",
                       valueType = Type.INT,
                       default = DEFAULT_NUMBER_OF_PARTITIONS),
      toDefWithDefault(key = NUMBER_OF_REPLICATIONS_KEY,
                       group = coreGroup,
                       doc = "the number of replications",
                       valueType = Type.SHORT,
                       default = NUMBER_OF_REPLICATIONS_KEY),
      toDefWithoutDefault(key = BROKER_CLUSTER_NAME_KEY,
                          group = coreGroup,
                          valueType = Type.STRING,
                          doc = "the broker cluster to deploy this topic"),
      toDefWithoutDefault(key = TAGS_KEY, group = coreGroup, doc = "the tags to this topic", valueType = Type.TAGS),
      //-----------[kafka custom]-----------
      toDefWithDefault(key = TopicConfig.SEGMENT_BYTES_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.SEGMENT_BYTES_DOC,
                       valueType = Type.LONG,
                       default = 1 * 1024 * 1024 * 1024),
      toDefWithDefault(key = TopicConfig.SEGMENT_MS_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.SEGMENT_MS_DOC,
                       valueType = Type.LONG,
                       default = 24 * 7 * 60 * 60 * 1000L),
      toDefWithDefault(key = TopicConfig.SEGMENT_JITTER_MS_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.SEGMENT_JITTER_MS_DOC,
                       valueType = Type.LONG,
                       default = 0 * 60 * 60 * 1000L),
      toDefWithDefault(
        key = TopicConfig.SEGMENT_INDEX_BYTES_CONFIG,
        group = GROUP_TO_EXTRA_CONFIG,
        doc = TopicConfig.SEGMENT_INDEX_BYTES_DOC,
        valueType = Type.LONG,
        default = 10 * 1024 * 1024
      ),
      toDefWithDefault(
        key = TopicConfig.FLUSH_MESSAGES_INTERVAL_CONFIG,
        group = GROUP_TO_EXTRA_CONFIG,
        doc = TopicConfig.FLUSH_MESSAGES_INTERVAL_DOC,
        valueType = Type.LONG,
        default = Long.MaxValue
      ),
      toDefWithDefault(TopicConfig.FLUSH_MS_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.FLUSH_MS_DOC,
                       valueType = Type.LONG,
                       default = Long.MaxValue),
      toDefWithDefault(key = TopicConfig.RETENTION_BYTES_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.RETENTION_BYTES_DOC,
                       valueType = Type.LONG,
                       default = -1L),
      toDefWithDefault(key = TopicConfig.RETENTION_MS_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.RETENTION_MS_DOC,
                       valueType = Type.LONG,
                       default = 24 * 7 * 60 * 60 * 1000L),
      toDefWithDefault(
        key = TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
        group = GROUP_TO_EXTRA_CONFIG,
        doc = TopicConfig.MAX_MESSAGE_BYTES_DOC,
        valueType = Type.LONG,
        default = 1000000 + Records.LOG_OVERHEAD
      ),
      toDefWithDefault(key = TopicConfig.INDEX_INTERVAL_BYTES_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.INDEX_INTERVAL_BYTES_DOCS,
                       valueType = Type.INT,
                       4096),
      toDefWithDefault(TopicConfig.FILE_DELETE_DELAY_MS_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.FILE_DELETE_DELAY_MS_DOC,
                       valueType = Type.INT,
                       default = 60000),
      toDefWithDefault(
        key = TopicConfig.DELETE_RETENTION_MS_CONFIG,
        group = GROUP_TO_EXTRA_CONFIG,
        doc = TopicConfig.DELETE_RETENTION_MS_DOC,
        valueType = Type.LONG,
        default = 24 * 60 * 60 * 1000L
      ),
      toDefWithDefault(key = TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.MIN_COMPACTION_LAG_MS_DOC,
                       valueType = Type.LONG,
                       default = 0L),
      toDefWithDefault(
        key = TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG,
        group = GROUP_TO_EXTRA_CONFIG,
        doc = TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_DOC,
        valueType = Type.DOUBLE,
        default = 0.5d
      ),
      toDefWithDefault(key = TopicConfig.CLEANUP_POLICY_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.CLEANUP_POLICY_DOC,
                       valueType = Type.STRING,
                       default = "delete"),
      toDefWithDefault(
        key = TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG,
        group = GROUP_TO_EXTRA_CONFIG,
        doc = TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_DOC,
        valueType = Type.BOOLEAN,
        default = false
      ),
      toDefWithDefault(key = TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.MIN_IN_SYNC_REPLICAS_DOC,
                       valueType = Type.INT,
                       default = 1),
      toDefWithDefault(TopicConfig.COMPRESSION_TYPE_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.COMPRESSION_TYPE_DOC,
                       valueType = Type.STRING,
                       default = "producer"),
      toDefWithDefault(key = TopicConfig.PREALLOCATE_CONFIG,
                       group = GROUP_TO_EXTRA_CONFIG,
                       doc = TopicConfig.PREALLOCATE_DOC,
                       valueType = Type.BOOLEAN,
                       default = false),
      // this config impacts the available APIs so we don't expose it.
      //      toDefWithDefault(TopicConfig.MESSAGE_FORMAT_VERSION_CONFIG, TopicConfig.MESSAGE_FORMAT_VERSION_DOC, ApiVersion.latestVersion.toString),
      toDefWithDefault(
        key = TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG,
        group = GROUP_TO_EXTRA_CONFIG,
        doc = TopicConfig.MESSAGE_TIMESTAMP_TYPE_DOC,
        valueType = Type.STRING,
        default = "CreateTime"
      ),
      toDefWithDefault(
        key = TopicConfig.MESSAGE_TIMESTAMP_DIFFERENCE_MAX_MS_CONFIG,
        group = GROUP_TO_EXTRA_CONFIG,
        doc = TopicConfig.MESSAGE_TIMESTAMP_DIFFERENCE_MAX_MS_DOC,
        valueType = Type.LONG,
        default = Long.MaxValue
      ),
      toDefWithDefault(
        key = TopicConfig.MESSAGE_DOWNCONVERSION_ENABLE_CONFIG,
        group = GROUP_TO_EXTRA_CONFIG,
        doc = TopicConfig.MESSAGE_DOWNCONVERSION_ENABLE_DOC,
        valueType = Type.BOOLEAN,
        default = true
      )
    )
  }

  /**
    * only the custom definitions.
    */
  val TOPIC_CUSTOM_DEFINITIONS: Seq[SettingDef] = TOPIC_DEFINITIONS.filter(_.group() == GROUP_TO_EXTRA_CONFIG)

  case class Update private[TopicApi] (settings: Map[String, JsValue]) {
    private[this] def plain: Map[String, String] = settings.filter(_._2.isInstanceOf[JsString]).map {
      case (key, value) => key -> value.convertTo[String]
    }

    def brokerClusterName: Option[String] = plain.get(BROKER_CLUSTER_NAME_KEY)
  }

  implicit val TOPIC_UPDATE_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update]
      .format(new RootJsonFormat[Update] {
        override def read(json: JsValue): Update = Update(noJsNull(json.asJsObject.fields))
        override def write(obj: Update): JsValue = JsObject(obj.settings)
      })
      .rejectEmptyString()
      .refine

  case class Creation private[TopicApi] (settings: Map[String, JsValue]) extends CreationRequest {
    def key: TopicKey = TopicKey.of(group, name)

    private[this] def plain: Map[String, String] = settings.filter(_._2.isInstanceOf[JsString]).map {
      case (key, value) => key -> value.convertTo[String]
    }

    def brokerClusterName: Option[String] = plain.get(BROKER_CLUSTER_NAME_KEY)

    def numberOfPartitions: Int = settings(NUMBER_OF_PARTITIONS_KEY).convertTo[Int]
    def numberOfReplications: Short = settings(NUMBER_OF_REPLICATIONS_KEY).convertTo[Short]

    override def group: String = plain(GROUP_KEY)

    override def name: String = plain(NAME_KEY)

    override def tags: Map[String, JsValue] = settings(TAGS_KEY).asJsObject.fields
  }

  implicit val TOPIC_CREATION_FORMAT: OharaJsonFormat[Creation] = JsonRefiner[Creation]
    .format(new RootJsonFormat[Creation] {
      override def read(json: JsValue): Creation = Creation(noJsNull(json.asJsObject.fields))
      override def write(obj: Creation): JsValue = JsObject(obj.settings)
    })
    .stringRestriction(Set(GROUP_KEY, NAME_KEY))
    .withNumber()
    .withCharset()
    .withDot()
    .withDash()
    .withUnderLine()
    .toRefiner
    .nullToInt(NUMBER_OF_PARTITIONS_KEY, DEFAULT_NUMBER_OF_REPLICATIONS)
    .nullToInt(NUMBER_OF_REPLICATIONS_KEY, DEFAULT_NUMBER_OF_REPLICATIONS)
    .rejectEmptyString()
    .nullToString(GROUP_KEY, () => GROUP_DEFAULT)
    .nullToString(NAME_KEY, () => CommonUtils.randomString(10))
    .nullToEmptyObject(TAGS_KEY)
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

  implicit val TOPIC_PARTITION_FORMAT: RootJsonFormat[PartitionInfo] = jsonFormat6(PartitionInfo)

  case class TopicInfo(settings: Map[String, JsValue],
                       partitionInfos: Seq[PartitionInfo],
                       metrics: Metrics,
                       state: Option[TopicState],
                       lastModified: Long)
      extends Data {
    override def key: TopicKey = TopicKey.of(group, name)
    override def kind: String = "topic"

    /**
      * kafka topic does not support to group topic so we salt the group with name.
      * @return topic name for kafka
      */
    def topicNameOnKafka: String = key.topicNameOnKafka

    override def group: String = noJsNull(settings)(GROUP_KEY).convertTo[String]

    override def name: String = noJsNull(settings)(NAME_KEY).convertTo[String]

    override def tags: Map[String, JsValue] =
      noJsNull(settings).get(TAGS_KEY).map(_.asJsObject.fields).getOrElse(Map.empty)

    def brokerClusterName: String = noJsNull(settings)(BROKER_CLUSTER_NAME_KEY).convertTo[String]
    def numberOfPartitions: Int =
      noJsNull(settings).get(NUMBER_OF_PARTITIONS_KEY).map(_.convertTo[Int]).getOrElse(DEFAULT_NUMBER_OF_PARTITIONS)
    def numberOfReplications: Short = noJsNull(settings)
      .get(NUMBER_OF_REPLICATIONS_KEY)
      .map(_.convertTo[Short])
      .getOrElse(DEFAULT_NUMBER_OF_REPLICATIONS)

    /**
      * @return the custom configs. the core configs are not included
      */
    def configs: Map[String, String] = settings
      .filter {
        case (key, value) =>
          TOPIC_DEFINITIONS.filter(_.group() == GROUP_TO_EXTRA_CONFIG).exists(_.key() == key) &&
            value.isInstanceOf[JsString]
      }
      .map {
        case (key, value) => key -> value.convertTo[String]
      }

  }

  implicit val TOPIC_INFO_FORMAT: RootJsonFormat[TopicInfo] = new RootJsonFormat[TopicInfo] {
    private[this] val format = jsonFormat5(TopicInfo)
    override def read(json: JsValue): TopicInfo = format.read(json)

    override def write(obj: TopicInfo): JsValue = JsObject(
      format.write(obj).asJsObject.fields ++
        // TODO: remove this stale fields ... by chia
        Map(
          GROUP_KEY -> JsString(obj.group),
          NAME_KEY -> JsString(obj.name),
          BROKER_CLUSTER_NAME_KEY -> JsString(obj.brokerClusterName),
          NUMBER_OF_PARTITIONS_KEY -> JsNumber(obj.numberOfPartitions),
          NUMBER_OF_REPLICATIONS_KEY -> JsNumber(obj.numberOfReplications),
          TAGS_KEY -> JsObject(obj.tags)
        ))
  }

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {

    /**
      * set the group and name via key
      * @param topicKey topic key
      * @return this request
      */
    def key(topicKey: TopicKey): Request = {
      group(topicKey.group())
      name(topicKey.name())
    }

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
      // add the default value to group
      private[this] var settings: mutable.Map[String, JsValue] = new mutable.HashMap() + (GROUP_KEY -> JsString(
        GROUP_DEFAULT))

      override def group(group: String): Request =
        add(GROUP_KEY, JsString(CommonUtils.requireNonEmpty(group)))

      override def name(name: String): Request =
        add(NAME_KEY, JsString(CommonUtils.requireNonEmpty(name)))

      override def brokerClusterName(brokerClusterName: String): Request =
        add(BROKER_CLUSTER_NAME_KEY, JsString(CommonUtils.requireNonEmpty(brokerClusterName)))

      override def numberOfPartitions(numberOfPartitions: Int): Request =
        add(NUMBER_OF_PARTITIONS_KEY, JsNumber(CommonUtils.requirePositiveInt(numberOfPartitions)))

      override def numberOfReplications(numberOfReplications: Short): Request =
        add(NUMBER_OF_REPLICATIONS_KEY, JsNumber(CommonUtils.requirePositiveShort(numberOfReplications)))

      override def configs(configs: Map[String, String]): Request = {
        settings ++= configs.map {
          case (key, value) => key -> JsString(value)
        }
        this
      }

      override def tags(tags: Map[String, JsValue]): Request =
        add(TAGS_KEY, JsObject(Objects.requireNonNull(tags)))

      private[this] def add(key: String, value: JsValue): Request = {
        settings += key -> value
        this
      }

      override private[v0] def creation: Creation =
        // rewrite the creation via format since the format will auto-complete the creation
        // this make the creaion is consistent to creation sent to server
        TOPIC_CREATION_FORMAT.read(TOPIC_CREATION_FORMAT.write(Creation(settings.toMap)))

      override private[v0] def update: Update =
        // rewrite the update via format since the format will auto-complete the creation
        // this make the update is consistent to creation sent to server
        TOPIC_UPDATE_FORMAT.read(TOPIC_UPDATE_FORMAT.write(Update(settings.toMap)))

      override def create()(implicit executionContext: ExecutionContext): Future[TopicInfo] =
        exec.post[Creation, TopicInfo, ErrorApi.Error](
          url,
          creation
        )
      override def update()(implicit executionContext: ExecutionContext): Future[TopicInfo] =
        exec.put[Update, TopicInfo, ErrorApi.Error](
          url(
            TopicKey.of(settings.get(GROUP_KEY).map(_.convertTo[String]).getOrElse(GROUP_DEFAULT),
                        settings.get(NAME_KEY).map(_.convertTo[String]).orNull)),
          update
        )
    }
  }

  def access: Access = new Access
}
