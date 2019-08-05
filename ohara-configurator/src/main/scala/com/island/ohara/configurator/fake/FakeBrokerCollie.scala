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

import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.agent.{BrokerCollie, ClusterState, NodeCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.{ClusterInfo, ContainerApi, NodeApi}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.kafka.TopicAdmin
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.kafka.TopicMeter

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeBrokerCollie(node: NodeCollie, bkConnectionProps: String)
    extends FakeCollie[BrokerClusterInfo, BrokerCollie.ClusterCreator](node)
    with BrokerCollie {

  override def topicMeters(cluster: BrokerClusterInfo): Seq[TopicMeter] =
    // we don't care for the fake mode since both fake mode and embedded mode are run on local jvm
    BeanChannel.local().topicMeters().asScala

  /**
    * cache all topics info in-memory so we should keep instance for each fake cluster.
    */
  private[this] val fakeAdminCache = new ConcurrentHashMap[BrokerClusterInfo, FakeTopicAdmin]

  override def creator: BrokerCollie.ClusterCreator =
    (_, clusterName, imageName, zookeeperClusterName, clientPort, exporterPort, jmxPort, nodeNames) =>
      Future.successful(
        addCluster(
          BrokerClusterInfo(
            name = clusterName,
            imageName = imageName,
            clientPort = clientPort,
            exporterPort = exporterPort,
            jmxPort = jmxPort,
            zookeeperClusterName = Some(zookeeperClusterName),
            nodeNames = nodeNames,
            deadNodes = Set.empty,
            // In fake mode, we need to assign a state in creation for "GET" method to act like real case
            state = Some(ClusterState.RUNNING.name),
            error = None,
            tags = Map.empty,
            lastModified = CommonUtils.current(),
            topicSettingDefinitions = BrokerCollie.TOPIC_CUSTOM_DEFINITIONS
          )))

  override protected def doRemoveNode(previousCluster: BrokerClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] = Future
    .successful(
      addCluster(BrokerClusterInfo(
        name = previousCluster.name,
        imageName = previousCluster.imageName,
        zookeeperClusterName = previousCluster.zookeeperClusterName,
        exporterPort = previousCluster.exporterPort,
        clientPort = previousCluster.clientPort,
        jmxPort = previousCluster.jmxPort,
        nodeNames = previousCluster.nodeNames.filterNot(_ == beRemovedContainer.nodeName),
        deadNodes = Set.empty,
        // In fake mode, we need to assign a state in creation for "GET" method to act like real case
        state = Some(ClusterState.RUNNING.name),
        error = None,
        tags = Map.empty,
        lastModified = CommonUtils.current(),
        topicSettingDefinitions = BrokerCollie.TOPIC_CUSTOM_DEFINITIONS
      )))
    .map(_ => true)

  override def topicAdmin(cluster: BrokerClusterInfo): TopicAdmin =
    if (bkConnectionProps == null) {
      val fake = new FakeTopicAdmin
      val r = fakeAdminCache.putIfAbsent(cluster, fake)
      if (r == null) fake else r
    } else TopicAdmin(bkConnectionProps)

  override protected def doAddNode(
    previousCluster: BrokerClusterInfo,
    previousContainers: Seq[ContainerApi.ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = Future.successful(
    addCluster(
      BrokerClusterInfo(
        name = previousCluster.name,
        imageName = previousCluster.imageName,
        zookeeperClusterName = previousCluster.zookeeperClusterName,
        clientPort = previousCluster.clientPort,
        exporterPort = previousCluster.exporterPort,
        jmxPort = previousCluster.jmxPort,
        nodeNames = previousCluster.nodeNames ++ Set(newNodeName),
        deadNodes = Set.empty,
        // In fake mode, we need to assign a state in creation for "GET" method to act like real case
        state = Some(ClusterState.RUNNING.name),
        error = None,
        tags = Map.empty,
        lastModified = CommonUtils.current(),
        topicSettingDefinitions = BrokerCollie.TOPIC_CUSTOM_DEFINITIONS
      )))

  override protected def doCreator(executionContext: ExecutionContext,
                                   clusterName: String,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: NodeApi.Node,
                                   route: Map[String, String]): Future[Unit] =
    throw new UnsupportedOperationException("Fake broker collie doesn't support doCreator function")

  override protected def zookeeperClusters(
    implicit executionContext: ExecutionContext): Future[Map[ClusterInfo, Seq[ContainerInfo]]] =
    throw new UnsupportedOperationException("Fake broker doesn't support zookeeperCluster function")

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
}
