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

package oharastream.ohara.client.configurator.v0

import java.util.concurrent.TimeUnit

import oharastream.ohara.client.configurator.QueryRequest
import oharastream.ohara.client.configurator.v0.ClusterAccess.Query
import oharastream.ohara.common.annotations.Optional
import oharastream.ohara.common.setting.{ObjectKey, SettingDef}
import oharastream.ohara.common.util.{CommonUtils, VersionUtils}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNumber, JsObject, JsValue, RootJsonFormat}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
object ZookeeperApi {
  val KIND: String = SettingDef.Reference.ZOOKEEPER.name().toLowerCase

  @deprecated(message = s"replaced by $KIND", since = "0.11.0")
  val ZOOKEEPER_PREFIX_PATH: String = "zookeepers"

  /**
    * the default docker image used to run containers of worker cluster
    */
  val IMAGE_NAME_DEFAULT: String = s"oharastream/zookeeper:${VersionUtils.VERSION}"

  //------------------------ The key name list in settings field ---------------------------------/
  private[this] val _DEFINITIONS = mutable.Map[String, SettingDef]()
  private[this] def createDef(f: SettingDef.Builder => SettingDef): SettingDef = {
    val settingDef = f(SettingDef.builder().orderInGroup(_DEFINITIONS.size).group("core"))
    assert(!_DEFINITIONS.contains(settingDef.key()), s"duplicate key:${settingDef.key()} is illegal")
    _DEFINITIONS += (settingDef.key() -> settingDef)
    settingDef
  }
  val GROUP_DEFINITION: SettingDef       = createDef(groupDefinition)
  val NAME_DEFINITION: SettingDef        = createDef(nameDefinition)
  val IMAGE_NAME_DEFINITION: SettingDef  = createDef(imageNameDefinition(IMAGE_NAME_DEFAULT))
  val CLIENT_PORT_DEFINITION: SettingDef = createDef(clientPortDefinition)
  val JMX_PORT_DEFINITION: SettingDef    = createDef(jmxPortDefinition)
  val NODE_NAMES_DEFINITION: SettingDef  = createDef(nodeDefinition)
  val ROUTES_DEFINITION: SettingDef      = createDef(routesDefinition)
  val TAGS_DEFINITION: SettingDef        = createDef(tagsDefinition)
  val MAX_HEAP_DEFINITION: SettingDef    = createDef(maxHeapDefinition)
  val INIT_HEAP_DEFINITION: SettingDef   = createDef(initHeapDefinition)
  private[this] val PEER_PORT_KEY        = "peerPort"
  val PEER_PORT_DEFINITION: SettingDef =
    createDef(
      _.key(PEER_PORT_KEY).documentation("the port exposed to each quorum").bindingPortWithRandomDefault().build()
    )
  private[this] val ELECTION_PORT_KEY = "electionPort"
  val ELECTION_PORT_DEFINITION: SettingDef =
    createDef(
      _.key(ELECTION_PORT_KEY).documentation("quorum leader election port").bindingPortWithRandomDefault().build()
    )
  // export these variables to collie for creating
  private[this] val TICK_TIME_KEY          = "tickTime"
  private[this] val TICK_TIME_DEFAULT: Int = 2000
  val TICK_TIME_DEFINITION: SettingDef = createDef(
    _.key(TICK_TIME_KEY)
      .documentation("basic time unit in zookeeper")
      .positiveNumber(TICK_TIME_DEFAULT)
      .build()
  )
  private[this] val INIT_LIMIT_KEY          = "initLimit"
  private[this] val INIT_LIMIT_DEFAULT: Int = 10
  val INIT_LIMIT_DEFINITION: SettingDef = createDef(
    _.key(INIT_LIMIT_KEY)
      .documentation("timeout to connect to leader")
      .positiveNumber(INIT_LIMIT_DEFAULT)
      .build()
  )
  private[this] val SYNC_LIMIT_KEY          = "syncLimit"
  private[this] val SYNC_LIMIT_DEFAULT: Int = 5
  val SYNC_LIMIT_DEFINITION: SettingDef = createDef(
    _.key(SYNC_LIMIT_KEY)
      .documentation("the out-of-date of a sever from leader")
      .positiveNumber(SYNC_LIMIT_DEFAULT)
      .build()
  )
  private[this] val DATA_DIR_KEY = "dataDir"
  val DATA_DIR_DEFINITION: SettingDef = createDef(
    _.key(DATA_DIR_KEY)
      .documentation("the volume used to store zookeeper data")
      .optional(SettingDef.Type.OBJECT_KEY)
      .reference(SettingDef.Reference.VOLUME)
      .build()
  )

  private[this] val CONNECTION_TIMEOUT_KEY = "zookeeper.connection.timeout.ms"
  val CONNECTION_TIMEOUT_DEFINITION: SettingDef = createDef(
    _.key(CONNECTION_TIMEOUT_KEY)
      .documentation("zookeeper connection timeout")
      .optional(java.time.Duration.ofMillis(10 * 1000))
      .build()
  )

  /**
    * all public configs
    */
  def DEFINITIONS: Seq[SettingDef] = _DEFINITIONS.values.toSeq

  final class Creation(val settings: Map[String, JsValue]) extends ClusterCreation {
    /**
      * reuse the parser from Update.
      * @param settings settings
      * @return update
      */
    private[this] implicit def update(settings: Map[String, JsValue]): Updating = new Updating(noJsNull(settings))
    override def ports: Set[Int]                                                = Set(clientPort, peerPort, electionPort, jmxPort)
    def clientPort: Int                                                         = settings.clientPort.get
    def peerPort: Int                                                           = settings.peerPort.get
    def electionPort: Int                                                       = settings.electionPort.get
    def tickTime: Int                                                           = settings.tickTime.get
    def initLimit: Int                                                          = settings.initLimit.get
    def syncLimit: Int                                                          = settings.syncLimit.get
    def connectionTimeout: Duration                                             = settings.connectionTimeout.get
    def dataFolder: String                                                      = "/tmp/zk_data"

