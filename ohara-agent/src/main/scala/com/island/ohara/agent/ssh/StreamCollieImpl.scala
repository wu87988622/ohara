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

import com.island.ohara.agent.{DataCollie, StreamCollie}
import com.island.ohara.client.configurator.v0.ClusterStatus
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node

import scala.concurrent.{ExecutionContext, Future}
private class StreamCollieImpl(dataCollie: DataCollie, dockerCache: DockerClientCache, clusterCache: ServiceCache)
    extends BasicCollieImpl(dataCollie, dockerCache, clusterCache)
    with StreamCollie {
  override protected def doCreator(
    executionContext: ExecutionContext,
    containerInfo: ContainerInfo,
    node: Node,
    route: Map[String, String],
    arguments: Seq[String]
  ): Future[Unit] =
    Future.successful(try {
      dockerCache.exec(
        node,
        _.containerCreator()
          .imageName(containerInfo.imageName)
          .hostname(containerInfo.name)
          .envs(containerInfo.environments)
          .name(containerInfo.name)
          .portMappings(
            containerInfo.portMappings.map(portMapping => portMapping.hostPort -> portMapping.containerPort).toMap
          )
          .route(route)
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

  override protected def postCreate(
    clusterStatus: ClusterStatus
  ): Unit =
    clusterCache.put(clusterStatus)
}
