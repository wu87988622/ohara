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

import java.net.URL

import com.island.ohara.client.configurator.v0.InfoApi.ConnectorVersion
import com.island.ohara.common.util.VersionUtils
import com.island.ohara.kafka.connector.json.SettingDefinition
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}
object WorkerApi {
  val WORKER_PREFIX_PATH: String = "workers"

  /**
    * the default docker image used to run containers of worker cluster
    */
  val IMAGE_NAME_DEFAULT: String = s"oharastream/connect-worker:${VersionUtils.VERSION}"

  /**
    * bound by worker. It supplies the restful APIs of worker.
    */
  val CLIENT_PORT_DEFAULT: Int = 8083

  /**
    * bound by jmx service which supplies a connection to get java beans from worker process.
    */
  val JMX_PORT_DEFAULT: Int = 8084

  val CONFIG_TOPIC_REPLICATIONS_DEFAULT: Short = 1

  val STATUS_TOPIC_PARTITIONS_DEFAULT: Int = 1

  val STATUS_TOPIC_REPLICATIONS_DEFAULT: Short = 1

  val OFFSET_TOPIC_PARTITIONS_DEFAULT: Int = 1

  val OFFSET_TOPIC_REPLICATIONS_DEFAULT: Short = 1

  private[this] val NAME_KEY = "name"
  private[this] val IMAGE_NAME_KEY = "imageName"
  private[this] val BROKER_CLUSTER_NAME_KEY = "brokerClusterName"
  private[this] val CLIENT_PORT_KEY = "clientPort"
  private[this] val JMX_PORT_KEY = "jmxPort"
  private[this] val GROUP_ID_KEY = "groupId"
  private[this] val STATUS_TOPIC_NAME_KEY = "statusTopicName"
  private[this] val STATUS_TOPIC_PARTITIONS_KEY = "statusTopicPartitions"
  private[this] val STATUS_TOPIC_REPLICATIONS_KEY = "statusTopicReplications"
  private[this] val CONFIG_TOPIC_NAME_KEY = "configTopicName"
  private[this] val CONFIG_TOPIC_PARTITIONS_KEY = "configTopicPartitions"
  private[this] val CONFIG_TOPIC_REPLICATIONS_KEY = "configTopicReplications"
  private[this] val OFFSET_TOPIC_NAME_KEY = "offsetTopicName"
  private[this] val OFFSET_TOPIC_PARTITIONS_KEY = "offsetTopicPartitions"
  private[this] val OFFSET_TOPIC_REPLICATIONS_KEY = "offsetTopicReplications"
  private[this] val JAR_IDS_KEY = "jarIds"
  private[this] val JAR_URLS_KEY = "jarUrls"
  // TODO: deprecated key
  private[this] val JARS_KEY = "jars"
  private[this] val JAR_NAMES_KEY = "jarNames"
  private[this] val CONNECTORS_KEY = "connectors"
  // TODO: deprecated key
  private[this] val SOURCES_KEY = "sources"
  // TODO: deprecated key
  private[this] val SINKS_KEY = "sinks"
  private[this] val NODE_NAMES_KEY = "nodeNames"

  /**
    * Create a basic request with default value.
    * @param name cluster name
    * @param nodeNames node names
    * @return request
    */
  def creationRequest(name: String, nodeNames: Seq[String]): WorkerClusterCreationRequest =
    WorkerClusterCreationRequest(
      name = name,
      imageName = None,
      brokerClusterName = None,
      clientPort = None,
      jmxPort = None,
      groupId = None,
      statusTopicName = None,
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = None,
      configTopicReplications = None,
      offsetTopicName = None,
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jarIds = Seq.empty,
      nodeNames = nodeNames
    )

  final case class WorkerClusterCreationRequest(name: String,
                                                imageName: Option[String],
                                                brokerClusterName: Option[String],
                                                clientPort: Option[Int],
                                                jmxPort: Option[Int],
                                                groupId: Option[String],
                                                configTopicName: Option[String],
                                                // configTopicPartitions must be 1
                                                configTopicReplications: Option[Short],
                                                offsetTopicName: Option[String],
                                                offsetTopicPartitions: Option[Int],
                                                offsetTopicReplications: Option[Short],
                                                statusTopicName: Option[String],
                                                statusTopicPartitions: Option[Int],
                                                statusTopicReplications: Option[Short],
                                                jarIds: Seq[String],
                                                nodeNames: Seq[String])
      extends ClusterCreationRequest {
    override def ports: Set[Int] = Set(clientPort.getOrElse(CLIENT_PORT_DEFAULT), jmxPort.getOrElse(JMX_PORT_DEFAULT))
  }

