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

import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonFormat}

object BrokerApi {
  val BROKER_PREFIX_PATH: String = "brokers"

  /**
    * Create a basic request with default value.
    * @param name cluster name
    * @param nodeNames node names
    * @return request
    */
  def creationRequest(name: String, nodeNames: Seq[String]): BrokerClusterCreationRequest =
    BrokerClusterCreationRequest(
      name = name,
      imageName = None,
      zookeeperClusterName = None,
      exporterPort = None,
      clientPort = None,
      nodeNames = nodeNames
    )
  final case class BrokerClusterCreationRequest(name: String,
                                                imageName: Option[String],
                                                zookeeperClusterName: Option[String],
                                                exporterPort: Option[Int],
                                                clientPort: Option[Int],
                                                nodeNames: Seq[String])
      extends ClusterCreationRequest

  implicit val BROKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[BrokerClusterCreationRequest] =
    jsonFormat6(BrokerClusterCreationRequest)

  /**
    * We need to fake cluster info in fake mode so we extract a layer to open the door to fake broker cluster.
    */
  trait BrokerClusterInfo extends ClusterInfo {
    def zookeeperClusterName: String
    def clientPort: Int
    def exporterPort: Int
    def connectionProps: String = nodeNames.map(n => s"$n:$clientPort").mkString(",")
  }

  implicit val BROKER_CLUSTER_INFO_JSON_FORMAT: RootJsonFormat[BrokerClusterInfo] =
    new RootJsonFormat[BrokerClusterInfo] {
      override def read(json: JsValue): BrokerClusterInfo = BROKER_CLUSTER_INFO_IMPL_JSON_FORMAT.read(json)

      override def write(obj: BrokerClusterInfo): JsValue = BROKER_CLUSTER_INFO_IMPL_JSON_FORMAT.write(
        toCaseClass(obj)
      )
    }

  private[this] def toCaseClass(obj: BrokerClusterInfo): BrokerClusterInfoImpl = obj match {
    case _: BrokerClusterInfoImpl => obj.asInstanceOf[BrokerClusterInfoImpl]
    case _ =>
      BrokerClusterInfoImpl(
        name = obj.name,
        imageName = obj.imageName,
        zookeeperClusterName = obj.zookeeperClusterName,
        clientPort = obj.clientPort,
        exporterPort = obj.exporterPort,
        nodeNames = obj.nodeNames
      )
  }

  object BrokerClusterInfo {
    def apply(name: String,
              imageName: String,
              zookeeperClusterName: String,
              exporterPort: Int,
              clientPort: Int,
              nodeNames: Seq[String]): BrokerClusterInfo = BrokerClusterInfoImpl(
      name = name,
      imageName = imageName,
      zookeeperClusterName = zookeeperClusterName,
      clientPort = clientPort,
      exporterPort = exporterPort,
      nodeNames = nodeNames,
    )
  }

  private[this] case class BrokerClusterInfoImpl(name: String,
                                                 imageName: String,
                                                 zookeeperClusterName: String,
                                                 clientPort: Int,
                                                 exporterPort: Int,
                                                 nodeNames: Seq[String])
      extends BrokerClusterInfo
  private[this] implicit val BROKER_CLUSTER_INFO_IMPL_JSON_FORMAT: RootJsonFormat[BrokerClusterInfoImpl] = jsonFormat6(
    BrokerClusterInfoImpl)

  def access(): ClusterAccess[BrokerClusterCreationRequest, BrokerClusterInfo] =
    new ClusterAccess[BrokerClusterCreationRequest, BrokerClusterInfo](BROKER_PREFIX_PATH)
}
