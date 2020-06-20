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

import java.util.Objects

import oharastream.ohara.client.configurator.QueryRequest
import oharastream.ohara.client.configurator.ClusterAccess.Query
import oharastream.ohara.common.annotations.Optional
import oharastream.ohara.common.setting.SettingDef.{Reference, Type}
import oharastream.ohara.common.setting.{ObjectKey, SettingDef}
import oharastream.ohara.common.util.{CommonUtils, VersionUtils}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNumber, JsObject, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}
object BrokerApi {
  val KIND: String = SettingDef.Reference.BROKER.name().toLowerCase

  @deprecated(message = s"replaced by $KIND", since = "0.11.0")
  val BROKER_PREFIX_PATH: String = "brokers"

  /**
    * the default docker image used to run containers of broker cluster
    */
  val IMAGE_NAME_DEFAULT: String = s"oharastream/broker:${VersionUtils.VERSION}"

  val ZOOKEEPER_CLUSTER_KEY_KEY: String = "zookeeperClusterKey"

  val LOG_DIRS_KEY: String             = "log.dirs"
  val NUMBER_OF_PARTITIONS_KEY: String = "num.partitions"

  val NUMBER_OF_REPLICATIONS_4_OFFSETS_TOPIC_KEY: String = "offsets.topic.replication.factor"

  val NUMBER_OF_NETWORK_THREADS_KEY: String = "num.network.threads"

  val NUMBER_OF_IO_THREADS_KEY: String = "num.io.threads"

  val DEFINITIONS: Seq[SettingDef] = DefinitionCollector()
    .addFollowupTo("core")
    .group()
    .name()
    .imageName(IMAGE_NAME_DEFAULT)
    .nodeNames()
    .routes()
    .tags()
    .definition(
      _.key(ZOOKEEPER_CLUSTER_KEY_KEY)
        .documentation("the zookeeper cluster used to manage broker nodes")
        .required(Type.OBJECT_KEY)
        .reference(Reference.ZOOKEEPER)
    )
    .addFollowupTo("performance")
    .definition(
      _.key(LOG_DIRS_KEY)
        .documentation("the folder used to store data of broker")
        // broker service can take multiples folders to speedup the I/O
        .optional(SettingDef.Type.OBJECT_KEYS)
        .reference(SettingDef.Reference.VOLUME)
    )
    .definition(
      _.key(NUMBER_OF_PARTITIONS_KEY)
        .documentation("the number of partitions for all topics by default")
        .positiveNumber(1)
    )
    .definition(
      _.key(NUMBER_OF_REPLICATIONS_4_OFFSETS_TOPIC_KEY)
        .documentation("the number of replications for internal offset topic")
        .positiveNumber(1)
    )
    .definition(
      _.key(NUMBER_OF_NETWORK_THREADS_KEY)
        .documentation("the number of threads used to accept network requests")
        .positiveNumber(1)
    )
    .definition(
      _.key(NUMBER_OF_IO_THREADS_KEY)
        .documentation("the number of threads used to process network requests")
        .positiveNumber(1)
    )
    .initHeap()
    .maxHeap()
    .addFollowupTo("public")
    .clientPort()
    .jmxPort()
    .result

  val TOPIC_DEFINITION: TopicDefinition = TopicDefinition(TopicApi.DEFINITIONS)

  final class Creation(val settings: Map[String, JsValue]) extends ClusterCreation {
    /**
      * reuse the parser from Update.
      * @param settings settings
      * @return update
      */
    private[this] implicit def update(settings: Map[String, JsValue]): Updating = new Updating(noJsNull(settings))

    override def ports: Set[Int]       = Set(clientPort, jmxPort)
    def clientPort: Int                = settings.clientPort.get
    def zookeeperClusterKey: ObjectKey = settings.zookeeperClusterKey.get
    private[this] def logVolumeKeys: Set[ObjectKey] =
      settings
        .get(LOG_DIRS_KEY)
        .map(_.convertTo[Set[ObjectKey]])
        .getOrElse(Set.empty)

    /**
      * @param index dir index
      * @return dir path with fixed postfix
      */
    private[this] def logDir(index: Int) = s"/tmp/bk_data_$index"

    def dataFolders: String =
      if (logVolumeKeys.isEmpty) "/tmp/bk_data"
      else
        logVolumeKeys.zipWithIndex
          .map {
            case (_, index) => logDir(index)
          }
          .mkString(",")
    def numberOfPartitions: Int = settings.numberOfPartitions.get
    def numberOfReplications4OffsetsTopic: Int =
      settings.numberOfReplications4OffsetsTopic.get
    def numberOfNetworkThreads: Int = settings.numberOfNetworkThreads.get
    def numberOfIoThreads: Int      = settings.numberOfIoThreads.get

    override def volumeMaps: Map[ObjectKey, String] =
      if (logVolumeKeys.isEmpty) Map.empty
      else
        logVolumeKeys.zipWithIndex.map {
          case (key, index) => key -> logDir(index)
        }.toMap
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
    def clientPort: Option[Int] = noJsNull(settings).get(CLIENT_PORT_KEY).map(_.convertTo[Int])

    def zookeeperClusterKey: Option[ObjectKey] =
      noJsNull(settings).get(ZOOKEEPER_CLUSTER_KEY_KEY).map(_.convertTo[ObjectKey])

