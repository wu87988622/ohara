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
import com.island.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

/**
  * the cluster-related data is different from normal data so we need another type of access.
  * @param prefixPath path to remote resource
  */
abstract class ClusterAccess[Res <: ClusterInfo] private[v0] (prefixPath: String)(implicit rm: RootJsonFormat[Res])
    extends BasicAccess(prefixPath) {

  private[this] def _clusterName(name: String): String =
    CommonUtils.requireNonEmpty(name, () => "cluster name can't be empty")
  private[this] def _nodeName(name: String): String =
    CommonUtils.requireNonEmpty(name, () => "node name can't be empty")

  def get(clusterName: String)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.get[Res, ErrorApi.Error](s"${_url}/${_clusterName(clusterName)}")
  def delete(clusterName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](s"${_url}/$clusterName")
  def forceDelete(clusterName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](s"${_url}/$clusterName?${Parameters.FORCE_REMOVE}=true")
  def list(implicit executionContext: ExecutionContext): Future[Seq[Res]] =
    exec.get[Seq[Res], ErrorApi.Error](s"${_url}")
  def addNode(clusterName: String, nodeName: String)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.post[Res, ErrorApi.Error](s"${_url}/${_clusterName(clusterName)}/${_nodeName(nodeName)}")
  def removeNode(clusterName: String, nodeName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](s"${_url}/${_clusterName(clusterName)}/${_nodeName(nodeName)}")
}
