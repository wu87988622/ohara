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

import com.island.ohara.agent.{NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}
private class ZookeeperCollieImpl(nodeCollie: NodeCollie,
                                  dockerCache: DockerClientCache,
                                  clusterCache: Cache[Map[ClusterInfo, Seq[ContainerInfo]]])
    extends BasicCollieImpl[ZookeeperClusterInfo, ZookeeperCollie.ClusterCreator](nodeCollie, dockerCache, clusterCache)
    with ZookeeperCollie {

  /**
    * This is a complicated process. We must address following issues.
    * 1) check the existence of cluster
    * 2) check the existence of nodes
    * 3) Each zookeeper container has got to export peer port, election port, and client port
    * 4) Each zookeeper container should use "docker host name" to replace "container host name".
    * 4) Add routes to all zookeeper containers
    * @return creator of broker cluster
    */
  override def creator(): ZookeeperCollie.ClusterCreator =
    (executionContext, clusterName, imageName, clientPort, peerPort, electionPort, nodeNames) => {
      implicit val exec: ExecutionContext = executionContext
      clusterCache.get.flatMap { clusters =>
        if (clusters.keys.filter(_.isInstanceOf[ZookeeperClusterInfo]).exists(_.name == clusterName))
          Future.failed(new IllegalArgumentException(s"zookeeper cluster:$clusterName exists!"))
        else
          nodeCollie
            .nodes(nodeNames)
            .map(_.map(node => node -> format(PREFIX_KEY, clusterName, serviceName)).toMap)
            .flatMap { nodes =>
              // add route in order to make zk node can connect to each other.
              val route: Map[String, String] = nodes.map {
                case (node, _) =>
                  node.name -> CommonUtils.address(node.name)
              }
              val zkServers: String = nodes.keys.map(_.name).mkString(" ")
              // ssh connection is slow so we submit request by multi-thread
              Future
                .sequence(nodes.zipWithIndex.map {
                  case ((node, containerName), index) =>
                    Future {
                      try {
                        dockerCache.exec(
                          node,
                          _.containerCreator()
                            .imageName(imageName)
                            .portMappings(Map(
                              clientPort -> clientPort,
                              peerPort -> peerPort,
                              electionPort -> electionPort
                            ))
                            // zookeeper doesn't have advertised hostname/port so we assign the "docker host" directly
                            .hostname(node.name)
                            .envs(Map(
                              ZookeeperCollie.ID_KEY -> index.toString,
                              ZookeeperCollie.CLIENT_PORT_KEY -> clientPort.toString,
                              ZookeeperCollie.PEER_PORT_KEY -> peerPort.toString,
                              ZookeeperCollie.ELECTION_PORT_KEY -> electionPort.toString,
                              ZookeeperCollie.SERVERS_KEY -> zkServers
                            ))
                            .name(containerName)
                            .route(route)
                            .execute()
                        )

                        Some(node.name)
                      } catch {
                        case e: Throwable =>
                          try dockerCache.exec(node, _.forceRemove(containerName))
                          catch {
                            case _: Throwable =>
                            // do nothing
                          }
                          LOG.error(s"failed to start $clusterName", e)
                          None
                      }
                    }
                })
                .map(_.flatten.toSeq)
                .map { successfulNodeNames =>
                  if (successfulNodeNames.isEmpty)
                    throw new IllegalArgumentException(s"failed to create $clusterName on $serviceName")
                  clusterCache.requestUpdate()
                  ZookeeperClusterInfo(
                    name = clusterName,
                    imageName = imageName,
                    clientPort = clientPort,
                    peerPort = peerPort,
                    electionPort = electionPort,
                    nodeNames = successfulNodeNames
                  )
                }
            }
      }
    }

  override def removeNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = Future.failed(
    new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

  override protected def doAddNodeContainer(
    previousCluster: ZookeeperClusterInfo,
    previousContainers: Seq[ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = Future.failed(
    new UnsupportedOperationException("zookeeper collie doesn't support to add node from a running cluster"))
}
