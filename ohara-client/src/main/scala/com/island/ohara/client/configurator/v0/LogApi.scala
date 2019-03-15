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

import scala.concurrent.{ExecutionContext, Future}

object LogApi {
  val LOG_PREFIX_PATH: String = "logs"

  case class NodeLog(name: String, value: String)
  implicit val NODE_LOG_FORMAT: RootJsonFormat[NodeLog] = jsonFormat2(NodeLog)

  case class ClusterLog(name: String, logs: Seq[NodeLog])
  implicit val CLUSTER_LOG_FORMAT: RootJsonFormat[ClusterLog] = jsonFormat2(ClusterLog)

  class Access extends BasicAccess(LOG_PREFIX_PATH) {

    private[this] def url(service: String, clusterName: String): String =
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$service/$clusterName"

    def log4ZookeeperCluster(clusterName: String)(implicit executionContext: ExecutionContext): Future[ClusterLog] =
      exec.get[ClusterLog, ErrorApi.Error](url(ZookeeperApi.ZOOKEEPER_PREFIX_PATH, clusterName))

    def log4BrokerCluster(clusterName: String)(implicit executionContext: ExecutionContext): Future[ClusterLog] =
      exec.get[ClusterLog, ErrorApi.Error](url(BrokerApi.BROKER_PREFIX_PATH, clusterName))

    def log4WorkerCluster(clusterName: String)(implicit executionContext: ExecutionContext): Future[ClusterLog] =
      exec.get[ClusterLog, ErrorApi.Error](url(WorkerApi.WORKER_PREFIX_PATH, clusterName))
  }

  def access(): Access = new Access
}
