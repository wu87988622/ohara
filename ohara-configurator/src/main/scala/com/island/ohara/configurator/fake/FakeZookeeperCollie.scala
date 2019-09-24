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

import com.island.ohara.agent.{ClusterState, NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}

import scala.collection.JavaConverters._

private[configurator] class FakeZookeeperCollie(node: NodeCollie)
    extends FakeCollie[ZookeeperClusterInfo](node)
    with ZookeeperCollie {
  override def creator: ZookeeperCollie.ClusterCreator = (_, creation) =>
    if (clusterCache.asScala.exists(_._1.key == creation.key))
      Future.failed(new IllegalArgumentException(s"zookeeper can't increase nodes at runtime"))
    else
      Future.successful(
        addCluster(
          ZookeeperClusterInfo(
            settings = creation.settings,
            aliveNodes = creation.nodeNames,
            // In fake mode, we need to assign a state in creation for "GET" method to act like real case
            state = Some(ClusterState.RUNNING.name),
            error = None,
            lastModified = CommonUtils.current()
          )))

  override protected def doRemoveNode(previousCluster: ZookeeperClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

  override protected def doCreator(executionContext: ExecutionContext,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: NodeApi.Node,
                                   route: Map[String, String]): Future[Unit] =
    throw new UnsupportedOperationException("zookeeper collie doesn't support to doCreator function")

  override protected def nodeCollie: NodeCollie = node

  override protected def prefixKey: String = "fakezookeeper"
}
