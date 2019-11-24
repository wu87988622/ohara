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
import com.island.ohara.agent.{Collie, DataCollie, ServiceState}
import com.island.ohara.client.configurator.v0.ClusterStatus
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.setting.ObjectKey

import scala.concurrent.{ExecutionContext, Future}
private abstract class BasicCollieImpl(
  dataCollie: DataCollie,
  dockerCache: DockerClientCache,
  clusterCache: ServiceCache
) extends Collie {
  /**
    * all ssh collies share the single cache, and we need a way to distinguish the difference status from different
    * services. Hence, this implicit field is added to cache to find out the cached data belonging to this collie.
    */
  protected implicit val kind: ClusterStatus.Kind

  final override def clusters()(implicit executionContext: ExecutionContext): Future[Seq[ClusterStatus]] =
    Future.successful(clusterCache.snapshot.filter(_.kind == kind))

  protected def updateRoute(node: Node, containerName: String, route: Map[String, String]): Unit =
    dockerCache.exec(
      node,
      _.containerInspector(containerName)
        .asRoot()
        .append("/etc/hosts", route.map {
          case (hostname, ip) => s"$ip $hostname"
        }.toSeq)
    )

  override protected def doForceRemove(clusterInfo: ClusterStatus, beRemovedContainers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext
  ): Future[Boolean] =
    remove(clusterInfo, beRemovedContainers, true)

  override protected def doRemove(clusterInfo: ClusterStatus, beRemovedContainers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext
  ): Future[Boolean] =
    remove(clusterInfo, beRemovedContainers, false)

  private[this] def remove(clusterInfo: ClusterStatus, beRemovedContainers: Seq[ContainerInfo], force: Boolean)(
    implicit executionContext: ExecutionContext
  ): Future[Boolean] =
    Future
      .traverse(beRemovedContainers) { containerInfo =>
        dataCollie
          .value[Node](containerInfo.nodeName)
          .map(
            node =>
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
              )
          )
      }
      .map { _ =>
        val newContainers =
          clusterInfo.containers.filterNot(container => beRemovedContainers.exists(_.name == container.name))
        if (newContainers.isEmpty) clusterCache.remove(clusterInfo)
        else clusterCache.put(clusterInfo.copy(containers = newContainers))
        // return true if it does remove something
        newContainers.size != clusterInfo.containers.size
      }

  override def logs(key: ObjectKey, sinceSeconds: Option[Long])(
    implicit executionContext: ExecutionContext
  ): Future[Map[ContainerInfo, String]] =
    dataCollie
      .values[Node]()
      .flatMap(
        Future.traverse(_)(
          // form: PREFIX_KEY-GROUP-CLUSTER_NAME-SERVICE-HASH
          dockerCache.exec(
            _,
            _.containers()
              .map(_.filter(container => Collie.objectKeyOfContainerName(container.name) == key))
          )
        )
      )
      .map(_.flatten)
      .flatMap { containers =>
        Future
          .sequence(containers.map { container =>
            dataCollie.value[Node](container.nodeName).map { node =>
              container -> dockerCache.exec(
                node,
                client =>
                  try client.log(container.name, sinceSeconds)
                  catch {
                    case _: Throwable => s"failed to get log from ${container.name}"
                  }
              )
            }
          })
          .map(_.toMap)
      }

  override protected def toClusterState(containers: Seq[ContainerInfo]): Option[ServiceState] =
    if (containers.isEmpty) None
    else {
      // one of the containers in pending state means cluster pending
      if (containers.exists(_.state == ContainerState.CREATED.name)) Some(ServiceState.PENDING)
      // not pending, if one of the containers in running state means cluster running (even other containers are in
      // restarting, paused, exited or dead state
      else if (containers.exists(_.state == ContainerState.RUNNING.name)) Some(ServiceState.RUNNING)
      // since cluster(collie) is a collection of long running containers,
      // we could assume cluster failed if containers are run into "exited" or "dead" state
      else if (containers.forall(c => c.state == ContainerState.EXITED.name || c.state == ContainerState.DEAD.name))
        Some(ServiceState.FAILED)
      // we set failed state is ok here
      // since there are too many cases that we could not handle for now, we should open the door for whitelist only
      else Some(ServiceState.FAILED)
    }
}