  implicit val WORKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[WorkerClusterCreationRequest] =
    new RootJsonFormat[WorkerClusterCreationRequest] {
      override def write(obj: WorkerClusterCreationRequest): JsValue = JsObject(
        noJsNull(
          Map(
            NAME_KEY -> JsString(obj.name),
            IMAGE_NAME_KEY -> obj.imageName.map(JsString(_)).getOrElse(JsNull),
            BROKER_CLUSTER_NAME_KEY -> obj.brokerClusterName.map(JsString(_)).getOrElse(JsNull),
            CLIENT_PORT_KEY -> obj.clientPort.map(JsNumber(_)).getOrElse(JsNull),
            JMX_PORT_KEY -> obj.jmxPort.map(JsNumber(_)).getOrElse(JsNull),
            GROUP_ID_KEY -> obj.groupId.map(JsString(_)).getOrElse(JsNull),
            STATUS_TOPIC_NAME_KEY -> obj.statusTopicName.map(JsString(_)).getOrElse(JsNull),
            STATUS_TOPIC_PARTITIONS_KEY -> obj.statusTopicPartitions.map(JsNumber(_)).getOrElse(JsNull),
            STATUS_TOPIC_REPLICATIONS_KEY -> obj.statusTopicReplications.map(JsNumber(_)).getOrElse(JsNull),
            CONFIG_TOPIC_NAME_KEY -> obj.configTopicName.map(JsString(_)).getOrElse(JsNull),
            CONFIG_TOPIC_REPLICATIONS_KEY -> obj.configTopicReplications.map(JsNumber(_)).getOrElse(JsNull),
            OFFSET_TOPIC_NAME_KEY -> obj.offsetTopicName.map(JsString(_)).getOrElse(JsNull),
            OFFSET_TOPIC_PARTITIONS_KEY -> obj.offsetTopicPartitions.map(JsNumber(_)).getOrElse(JsNull),
            OFFSET_TOPIC_REPLICATIONS_KEY -> obj.offsetTopicReplications.map(JsNumber(_)).getOrElse(JsNull),
            JAR_IDS_KEY -> JsArray(obj.jarIds.map(JsString(_)).toVector),
            NODE_NAMES_KEY -> JsArray(obj.nodeNames.map(JsString(_)).toVector)
          ))
      )

      override def read(json: JsValue): WorkerClusterCreationRequest = WorkerClusterCreationRequest(
        name = noJsNull(json.asJsObject.fields)(NAME_KEY).convertTo[String],
        imageName = noJsNull(json.asJsObject.fields).get(IMAGE_NAME_KEY).map(_.convertTo[String]),
        brokerClusterName = noJsNull(json.asJsObject.fields).get(BROKER_CLUSTER_NAME_KEY).map(_.convertTo[String]),
        clientPort = noJsNull(json.asJsObject.fields).get(CLIENT_PORT_KEY).map(_.convertTo[Int]),
        jmxPort = noJsNull(json.asJsObject.fields).get(JMX_PORT_KEY).map(_.convertTo[Int]),
        groupId = noJsNull(json.asJsObject.fields).get(GROUP_ID_KEY).map(_.convertTo[String]),
        statusTopicName = noJsNull(json.asJsObject.fields).get(STATUS_TOPIC_NAME_KEY).map(_.convertTo[String]),
        statusTopicPartitions = noJsNull(json.asJsObject.fields).get(STATUS_TOPIC_PARTITIONS_KEY).map(_.convertTo[Int]),
        statusTopicReplications =
          noJsNull(json.asJsObject.fields).get(STATUS_TOPIC_REPLICATIONS_KEY).map(_.convertTo[Short]),
        configTopicName = noJsNull(json.asJsObject.fields).get(CONFIG_TOPIC_NAME_KEY).map(_.convertTo[String]),
        configTopicReplications =
          noJsNull(json.asJsObject.fields).get(CONFIG_TOPIC_REPLICATIONS_KEY).map(_.convertTo[Short]),
        offsetTopicName = noJsNull(json.asJsObject.fields).get(OFFSET_TOPIC_NAME_KEY).map(_.convertTo[String]),
        offsetTopicPartitions = noJsNull(json.asJsObject.fields).get(OFFSET_TOPIC_PARTITIONS_KEY).map(_.convertTo[Int]),
        offsetTopicReplications =
          noJsNull(json.asJsObject.fields).get(OFFSET_TOPIC_REPLICATIONS_KEY).map(_.convertTo[Short]),
        jarIds = noJsNull(json.asJsObject.fields)
          .get(JAR_IDS_KEY)
          .map(_.convertTo[Seq[String]])
          .getOrElse(
            json.asJsObject.fields
              .get(JARS_KEY)
              .map(_.convertTo[Seq[String]])
              .getOrElse(throw DeserializationException(s"$JAR_IDS_KEY is required!!!"))),
        nodeNames = noJsNull(json.asJsObject.fields)(NODE_NAMES_KEY).convertTo[Seq[String]]
      )
    }

