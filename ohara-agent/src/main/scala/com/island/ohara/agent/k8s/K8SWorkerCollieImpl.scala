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
import com.island.ohara.client.configurator.v0.{ClusterInfo, NodeApi}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, ExecutionContext, Future}

private class K8SWorkerCollieImpl(node: NodeCollie, bkCollie: BrokerCollie, k8sClient: K8SClient)
    extends K8SBasicCollieImpl[WorkerClusterInfo, WorkerCollie.ClusterCreator](node, k8sClient)
    with WorkerCollie {
  private[this] val LOG = Logger(classOf[K8SWorkerCollieImpl])
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[WorkerClusterInfo] = toWorkerCluster(clusterName, containers)

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
        .portMappings(
          containerInfo.portMappings.flatMap(_.portPairs).map(pair => pair.hostPort -> pair.containerPort).toMap)
        .hostname(s"${containerInfo.name}$DIVIDER${node.name}")
        .nodename(node.name)
        .envs(containerInfo.environments)
        .labelName(OHARA_LABEL)
        .domainName(K8S_DOMAIN_NAME)
        .name(containerInfo.name)
        .threadPool(executionContext)
        .create()
      Await.result(creator, TIMEOUT)
    } catch {
      case e: Throwable =>
        LOG.error(s"failed to start ${containerInfo.imageName}", e)
        None
    }
  }

  override protected def brokerClusters(
    implicit executionContext: ExecutionContext): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = {
    bkCollie.clusters().asInstanceOf[Future[Map[ClusterInfo, Seq[ContainerInfo]]]]
  }

  /**
    * Please implement nodeCollie
    *
    * @return
    */
  override protected def nodeCollie: NodeCollie = node

  /**
    * Implement prefix name for paltform
    *
    * @return
    */
  override protected def prefixKey: String = PREFIX_KEY
}
