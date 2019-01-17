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
import spray.json.RootJsonFormat

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
      clientPort = None,
      nodeNames = nodeNames
    )
  final case class BrokerClusterCreationRequest(name: String,
                                                imageName: Option[String],
                                                zookeeperClusterName: Option[String],
                                                clientPort: Option[Int],
                                                nodeNames: Seq[String])
      extends ClusterCreationRequest

  implicit val BROKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[BrokerClusterCreationRequest] =
    jsonFormat5(BrokerClusterCreationRequest)
  final case class BrokerClusterInfo(name: String,
                                     imageName: String,
                                     zookeeperClusterName: String,
                                     clientPort: Int,
                                     nodeNames: Seq[String])
      extends ClusterInfo
  implicit val BROKER_CLUSTER_INFO_JSON_FORMAT: RootJsonFormat[BrokerClusterInfo] = jsonFormat5(BrokerClusterInfo)

  def access(): ClusterAccess[BrokerClusterCreationRequest, BrokerClusterInfo] =
    new ClusterAccess[BrokerClusterCreationRequest, BrokerClusterInfo](BROKER_PREFIX_PATH)
}
