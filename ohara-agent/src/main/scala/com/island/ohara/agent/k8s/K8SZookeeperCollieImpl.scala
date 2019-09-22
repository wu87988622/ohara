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
import com.island.ohara.common.setting.ObjectKey
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}

private class K8SZookeeperCollieImpl(node: NodeCollie, k8sClient: K8SClient)
    extends K8SBasicCollieImpl[ZookeeperClusterInfo](node, k8sClient)
    with ZookeeperCollie {
  private[this] val LOG = Logger(classOf[K8SZookeeperCollieImpl])

  override protected def doCreator(executionContext: ExecutionContext,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: Node,
                                   route: Map[String, String]): Future[Unit] = {
    implicit val exec: ExecutionContext = executionContext
    k8sClient
      .containerCreator()
      .imageName(containerInfo.imageName)
      .portMappings(
        containerInfo.portMappings.flatMap(_.portPairs).map(pair => pair.hostPort -> pair.containerPort).toMap)
      .nodeName(containerInfo.nodeName)
      // this hostname has a length limit that <=63
      // see https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#syntax-and-character-set
      // zookeeper doesn't have advertised hostname/port so we assign the "docker host" directly
      // Note: We should assign "node" name to the container hostname directly here to avoid some
      // dns problem. For example, we may want to connect to zk to dig something issue and assign
      // node name here can save our life to solve the connection problem...
      .hostname(node.name)
      .labelName(OHARA_LABEL)
      .domainName(K8S_DOMAIN_NAME)
      .envs(containerInfo.environments)
      .name(containerInfo.name)
      .threadPool(executionContext)
      .create()
      .recover {
        case e: Throwable =>
          LOG.error(s"failed to start ${containerInfo.imageName} on ${node.name}", e)
          None
      }
      .map(_ => Unit)
  }

  override protected def toClusterDescription(key: ObjectKey, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
    toZookeeperCluster(key, containers)

  override protected def doRemoveNode(previousCluster: ZookeeperClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

  override protected def nodeCollie: NodeCollie = node

  override protected def prefixKey: String = PREFIX_KEY
}
