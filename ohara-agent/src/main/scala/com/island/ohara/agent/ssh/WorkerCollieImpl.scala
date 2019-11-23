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

import com.island.ohara.agent.{DataCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.{ClusterStatus, NodeApi}

import scala.concurrent.{ExecutionContext, Future}

private class WorkerCollieImpl(val dataCollie: DataCollie, dockerCache: DockerClientCache, clusterCache: ServiceCache)
    extends BasicCollieImpl(dataCollie, dockerCache, clusterCache)
    with WorkerCollie {
  protected implicit val kind: ClusterStatus.Kind = ClusterStatus.Kind.WORKER

  override protected def postCreate(
    workerClusterStatus: ClusterStatus
  ): Unit =
    clusterCache.put(workerClusterStatus)

  override protected def doCreator(
    executionContext: ExecutionContext,
    containerInfo: ContainerInfo,
    node: NodeApi.Node,
    route: Map[String, String],
    arguments: Seq[String]
  ): Future[Unit] =
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
          // similar case in other type (stream and k8s impl). Hence we change the network type from host to bridge
          .portMappings(
            containerInfo.portMappings.map(portMapping => portMapping.hostPort -> portMapping.containerPort).toMap
          )
          .arguments(arguments)
          .create()
      )
    } catch {
      case e: Throwable =>
        try dockerCache.exec(node, _.forceRemove(containerInfo.name))
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

  /**
    * Implement prefix name for paltform
    *
    * @return
    */
  override protected def prefixKey: String = PREFIX_KEY
}
