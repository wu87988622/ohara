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

import com.island.ohara.agent.{ServiceState, Collie, DataCollie}
import com.island.ohara.client.configurator.v0.ClusterStatus
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.setting.ObjectKey
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

private[this] abstract class K8SBasicCollieImpl[T <: ClusterStatus: ClassTag](
  dataCollie: DataCollie,
  k8sClient: K8SClient
) extends Collie[T] {
  override protected def doRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext
  ): Future[Boolean] = {
    Future.sequence(containerInfos.map(c => k8sClient.remove(c.name))).map(_.nonEmpty)
  }

  override protected def doForceRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext
  ): Future[Boolean] = {
    Future.sequence(containerInfos.map(c => k8sClient.forceRemove(c.name))).map(_.nonEmpty)
  }

  override protected def doRemoveNode(previousCluster: T, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext
  ): Future[Boolean] =
    k8sClient.remove(beRemovedContainer.name).map(_ => true)

  override def logs(objectKey: ObjectKey, sinceSeconds: Option[Long])(
    implicit executionContext: ExecutionContext
  ): Future[Map[ContainerInfo, String]] =
    dataCollie
      .values[Node]()
      .flatMap(
        nodes => filterContainerService(nodes)
      )
      .flatMap(
        cs =>
          Future.sequence(
            cs.filter(
                container =>
                  Collie.objectKeyOfContainerName(container.name) == objectKey && container.name.contains(serviceName)
              )
              .map(container => k8sClient.log(container.name, sinceSeconds).map(container -> _))
          )
      )
      .map(_.toMap)

  override def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext
  ): Future[Map[T, Seq[ContainerInfo]]] =
    dataCollie
      .values[Node]()
      .flatMap(filterContainerService)
      .map(
        _.map(container => Collie.objectKeyOfContainerName(container.name) -> container)
          .groupBy(_._1)
          .map {
            case (objectKey, value) => objectKey -> value.map(_._2)
          }
          .map {
            case (objectKey, containers) => toStatus(objectKey, containers).map(_ -> containers)
          }
          .toSeq
      )
      .flatMap(Future.sequence(_))
      .map(_.toMap)

  private[this] def filterContainerService(
    nodes: Seq[Node]
  )(implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    Future
      .sequence(
        nodes.map { node =>
          k8sClient
            .containers()
            .map(
              _.filter(_.name.split(DIVIDER).length >= 4) //Container name format is PREFIX_KEY-GROUP-CLUSTER_NAME-SERVICE-HASH
                .filter { container =>
                  container.nodeName.equals(node.name) && container.name
                    .startsWith(PREFIX_KEY) && container.name.split(DIVIDER)(3).equals(serviceName)
                }
            )
        }
      )
      .map(_.flatten)

  override protected def toClusterState(containers: Seq[ContainerInfo]): Option[ServiceState] =
    if (containers.isEmpty) None
    else {
      // we use a "pod" as a container of ohara cluster, so it is more easy to define a cluster state than docker
      // since a "pod" in k8s is actually an application with multiple containers...
      if (containers.exists(_.state == K8sContainerState.RUNNING.name)) Some(ServiceState.RUNNING)
      else if (containers.exists(_.state == K8sContainerState.FAILED.name)) Some(ServiceState.FAILED)
      else if (containers.exists(_.state == K8sContainerState.PENDING.name)) Some(ServiceState.PENDING)
      // All Containers in the Pod have terminated in success, BUT it is still failed :(
      else if (containers.exists(_.state == K8sContainerState.SUCCEEDED.name)) Some(ServiceState.FAILED)
      else Some(ServiceState.UNKNOWN)
    }
}
