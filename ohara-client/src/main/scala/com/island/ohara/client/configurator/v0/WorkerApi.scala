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

import com.island.ohara.client.configurator.v0.InfoApi.ConnectorVersion
import com.island.ohara.common.util.VersionUtils
import com.island.ohara.kafka.connector.json.SettingDefinition
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonFormat}
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
      jars = Seq.empty,
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
                                                jars: Seq[String],
                                                nodeNames: Seq[String])
      extends ClusterCreationRequest {
    override def ports: Set[Int] = Set(clientPort.getOrElse(CLIENT_PORT_DEFAULT), jmxPort.getOrElse(JMX_PORT_DEFAULT))
  }

  implicit val WORKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[WorkerClusterCreationRequest] =
    jsonFormat16(WorkerClusterCreationRequest)

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
    def jarNames: Seq[String]
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
        jarNames = obj.jarNames,
        connectors = obj.connectors,
        sources = obj.sources,
        sinks = obj.sinks,
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
              jarNames: Seq[String],
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
      jarNames = jarNames,
      connectors = connectors,
      sources = connectors.map(InfoApi.toConnectorVersion).filter(_.typeName == "source"),
      sinks = connectors.map(InfoApi.toConnectorVersion).filter(_.typeName == "sink"),
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
                                                 jarNames: Seq[String],
                                                 connectors: Seq[ConnectorDefinitions],
                                                 sources: Seq[ConnectorVersion],
                                                 sinks: Seq[ConnectorVersion],
                                                 nodeNames: Seq[String])
      extends WorkerClusterInfo
  private[this] implicit val WORKER_CLUSTER_INFO_IMPL_JSON_FORMAT: RootJsonFormat[WorkerClusterInfoImpl] = jsonFormat20(
    WorkerClusterInfoImpl)

  def access(): ClusterAccess[WorkerClusterCreationRequest, WorkerClusterInfo] =
    new ClusterAccess[WorkerClusterCreationRequest, WorkerClusterInfo](WORKER_PREFIX_PATH)
}
