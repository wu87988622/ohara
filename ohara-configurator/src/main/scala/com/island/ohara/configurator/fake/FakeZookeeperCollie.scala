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

package com.island.ohara.configurator.fake

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeZookeeperCollie(node: NodeCollie)
    extends FakeCollie[ZookeeperClusterInfo, ZookeeperCollie.ClusterCreator](node)
    with ZookeeperCollie {
  override def creator: ZookeeperCollie.ClusterCreator =
    (_, clusterName, imageName, clientPort, peerPort, electionPort, nodeNames) =>
      Future.successful(
        addCluster(
          ZookeeperClusterInfo(
            name = clusterName,
            imageName = imageName,
            clientPort = clientPort,
            peerPort = peerPort,
            electionPort = electionPort,
            nodeNames = nodeNames,
            deadNodes = Set.empty,
            state = Some(ContainerState.RUNNING.name),
            error = None,
            tags = Map.empty,
            lastModified = CommonUtils.current()
          )))

  override protected def doRemoveNode(previousCluster: ZookeeperClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

  override protected def doAddNode(
    previousCluster: ZookeeperClusterInfo,
    previousContainers: Seq[ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = Future.failed(
    new UnsupportedOperationException("zookeeper collie doesn't support to add node from a running cluster"))

  override protected def doCreator(executionContext: ExecutionContext,
                                   clusterName: String,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: NodeApi.Node,
                                   route: Map[String, String]): Unit =
    new UnsupportedOperationException("zookeeper collie doesn't support to doCreator function")

  override protected def nodeCollie: NodeCollie = node

  override protected def prefixKey: String = "fakezookeeper"
}
