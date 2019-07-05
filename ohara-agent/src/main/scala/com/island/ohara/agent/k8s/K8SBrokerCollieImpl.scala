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

package com.island.ohara.agent.k8s

import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.{ClusterInfo, NodeApi}
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

private class K8SBrokerCollieImpl(node: NodeCollie, zkCollie: ZookeeperCollie, k8sClient: K8SClient)
    extends K8SBasicCollieImpl[BrokerClusterInfo, BrokerCollie.ClusterCreator](node, k8sClient)
    with BrokerCollie {

  private[this] val LOG = Logger(classOf[K8SBrokerCollieImpl])
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  override protected def doCreator(executionContext: ExecutionContext,
                                   clusterName: String,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: NodeApi.Node,
                                   route: Map[String, String]): Unit = {
    implicit val exec: ExecutionContext = executionContext
    try {
      val creator: Future[Option[ContainerInfo]] = k8sClient
        .containerCreator()
        .imageName(containerInfo.imageName)
        .nodename(node.name)
        .labelName(OHARA_LABEL)
        .domainName(K8S_DOMAIN_NAME)
        .portMappings(
          containerInfo.portMappings.flatMap(_.portPairs).map(pair => pair.hostPort -> pair.containerPort).toMap)
        .hostname(s"${containerInfo.name}$DIVIDER${node.name}")
        .envs(containerInfo.environments)
        .name(containerInfo.name)
        .threadPool(executionContext)
        .create()
      Await.result(creator, TIMEOUT)
    } catch {
      case e: Throwable =>
        LOG.error(s"failed to start ${containerInfo.imageName} on ${node.name}", e)
        None
    }
  }

  override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = toBrokerCluster(clusterName, containers)
  override protected def zookeeperClusters(
    implicit executionContext: ExecutionContext): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = {
    zkCollie.clusters.asInstanceOf[Future[Map[ClusterInfo, Seq[ContainerInfo]]]]
  }

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
  override protected def prefixKey: String = PREFIX_KEY
}
