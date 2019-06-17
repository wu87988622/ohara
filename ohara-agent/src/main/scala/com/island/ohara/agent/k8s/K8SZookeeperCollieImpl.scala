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

import com.island.ohara.agent.{NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

private class K8SZookeeperCollieImpl(node: NodeCollie, k8sClient: K8SClient)
    extends K8SBasicCollieImpl[ZookeeperClusterInfo, ZookeeperCollie.ClusterCreator](node, k8sClient)
    with ZookeeperCollie {
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  override protected def doCreator(executionContext: ExecutionContext,
                                   clusterName: String,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: Node,
                                   route: Map[String, String]): Unit = {
    implicit val exec: ExecutionContext = executionContext
    // Kubernetes create pod async to need to await container create completely.
    Await.result(
      k8sClient
        .containerCreator()
        .imageName(containerInfo.imageName)
        .portMappings(
          containerInfo.portMappings.flatMap(_.portPairs).map(pair => pair.hostPort -> pair.containerPort).toMap)
        .nodename(containerInfo.nodeName)
        .hostname(s"${containerInfo.name}$DIVIDER${node.name}")
        .labelName(OHARA_LABEL)
        .domainName(K8S_DOMAIN_NAME)
        .envs(containerInfo.environments)
        .name(containerInfo.name)
        .run(),
      TIMEOUT
    )
  }

  override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
    toZookeeperCluster(clusterName, containers)

  override protected def doRemoveNode(previousCluster: ZookeeperClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

  override protected def doAddNode(
    previousCluster: ZookeeperClusterInfo,
    previousContainers: Seq[ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = Future.failed(
    new UnsupportedOperationException("zookeeper collie doesn't support to add node from a running cluster"))

  override protected def nodeCollie: NodeCollie = node

  override protected def prefixKey: String = PREFIX_KEY
}
