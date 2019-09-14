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
import com.island.ohara.common.setting.{ObjectKey, SettingDef}
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object BrokerApi {

  /**
    * The default value of group for this API.
    */
  val BROKER_GROUP_DEFAULT: String = com.island.ohara.client.configurator.v0.GROUP_DEFAULT

  val BROKER_PREFIX_PATH: String = "brokers"

  val BROKER_SERVICE_NAME: String = "bk"

  /**
    * the default docker image used to run containers of broker cluster
    */
  val IMAGE_NAME_DEFAULT: String = s"oharastream/broker:${VersionUtils.VERSION}"

  //------------------------ The key name list in settings field ---------------------------------/
  private[ohara] val CLIENT_PORT_KEY: String = "clientPort"
  private[this] val EXPORTER_PORT_KEY: String = "exporterPort"
  private[this] val JMX_PORT_KEY: String = "jmxPort"
  private[ohara] val ADVERTISED_HOSTNAME_KEY: String = "advertisedHostname"
  private[ohara] val ADVERTISED_CLIENT_PORT_KEY: String = "advertisedClientPort"
  private[ohara] val ID_KEY: String = "brokerId"
  private[ohara] val DATA_DIRECTORY_KEY: String = "dataDir"
  private[ohara] val ZOOKEEPERS_KEY: String = "zookeepers"
  private[ohara] val JMX_HOSTNAME_KEY: String = "jmxHostname"

  /**
    * internal key used to save the zookeeper cluster name.
    * All nodes of broker cluster should have this environment variable.
    */
  private[ohara] val ZOOKEEPER_CLUSTER_NAME_KEY: String = "zookeeperClusterName"

  final class Creation(val settings: Map[String, JsValue]) extends ClusterCreationRequest {

    /**
      * reuse the parser from Update.
      * @param settings settings
      * @return update
      */
    private[this] implicit def update(settings: Map[String, JsValue]): Update = new Update(settings)
    // the name and group fields are used to identify zookeeper cluster object
    // we should give them default value in JsonRefiner
    override def name: String = settings.name.get
    override def group: String = settings.group.get
    // helper method to get the key
    private[ohara] def key: ObjectKey = ObjectKey.of(group, name)

    override def imageName: String = settings.imageName.get
    override def nodeNames: Set[String] = settings.nodeNames.get
    override def ports: Set[Int] = Set(clientPort, exporterPort, jmxPort)
    override def tags: Map[String, JsValue] = settings.tags.get

    def exporterPort: Int = settings.exporterPort.get
    def clientPort: Int = settings.clientPort.get
    def jmxPort: Int = settings.jmxPort.get
    def zookeeperClusterName: Option[String] = settings.zookeeperClusterName
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val BROKER_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    basicRulesOfCreation[Creation](IMAGE_NAME_DEFAULT, BROKER_GROUP_DEFAULT)
      .format(new RootJsonFormat[Creation] {
        override def write(obj: Creation): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Creation = new Creation(json.asJsObject.fields)
      })
      .nullToRandomPort(CLIENT_PORT_KEY)
      .requireBindPort(CLIENT_PORT_KEY)
      .nullToRandomPort(EXPORTER_PORT_KEY)
      .requireBindPort(EXPORTER_PORT_KEY)
      .nullToRandomPort(JMX_PORT_KEY)
      .requireBindPort(JMX_PORT_KEY)
      .rejectEmptyString(ZOOKEEPER_CLUSTER_NAME_KEY)
      .refine

  final class Update(val settings: Map[String, JsValue]) extends ClusterUpdateRequest {
    // We use the update parser to get the name and group
    private[BrokerApi] def name: Option[String] = noJsNull(settings).get(NAME_KEY).map(_.convertTo[String])
    private[BrokerApi] def group: Option[String] = noJsNull(settings).get(GROUP_KEY).map(_.convertTo[String])
    override def imageName: Option[String] = noJsNull(settings).get(IMAGE_NAME_KEY).map(_.convertTo[String])
    override def nodeNames: Option[Set[String]] =
      noJsNull(settings).get(NODE_NAMES_KEY).map(_.convertTo[Seq[String]].toSet)
    override def tags: Option[Map[String, JsValue]] = noJsNull(settings).get(TAGS_KEY).map {
      case s: JsObject => s.fields
      case other: JsValue =>
        throw new IllegalArgumentException(s"the type of tags should be JsObject, actual type is ${other.getClass}")
    }

    def exporterPort: Option[Int] = noJsNull(settings).get(EXPORTER_PORT_KEY).map(_.convertTo[Int])
    def clientPort: Option[Int] = noJsNull(settings).get(CLIENT_PORT_KEY).map(_.convertTo[Int])
    def jmxPort: Option[Int] = noJsNull(settings).get(JMX_PORT_KEY).map(_.convertTo[Int])
    def zookeeperClusterName: Option[String] =
      noJsNull(settings).get(ZOOKEEPER_CLUSTER_NAME_KEY).map(_.convertTo[String])
  }

  implicit val BROKER_UPDATE_JSON_FORMAT: OharaJsonFormat[Update] =
    basicRulesOfUpdate[Update]
      .format(new RootJsonFormat[Update] {
        override def write(obj: Update): JsValue = JsObject(noJsNull(obj.settings))
        override def read(json: JsValue): Update = new Update(json.asJsObject.fields)
      })
      .requireBindPort(CLIENT_PORT_KEY)
      .requireBindPort(EXPORTER_PORT_KEY)
      .requireBindPort(JMX_PORT_KEY)
      .rejectEmptyString(ZOOKEEPER_CLUSTER_NAME_KEY)
      .refine

  final case class BrokerClusterInfo private[BrokerApi] (settings: Map[String, JsValue],
                                                         deadNodes: Set[String],
                                                         lastModified: Long,
                                                         state: Option[String],
                                                         error: Option[String],
                                                         topicSettingDefinitions: Seq[SettingDef])
      extends ClusterInfo {

    /**
      * reuse the parser from Creation.
      * @param settings settings
      * @return creation
      */
    private[this] implicit def creation(settings: Map[String, JsValue]): Creation = new Creation(noJsNull(settings))
    override def name: String = settings.name
    override def group: String = settings.group
    override def kind: String = BROKER_SERVICE_NAME
    override def ports: Set[Int] = Set(clientPort, exporterPort, jmxPort)
    override def tags: Map[String, JsValue] = settings.tags
    def nodeNames: Set[String] = settings.nodeNames
    def connectionProps: String = nodeNames.map(n => s"$n:$clientPort").mkString(",")

    // TODO remove this duplicated fields after #2191
    def imageName: String = settings.imageName
    def exporterPort: Int = settings.exporterPort
    def clientPort: Int = settings.clientPort
    def jmxPort: Int = settings.jmxPort
    def zookeeperClusterName: String = settings.zookeeperClusterName.get
    // TODO: expose the metrics for bk
    override def metrics: Metrics = Metrics.EMPTY
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val BROKER_CLUSTER_INFO_JSON_FORMAT: OharaJsonFormat[BrokerClusterInfo] =
    JsonRefiner[BrokerClusterInfo]
      .format(new RootJsonFormat[BrokerClusterInfo] {
        private[this] val format = jsonFormat6(BrokerClusterInfo)
        override def read(json: JsValue): BrokerClusterInfo = format.read(json)
        override def write(obj: BrokerClusterInfo): JsValue =
          JsObject(
            noJsNull(
              format.write(obj).asJsObject.fields ++
                // TODO: remove this stale fields
                Map(
                  NAME_KEY -> JsString(obj.name),
                  GROUP_KEY -> JsString(obj.group),
                  IMAGE_NAME_KEY -> JsString(obj.imageName),
                  NODE_NAMES_KEY -> JsArray(obj.nodeNames.map(JsString(_)).toVector),
                  EXPORTER_PORT_KEY -> JsNumber(obj.exporterPort),
                  CLIENT_PORT_KEY -> JsNumber(obj.clientPort),
                  JMX_PORT_KEY -> JsNumber(obj.jmxPort),
                  TAGS_KEY -> JsObject(obj.tags),
                  ZOOKEEPER_CLUSTER_NAME_KEY -> JsString(obj.zookeeperClusterName)
                )
            ))
      })
      .refine

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  sealed trait Request {
    @Optional("default name is a random string. But it is required in updating")
    def name(name: String): Request = setting(NAME_KEY, JsString(CommonUtils.requireNonEmpty(name)))
    @Optional("default is GROUP_DEFAULT")
    def group(group: String): Request =
      setting(GROUP_KEY, JsString(CommonUtils.requireNonEmpty(group)))
    @Optional("the default image is IMAGE_NAME_DEFAULT")
    def imageName(imageName: String): Request =
      setting(IMAGE_NAME_KEY, JsString(CommonUtils.requireNonEmpty(imageName)))
    @Optional("Ignoring zookeeper cluster name enable server to match a zk for you")
    def zookeeperClusterName(zookeeperClusterName: String): Request =
      setting(ZOOKEEPER_CLUSTER_NAME_KEY, JsString(CommonUtils.requireNonEmpty(zookeeperClusterName)))
    @Optional("the default port is random")
    def clientPort(clientPort: Int): Request =
      setting(CLIENT_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(clientPort)))
    @Optional("the default port is random")
    def exporterPort(exporterPort: Int): Request =
      setting(EXPORTER_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(exporterPort)))
    @Optional("the default port is random")
    def jmxPort(jmxPort: Int): Request = setting(JMX_PORT_KEY, JsNumber(CommonUtils.requireConnectionPort(jmxPort)))
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
    def create()(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo]

    /**
      * generate the PUT request
      * @param executionContext execution context
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo]

    /**
      * broker information creation.
      *  Here we open the access for reusing the creation to other module
      *
      * @return the payload of creation
      */
    def creation: Creation

    /**
      * for testing only
      * @return the payload of update
      */
    private[v0] def update: Update
  }

  final class Access private[BrokerApi] extends ClusterAccess[Creation, Update, BrokerClusterInfo](BROKER_PREFIX_PATH) {
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
        BROKER_CREATION_JSON_FORMAT.read(BROKER_CREATION_JSON_FORMAT.write(new Creation(settings.toMap)))

      override private[v0] def update: Update =
        // auto-complete the update via our refiner
        BROKER_UPDATE_JSON_FORMAT.read(BROKER_UPDATE_JSON_FORMAT.write(new Update(noJsNull(settings.toMap))))

      override def create()(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = post(creation)

      override def update()(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] =
        put(
          // for update request, we should use default group if it was absent
          key(update.group.getOrElse(BROKER_GROUP_DEFAULT),
              update.name.getOrElse(throw new IllegalArgumentException("name is required in update request"))),
          update
        )
    }
  }

  def access: Access = new Access
}
