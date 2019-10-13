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

import com.island.ohara.client.configurator.QueryRequest
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNumber, JsObject, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object ZookeeperApi {

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
  private[ohara] val TICK_TIME_KEY = "tickTime"
  private[ohara] val INIT_LIMIT_KEY = "initLimit"
  private[ohara] val SYNC_LIMIT_KEY = "syncLimit"
  private[ohara] val MAX_CLIENT_CNXNS_KEY = "maxClientCnxns"

  private[ohara] val ZK_ID_KEY = "zkId"
  private[ohara] val SERVERS_KEY = "servers"
  private[ohara] val DATA_DIR_KEY = "dataDir"

  final class Creation(val settings: Map[String, JsValue]) extends ClusterCreation {

    /**
      * reuse the parser from Update.
      * @param settings settings
      * @return update
      */
    private[this] implicit def update(settings: Map[String, JsValue]): Updating = new Updating(noJsNull(settings))
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
    basicRulesOfCreation[Creation](IMAGE_NAME_DEFAULT)
      .format(new RootJsonFormat[Creation] {
        override def write(obj: Creation): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Creation = new Creation(json.asJsObject.fields)
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

  final class Updating(val settings: Map[String, JsValue]) extends ClusterUpdating {
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

  implicit val ZOOKEEPER_UPDATING_JSON_FORMAT: OharaJsonFormat[Updating] =
    basicRulesOfUpdating[Updating]
      .format(new RootJsonFormat[Updating] {
        override def write(obj: Updating): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Updating = new Updating(json.asJsObject.fields)
      })
      // restrict rules
      .requireBindPort(CLIENT_PORT_KEY)
      .requireBindPort(PEER_PORT_KEY)
      .requireBindPort(ELECTION_PORT_KEY)
      .refine

  /**
    * There is no extra information for a running zookeeper cluster :)
    */
  class ZookeeperClusterStatus(val group: String,
                               val name: String,
                               val aliveNodes: Set[String],
                               val state: Option[String],
                               val error: Option[String])
      extends ClusterStatus

  final case class ZookeeperClusterInfo private[ZookeeperApi] (settings: Map[String, JsValue],
                                                               aliveNodes: Set[String],
                                                               lastModified: Long,
                                                               state: Option[String],
                                                               error: Option[String])
      extends ClusterInfo {

    /**
      * update the runtime information for this cluster info
      * @param status runtime information
      * @return a updated cluster info
      */
    def update(status: ZookeeperClusterStatus): ZookeeperClusterInfo = copy(
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
          JsObject(noJsNull(format.write(obj).asJsObject.fields))
      })
      .refine

  /**
    * used to generate the payload and url for POST/PUT request.
    * this request is extended by collie also so it is public than sealed.
    */
  trait Request extends ClusterRequest {
    @Optional("the default port is random")
    def clientPort(clientPort: Int): Request.this.type =
      setting(CLIENT_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(clientPort)))
    @Optional("the default port is random")
    def peerPort(peerPort: Int): Request.this.type =
      setting(PEER_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(peerPort)))
    @Optional("the default port is random")
    def electionPort(electionPort: Int): Request.this.type =
      setting(ELECTION_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(electionPort)))
    @Optional("default value is empty array in creation and None in update")
    def tags(tags: Map[String, JsValue]): Request.this.type = setting(TAGS_KEY, JsObject(tags))

    /**
      * zookeeper information creation.
      * Here we open the access for reusing the creation to other module
      *
      * @return the payload of create
      */
    final def creation: Creation =
      // auto-complete the creation via our refiner
      ZOOKEEPER_CREATION_JSON_FORMAT.read(ZOOKEEPER_CREATION_JSON_FORMAT.write(new Creation(noJsNull(settings.toMap))))

    /**
      * for testing only
      * @return the payload of update
      */
    private[v0] final def updating: Updating =
      // auto-complete the update via our refiner
      ZOOKEEPER_UPDATING_JSON_FORMAT.read(ZOOKEEPER_UPDATING_JSON_FORMAT.write(new Updating(noJsNull(settings.toMap))))
  }

  /**
    * similar to Request but it has execution methods.
    *
    */
  sealed trait ExecutableRequest extends Request {
    def create()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo]
    def update()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo]
  }

  sealed trait Query extends BasicQuery[ZookeeperClusterInfo] {
    import spray.json._

    def state(value: String): Query = set("state", value)

    def aliveNodes(value: Set[String]): Query = set("aliveNodes", JsArray(value.map(JsString(_)).toVector).toString())

    def setting(key: String, value: JsValue): Query = set(key, value match {
      case JsString(s) => s
      case _           => value.toString
    })

    // TODO: there are a lot of settings which is worth of having parameters ... by chia
  }

  final class Access private[ZookeeperApi]
      extends ClusterAccess[Creation, Updating, ZookeeperClusterInfo](ZOOKEEPER_PREFIX_PATH) {

    def query: Query = new Query {
      override protected def doExecute(request: QueryRequest)(
        implicit executionContext: ExecutionContext): Future[Seq[ZookeeperClusterInfo]] = list(request)
    }

    def request: ExecutableRequest = new ExecutableRequest {

      override def create()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = post(creation)

      override def update()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
        put(ObjectKey.of(updating.group.getOrElse(GROUP_DEFAULT), updating.name.get), updating)
    }
  }

  def access: Access = new Access
}
