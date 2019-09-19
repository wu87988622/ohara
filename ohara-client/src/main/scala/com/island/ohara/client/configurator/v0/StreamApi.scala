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
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.setting.{ObjectKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import com.island.ohara.streams.config.StreamDefUtils
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object StreamApi {

  /**
    * container name is controlled by streamRoute, the service name here use five words was ok.
    * TODO: "stream" is derpecated now. see https://github.com/oharastream/ohara/issues/2115
    */
  val STREAM_SERVICE_NAME: String = "stream"
  // TODO: TODO: "stream" is derpecated now. see https://github.com/oharastream/ohara/issues/2115
  val STREAM_PREFIX_PATH: String = STREAM_SERVICE_NAME
  val STREAMS_PREFIX_PATH: String = "streams"

  /**
    * The default value of group for this API.
    */
  val STREAM_GROUP_DEFAULT: String = com.island.ohara.client.configurator.v0.GROUP_DEFAULT

  /**
    * StreamApp Docker Image name
    */
  final val IMAGE_NAME_DEFAULT: String = s"oharastream/streamapp:${VersionUtils.VERSION}"

  final class Creation(val settings: Map[String, JsValue]) extends ClusterCreationRequest {

    private[this] implicit def update(settings: Map[String, JsValue]): Update = new Update(settings)
    // the name and group fields are used to identify zookeeper cluster object
    // we should give them default value in JsonRefiner
    override def name: String = settings.name.get
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

    def brokerClusterName: Option[String] = settings.brokerClusterName

    override def imageName: String = settings.imageName.get

    override def nodeNames: Set[String] = settings.nodeNames.get

    override def tags: Map[String, JsValue] = settings.tags.get

    override def ports: Set[Int] = Set(jmxPort)

    // TODO: the creation should carry the jar key ... by chia
    def jarKey: Option[ObjectKey] = settings.jarKey

    /**
      * exposed to StreamCollie
      */
    private[ohara] def jarInfo: Option[FileInfo] = settings.jarInfo

    private[ohara] def connectionProps: String = settings.connectionProps.get

    def jmxPort: Int = settings.jmxPort.get

    def fromTopicKeys: Set[TopicKey] = settings.fromTopicKeys.get
    def toTopicKeys: Set[TopicKey] = settings.toTopicKeys.get

    //TODO remove this field after #2288
    def instances: Option[Int] = settings.instances
  }
  implicit val STREAM_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    // TODO: reuse the global checks for streamapp ...
    // the following checkers is a part of global cluster checks.
    // We don't reuse the global checks since streamapp accept empty/null nodeNames ... by chia
    basicRulesOfKey[Creation](STREAM_GROUP_DEFAULT)
      .rejectEmptyString()
      .arrayRestriction(NODE_NAMES_KEY)
      // we use the same sub-path for "node" and "actions" urls:
      // xxx/cluster/{name}/{node}
      // xxx/cluster/{name}/[start|stop]
      // the "actions" keywords must be avoided in nodeNames parameter
      .rejectKeyword(START_COMMAND)
      .rejectKeyword(STOP_COMMAND)
      .toRefiner
      .nullToString(IMAGE_NAME_KEY, IMAGE_NAME_DEFAULT)
      .nullToEmptyObject(TAGS_KEY)
      //----------------------------------------//
      .format(new RootJsonFormat[Creation] {
        override def write(obj: Creation): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Creation = new Creation(json.asJsObject.fields)
      })
      .nullToRandomPort(StreamDefUtils.JMX_PORT_DEFINITION.key())
      //TODO remove this default value after #2288
      .nullToEmptyArray(StreamDefUtils.NODE_NAMES_DEFINITION.key())
      // TODO: we should reject the request carrying no from topics ... by chia
      .nullToEmptyArray(StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key())
      // TODO: we should reject the request carrying no to topics ... by chia
      .nullToEmptyArray(StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key())
      // restrict rules
      .requireBindPort(StreamDefUtils.JMX_PORT_DEFINITION.key())
      .requirePositiveNumber(StreamDefUtils.INSTANCES_DEFINITION.key())
      .refine

  final class Update(val settings: Map[String, JsValue]) extends ClusterUpdateRequest {
    // We use the update parser to get the name and group
    private[StreamApi] def name: Option[String] = noJsNull(settings).get(NAME_KEY).map(_.convertTo[String])
    private[StreamApi] def group: Option[String] = noJsNull(settings).get(GROUP_KEY).map(_.convertTo[String])
    def brokerClusterName: Option[String] =
      noJsNull(settings).get(StreamDefUtils.BROKER_CLUSTER_NAME_DEFINITION.key()).map(_.convertTo[String])

    override def imageName: Option[String] =
      noJsNull(settings).get(StreamDefUtils.IMAGE_NAME_DEFINITION.key()).map(_.convertTo[String])

    def jarKey: Option[ObjectKey] = jarInfo
      .map(_.key)
      .orElse(noJsNull(settings).get(StreamDefUtils.JAR_KEY_DEFINITION.key()).map(OBJECT_KEY_FORMAT.read))

    /**
      * Normally, Update request should not carry the jar info since the jar info is returned by file store according
      * to input jar key. Hence, this method is not public and it is opened to this scope only.
      * @return jar info
      */
    private[StreamApi] def jarInfo: Option[FileInfo] =
      noJsNull(settings).get(StreamDefUtils.JAR_INFO_DEFINITION.key()).map(FileInfoApi.FILE_INFO_JSON_FORMAT.read)

    private[StreamApi] def connectionProps: Option[String] =
      noJsNull(settings).get(StreamDefUtils.BROKER_DEFINITION.key()).map(_.convertTo[String])

    def jmxPort: Option[Int] = noJsNull(settings).get(StreamDefUtils.JMX_PORT_DEFINITION.key()).map(_.convertTo[Int])

    def fromTopicKeys: Option[Set[TopicKey]] =
      noJsNull(settings).get(StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key()).map(_.convertTo[Set[TopicKey]])

    def toTopicKeys: Option[Set[TopicKey]] =
      noJsNull(settings).get(StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key()).map(_.convertTo[Set[TopicKey]])

    override def nodeNames: Option[Set[String]] =
      noJsNull(settings).get(StreamDefUtils.NODE_NAMES_DEFINITION.key()).map(_.convertTo[Seq[String]].toSet)

    //TODO remove this field after #2288
    def instances: Option[Int] =
      noJsNull(settings).get(StreamDefUtils.INSTANCES_DEFINITION.key()).map(_.convertTo[Int])

    override def tags: Option[Map[String, JsValue]] =
      noJsNull(settings).get(StreamDefUtils.TAGS_DEFINITION.key()).map {
        case s: JsObject => s.fields
        case other: JsValue =>
          throw new IllegalArgumentException(s"the type of tags should be JsObject, actual type is ${other.getClass}")
      }
  }
  implicit val STREAM_UPDATE_JSON_FORMAT: OharaJsonFormat[Update] =
    basicRulesOfUpdate[Update]
      .format(new RootJsonFormat[Update] {
        override def write(obj: Update): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Update = new Update(json.asJsObject.fields)
      })
      .requireBindPort(StreamDefUtils.JMX_PORT_DEFINITION.key())
      .requirePositiveNumber(StreamDefUtils.INSTANCES_DEFINITION.key())
      .refine

  implicit val DEFINITION_JSON_FORMAT: OharaJsonFormat[Definition] = Definition.DEFINITION_JSON_FORMAT

  /**
    * The Stream Cluster Information stored in configurator
    *
    * @param settings streamApp key-value pair settings
    * @param definition the core and custom definition that defined in jar
    * @param deadNodes dead node list of the exited containers from this cluster
    * @param state the state of streamApp (stopped streamApp does not have this field)
    * @param error the error message if the state was failed to fetch
    * @param metrics the metrics bean
    * @param lastModified this data change time
    */
  final case class StreamClusterInfo(settings: Map[String, JsValue],
                                     definition: Option[Definition],
                                     deadNodes: Set[String],
                                     state: Option[String],
                                     error: Option[String],
                                     metrics: Metrics,
                                     lastModified: Long)
      extends ClusterInfo {

    /**
      * reuse the parser from Creation.
      * @param settings settings
      * @return creation
      */
    private[this] implicit def creation(settings: Map[String, JsValue]): Creation = new Creation(noJsNull(settings))

    override def name: String = settings.name
    override def group: String = settings.group
    override def kind: String = STREAM_SERVICE_NAME
    override def ports: Set[Int] = settings.ports
    override def tags: Map[String, JsValue] = settings.tags

    def imageName: String = settings.imageName
    // TODO this field is deprecated and should be removed in #2288
    def instances: Int = settings.instances.get

    /**
      * Return the key of explicit value. Otherwise, return the key of jar info.
      * Normally, the key should be equal to jar info
      * @return key of jar
      */
    def jarKey: ObjectKey = settings.jarKey.get

    def jarInfo: FileInfo = settings.jarInfo.get

    def brokerClusterName: String = settings.brokerClusterName.get
    def fromTopicKeys: Set[TopicKey] = settings.fromTopicKeys
    def toTopicKeys: Set[TopicKey] = settings.toTopicKeys
    def jmxPort: Int = settings.jmxPort
    // TODO remove this default value after we could handle from UI
    def exactlyOnce: Boolean = false

    def nodeNames: Set[String] = settings.nodeNames

    def connectionProps: String = settings.connectionProps
  }

  private[ohara] implicit val STREAM_CLUSTER_INFO_JSON_FORMAT: OharaJsonFormat[StreamClusterInfo] =
    JsonRefiner[StreamClusterInfo]
      .format(new RootJsonFormat[StreamClusterInfo] {
        private[this] val format = jsonFormat7(StreamClusterInfo)
        override def read(json: JsValue): StreamClusterInfo = format.read(json)
        override def write(obj: StreamClusterInfo): JsValue =
          JsObject(
            noJsNull(
              format.write(obj).asJsObject.fields ++
                // TODO: remove these stale fields
                Map(
                  NAME_KEY -> JsString(obj.name),
                  GROUP_KEY -> JsString(obj.group),
                  IMAGE_NAME_KEY -> JsString(obj.imageName),
                  NODE_NAMES_KEY -> JsArray(obj.nodeNames.map(JsString(_)).toVector)
                )
            ))
      })
      .refine

  //TODO We should extends this class from ClusterRequest after #2288
  sealed trait Request {
    @Optional("default name is a random string. But it is required in updating")
    def name(name: String): Request =
      setting(StreamDefUtils.NAME_DEFINITION.key(), JsString(CommonUtils.requireNonEmpty(name)))
    @Optional("default is GROUP_DEFAULT")
    def group(group: String): Request =
      setting(GROUP_KEY, JsString(CommonUtils.requireNonEmpty(group)))
    @Optional("the default image is IMAGE_NAME_DEFAULT")
    def imageName(imageName: String): Request =
      setting(StreamDefUtils.IMAGE_NAME_DEFINITION.key(), JsString(CommonUtils.requireNonEmpty(imageName)))
    def jarKey(jarKey: ObjectKey): Request =
      setting(StreamDefUtils.JAR_KEY_DEFINITION.key(), ObjectKey.toJsonString(jarKey).parseJson)
    def jarInfo(jarInfo: FileInfo): Request =
      setting(StreamDefUtils.JAR_INFO_DEFINITION.key(), FileInfoApi.FILE_INFO_JSON_FORMAT.write(jarInfo))
    def fromTopicKey(fromTopicKey: TopicKey): Request = fromTopicKeys(Set(fromTopicKey))
    def fromTopicKeys(fromTopicKeys: Set[TopicKey]): Request =
      setting(StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key(),
              JsArray(fromTopicKeys.map(TOPIC_KEY_FORMAT.write).toVector))
    def toTopicKey(toTopicKey: TopicKey): Request = toTopicKeys(Set(toTopicKey))
    def toTopicKeys(toTopicKeys: Set[TopicKey]): Request =
      setting(StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key(), JsArray(toTopicKeys.map(TOPIC_KEY_FORMAT.write).toVector))
    @Optional("server picks up a broker cluster for you if broker cluster name is empty")
    def brokerClusterName(brokerClusterName: String): Request = setting(
      StreamDefUtils.BROKER_CLUSTER_NAME_DEFINITION.key(),
      JsString(CommonUtils.requireNonEmpty(brokerClusterName)))
    @Optional("the default port is random")
    def jmxPort(jmxPort: Int): Request =
      setting(StreamDefUtils.JMX_PORT_DEFINITION.key(), JsNumber(CommonUtils.requireConnectionPort(jmxPort)))
    @Optional("you should not set both nodeNames and instances")
    //TODO "This should be removed after #2288"
    def instances(instances: Int): Request =
      setting(StreamDefUtils.INSTANCES_DEFINITION.key(), JsNumber(CommonUtils.requirePositiveInt(instances)))
    //TODO: add nodeNames checking in #2288...by Sam
    @Optional("you should not set both nodeNames and instances")
    def nodeName(nodeName: String): Request = nodeNames(Set(nodeName))
    @Optional("you should not set both nodeNames and instances")
    def nodeNames(nodeNames: Set[String]): Request =
      setting(StreamDefUtils.NODE_NAMES_DEFINITION.key(), JsArray(nodeNames.map(JsString(_)).toVector))
    @Optional("default value is empty array in creation and None in update")
    def tags(tags: Map[String, JsValue]): Request = setting(StreamDefUtils.TAGS_DEFINITION.key(), JsObject(tags))

    @Optional("default connection props is generated by broker cluster name")
    def connectionProps(connectionProps: String): Request =
      setting(StreamDefUtils.BROKER_DEFINITION.key(), JsString(connectionProps))

    @Optional("extra settings is empty by default")
    def setting(key: String, value: JsValue): Request = settings(Map(key -> value))
    @Optional("extra settings is empty by default")
    def settings(settings: Map[String, JsValue]): Request

    /**
      * generate POST request
      *
      * @param executionContext execution context
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo]

    /**
      * generate the PUT request
      *
      * @param executionContext execution context
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo]

    /**
      * Creation instance includes many useful parsers for custom settings so we open it to code with a view to reusing
      * those convenient parsers.
      * @return the payload of creation
      */
    def creation: Creation

    /**
      * for testing only
      * @return the payload of update
      */
    @VisibleForTesting
    private[v0] def update: Update
  }

  final class Access private[StreamApi] extends ClusterAccess[Creation, Update, StreamClusterInfo](STREAM_PREFIX_PATH) {

    def request: Request = new Request {
      private[this] val settings: mutable.Map[String, JsValue] = mutable.Map[String, JsValue]()

      override def settings(settings: Map[String, JsValue]): Request = {
        // We don't have to check the settings is empty here for the following reasons:
        // 1) we may want to use the benefit of default creation without specify settings
        // 2) actual checking will be done in the json parser phase of creation or update
        this.settings ++= settings
        this
      }
      override def creation: Creation =
        // auto-complete the creation via our refiner
        STREAM_CREATION_JSON_FORMAT.read(STREAM_CREATION_JSON_FORMAT.write(new Creation(settings.toMap)))

      override private[v0] def update: Update =
        // auto-complete the update via our refiner
        STREAM_UPDATE_JSON_FORMAT.read(STREAM_UPDATE_JSON_FORMAT.write(new Update(noJsNull(settings.toMap))))

      override def create()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] = post(creation)

      override def update()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] =
        put(
          // for update request, we should use default group if it was absent
          key(update.group.getOrElse(STREAM_GROUP_DEFAULT),
              update.name.getOrElse(throw new IllegalArgumentException("name is required in update request"))),
          update
        )
    }
  }

  def access: Access = new Access
}
