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
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.common.setting.ObjectKey
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}

private class K8SWorkerCollieImpl(node: NodeCollie, bkCollie: BrokerCollie, k8sClient: K8SClient)
    extends K8SBasicCollieImpl[WorkerClusterInfo](node, k8sClient)
    with WorkerCollie {
  private[this] val LOG = Logger(classOf[K8SWorkerCollieImpl])

  override protected def toClusterDescription(key: ObjectKey, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[WorkerClusterInfo] = toWorkerCluster(key, containers)

  override protected def doCreator(executionContext: ExecutionContext,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: NodeApi.Node,
                                   route: Map[String, String]): Future[Unit] = {
    implicit val exec: ExecutionContext = executionContext
    k8sClient
      .containerCreator()
      .imageName(containerInfo.imageName)
      .portMappings(
        containerInfo.portMappings.flatMap(_.portPairs).map(pair => pair.hostPort -> pair.containerPort).toMap)
      // this hostname has a length limit that <=63
      // see https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#syntax-and-character-set
      // we change the actual value to containerName here which is always <= 63 (prefix-group-name-service-hash)
      // and it won't hurt the Ohara system or user since it is unused after setting...by Sam
      .hostname(containerInfo.name)
      .nodeName(node.name)
      .envs(containerInfo.environments)
      .labelName(OHARA_LABEL)
      .domainName(K8S_DOMAIN_NAME)
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

  override protected def brokerContainers(classKey: ObjectKey)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    bkCollie
      .clusters()
      .map(
        _.find(_._1.key == classKey)
          .map(_._2)
          .getOrElse(throw new NoSuchClusterException(s"broker cluster:$classKey does not exist")))

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
