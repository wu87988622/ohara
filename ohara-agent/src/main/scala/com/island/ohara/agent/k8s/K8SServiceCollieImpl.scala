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
import com.island.ohara.agent.container.ContainerName
import com.island.ohara.client.configurator.v0.NodeApi.{Node, Resource}

import scala.concurrent.{ExecutionContext, Future}

// accessible to configurator
private[ohara] class K8SServiceCollieImpl(dataCollie: DataCollie, k8sClient: K8SClient) extends ServiceCollie {
  override val zookeeperCollie: ZookeeperCollie = new K8SBasicCollieImpl(dataCollie, k8sClient) with ZookeeperCollie

  override val brokerCollie: BrokerCollie = new K8SBasicCollieImpl(dataCollie, k8sClient) with BrokerCollie

  override val workerCollie: WorkerCollie = new K8SBasicCollieImpl(dataCollie, k8sClient) with WorkerCollie

  override val streamCollie: StreamCollie = new K8SBasicCollieImpl(dataCollie, k8sClient) with StreamCollie

  override def imageNames()(implicit executionContext: ExecutionContext): Future[Map[Node, Seq[String]]] =
    dataCollie.values[Node]().flatMap { nodes =>
      Future
        .traverse(nodes) { node =>
          k8sClient.imageNames(node.name).map(images => node -> images)
        }
        .map(_.toMap)
    }

  override def verifyNode(node: Node)(implicit executionContext: ExecutionContext): Future[String] =
    k8sClient
      .checkNode(node.name)
      .map(report => {
        val statusInfo = report.statusInfo.getOrElse(K8SStatusInfo(false, s"${node.name} node doesn't exists."))
        if (statusInfo.isHealth) s"${node.name} node is running."
        else
          throw new IllegalStateException(s"${node.name} node doesn't running container. cause: ${statusInfo.message}")
      })

  override def containerNames()(implicit executionContext: ExecutionContext): Future[Seq[ContainerName]] =
    k8sClient.containerNames()

  override def log(containerName: String, sinceSeconds: Option[Long])(
    implicit executionContext: ExecutionContext
  ): Future[(ContainerName, String)] =
    k8sClient.log(containerName, sinceSeconds)

  override def resources()(implicit executionContext: ExecutionContext): Future[Map[String, Seq[Resource]]] =
    k8sClient.resources()

  override def close(): Unit = {
    // do nothing
  }
}
