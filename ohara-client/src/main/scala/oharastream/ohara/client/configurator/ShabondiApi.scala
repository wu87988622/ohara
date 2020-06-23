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

package oharastream.ohara.client.configurator

import java.time.{Duration => JDuration}
import java.util.Objects

import oharastream.ohara.client.configurator.QueryRequest
import oharastream.ohara.client.configurator.ClusterAccess.Query
import oharastream.ohara.client.configurator.MetricsApi.Metrics
import oharastream.ohara.common.annotations.Optional
import oharastream.ohara.common.setting.{ObjectKey, SettingDef, TopicKey}
import oharastream.ohara.common.util.CommonUtils
import oharastream.ohara.shabondi.{ShabondiDefinitions, ShabondiSink, ShabondiSource}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

object ShabondiApi {
  val KIND: String = SettingDef.Reference.SHABONDI.name().toLowerCase

  @deprecated(message = s"replaced by $KIND", since = "0.11.0")
  val SHABONDI_PREFIX_PATH = "shabondis"

  val SHABONDI_SOURCE_CLASS: Class[ShabondiSource] = classOf[ShabondiSource]
  val SHABONDI_SOURCE_CLASS_NAME: String           = SHABONDI_SOURCE_CLASS.getName
  val SHABONDI_SINK_CLASS: Class[ShabondiSink]     = classOf[ShabondiSink]
  val SHABONDI_SINK_CLASS_NAME: String             = SHABONDI_SINK_CLASS.getName

  val IMAGE_NAME_DEFAULT: String = ShabondiDefinitions.IMAGE_NAME_DEFAULT

  def ALL_DEFINITIONS: Seq[SettingDef]        = (SOURCE_ALL_DEFINITIONS ++ SINK_ALL_DEFINITIONS).distinct
  def SOURCE_ALL_DEFINITIONS: Seq[SettingDef] = ShabondiDefinitions.sourceDefinitions
  def SINK_ALL_DEFINITIONS: Seq[SettingDef]   = ShabondiDefinitions.sinkDefinitions

  final case class ShabondiClusterInfo(
    settings: Map[String, JsValue],
    aliveNodes: Set[String],
    state: Option[ClusterState],
    error: Option[String],
    nodeMetrics: Map[String, Metrics],
    override val lastModified: Long
  ) extends ClusterInfo
      with Metricsable {
    private[this] implicit def creation(settings: Map[String, JsValue]): ShabondiClusterCreation =
      new ShabondiClusterCreation(settings)
    override def kind: String       = KIND
    override def ports: Set[Int]    = settings.ports
    def shabondiClass: String       = settings.shabondiClass
    def clientPort: Int             = settings.clientPort
    def endpoint: String            = settings.endpoint
    def brokerClusterKey: ObjectKey = settings.brokerClusterKey

    def sourceToTopics: Set[TopicKey] = settings.sourceToTopics
    def sinkFromTopics: Set[TopicKey] = settings.sinkFromTopics

    override def raw: Map[String, JsValue] = SHABONDI_CLUSTER_INFO_JSON_FORMAT.write(this).asJsObject.fields
  }

  final class ShabondiClusterCreation(val raw: Map[String, JsValue]) extends ClusterCreation {
    private val updating         = new ShabondiClusterUpdating(noJsNull(raw))
    override def ports: Set[Int] = Set(clientPort, jmxPort)

    def shabondiClass: String = updating.shabondiClass.get

    /**
      * a helper to fetch the related definitions.
      * @return definitions to this shabondi type
      */
    def definitions: Seq[SettingDef] =
      if (shabondiClass == SHABONDI_SOURCE_CLASS_NAME) SOURCE_ALL_DEFINITIONS
      else if (shabondiClass == SHABONDI_SINK_CLASS_NAME) SOURCE_ALL_DEFINITIONS
      else throw DeserializationException(s"$shabondiClass is NOT supported")

    def clientPort: Int               = updating.clientPort.get
    def endpoint: String              = updating.endpoint.get
    def brokerClusterKey: ObjectKey   = updating.brokerClusterKey.get
    def sourceToTopics: Set[TopicKey] = updating.sourceToTopics.getOrElse(null)
    def sinkFromTopics: Set[TopicKey] = updating.sinkFromTopics.getOrElse(null)
  }

  final class ShabondiClusterUpdating(val settings: Map[String, JsValue]) extends ClusterUpdating {
    import ShabondiDefinitions._
    def shabondiClass: Option[String] = noJsNull(settings).get(SHABONDI_CLASS_DEFINITION.key).map(_.convertTo[String])
    def clientPort: Option[Int]       = noJsNull(settings).get(CLIENT_PORT_DEFINITION.key).map(_.convertTo[Int])
    def endpoint: Option[String]      = noJsNull(settings).get(ENDPOINT_DEFINITION.key).map(_.convertTo[String])
    def brokerClusterKey: Option[ObjectKey] =
      noJsNull(settings).get(BROKER_CLUSTER_KEY_DEFINITION.key).map(_.convertTo[ObjectKey])
    def sourceToTopics: Option[Set[TopicKey]] =
      noJsNull(settings).get(SOURCE_TO_TOPICS_DEFINITION.key).map(_.convertTo[Set[TopicKey]])
    def sinkFromTopics: Option[Set[TopicKey]] =
      noJsNull(settings).get(SINK_FROM_TOPICS_DEFINITION.key).map(_.convertTo[Set[TopicKey]])
  }

