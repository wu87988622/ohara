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

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{ClusterCache, ClusterState, Collie, NodeCollie}
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.setting.ObjectKey

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}
private abstract class BasicCollieImpl[T <: ClusterInfo: ClassTag](nodeCollie: NodeCollie,
                                                                   dockerCache: DockerClientCache,
                                                                   clusterCache: ClusterCache)
    extends Collie[T] {

  final override def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] = {

    Future.successful(
      clusterCache.snapshot.filter(entry => classTag[T].runtimeClass.isInstance(entry._1)).map {
        case (cluster, containers) => cluster.asInstanceOf[T] -> containers
      }
    )
  }

  protected def updateRoute(node: Node, containerName: String, route: Map[String, String]): Unit =
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

  override def logs(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] =
    nodeCollie
      .nodes()
      .flatMap(
        Future.traverse(_)(
          // form: PREFIX_KEY-GROUP-CLUSTER_NAME-SERVICE-HASH
          dockerCache
            .exec(_, _.containers(name => Collie.objectKeyOfContainerName(name) == key && name.contains(serviceName)))))
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
        previousCluster.newNodeNames(previousCluster.nodeNames.filter(_ != beRemovedContainer.nodeName)),
        clusterCache.get(previousCluster).filter(_.name != beRemovedContainer.name)
      )
      true
    }
  }

  override protected def toClusterState(containers: Seq[ContainerInfo]): Option[ClusterState] =
    if (containers.isEmpty) None
    else {
      // one of the containers in pending state means cluster pending
      if (containers.exists(_.state == ContainerState.CREATED.name)) Some(ClusterState.PENDING)
      // not pending, if one of the containers in running state means cluster running (even other containers are in
      // restarting, paused, exited or dead state
      else if (containers.exists(_.state == ContainerState.RUNNING.name)) Some(ClusterState.RUNNING)
      // since cluster(collie) is a collection of long running containers,
      // we could assume cluster failed if containers are run into "exited" or "dead" state
      else if (containers.forall(c => c.state == ContainerState.EXITED.name || c.state == ContainerState.DEAD.name))
        Some(ClusterState.FAILED)
      // we set failed state is ok here
      // since there are too many cases that we could not handle for now, we should open the door for whitelist only
      else Some(ClusterState.FAILED)
    }
}
