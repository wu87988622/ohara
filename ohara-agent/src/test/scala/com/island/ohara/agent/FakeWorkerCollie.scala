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

package com.island.ohara.agent

import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.{NodeApi, WorkerApi}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}

class FakeWorkerCollie(nodes: Seq[Node],
                       brokerClusters: Map[String, Seq[ContainerInfo]],
                       existedWorkerClusterName: String = CommonUtils.randomString(5))
    extends WorkerCollie {
  override protected def brokerContainers(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    Future.successful(
      brokerClusters.getOrElse(clusterName, throw new NoSuchClusterException(s"$clusterName does not exist")))

  override protected def doCreator(executionContext: ExecutionContext,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: NodeApi.Node,
                                   route: Map[String, String]): Future[Unit] = Future.unit

  override def logs(objectKey: ObjectKey)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] =
    throw new UnsupportedOperationException("Not support logs function")

  override def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext): Future[Map[WorkerClusterInfo, Seq[ContainerInfo]]] =
    Future.successful(
      Map(
        WorkerClusterInfo(
          settings = WorkerApi.access.request
            .name(existedWorkerClusterName)
            .nodeNames(nodes.map(_.hostname).toSet)
            .creation
            .settings,
          connectors = Seq.empty,
          deadNodes = Set.empty,
          lastModified = CommonUtils.current(),
          state = Some(ClusterState.RUNNING.name),
          error = None
        ) -> nodes
          .map(_.hostname)
          .map(
            hostname =>
              ContainerInfo(hostname,
                            "aaaa",
                            "connect-worker",
                            "2019-05-28 00:00:00",
                            "RUNNING",
                            "unknown",
                            "ohara-xxx-wk-0000",
                            "unknown",
                            Seq.empty,
                            Map.empty,
                            "ohara-xxx-wk-0000"))
      )
    )
  override protected def resolveHostName(hostname: String): String = hostname

  override protected val nodeCollie: NodeCollie = NodeCollie(nodes)

  override protected def prefixKey: String = "fakeworker"

  override protected def doRemove(clusterInfo: WorkerClusterInfo, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    throw new UnsupportedOperationException("FakeWorkerCollie doesn't support this function")

  override protected def doRemoveNode(previousCluster: WorkerClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    throw new UnsupportedOperationException("FakeWorkerCollie doesn't support this function")

  // In fake mode, we don't care the cluster state
  override protected def toClusterState(containers: Seq[ContainerInfo]): Option[ClusterState] =
    Some(ClusterState.RUNNING)
}
