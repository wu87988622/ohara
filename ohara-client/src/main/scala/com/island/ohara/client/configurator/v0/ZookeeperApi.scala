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

import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object ZookeeperApi {

  /**
    * The default value of group for this API.
    */
  val ZOOKEEPER_GROUP_DEFAULT: String = com.island.ohara.client.configurator.v0.GROUP_DEFAULT

  val ZOOKEEPER_PREFIX_PATH: String = "zookeepers"

  val ZOOKEEPER_SERVICE_NAME: String = "zk"

  /**
    * the default docker image used to run containers of worker cluster
    */
  val IMAGE_NAME_DEFAULT: String = s"oharastream/zookeeper:${VersionUtils.VERSION}"

  //------------------------ The key name list in settings field ---------------------------------/
  // export this variable to broker collie
  private[ohara] val CLIENT_PORT_KEY = "clientPort"
  private[this] val PEER_PORT_KEY = "peerPort"
  private[this] val ELECTION_PORT_KEY = "electionPort"
  // export these variables to collie for creating
  private[ohara] val ZK_ID_KEY = "zkId"
  private[ohara] val SERVERS_KEY = "servers"
  private[ohara] val DATA_DIR_KEY = "dataDir"

  final case class Creation(settings: Map[String, JsValue]) extends ClusterCreationRequest {

    /**
      * reuse the parser from Update.
      * @param settings settings
      * @return update
      */
    private[this] implicit def update(settings: Map[String, JsValue]): Update = Update(settings)
    // the name and group fields are used to identify zookeeper cluster object
    // we should give them default value in JsonRefiner
    override def name: String = settings.name.get
    override def group: String = settings.group.get
    // helper method to get the key
    private[ohara] def key: ObjectKey = ObjectKey.of(group, name)

    override def imageName: String = settings.imageName.get
    override def nodeNames: Set[String] = settings.nodeNames.get
    override def ports: Set[Int] = Set(clientPort, peerPort, electionPort)
    override def tags: Map[String, JsValue] = settings.tags.get

    def clientPort: Int = settings.clientPort.get
    def peerPort: Int = settings.peerPort.get
    def electionPort: Int = settings.electionPort.get
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val ZOOKEEPER_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    basicRulesOfCreation[Creation](IMAGE_NAME_DEFAULT, ZOOKEEPER_GROUP_DEFAULT)
      .format(new RootJsonFormat[Creation] {
        override def write(obj: Creation): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Creation = Creation(json.asJsObject.fields)
      })
      // default values
      .nullToRandomPort(CLIENT_PORT_KEY)
      .nullToRandomPort(PEER_PORT_KEY)
      .nullToRandomPort(ELECTION_PORT_KEY)
      // restrict rules
      .requireBindPort(CLIENT_PORT_KEY)
      .requireBindPort(PEER_PORT_KEY)
      .requireBindPort(ELECTION_PORT_KEY)
      .refine

  final case class Update(settings: Map[String, JsValue]) extends ClusterUpdateRequest {
    // We use the update parser to get the name and group
    private[ZookeeperApi] def name: Option[String] = noJsNull(settings).get(NAME_KEY).map(_.convertTo[String])
    private[ZookeeperApi] def group: Option[String] = noJsNull(settings).get(GROUP_KEY).map(_.convertTo[String])
    override def imageName: Option[String] =
      noJsNull(settings).get(IMAGE_NAME_KEY).map(_.convertTo[String])
    override def nodeNames: Option[Set[String]] =
      noJsNull(settings).get(NODE_NAMES_KEY).map(_.convertTo[Seq[String]].toSet)
    override def tags: Option[Map[String, JsValue]] =
      noJsNull(settings).get(TAGS_KEY).map {
        case s: JsObject => s.fields
        case other: JsValue =>
          throw new IllegalArgumentException(s"the type of tags should be JsObject, actual type is ${other.getClass}")
      }

    def clientPort: Option[Int] =
      noJsNull(settings).get(CLIENT_PORT_KEY).map(_.convertTo[Int])
    def peerPort: Option[Int] =
      noJsNull(settings).get(PEER_PORT_KEY).map(_.convertTo[Int])
    def electionPort: Option[Int] =
      noJsNull(settings).get(ELECTION_PORT_KEY).map(_.convertTo[Int])
  }

  implicit val ZOOKEEPER_UPDATE_JSON_FORMAT: OharaJsonFormat[Update] =
    basicRulesOfUpdate[Update]
      .format(new RootJsonFormat[Update] {
        override def write(obj: Update): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Update = Update(json.asJsObject.fields)
      })
      // restrict rules
      .requireBindPort(CLIENT_PORT_KEY)
      .requireBindPort(PEER_PORT_KEY)
      .requireBindPort(ELECTION_PORT_KEY)
      .refine

  final case class ZookeeperClusterInfo private[ZookeeperApi] (settings: Map[String, JsValue],
                                                               deadNodes: Set[String],
                                                               lastModified: Long,
                                                               state: Option[String],
                                                               error: Option[String])
      extends ClusterInfo {

    /**
      * reuse the parser from Creation.
      * @param settings settings
      * @return creation
      */
    private[this] implicit def creation(settings: Map[String, JsValue]): Creation = Creation(noJsNull(settings))

    override def name: String = settings.name
    override def group: String = settings.group
    override def kind: String = ZOOKEEPER_SERVICE_NAME
    override def ports: Set[Int] = Set(clientPort, peerPort, electionPort)
    override def tags: Map[String, JsValue] = settings.tags
    def nodeNames: Set[String] = settings.nodeNames

    // TODO remove this duplicated fields after #2191
    def imageName: String = settings.imageName
    def clientPort: Int = settings.clientPort
    def peerPort: Int = settings.peerPort
    def electionPort: Int = settings.electionPort

    // TODO: expose the metrics for zk
    override def metrics: Metrics = Metrics.EMPTY
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val ZOOKEEPER_CLUSTER_INFO_JSON_FORMAT: OharaJsonFormat[ZookeeperClusterInfo] =
    JsonRefiner[ZookeeperClusterInfo]
      .format(new RootJsonFormat[ZookeeperClusterInfo] {
        private[this] val format = jsonFormat5(ZookeeperClusterInfo)
        override def read(json: JsValue): ZookeeperClusterInfo = format.read(json)
        override def write(obj: ZookeeperClusterInfo): JsValue =
          JsObject(
            noJsNull(
              format.write(obj).asJsObject.fields ++
                // TODO: remove this stale fields
                Map(
                  NAME_KEY -> JsString(obj.name),
                  GROUP_KEY -> JsString(obj.group),
                  IMAGE_NAME_KEY -> JsString(obj.imageName),
                  NODE_NAMES_KEY -> JsArray(obj.nodeNames.map(JsString(_)).toVector),
                  CLIENT_PORT_KEY -> JsNumber(obj.clientPort),
                  PEER_PORT_KEY -> JsNumber(obj.peerPort),
                  ELECTION_PORT_KEY -> JsNumber(obj.electionPort),
                  TAGS_KEY -> JsObject(obj.tags)
                )
            ))
      })
      .refine

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  sealed trait Request {
    @Optional("default name is a random string. But it is required in updating")
    def name(name: String): Request =
      setting(NAME_KEY, JsString(CommonUtils.requireNonEmpty(name)))
    @Optional("default is GROUP_DEFAULT")
    def group(group: String): Request =
      setting(GROUP_KEY, JsString(CommonUtils.requireNonEmpty(group)))
    @Optional("the default image is IMAGE_NAME_DEFAULT")
    def imageName(imageName: String): Request =
      setting(IMAGE_NAME_KEY, JsString(CommonUtils.requireNonEmpty(imageName)))
    @Optional("the default port is random")
    def clientPort(clientPort: Int): Request =
      setting(CLIENT_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(clientPort)))
    @Optional("the default port is random")
    def peerPort(peerPort: Int): Request =
      setting(PEER_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(peerPort)))
    @Optional("the default port is random")
    def electionPort(electionPort: Int): Request =
      setting(ELECTION_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(electionPort)))
    def nodeName(nodeName: String): Request = nodeNames(Set(CommonUtils.requireNonEmpty(nodeName)))
    def nodeNames(nodeNames: Set[String]): Request =
      setting(NODE_NAMES_KEY, JsArray(CommonUtils.requireNonEmpty(nodeNames.asJava).asScala.map(JsString(_)).toVector))
    @Optional("default value is empty array in creation and None in update")
    def tags(tags: Map[String, JsValue]): Request = setting(TAGS_KEY, JsObject(tags))

    @Optional("extra settings is empty by default")
    def setting(key: String, value: JsValue): Request = settings(Map(key -> value))
    @Optional("extra settings is empty by default")
    def settings(settings: Map[String, JsValue]): Request

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo]

    /**
      * generate the PUT request
      * @param executionContext execution context
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo]

    /**
      * zookeeper information creation.
      * Here we open the access for reusing the creation to other module
      *
      * @return the payload of create
      */
    def creation: Creation

    /**
      * for testing only
      * @return the payload of update
      */
    private[v0] def update: Update
  }

  final class Access private[ZookeeperApi]
      extends ClusterAccess[Creation, Update, ZookeeperClusterInfo](ZOOKEEPER_PREFIX_PATH, ZOOKEEPER_GROUP_DEFAULT) {
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
        ZOOKEEPER_CREATION_JSON_FORMAT.read(ZOOKEEPER_CREATION_JSON_FORMAT.write(Creation(settings.toMap)))

      override private[v0] def update: Update =
        // auto-complete the update via our refiner
        ZOOKEEPER_UPDATE_JSON_FORMAT.read(ZOOKEEPER_UPDATE_JSON_FORMAT.write(Update(noJsNull(settings.toMap))))

      override def create()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = post(creation)

      override def update()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
        // for update request, we should use default group if it was absent
        put(key(update.group.getOrElse(ZOOKEEPER_GROUP_DEFAULT), update.name.get), update)
    }
  }

  def access: Access = new Access
}