  implicit val SETTING_DEFINITION_JSON_FORMAT: RootJsonFormat[SettingDefinition] =
    new RootJsonFormat[SettingDefinition] {
      import spray.json._
      override def read(json: JsValue): SettingDefinition = SettingDefinition.ofJson(json.toString())

      override def write(obj: SettingDefinition): JsValue = obj.toJsonString.parseJson
    }

  case class ConnectorDefinitions(className: String, definitions: Seq[SettingDefinition])
  implicit val CONNECTION_DEFINITIONS_JSON_FORMAT: RootJsonFormat[ConnectorDefinitions] = jsonFormat2(
    ConnectorDefinitions)

  /**
    * We need to fake cluster info in fake mode so we extract a layer to open the door to fake worker cluster.
    */
  trait WorkerClusterInfo extends ClusterInfo {
    def brokerClusterName: String
    def clientPort: Int
    def jmxPort: Int
    def groupId: String
    def statusTopicName: String
    def statusTopicPartitions: Int
    def statusTopicReplications: Short
    def configTopicName: String
    def configTopicPartitions: Int
    def configTopicReplications: Short
    def offsetTopicName: String
    def offsetTopicPartitions: Int
    def offsetTopicReplications: Short
    def jarIds: Seq[String]
    def jarUrls: Seq[URL]
    def connectors: Seq[ConnectorDefinitions]
    def sources: Seq[ConnectorVersion]
    def sinks: Seq[ConnectorVersion]

    /**
      * Our client to broker and worker accept the connection props:host:port,host2:port2
      */
    def connectionProps: String = nodeNames.map(n => s"$n:$clientPort").mkString(",")
    override def ports: Set[Int] = Set(clientPort, jmxPort)
  }

  private[this] def toCaseClass(obj: WorkerClusterInfo): WorkerClusterInfoImpl = obj match {
    case _: WorkerClusterInfoImpl => obj.asInstanceOf[WorkerClusterInfoImpl]
    case _ =>
      WorkerClusterInfoImpl(
        name = obj.name,
        imageName = obj.imageName,
        brokerClusterName = obj.brokerClusterName,
        clientPort = obj.clientPort,
        jmxPort = obj.jmxPort,
        groupId = obj.groupId,
        statusTopicName = obj.statusTopicName,
        statusTopicPartitions = obj.statusTopicPartitions,
        statusTopicReplications = obj.statusTopicReplications,
        configTopicName = obj.configTopicName,
        configTopicPartitions = obj.configTopicPartitions,
        configTopicReplications = obj.configTopicReplications,
        offsetTopicName = obj.offsetTopicName,
        offsetTopicPartitions = obj.offsetTopicPartitions,
        offsetTopicReplications = obj.offsetTopicReplications,
        jarIds = obj.jarIds,
        jarUrls = obj.jarUrls,
        connectors = obj.connectors,
        nodeNames = obj.nodeNames
      )
  }

