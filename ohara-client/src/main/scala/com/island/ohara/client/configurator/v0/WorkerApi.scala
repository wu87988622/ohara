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

object WorkerApi {
  val WORKER_PREFIX_PATH: String = "workers"

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
      jars = Seq.empty,
      nodeNames = nodeNames
    )
  final case class WorkerClusterCreationRequest(name: String,
                                                imageName: Option[String],
                                                brokerClusterName: Option[String],
                                                clientPort: Option[Int],
                                                jars: Seq[String],
                                                nodeNames: Seq[String])
      extends ClusterCreationRequest

  implicit val WORKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[WorkerClusterCreationRequest] =
    jsonFormat6(WorkerClusterCreationRequest)

  final case class WorkerClusterInfo(name: String,
                                     imageName: String,
                                     brokerClusterName: String,
                                     clientPort: Int,
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
                                     nodeNames: Seq[String])
      extends ClusterInfo {

    /**
      * Our client to broker and worker accept the connection props:host:port,host2:port2
      */
    def connectionProps: String = nodeNames.map(n => s"$n:$clientPort").mkString(",")
  }
  implicit val WORKER_CLUSTER_INFO_JSON_FORMAT: RootJsonFormat[WorkerClusterInfo] = jsonFormat16(WorkerClusterInfo)

  def access(): ClusterAccess[WorkerClusterCreationRequest, WorkerClusterInfo] =
    new ClusterAccess[WorkerClusterCreationRequest, WorkerClusterInfo](WORKER_PREFIX_PATH)
}
