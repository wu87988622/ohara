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

import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterStatus
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterStatus
import com.island.ohara.common.setting.ObjectKey

import scala.concurrent.{ExecutionContext, Future}

private class WorkerCollieImpl(node: NodeCollie, dockerCache: DockerClientCache, clusterCache: ClusterCache)
    extends BasicCollieImpl[WorkerClusterStatus](node, dockerCache, clusterCache)
    with WorkerCollie {

  override protected def postCreate(workerClusterStatus: WorkerClusterStatus,
                                    successfulContainers: Seq[ContainerInfo]): Unit =
    clusterCache.put(workerClusterStatus, clusterCache.get(workerClusterStatus) ++ successfulContainers)

  override protected def doCreator(executionContext: ExecutionContext,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: NodeApi.Node,
                                   route: Map[String, String]): Future[Unit] =
    Future.successful(try {
      dockerCache.exec(
        node,
        _.containerCreator()
          .imageName(containerInfo.imageName)
          // In --network=host mode, we don't need to export port for containers.
          //                          .portMappings(Map(clientPort -> clientPort))
          .hostname(containerInfo.hostname)
          .envs(containerInfo.environments)
          .name(containerInfo.name)
          .route(route)
          // [Before] we use --network=host for worker cluster since the connectors run on worker cluster may need to
          // access external system to request data. In ssh mode, dns service "may" be not deployed.
          // In order to simplify their effort, we directly mount host's route on the container.
          // This is not a normal case I'd say. However, we always meet special case which must be addressed
          // by this "special" solution...
          //.networkDriver(NETWORK_DRIVER)
          // [AFTER] Given that we have no use case about using port in custom connectors and there is no
          // similar case in other type (streamapp and k8s impl). Hence we change the network type from host to bridge
          .portMappings(
            containerInfo.portMappings.flatMap(_.portPairs).map(pair => pair.hostPort -> pair.containerPort).toMap)
          .create()
      )

    } catch {
      case e: Throwable =>
        try dockerCache.exec(node, _.forceRemove(containerName))
        catch {
          case _: Throwable =>
          // do nothing
        }
        LOG.error(s"failed to start ${containerInfo.imageName} on ${node.name}", e)
        None
    })

  override protected def hookOfNewRoute(node: Node, container: ContainerInfo, route: Map[String, String]): Unit = {
    updateRoute(node, container.name, route)
  }

  override protected def brokerContainers(classKey: ObjectKey)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    Future.successful(
      clusterCache.snapshot
        .filter(_._1.isInstanceOf[BrokerClusterStatus])
        .find(_._1.key == classKey)
        .map(_._2)
        .getOrElse(throw new NoSuchClusterException(s"broker cluster:$classKey doesn't exist. other broker clusters")))

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
