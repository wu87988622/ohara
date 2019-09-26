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

import com.island.ohara.common.setting.ObjectKey
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object LogApi {
  val LOG_PREFIX_PATH: String = "logs"

  case class NodeLog(hostname: String, value: String)
  implicit val NODE_LOG_FORMAT: RootJsonFormat[NodeLog] = new RootJsonFormat[NodeLog] {
    private[this] val format = jsonFormat2(NodeLog)
    override def read(json: JsValue): NodeLog = format.read(json)
    override def write(obj: NodeLog): JsValue = JsObject(
      format.write(obj).asJsObject.fields +
        // TODO: remove this stale field (see https://github.com/oharastream/ohara/issues/2769)
        ("name" -> JsString(obj.hostname))
    )
  }

  case class ClusterLog(clusterKey: ObjectKey, logs: Seq[NodeLog])
  implicit val CLUSTER_LOG_FORMAT: RootJsonFormat[ClusterLog] = new RootJsonFormat[ClusterLog] {
    private[this] val format = jsonFormat2(ClusterLog)
    override def read(json: JsValue): ClusterLog = format.read(json)
    override def write(obj: ClusterLog): JsValue = JsObject(
      format.write(obj).asJsObject.fields +
        // TODO: remove this stale field (see https://github.com/oharastream/ohara/issues/2769)
        ("name" -> JsString(obj.clusterKey.name()))
    )
  }

  class Access extends BasicAccess(LOG_PREFIX_PATH) {

    private[this] def _url(service: String, clusterKey: ObjectKey): String =
      s"$url/$service/${clusterKey.name()}?$GROUP_KEY=${clusterKey.group()}"

    def log4ZookeeperCluster(clusterKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[ClusterLog] =
      exec.get[ClusterLog, ErrorApi.Error](_url(ZookeeperApi.ZOOKEEPER_PREFIX_PATH, clusterKey))

    def log4BrokerCluster(clusterKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[ClusterLog] =
      exec.get[ClusterLog, ErrorApi.Error](_url(BrokerApi.BROKER_PREFIX_PATH, clusterKey))

    def log4WorkerCluster(clusterKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[ClusterLog] =
      exec.get[ClusterLog, ErrorApi.Error](_url(WorkerApi.WORKER_PREFIX_PATH, clusterKey))

    def log4StreamCluster(clusterKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[ClusterLog] =
      exec.get[ClusterLog, ErrorApi.Error](_url(StreamApi.STREAMS_PREFIX_PATH, clusterKey))
  }

  def access: Access = new Access
}
