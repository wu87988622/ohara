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
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.{Releasable, ReleaseOnce}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

private[agent] class K8SClusterCollieImpl(nodeCollie: NodeCollie, k8sClient: K8SClient)(
  implicit executionContext: ExecutionContext)
    extends ReleaseOnce
    with ClusterCollie {

  override def zookeeperCollie(): ZookeeperCollie = new K8SZookeeperCollieImpl(nodeCollie, k8sClient, toZkCluster)

  override def brokerCollie(): BrokerCollie = new K8SBrokerCollieImpl(nodeCollie, k8sClient, toBkCluster)

  override def workerCollie(): WorkerCollie = new K8SWorkerCollieImpl(nodeCollie, k8sClient, toWkCluster)

  override protected def doClose(): Unit = Releasable.close(k8sClient)

  override def images(nodes: Seq[Node])(implicit executionContext: ExecutionContext): Future[Map[Node, Seq[String]]] =
    Future
      .traverse(nodes) { node =>
        k8sClient.images(node.name).map(images => node -> images)
      }
      .map(_.toMap)

  override def verifyNode(node: Node)(implicit executionContext: ExecutionContext): Future[Try[String]] =
    k8sClient
      .checkNode(node.name)
      .map(report => {
        val statusInfo = report.statusInfo.getOrElse(K8SStatusInfo(false, s"${node.name} node doesn't exists."))
        if (statusInfo.isHealth)
          Try(s"${node.name} node is running.")
        else
          Failure(
            new IllegalStateException(s"${node.name} node doesn't running container. cause: ${statusInfo.message}"))
      })
}
