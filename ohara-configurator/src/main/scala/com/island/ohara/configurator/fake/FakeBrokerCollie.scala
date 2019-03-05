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

import com.island.ohara.agent.BrokerCollie
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.kafka.TopicAdmin

import scala.concurrent.Future

private[configurator] class FakeBrokerCollie(bkConnectionProps: String)
    extends FakeCollie[BrokerClusterInfo, BrokerCollie.ClusterCreator]
    with BrokerCollie {

  /**
    * cache all topics info in-memory so we should keep instance for each fake cluster.
    */
  private[this] val fakeAdminCache = new ConcurrentHashMap[BrokerClusterInfo, FakeTopicAdmin]
  override def creator(): BrokerCollie.ClusterCreator =
    (clusterName, imageName, zookeeperClusterName, clientPort, exporterPort, nodeNames) =>
      Future.successful(
        addCluster(
          FakeBrokerClusterInfo(
            name = clusterName,
            imageName = imageName,
            clientPort = clientPort,
            exporterPort = exporterPort,
            zookeeperClusterName = zookeeperClusterName,
            nodeNames = nodeNames
          )))

  override def removeNode(clusterName: String, nodeName: String): Future[BrokerClusterInfo] = {
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
            nodeNames = previous.nodeNames.filterNot(_ == nodeName)
          )))
  }

  override def addNode(clusterName: String, nodeName: String): Future[BrokerClusterInfo] = {
    val previous = clusterCache.find(_._1.name == clusterName).get._1
    if (previous.nodeNames.contains(nodeName))
      Future.failed(new IllegalArgumentException(s"$nodeName already run on $clusterName!!!"))
    else
      Future.successful(
        addCluster(
          FakeBrokerClusterInfo(
            name = previous.name,
            imageName = previous.imageName,
            zookeeperClusterName = previous.zookeeperClusterName,
            clientPort = previous.clientPort,
            exporterPort = previous.exporterPort,
            nodeNames = previous.nodeNames :+ nodeName
          )))
  }
  override def topicAdmin(cluster: BrokerClusterInfo): TopicAdmin =
    // < 0 means it is a fake cluster
    if (cluster.isInstanceOf[FakeBrokerClusterInfo]) {
      val fake = new FakeTopicAdmin
      val r = fakeAdminCache.putIfAbsent(cluster, fake)
      if (r == null) fake else r
    } else TopicAdmin(bkConnectionProps)
}