  object WorkerClusterInfo {
    def apply(name: String,
              imageName: String,
              brokerClusterName: String,
              clientPort: Int,
              jmxPort: Int,
              groupId: String,
              statusTopicName: String,
              statusTopicPartitions: Int,
              statusTopicReplications: Short,
              configTopicName: String,
              configTopicPartitions: Int,
              configTopicReplications: Short,
              offsetTopicName: String,
              offsetTopicPartitions: Int,
              offsetTopicReplications: Short,
              jarIds: Seq[String],
              jarUrls: Seq[URL],
              connectors: Seq[ConnectorDefinitions],
              nodeNames: Seq[String]): WorkerClusterInfo = WorkerClusterInfoImpl(
      name = name,
      imageName = imageName,
      brokerClusterName = brokerClusterName,
      clientPort = clientPort,
      jmxPort = jmxPort,
      groupId = groupId,
      statusTopicName = statusTopicName,
      statusTopicPartitions = statusTopicPartitions,
      statusTopicReplications = statusTopicReplications,
      configTopicName = configTopicName,
      configTopicPartitions = configTopicPartitions,
      configTopicReplications = configTopicReplications,
      offsetTopicName = offsetTopicName,
      offsetTopicPartitions = offsetTopicPartitions,
      offsetTopicReplications = offsetTopicReplications,
      jarIds = jarIds,
      jarUrls = jarUrls,
      connectors = connectors,
      nodeNames = nodeNames
    )
  }

  implicit val WORKER_CLUSTER_INFO_JSON_FORMAT: RootJsonFormat[WorkerClusterInfo] =
    new RootJsonFormat[WorkerClusterInfo] {
      override def read(json: JsValue): WorkerClusterInfo = WORKER_CLUSTER_INFO_IMPL_JSON_FORMAT.read(json)

      override def write(obj: WorkerClusterInfo): JsValue = WORKER_CLUSTER_INFO_IMPL_JSON_FORMAT.write(
        toCaseClass(obj)
      )
    }

  private[this] case class WorkerClusterInfoImpl(name: String,
                                                 imageName: String,
                                                 brokerClusterName: String,
                                                 clientPort: Int,
                                                 jmxPort: Int,
                                                 groupId: String,
                                                 statusTopicName: String,
                                                 statusTopicPartitions: Int,
                                                 statusTopicReplications: Short,
                                                 configTopicName: String,
                                                 configTopicPartitions: Int,
                                                 configTopicReplications: Short,
                                                 offsetTopicName: String,
                                                 offsetTopicPartitions: Int,
                                                 offsetTopicReplications: Short,
                                                 jarIds: Seq[String],
                                                 jarUrls: Seq[URL],
                                                 connectors: Seq[ConnectorDefinitions],
                                                 nodeNames: Seq[String])
      extends WorkerClusterInfo {

    def sources: Seq[ConnectorVersion] = connectors.map(InfoApi.toConnectorVersion).filter(_.typeName == "source")
    def sinks: Seq[ConnectorVersion] = connectors.map(InfoApi.toConnectorVersion).filter(_.typeName == "sink")
  }