    def numberOfPartitions: Option[Int] =
      noJsNull(settings).get(NUMBER_OF_PARTITIONS_KEY).map(_.convertTo[Int])

    def numberOfReplications4OffsetsTopic: Option[Int] =
      noJsNull(settings).get(NUMBER_OF_REPLICATIONS_4_OFFSETS_TOPIC_KEY).map(_.convertTo[Int])

    def numberOfNetworkThreads: Option[Int] =
      noJsNull(settings).get(NUMBER_OF_NETWORK_THREADS_KEY).map(_.convertTo[Int])

    def numberOfIoThreads: Option[Int] =
      noJsNull(settings).get(NUMBER_OF_IO_THREADS_KEY).map(_.convertTo[Int])
  }

  implicit val UPDATING_JSON_FORMAT: JsonRefiner[Updating] =
    rulesOfUpdating[Updating](new RootJsonFormat[Updating] {
      override def write(obj: Updating): JsValue = JsObject(noJsNull(obj.settings))
      override def read(json: JsValue): Updating = new Updating(json.asJsObject.fields)
    })

  final case class TopicDefinition(settingDefinitions: Seq[SettingDef])

  implicit val TOPIC_DEFINITION_JSON_FORMAT: JsonRefiner[TopicDefinition] =
    JsonRefinerBuilder[TopicDefinition].format(jsonFormat1(TopicDefinition)).rejectEmptyString().build

  final case class BrokerClusterInfo private[BrokerApi] (
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
    override def ports: Set[Int]                                                  = Set(clientPort, jmxPort)

    /**
      * the node names is not equal to "running" nodes. The connection props may reference to invalid nodes and the error
      * should be handled by the client code.
      * @return a string host_0:port,host_1:port
      */
    def connectionProps: String =
      if (nodeNames.isEmpty) throw new IllegalArgumentException("there is no nodes!!!")
      else nodeNames.map(n => s"$n:$clientPort").mkString(",")

    def clientPort: Int                        = settings.clientPort
    def zookeeperClusterKey: ObjectKey         = settings.zookeeperClusterKey
    def logDirs: String                        = settings.dataFolders
    def numberOfPartitions: Int                = settings.numberOfPartitions
    def numberOfReplications4OffsetsTopic: Int = settings.numberOfReplications4OffsetsTopic
    def numberOfNetworkThreads: Int            = settings.numberOfNetworkThreads
    def numberOfIoThreads: Int                 = settings.numberOfIoThreads

    override protected def raw: Map[String, JsValue] = BROKER_CLUSTER_INFO_FORMAT.write(this).asJsObject.fields
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val BROKER_CLUSTER_INFO_FORMAT: RootJsonFormat[BrokerClusterInfo] =
    JsonRefinerBuilder[BrokerClusterInfo]
      .format(new RootJsonFormat[BrokerClusterInfo] {
        private[this] val format                            = jsonFormat5(BrokerClusterInfo)
        override def read(json: JsValue): BrokerClusterInfo = format.read(extractSetting(json.asJsObject))
        override def write(obj: BrokerClusterInfo): JsValue = flattenSettings(format.write(obj).asJsObject)
      })
      .build

  /**
    * used to generate the payload and url for POST/PUT request.
    * this request is extended by collie also so it is public than sealed.
    */
  trait Request extends ClusterRequest {
    @Optional("Ignoring zookeeper cluster key enable server to match a zk for you")
    def zookeeperClusterKey(zookeeperClusterKey: ObjectKey): Request.this.type =
      setting(ZOOKEEPER_CLUSTER_KEY_KEY, OBJECT_KEY_FORMAT.write(Objects.requireNonNull(zookeeperClusterKey)))
    @Optional("the default port is random")
    def clientPort(clientPort: Int): Request.this.type =
      setting(CLIENT_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(clientPort)))
    @Optional("the default port is random")
    def jmxPort(jmxPort: Int): Request.this.type =
      setting(JMX_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(jmxPort)))
    @Optional("default value is empty array")
    def tags(tags: Map[String, JsValue]): Request.this.type = setting(TAGS_KEY, JsObject(tags))

    /**
      * broker information creation.
      *  Here we open the access for reusing the creation to other module
      *
      * @return the payload of creation
      */
    final def creation: Creation =
      // auto-complete the creation via our refiner
      CREATION_JSON_FORMAT.read(CREATION_JSON_FORMAT.write(new Creation(noJsNull(settings.toMap))))

    /**
      * for testing only
      * @return the payload of update
      */
    private[configurator] final def updating: Updating =
      // auto-complete the update via our refiner
      UPDATING_JSON_FORMAT.read(UPDATING_JSON_FORMAT.write(new Updating(noJsNull(settings.toMap))))
  }

  /**
    * similar to Request but it has execution methods.
    */
  sealed trait ExecutableRequest extends Request {
    def create()(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo]
    def update()(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo]
  }

  final class Access private[BrokerApi] extends ClusterAccess[Creation, Updating, BrokerClusterInfo](KIND) {
    override def query: Query[BrokerClusterInfo] = new Query[BrokerClusterInfo] {
      override protected def doExecute(request: QueryRequest)(
        implicit executionContext: ExecutionContext
      ): Future[Seq[BrokerClusterInfo]] = list(request)
    }

    def request: ExecutableRequest = new ExecutableRequest {
      override def create()(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = post(creation)

      override def update()(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = put(key, updating)
    }
  }

  def access: Access = new Access
}
