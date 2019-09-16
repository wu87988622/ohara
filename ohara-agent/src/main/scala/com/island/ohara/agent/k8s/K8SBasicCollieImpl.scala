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

import com.island.ohara.agent.{ClusterState, Collie, NodeCollie}
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.Releasable

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

private[this] abstract class K8SBasicCollieImpl[T <: ClusterInfo: ClassTag](nodeCollie: NodeCollie,
                                                                            k8sClient: K8SClient)
    extends Collie[T]
    with Releasable {

  protected def toClusterDescription(key: ObjectKey, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[T]

  override protected def doAddNode(previousCluster: T, previousContainers: Seq[ContainerInfo], newNodeName: String)(
    implicit executionContext: ExecutionContext): Future[T] =
    // create the cluster with more nodes again. the creation progress handles the "add" than "creation" by default
    creator
      .settings(previousCluster.settings)
      .nodeNames(previousCluster.nodeNames + newNodeName)
      .threadPool(executionContext)
      .create()

  override protected def doRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] = {
    Future.sequence(containerInfos.map(c => k8sClient.remove(c.name))).map(_.nonEmpty)
  }

  override protected def doForceRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] = {
    Future.sequence(containerInfos.map(c => k8sClient.forceRemove(c.name))).map(_.nonEmpty)
  }

  override protected def doRemoveNode(previousCluster: T, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] = {
    k8sClient.removeNode(beRemovedContainer.name, beRemovedContainer.nodeName, serviceName).map(_ => true)
  }

  override def logs(objectKey: ObjectKey)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] = nodeCollie
    .nodes()
    .flatMap(
      nodes => filterContainerService(nodes)
    )
    .flatMap(
      cs =>
        Future.sequence(
          cs.filter(container =>
              Collie.objectKeyOfContainerName(container.name) == objectKey && container.name.contains(serviceName))
            .map(container => k8sClient.log(container.name).map(container -> _))
      ))
    .map(_.toMap)

  override def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] = nodeCollie
    .nodes()
    .flatMap(
      nodes => filterContainerService(nodes)
    )
    .map(f => {
      f.map(container => Collie.objectKeyOfContainerName(container.name) -> container)
        .groupBy(_._1)
        .map {
          case (objectKey, value) => objectKey -> value.map(_._2)
        }
        .map {
          case (objectKey, containers) => toClusterDescription(objectKey, containers).map(_ -> containers)
        }
        .toSeq
    })
    .flatMap(Future.sequence(_))
    .map(_.toMap)

  override def close(): Unit = {
    Releasable.close(k8sClient)
  }

  private[this] def filterContainerService(nodes: Seq[Node])(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = {
    Future
      .sequence(
        nodes.map(node => {
          k8sClient
            .containers()
            .map(cs =>
              cs.filter(_.name.split(DIVIDER).length >= 4) //Container name format is PREFIX_KEY-GROUP-CLUSTER_NAME-SERVICE-HASH
                .filter(x => {
                  x.nodeName.equals(node.name) && x.name
                    .startsWith(PREFIX_KEY) && x.name.split(DIVIDER)(3).equals(serviceName)
                }))
        })
      )
      .map(_.flatten)
  }

  override protected def toClusterState(containers: Seq[ContainerInfo]): Option[ClusterState] = {
    if (containers.isEmpty) None
    else {
      // we use a "pod" as a container of ohara cluster, so it is more easy to define a cluster state than docker
      // since a "pod" in k8s is actually an application with multiple containers...
      if (containers.exists(_.state == K8sContainerState.RUNNING.name)) Some(ClusterState.RUNNING)
      else if (containers.exists(_.state == K8sContainerState.FAILED.name)) Some(ClusterState.FAILED)
      else if (containers.exists(_.state == K8sContainerState.PENDING.name)) Some(ClusterState.PENDING)
      // All Containers in the Pod have terminated in success, BUT it is still failed :(
      else if (containers.exists(_.state == K8sContainerState.SUCCEEDED.name)) Some(ClusterState.FAILED)
      else Some(ClusterState.UNKNOWN)
    }
  }
}