  private[this] implicit val WORKER_CLUSTER_INFO_IMPL_JSON_FORMAT: RootJsonFormat[WorkerClusterInfoImpl] =
    new RootJsonFormat[WorkerClusterInfoImpl] {
      override def write(obj: WorkerClusterInfoImpl): JsValue = JsObject(
        noJsNull(
          Map(
            NAME_KEY -> JsString(obj.name),
            IMAGE_NAME_KEY -> JsString(obj.imageName),
            BROKER_CLUSTER_NAME_KEY -> JsString(obj.brokerClusterName),
            CLIENT_PORT_KEY -> JsNumber(obj.clientPort),
            JMX_PORT_KEY -> JsNumber(obj.jmxPort),
            GROUP_ID_KEY -> JsString(obj.groupId),
            STATUS_TOPIC_NAME_KEY -> JsString(obj.statusTopicName),
            STATUS_TOPIC_PARTITIONS_KEY -> JsNumber(obj.statusTopicPartitions),
            STATUS_TOPIC_REPLICATIONS_KEY -> JsNumber(obj.statusTopicReplications),
            CONFIG_TOPIC_NAME_KEY -> JsString(obj.configTopicName),
            CONFIG_TOPIC_PARTITIONS_KEY -> JsNumber(obj.configTopicPartitions),
            CONFIG_TOPIC_REPLICATIONS_KEY -> JsNumber(obj.configTopicReplications),
            OFFSET_TOPIC_NAME_KEY -> JsString(obj.offsetTopicName),
            OFFSET_TOPIC_PARTITIONS_KEY -> JsNumber(obj.offsetTopicPartitions),
            OFFSET_TOPIC_REPLICATIONS_KEY -> JsNumber(obj.offsetTopicReplications),
            JAR_IDS_KEY -> JsArray(obj.jarIds.map(JsString(_)).toVector),
            JAR_URLS_KEY -> JsArray(obj.jarUrls.map(_.toString).map(JsString(_)).toVector),
            CONNECTORS_KEY -> JsArray(obj.connectors.map(CONNECTION_DEFINITIONS_JSON_FORMAT.write).toVector),
            NODE_NAMES_KEY -> JsArray(obj.nodeNames.map(JsString(_)).toVector),
            // deprecated keys
            JAR_NAMES_KEY -> JsArray(obj.jarIds.map(JsString(_)).toVector),
            // deprecated keys
            SOURCES_KEY -> JsArray(
              obj.connectors
                .map(InfoApi.toConnectorVersion)
                .filter(_.typeName == "source")
                .map(InfoApi.CONNECTOR_VERSION_JSON_FORMAT.write)
                .toVector),
            // deprecated keys
            SINKS_KEY -> JsArray(
              obj.connectors
                .map(InfoApi.toConnectorVersion)
                .filter(_.typeName == "sink")
                .map(InfoApi.CONNECTOR_VERSION_JSON_FORMAT.write)
                .toVector)
          ))
      )

      override def read(json: JsValue): WorkerClusterInfoImpl = WorkerClusterInfoImpl(
        name = noJsNull(json.asJsObject.fields)(NAME_KEY).convertTo[String],
        imageName = noJsNull(json.asJsObject.fields)(IMAGE_NAME_KEY).convertTo[String],
        brokerClusterName = noJsNull(json.asJsObject.fields)(BROKER_CLUSTER_NAME_KEY).convertTo[String],
        clientPort = noJsNull(json.asJsObject.fields)(CLIENT_PORT_KEY).convertTo[Int],
        jmxPort = noJsNull(json.asJsObject.fields)(JMX_PORT_KEY).convertTo[Int],
        groupId = noJsNull(json.asJsObject.fields)(GROUP_ID_KEY).convertTo[String],
        statusTopicName = noJsNull(json.asJsObject.fields)(STATUS_TOPIC_NAME_KEY).convertTo[String],
        statusTopicPartitions = noJsNull(json.asJsObject.fields)(STATUS_TOPIC_PARTITIONS_KEY).convertTo[Int],
        statusTopicReplications = noJsNull(json.asJsObject.fields)(STATUS_TOPIC_REPLICATIONS_KEY).convertTo[Short],
        configTopicName = noJsNull(json.asJsObject.fields)(CONFIG_TOPIC_NAME_KEY).convertTo[String],
        configTopicPartitions = noJsNull(json.asJsObject.fields)(CONFIG_TOPIC_PARTITIONS_KEY).convertTo[Int],
        configTopicReplications = noJsNull(json.asJsObject.fields)(CONFIG_TOPIC_REPLICATIONS_KEY).convertTo[Short],
        offsetTopicName = noJsNull(json.asJsObject.fields)(OFFSET_TOPIC_NAME_KEY).convertTo[String],
        offsetTopicPartitions = noJsNull(json.asJsObject.fields)(OFFSET_TOPIC_PARTITIONS_KEY).convertTo[Int],
        offsetTopicReplications = noJsNull(json.asJsObject.fields)(OFFSET_TOPIC_REPLICATIONS_KEY).convertTo[Short],
        jarIds = noJsNull(json.asJsObject.fields)(JAR_IDS_KEY).convertTo[Seq[String]],
        jarUrls = noJsNull(json.asJsObject.fields)(JAR_URLS_KEY).convertTo[Seq[String]].map(s => new URL(s)),
        connectors = noJsNull(json.asJsObject.fields)(CONNECTORS_KEY).convertTo[Seq[ConnectorDefinitions]],
        nodeNames = noJsNull(json.asJsObject.fields)(NODE_NAMES_KEY).convertTo[Seq[String]]
      )
    }
  def access(): ClusterAccess[WorkerClusterCreationRequest, WorkerClusterInfo] =
    new ClusterAccess[WorkerClusterCreationRequest, WorkerClusterInfo](WORKER_PREFIX_PATH)
}
