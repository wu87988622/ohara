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
import com.island.ohara.agent.{BrokerCollie, NodeCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi
import com.island.ohara.client.kafka.TopicAdmin
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.kafka.TopicMeter
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeBrokerCollie(nodeCollie: NodeCollie, bkConnectionProps: String)
    extends FakeCollie[BrokerClusterInfo, BrokerCollie.ClusterCreator](nodeCollie)
    with BrokerCollie {

  override def topicMeters(cluster: BrokerClusterInfo): Seq[TopicMeter] =
    // we don't care for the fake mode since both fake mode and embedded mode are run on local jvm
    BeanChannel.local().topicMeters().asScala

  /**
    * cache all topics info in-memory so we should keep instance for each fake cluster.
    */
  private[this] val fakeAdminCache = new ConcurrentHashMap[BrokerClusterInfo, FakeTopicAdmin]

  override def creator(): BrokerCollie.ClusterCreator =
    (executionContext, clusterName, imageName, zookeeperClusterName, clientPort, exporterPort, jmxPort, nodeNames) =>
      Future.successful(
        addCluster(
          FakeBrokerClusterInfo(
            name = clusterName,
            imageName = imageName,
            clientPort = clientPort,
            exporterPort = exporterPort,
            jmxPort = jmxPort,
            zookeeperClusterName = zookeeperClusterName,
            nodeNames = nodeNames
          )))

  override def removeNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = {
    val previous = clusterCache.find(_._1.name == clusterName).get._1
    if (!previous.nodeNames.contains(nodeName))
      Future.failed(new IllegalArgumentException(s"$nodeName doesn't run on $clusterName!!!"))
    else
      Future.successful(
        addCluster(
          FakeBrokerClusterInfo(
            name = previous.name,
            imageName = previous.imageName,
            zookeeperClusterName = previous.zookeeperClusterName,
            exporterPort = previous.exporterPort,
            clientPort = previous.clientPort,
            jmxPort = previous.jmxPort,
            nodeNames = previous.nodeNames.filterNot(_ == nodeName)
          )))
  }

  override def topicAdmin(cluster: BrokerClusterInfo): TopicAdmin = cluster match {
    case _: FakeBrokerClusterInfo =>
      val fake = new FakeTopicAdmin
      val r = fakeAdminCache.putIfAbsent(cluster, fake)
      if (r == null) fake else r
    case _ => TopicAdmin(bkConnectionProps)
  }

  override protected def doAddNodeContainer(
    previousCluster: BrokerClusterInfo,
    previousContainers: Seq[ContainerApi.ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = {
    doAddNode(previousCluster, previousContainers, newNodeName).map(
      _ =>
        FakeBrokerClusterInfo(
          name = previousCluster.name,
          imageName = previousCluster.imageName,
          zookeeperClusterName = previousCluster.zookeeperClusterName,
          clientPort = previousCluster.clientPort,
          exporterPort = previousCluster.exporterPort,
          jmxPort = previousCluster.jmxPort,
          nodeNames = previousCluster.nodeNames :+ newNodeName
      ))
  }

  override protected def doRemoveNode(
    previousCluster: BrokerClusterInfo,
    previousContainer: ContainerApi.ContainerInfo,
    removedNodeName: String)(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] =
    throw new UnsupportedOperationException("Fake not support doRemoveNode function")
}
