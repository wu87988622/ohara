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

import com.island.ohara.client.configurator.QueryRequest
import com.island.ohara.client.configurator.v0.ClusterAccess.Query
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.setting.{ObjectKey, SettingDef, TopicKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.streams.config.StreamDefUtils
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
object StreamApi {
  val STREAM_PREFIX_PATH: String = "streams"

  /**
    * container name is controlled by streamRoute, the service name here use six words was ok.
    */
  val STREAM_SERVICE_NAME: String = STREAM_PREFIX_PATH

  def DEFINITIONS: Seq[SettingDef] = StreamDefUtils.DEFAULT.asScala

  /**
    * Stream Docker Image name
    */
  final val IMAGE_NAME_DEFAULT: String = StreamDefUtils.IMAGE_NAME_DEFINITION.defaultString()

  final class Creation(val settings: Map[String, JsValue]) extends ClusterCreation {
    private[this] implicit def update(settings: Map[String, JsValue]): Updating = new Updating(noJsNull(settings))
    // the name and group fields are used to identify zookeeper cluster object
    // we should give them default value in JsonRefiner
    override def name: String  = settings.name.get
    override def group: String = settings.group.get
    // helper method to get the key
    private[ohara] def key: ObjectKey = ObjectKey.of(group, name)

    /**
      * Convert all json value to plain string. It keeps the json format but all stuff are in string.
      */
    def plain: Map[String, String] = noJsNull(settings).map {
      case (k, v) =>
        k -> (v match {
          case JsString(value) => value
          case _               => v.toString()
        })
    }

    def brokerClusterKey: ObjectKey = settings.brokerClusterKey.get

    override def imageName: String = settings.imageName.get

    def className: Option[String] = settings.className

    override def nodeNames: Set[String] = settings.nodeNames.get

    override def tags: Map[String, JsValue] = settings.tags.get

    override def ports: Set[Int] = Set(jmxPort)

    def jarKey: ObjectKey = settings.jarKey.get

    private[ohara] def connectionProps: String = settings.connectionProps.get

    def jmxPort: Int = settings.jmxPort.get

    def fromTopicKeys: Set[TopicKey] = settings.fromTopicKeys.get
    def toTopicKeys: Set[TopicKey]   = settings.toTopicKeys.get
  }
  implicit val STREAM_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    rulesOfCreation[Creation](
      new RootJsonFormat[Creation] {
        override def write(obj: Creation): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Creation = new Creation(json.asJsObject.fields)
      },
      DEFINITIONS
    )

  final class Updating(val settings: Map[String, JsValue]) extends ClusterUpdating {
    // We use the update parser to get the name and group
    private[StreamApi] def name: Option[String]  = noJsNull(settings).get(NAME_KEY).map(_.convertTo[String])
    private[StreamApi] def group: Option[String] = noJsNull(settings).get(GROUP_KEY).map(_.convertTo[String])

    def brokerClusterKey: Option[ObjectKey] =
      noJsNull(settings).get(StreamDefUtils.BROKER_CLUSTER_KEY_DEFINITION.key()).map(_.convertTo[ObjectKey])

    override def imageName: Option[String] =
      noJsNull(settings).get(StreamDefUtils.IMAGE_NAME_DEFINITION.key()).map(_.convertTo[String])

    def className: Option[String] =
      noJsNull(settings).get(StreamDefUtils.CLASS_NAME_DEFINITION.key()).map(_.convertTo[String])

    def jarKey: Option[ObjectKey] =
      noJsNull(settings).get(StreamDefUtils.JAR_KEY_DEFINITION.key()).map(OBJECT_KEY_FORMAT.read)

    private[StreamApi] def connectionProps: Option[String] =
      noJsNull(settings).get(StreamDefUtils.BROKER_DEFINITION.key()).map(_.convertTo[String])

    def jmxPort: Option[Int] = noJsNull(settings).get(StreamDefUtils.JMX_PORT_DEFINITION.key()).map(_.convertTo[Int])

    def fromTopicKeys: Option[Set[TopicKey]] =
      noJsNull(settings).get(StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key()).map(_.convertTo[Set[TopicKey]])

    def toTopicKeys: Option[Set[TopicKey]] =
      noJsNull(settings).get(StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key()).map(_.convertTo[Set[TopicKey]])

    override def nodeNames: Option[Set[String]] =
      noJsNull(settings).get(StreamDefUtils.NODE_NAMES_DEFINITION.key()).map(_.convertTo[Seq[String]].toSet)

    override def tags: Option[Map[String, JsValue]] =
      noJsNull(settings).get(StreamDefUtils.TAGS_DEFINITION.key()).map {
        case s: JsObject => s.fields
        case other: JsValue =>
          throw new IllegalArgumentException(s"the type of tags should be JsObject, actual type is ${other.getClass}")
      }
  }
  implicit val STREAM_UPDATING_JSON_FORMAT: OharaJsonFormat[Updating] =
    rulesOfUpdating[Updating](
      new RootJsonFormat[Updating] {
        override def write(obj: Updating): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Updating = new Updating(json.asJsObject.fields)
      }
    )

  class StreamClusterStatus(
    val group: String,
    val name: String,
    val aliveNodes: Set[String],
    val state: Option[String],
    val error: Option[String]
  ) extends ClusterStatus

  /**
    * The Stream Cluster Information stored in configurator
    *
    * @param settings stream key-value pair settings
    * @param aliveNodes alive node list of the running containers from this cluster
    * @param state the state of stream (stopped stream does not have this field)
    * @param error the error message if the state was failed to fetch
    * @param metrics the metrics bean
    * @param lastModified this data change time
    */
  final case class StreamClusterInfo(
    settings: Map[String, JsValue],
    aliveNodes: Set[String],
    state: Option[String],
    error: Option[String],
    metrics: Metrics,
    lastModified: Long
  ) extends ClusterInfo {
    /**
      * update the runtime information for this cluster info
      * @param status runtime information
      * @return a updated cluster info
      */
    def update(status: StreamClusterStatus): StreamClusterInfo = copy(
      aliveNodes = status.aliveNodes,
      state = status.state,
      error = status.error,
      lastModified = CommonUtils.current()
    )

    /**
      * reuse the parser from Creation.
      * @param settings settings
      * @return creation
      */
    private[this] implicit def creation(settings: Map[String, JsValue]): Creation = new Creation(noJsNull(settings))

    override def name: String               = settings.name
    override def group: String              = settings.group
    override def kind: String               = STREAM_SERVICE_NAME
    override def ports: Set[Int]            = settings.ports
    override def tags: Map[String, JsValue] = settings.tags

    def imageName: String = settings.imageName
    def className: String = settings.className.get

    /**
      * Return the key of explicit value. Otherwise, return the key of jar info.
      * Normally, the key should be equal to jar info
      * @return key of jar
      */
    def jarKey: ObjectKey = settings.jarKey

    def brokerClusterKey: ObjectKey  = settings.brokerClusterKey
    def fromTopicKeys: Set[TopicKey] = settings.fromTopicKeys
    def toTopicKeys: Set[TopicKey]   = settings.toTopicKeys
    def jmxPort: Int                 = settings.jmxPort

    def nodeNames: Set[String] = settings.nodeNames

    def connectionProps: String = settings.connectionProps
  }

  private[ohara] implicit val STREAM_CLUSTER_INFO_JSON_FORMAT: OharaJsonFormat[StreamClusterInfo] =
    JsonRefiner[StreamClusterInfo]
      .format(new RootJsonFormat[StreamClusterInfo] {
        private[this] val format                            = jsonFormat6(StreamClusterInfo)
        override def read(json: JsValue): StreamClusterInfo = format.read(json)
        override def write(obj: StreamClusterInfo): JsValue =
          JsObject(noJsNull(format.write(obj).asJsObject.fields))
      })
      .refine

  /**
    * used to generate the payload and url for POST/PUT request.
    * this request is extended by collie also so it is public than sealed.
    */
  trait Request extends ClusterRequest {
    @Optional("if you don't define the class, configurator will seek all files to find available one")
    def className(className: String): Request.this.type =
      setting(StreamDefUtils.CLASS_NAME_DEFINITION.key(), JsString(className))
    def jarKey(jarKey: ObjectKey): Request.this.type =
      setting(StreamDefUtils.JAR_KEY_DEFINITION.key(), ObjectKey.toJsonString(jarKey).parseJson)
    def fromTopicKey(fromTopicKey: TopicKey): Request.this.type = fromTopicKeys(Set(fromTopicKey))
    def fromTopicKeys(fromTopicKeys: Set[TopicKey]): Request.this.type =
      setting(
        StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key(),
        JsArray(fromTopicKeys.map(TOPIC_KEY_FORMAT.write).toVector)
      )
    def toTopicKey(toTopicKey: TopicKey): Request.this.type = toTopicKeys(Set(toTopicKey))
    def toTopicKeys(toTopicKeys: Set[TopicKey]): Request.this.type =
      setting(StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key(), JsArray(toTopicKeys.map(TOPIC_KEY_FORMAT.write).toVector))
    @Optional("server picks up a broker cluster for you if broker cluster name is empty")
    def brokerClusterKey(brokerClusterKey: ObjectKey): Request.this.type =
      setting(
        StreamDefUtils.BROKER_CLUSTER_KEY_DEFINITION.key(),
        OBJECT_KEY_FORMAT.write(Objects.requireNonNull(brokerClusterKey))
      )
    @Optional("the default port is random")
    def jmxPort(jmxPort: Int): Request.this.type =
      setting(StreamDefUtils.JMX_PORT_DEFINITION.key(), JsNumber(CommonUtils.requireConnectionPort(jmxPort)))

    @Optional("default value is empty array in creation and None in update")
    def tags(tags: Map[String, JsValue]): Request.this.type =
      setting(StreamDefUtils.TAGS_DEFINITION.key(), JsObject(tags))

    @Optional("default connection props is generated by broker cluster name")
    def connectionProps(connectionProps: String): Request.this.type =
      setting(StreamDefUtils.BROKER_DEFINITION.key(), JsString(connectionProps))

    /**
      * stream app accept empty nodes, and the basic request reject the empty array.
      * Overriding this setter avoids the exception.
      */
    override def nodeNames(nodeNames: Set[String]): Request.this.type =
      setting(StreamDefUtils.NODE_NAMES_DEFINITION.key(), JsArray(nodeNames.map(JsString(_)).toVector))

    /**
      * Creation instance includes many useful parsers for custom settings so we open it to code with a view to reusing
      * those convenient parsers.
      * @return the payload of creation
      */
    final def creation: Creation =
      // auto-complete the creation via our refiner
      STREAM_CREATION_JSON_FORMAT.read(STREAM_CREATION_JSON_FORMAT.write(new Creation(noJsNull(settings.toMap))))

    /**
      * for testing only
      * @return the payload of update
      */
    @VisibleForTesting
    private[v0] final def updating: Updating =
      // auto-complete the update via our refiner
      STREAM_UPDATING_JSON_FORMAT.read(STREAM_UPDATING_JSON_FORMAT.write(new Updating(noJsNull(settings.toMap))))
  }

  /**
    * similar to Request but it has execution methods.
    *
    */
  sealed trait ExecutableRequest extends Request {
    def create()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo]
    def update()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo]
  }

  final class Access private[StreamApi]
      extends ClusterAccess[Creation, Updating, StreamClusterInfo](STREAM_PREFIX_PATH) {
    override def query: Query[StreamClusterInfo] = new Query[StreamClusterInfo] {
      override protected def doExecute(request: QueryRequest)(
        implicit executionContext: ExecutionContext
      ): Future[Seq[StreamClusterInfo]] = list(request)
    }

    def request: ExecutableRequest = new ExecutableRequest {
      override def create()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] = post(creation)

      override def update()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] =
        put(ObjectKey.of(updating.group.getOrElse(GROUP_DEFAULT), updating.name.get), updating)
    }
  }

  def access: Access = new Access
}
