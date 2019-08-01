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

import scala.concurrent.{ExecutionContext, Future}

/**
  * the cluster-related data is different from normal data so we need another type of access.
  * @param prefixPath path to remote resource
  */
abstract class ClusterAccess[Res <: ClusterInfo] private[v0] (prefixPath: String)(implicit rm: OharaJsonFormat[Res])
    extends BasicAccess(prefixPath) {
  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"

  private[this] def _clusterName(name: String): String =
    CommonUtils.requireNonEmpty(name, () => "cluster name can't be empty")
  private[this] def _nodeName(name: String): String = {
    CommonUtils.requireNonEmpty(name, () => "node name can't be empty")
  }
  private[this] def actionUrl(name: String, action: String): String =
    s"$url/$name/$action"

  def get(clusterName: String)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.get[Res, ErrorApi.Error](s"$url/${_clusterName(clusterName)}")
  def delete(clusterName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](s"$url/$clusterName")
  //TODO remove this after finished #1544...by Sam
  def forceDelete(clusterName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](s"$url/$clusterName?${Data.FORCE_KEY}=true")
  def list()(implicit executionContext: ExecutionContext): Future[Seq[Res]] =
    exec.get[Seq[Res], ErrorApi.Error](url)
  def addNode(clusterName: String, nodeName: String)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.put[Res, ErrorApi.Error](s"$url/${_clusterName(clusterName)}/${_nodeName(nodeName)}")
  def removeNode(clusterName: String, nodeName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](s"$url/${_clusterName(clusterName)}/${_nodeName(nodeName)}")

  /**
    *  start a cluster
    *
    * @param name object name
    * @param executionContext execution context
    * @return none
    */
  def start(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.put[ErrorApi.Error](actionUrl(name, START_COMMAND))

  /**
    * stop a cluster gracefully.
    *
    * @param name object name
    * @param executionContext execution context
    * @return none
    */
  def stop(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.put[ErrorApi.Error](actionUrl(name, STOP_COMMAND))

  /**
    * force to stop a cluster.
    * This action may cause some data loss if cluster was still running.
    *
    * @param name object name
    * @param executionContext execution context
    * @return none
    */
  def forceStop(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.put[ErrorApi.Error](s"${actionUrl(name, STOP_COMMAND)}?${Data.FORCE_KEY}=true")
}
