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

import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.agent.{ContainerCollie, NodeCollie}
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.Releasable

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

private[this] abstract class K8SBasicCollieImpl[T <: ClusterInfo: ClassTag, Creator <: ClusterCreator[T]](
  nodeCollie: NodeCollie,
  k8sClient: K8SClient)
    extends ContainerCollie[T, Creator](nodeCollie: NodeCollie)
    with Releasable {

  protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[T]

  override protected def doAddNode(previousCluster: T, previousContainers: Seq[ContainerInfo], newNodeName: String)(
    implicit executionContext: ExecutionContext): Future[T] =
    creator.copy(previousCluster).nodeName(newNodeName).threadPool(executionContext).create()

  override protected def doRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] = {
    Future.sequence(containerInfos.map(c => k8sClient.remove(c.name))).map(_.nonEmpty)
  }

  override protected def doRemoveNode(previousCluster: T, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] = {
    k8sClient
      .removeNode(s"$PREFIX_KEY$DIVIDER${previousCluster.name}$DIVIDER$serviceName",
                  beRemovedContainer.nodeName,
                  serviceName)
      .map(_ => true)
  }

  override def logs(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] =
    k8sClient
      .containers()
      .flatMap(
        cs =>
          Future.sequence(
            cs.filter(_.name.startsWith(s"$PREFIX_KEY$DIVIDER$clusterName"))
              .map(container => k8sClient.log(container.name).map(container -> _))
        ))
      .map(_.toMap)

  def query(clusterName: String, serviceName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = {
    nodeCollie.nodes().flatMap { nodes =>
      Future
        .sequence(nodes.map(_ => {
          k8sClient
            .containers()
            .map(cs => cs.filter(x => x.name.startsWith(s"$PREFIX_KEY$DIVIDER$clusterName$DIVIDER$serviceName")))
        }))
        .map(_.flatten)
    }
  }

  override def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] = nodeCollie
    .nodes()
    .flatMap(
      nodes => filterContainerService(nodes)
    )
    .map(f => {
      f.map(container => {
          container.name.split(DIVIDER)(1) -> container
        })
        .groupBy(_._1)
        .map {
          case (clusterName, value) => clusterName -> value.map(_._2)
        }
        .map {
          case (clusterName, containers) => toClusterDescription(clusterName, containers).map(_ -> containers)
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
              cs.filter(_.name.split(DIVIDER).length >= 3) //Container name format is PREFIX-CLUSTTERNAME-SERVICENAME-XXX
                .filter(x => {
                  x.nodeName.equals(node.name) && x.name
                    .startsWith(PREFIX_KEY) && x.name.split(DIVIDER)(2).equals(serviceName)
                }))
        })
      )
      .map(_.flatten)
  }
}