  implicit val SHABONDI_CLUSTER_INFO_JSON_FORMAT: JsonRefiner[ShabondiClusterInfo] =
    JsonRefinerBuilder[ShabondiClusterInfo]
      .format(new RootJsonFormat[ShabondiClusterInfo] {
        private[this] val format                              = jsonFormat6(ShabondiClusterInfo)
        override def write(obj: ShabondiClusterInfo): JsValue = flattenSettings(format.write(obj).asJsObject)
        override def read(json: JsValue): ShabondiClusterInfo = format.read(extractSetting(json.asJsObject))
      })
      .build

  implicit val SHABONDI_CLUSTER_CREATION_JSON_FORMAT: JsonRefiner[ShabondiClusterCreation] =
    rulesOfCreation[ShabondiClusterCreation](
      new RootJsonFormat[ShabondiClusterCreation] {
        override def write(obj: ShabondiClusterCreation): JsValue = JsObject(noJsNull(obj.raw))
        override def read(json: JsValue): ShabondiClusterCreation = new ShabondiClusterCreation(json.asJsObject.fields)
      },
      ShabondiDefinitions.basicDefinitions
    )

  implicit val SHABONDI_CLUSTER_UPDATING_JSON_FORMAT: JsonRefiner[ShabondiClusterUpdating] =
    rulesOfUpdating[ShabondiClusterUpdating](
      new RootJsonFormat[ShabondiClusterUpdating] {
        override def write(obj: ShabondiClusterUpdating): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): ShabondiClusterUpdating = new ShabondiClusterUpdating(json.asJsObject.fields)
      }
    )

  trait Request extends ClusterRequest {
    import ShabondiDefinitions._

    def shabondiClass(className: String): Request.this.type =
      setting(SHABONDI_CLASS_DEFINITION.key, JsString(className))

    def brokers(brokers: String): Request.this.type =
      setting(BROKERS_DEFINITION.key, JsString(brokers))

    def clientPort(port: Int): Request.this.type =
      setting(CLIENT_PORT_DEFINITION.key, JsNumber(CommonUtils.requireBindPort(port)))

    def imageName(imageName: String): Request.this.type =
      setting(IMAGE_NAME_DEFINITION.key, JsString(imageName))

    @Optional("the default port is random")
    def jmxPort(jmxPort: Int): Request.this.type =
      setting(JMX_PORT_DEFINITION.key(), JsNumber(CommonUtils.requireBindPort(jmxPort)))

    def brokerClusterKey(brokerClusterKey: ObjectKey): Request.this.type =
      setting(BROKER_CLUSTER_KEY_DEFINITION.key, OBJECT_KEY_FORMAT.write(Objects.requireNonNull(brokerClusterKey)))

    def sourceToTopics(topicKeys: Set[TopicKey]): Request.this.type =
      setting(SOURCE_TO_TOPICS_DEFINITION.key, JsArray(topicKeys.map(TOPIC_KEY_FORMAT.write).toVector))

    def sinkFromTopics(topicKeys: Set[TopicKey]): Request.this.type =
      setting(SINK_FROM_TOPICS_DEFINITION.key, JsArray(topicKeys.map(TOPIC_KEY_FORMAT.write).toVector))

    def sinkPollTimeout(duration: JDuration): Request.this.type = {
      setting(SINK_POLL_TIMEOUT_DEFINITION.key, JsString(duration.toMillis.toString + " milliseconds"))
    }

    def creation: ShabondiClusterCreation = {
      val jsValue = SHABONDI_CLUSTER_CREATION_JSON_FORMAT.write(new ShabondiClusterCreation(noJsNull(settings.toMap)))
      SHABONDI_CLUSTER_CREATION_JSON_FORMAT.read(jsValue)
    }

    /**
      * for testing only
      * @return the payload of update
      */
    private[configurator] final def updating: ShabondiClusterUpdating = {
      val jsValue = SHABONDI_CLUSTER_UPDATING_JSON_FORMAT.write(new ShabondiClusterUpdating(noJsNull(settings.toMap)))
      SHABONDI_CLUSTER_UPDATING_JSON_FORMAT.read(jsValue)
    }
  }

  trait ExecutableRequest extends Request {
    def create()(implicit executionContext: ExecutionContext): Future[ShabondiClusterInfo]
    def update()(implicit executionContext: ExecutionContext): Future[ShabondiClusterInfo]
  }

  final class Access private[ShabondiApi]
      extends ClusterAccess[ShabondiClusterCreation, ShabondiClusterUpdating, ShabondiClusterInfo](KIND) {
    override def query: Query[ShabondiClusterInfo] = new Query[ShabondiClusterInfo] {
      override protected def doExecute(
        request: QueryRequest
      )(implicit executionContext: ExecutionContext): Future[Seq[ShabondiClusterInfo]] = {
        list(request)
      }
    }

    def request: ExecutableRequest = new ExecutableRequest {
      override def create()(implicit executionContext: ExecutionContext): Future[ShabondiClusterInfo] =
        post(creation)

      override def update()(implicit executionContext: ExecutionContext): Future[ShabondiClusterInfo] =
        put(key, updating)
    }
  }

  def access: Access = new Access
}