    /**
      * @return the file containing zk quorum id. Noted that id file must be in data folder (see above)
      */
    def idFile: String = "/tmp/zk_data/myid"
    override def volumeMaps: Map[ObjectKey, String] =
      Map(DATA_DIR_KEY -> dataFolder)
        .flatMap {
          case (key, localPath) =>
            settings.get(key).map(_ -> localPath)
        }
        .map {
          case (js, localPath) =>
            js.convertTo[ObjectKey] -> localPath
        }
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val CREATION_JSON_FORMAT: JsonRefiner[Creation] =
    rulesOfCreation[Creation](
      new RootJsonFormat[Creation] {
        override def write(obj: Creation): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Creation = new Creation(json.asJsObject.fields)
      },
      DEFINITIONS
    )

  final class Updating(val settings: Map[String, JsValue]) extends ClusterUpdating {
    def clientPort: Option[Int] =
      noJsNull(settings).get(CLIENT_PORT_KEY).map(_.convertTo[Int])
    def peerPort: Option[Int] =
      noJsNull(settings).get(PEER_PORT_KEY).map(_.convertTo[Int])
    def electionPort: Option[Int] =
      noJsNull(settings).get(ELECTION_PORT_KEY).map(_.convertTo[Int])
    def tickTime: Option[Int] =
      noJsNull(settings).get(TICK_TIME_KEY).map(_.convertTo[Int])
    def initLimit: Option[Int] =
      noJsNull(settings).get(INIT_LIMIT_KEY).map(_.convertTo[Int])
    def syncLimit: Option[Int] =
      noJsNull(settings).get(SYNC_LIMIT_KEY).map(_.convertTo[Int])
    def connectionTimeout: Option[Duration] =
      noJsNull(settings)
        .get(CONNECTION_TIMEOUT_KEY)
        .map(_.convertTo[String])
        .map(CommonUtils.toDuration)
        .map(d => Duration(d.toMillis, TimeUnit.MILLISECONDS))
  }

  implicit val UPDATING_JSON_FORMAT: JsonRefiner[Updating] =
    rulesOfUpdating[Updating](
      new RootJsonFormat[Updating] {
        override def write(obj: Updating): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Updating = new Updating(json.asJsObject.fields)
      }
    )

  final case class ZookeeperClusterInfo private[ZookeeperApi] (
    settings: Map[String, JsValue],
    aliveNodes: Set[String],
    lastModified: Long,
    state: Option[ClusterState],
    error: Option[String]
  ) extends ClusterInfo {
    /**
      * reuse the parser from Creation.
      * @param settings settings
      * @return creation
      */
    private[this] implicit def creation(settings: Map[String, JsValue]): Creation = new Creation(noJsNull(settings))
    override def kind: String                                                     = KIND
    override def ports: Set[Int]                                                  = Set(clientPort, peerPort, electionPort, jmxPort)
    def clientPort: Int                                                           = settings.clientPort
    def peerPort: Int                                                             = settings.peerPort
    def electionPort: Int                                                         = settings.electionPort
    def tickTime: Int                                                             = settings.tickTime
    def initLimit: Int                                                            = settings.initLimit
    def syncLimit: Int                                                            = settings.syncLimit
    def dataDir: String                                                           = settings.dataFolder

    override protected def raw: Map[String, JsValue] = ZOOKEEPER_CLUSTER_INFO_FORMAT.write(this).asJsObject.fields
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val ZOOKEEPER_CLUSTER_INFO_FORMAT: JsonRefiner[ZookeeperClusterInfo] =
    JsonRefinerBuilder[ZookeeperClusterInfo]
      .format(new RootJsonFormat[ZookeeperClusterInfo] {
        private[this] val format                               = jsonFormat5(ZookeeperClusterInfo)
        override def read(json: JsValue): ZookeeperClusterInfo = format.read(extractSetting(json.asJsObject))
        override def write(obj: ZookeeperClusterInfo): JsValue = flattenSettings(format.write(obj).asJsObject)
      })
      .build

  /**
    * used to generate the payload and url for POST/PUT request.
    * this request is extended by collie also so it is public than sealed.
    */
  trait Request extends ClusterRequest {
    @Optional("the default port is random")
    def jmxPort(jmxPort: Int): Request.this.type =
      setting(JMX_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(jmxPort)))
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
      CREATION_JSON_FORMAT.read(CREATION_JSON_FORMAT.write(new Creation(noJsNull(settings.toMap))))

    /**
      * for testing only
      * @return the payload of update
      */
    private[v0] final def updating: Updating =
      // auto-complete the update via our refiner
      UPDATING_JSON_FORMAT.read(UPDATING_JSON_FORMAT.write(new Updating(noJsNull(settings.toMap))))
  }

  /**
    * similar to Request but it has execution methods.
    *
    */
  sealed trait ExecutableRequest extends Request {
    def create()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo]
    def update()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo]
  }

  final class Access private[ZookeeperApi] extends ClusterAccess[Creation, Updating, ZookeeperClusterInfo](KIND) {
    override def query: Query[ZookeeperClusterInfo] = new Query[ZookeeperClusterInfo] {
      override protected def doExecute(request: QueryRequest)(
        implicit executionContext: ExecutionContext
      ): Future[Seq[ZookeeperClusterInfo]] = list(request)
    }

    def request: ExecutableRequest = new ExecutableRequest {
      override def create()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = post(creation)

      override def update()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
        put(key, updating)
    }
  }

  def access: Access = new Access
}
