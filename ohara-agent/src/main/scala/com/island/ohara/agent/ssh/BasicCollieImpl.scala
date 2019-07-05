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

package com.island.ohara.agent.ssh

import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.agent.{ClusterCache, ContainerCollie, NoSuchClusterException, NodeCollie}
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}
private abstract class BasicCollieImpl[T <: ClusterInfo: ClassTag, Creator <: ClusterCreator[T]](
  nodeCollie: NodeCollie,
  dockerCache: DockerClientCache,
  clusterCache: ClusterCache)
    extends ContainerCollie[T, Creator](nodeCollie) {

  final override def clusterWithAllContainers(
    implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] = {

    Future.successful(
      clusterCache.snapshot.filter(entry => classTag[T].runtimeClass.isInstance(entry._1)).map {
        case (cluster, containers) => cluster.asInstanceOf[T] -> containers
      }
    )
  }

  final override def cluster(name: String)(
    implicit executionContext: ExecutionContext): Future[(T, Seq[ContainerInfo])] =
    Future.successful(
      clusterCache.snapshot
        .filter(entry => classTag[T].runtimeClass.isInstance(entry._1))
        .map {
          case (cluster, containers) => cluster.asInstanceOf[T] -> containers
        }
        .find(_._1.name == name)
        .getOrElse(throw new NoSuchClusterException(s"$name doesn't exist")))

  def updateRoute(node: Node, containerName: String, route: Map[String, String]): Unit =
    dockerCache.exec(node,
                     _.containerInspector(containerName)
                       .asRoot()
                       .append("/etc/hosts", route.map {
                         case (hostname, ip) => s"$ip $hostname"
                       }.toSeq))

  override protected def doForceRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    remove(clusterInfo, containerInfos, true)

  override protected def doRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    remove(clusterInfo, containerInfos, false)

  private[this] def remove(clusterInfo: T, containerInfos: Seq[ContainerInfo], force: Boolean)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future
      .traverse(containerInfos) { containerInfo =>
        nodeCollie
          .node(containerInfo.nodeName)
          .map(node =>
            dockerCache.exec(
              node,
              client =>
                if (force) client.forceRemove(containerInfo.name)
                else {
                  // by default, docker will try to stop container for 10 seconds
                  // after that, docker will issue a kill signal to the container
                  client.stop(containerInfo.name)
                  client.remove(containerInfo.name)
              }
          ))
      }
      .map { _ =>
        clusterCache.remove(clusterInfo)
        true
      }

  override def logs(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] = nodeCollie
    .nodes()
    .flatMap(Future.traverse(_)(
      dockerCache.exec(_, _.containers(_.startsWith(s"$PREFIX_KEY$DIVIDER$clusterName$DIVIDER$serviceName")))))
    .map(_.flatten)
    .flatMap { containers =>
      Future
        .sequence(containers.map { container =>
          nodeCollie.node(container.nodeName).map { node =>
            container -> dockerCache.exec(node,
                                          client =>
                                            try client.log(container.name)
                                            catch {
                                              case _: Throwable => s"failed to get log from ${container.name}"
                                          })
          }
        })
        .map(_.toMap)
    }

  override protected def doRemoveNode(previousCluster: T, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] = {
    nodeCollie.node(beRemovedContainer.nodeName).map { node =>
      dockerCache.exec(node, _.stop(beRemovedContainer.name))
      clusterCache.put(
        previousCluster.clone(
          newNodeNames = previousCluster.nodeNames.filter(_ != beRemovedContainer.nodeName)
        ),
        clusterCache.get(previousCluster).filter(_.name != beRemovedContainer.name)
      )
      true
    }
  }

  override protected def doAddNode(previousCluster: T, previousContainers: Seq[ContainerInfo], newNodeName: String)(
    implicit executionContext: ExecutionContext): Future[T] =
    creator().copy(previousCluster).nodeName(newNodeName).threadPool(executionContext).create()
}
