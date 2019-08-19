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
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}
private class FakeBrokerCollie(node: NodeCollie,
                               zkContainers: Seq[ContainerInfo],
                               bkExistContainers: Seq[ContainerInfo])
    extends BrokerCollie {
  override protected def zookeeperClusters(
    implicit executionContext: ExecutionContext): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = Future.successful(
    Map(
      ZookeeperClusterInfo(
        FakeBrokerCollie.zookeeperClusterName,
        ZookeeperApi.IMAGE_NAME_DEFAULT,
        2181,
        2182,
        2183,
        Set("node1"),
        Set.empty,
        Map.empty,
        0L,
        Some(ContainerState.RUNNING.name),
        None
      ) -> zkContainers,
    )
  )

  override protected def doCreator(executionContext: ExecutionContext,
                                   clusterName: String,
                                   containerName: String,
                                   containerInfo: ContainerApi.ContainerInfo,
                                   node: NodeApi.Node,
                                   route: Map[String, String]): Future[Unit] = {
    // Nothing
    Future.unit
  }

  override protected def doRemove(clusterInfo: BrokerClusterInfo, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    throw new UnsupportedOperationException("Not support remove function")

  override def logs(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] =
    throw new UnsupportedOperationException("Not support logs function")

  override def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext): Future[Map[BrokerClusterInfo, Seq[ContainerInfo]]] =
    Future.successful(
      //Pre create broker container for test
      Map(
        BrokerClusterInfo(
          name = "bk1",
          imageName = BrokerApi.IMAGE_NAME_DEFAULT,
          zookeeperClusterName = "zk1",
          clientPort = 2181,
          exporterPort = 2182,
          jmxPort = 2183,
          nodeNames = Set("node1"),
          deadNodes = Set.empty,
          tags = Map.empty,
          lastModified = CommonUtils.current(),
          state = None,
          error = None,
          topicSettingDefinitions = TopicApi.TOPIC_DEFINITIONS
        ) -> bkExistContainers)
    )

  override protected def resolveHostName(node: String): String = "1.1.1.1"

  override protected def doAddNode(
    previousCluster: BrokerClusterInfo,
    previousContainers: Seq[ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] =
    throw new UnsupportedOperationException("Not support addNode function")

  override protected def doRemoveNode(previousCluster: BrokerClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    throw new UnsupportedOperationException("Not support removeNode function")

  /**
    * Please setting nodeCollie to implement class
    *
    * @return
    */
  override protected def nodeCollie: NodeCollie = node

  /**
    * Implement prefix name for the platform
    *
    * @return
    */
  override protected def prefixKey: String = "fakebroker"

  // In fake mode, we don't care the cluster state
  override protected def toClusterState(containers: Seq[ContainerInfo]): Option[ClusterState] =
    Some(ClusterState.RUNNING)
}

object FakeBrokerCollie {
  val zookeeperClusterName: String = "zk1"
}
