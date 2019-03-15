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

import com.island.ohara.agent.{BrokerCollie, NoSuchClusterException, NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}

private class BrokerCollieImpl(nodeCollie: NodeCollie,
                               dockerCache: DockerClientCache,
                               clusterCache: Cache[Map[ClusterInfo, Seq[ContainerInfo]]])
    extends BasicCollieImpl[BrokerClusterInfo, BrokerCollie.ClusterCreator](nodeCollie, dockerCache, clusterCache)
    with BrokerCollie {

  /**
    * This is a complicated process. We must address following issues.
    * 1) check the existence of cluster
    * 2) check the existence of nodes
    * 3) Each broker container has got to export exporter port and client port
    * 4) Each broker container should assign "docker host name/port" to advertised name/port
    * 5) add zookeeper routes to all broker containers (broker needs to connect to zookeeper cluster)
    * 6) Add broker routes to all broker containers
    * 7) update existed containers (if we are adding new node into a running cluster)
    * @return creator of broker cluster
    */
  override def creator(): BrokerCollie.ClusterCreator =
    (executionContext, clusterName, imageName, zookeeperClusterName, clientPort, exporterPort, nodeNames) => {
      implicit val exec: ExecutionContext = executionContext
      clusterCache.get.flatMap { clusters =>
        clusters
          .filter(_._1.isInstanceOf[BrokerClusterInfo])
          .map {
            case (cluster, containers) => cluster.asInstanceOf[BrokerClusterInfo] -> containers
          }
          .find(_._1.name == clusterName)
          .map(_._2)
          .map(containers =>
            nodeCollie
              .nodes(containers.map(_.nodeName))
              .map(_.map(node => node -> containers.find(_.nodeName == node.name).get).toMap))
          .getOrElse(Future.successful(Map.empty))
          .map { existNodes =>
            // if there is a running cluster already, we should check the consistency of configuration
            existNodes.values.foreach { container =>
              def checkValue(previous: String, newValue: String): Unit =
                if (previous != newValue) throw new IllegalArgumentException(s"previous:$previous new:$newValue")

              def check(key: String, newValue: String): Unit = {
                val previous = container.environments(key)
                if (previous != newValue) throw new IllegalArgumentException(s"previous:$previous new:$newValue")
              }

              checkValue(container.imageName, imageName)
              check(BrokerCollie.CLIENT_PORT_KEY, clientPort.toString)
              check(ZOOKEEPER_CLUSTER_NAME, zookeeperClusterName)
            }
            existNodes
          }
          .flatMap(existNodes =>
            nodeCollie.nodes(nodeNames).map(_.map(node => node -> format(clusterName)).toMap).map((existNodes, _)))
          .map {
            case (existNodes, newNodes) =>
              existNodes.keys.foreach(node =>
                if (newNodes.keys.exists(_.name == node.name))
                  throw new IllegalArgumentException(s"${node.name} has run the broker service for $clusterName"))
              clusters
                .filter(_._1.isInstanceOf[ZookeeperClusterInfo])
                .find(_._1.name == zookeeperClusterName)
                .map(_._2)
                .map((existNodes, newNodes, _))
                .getOrElse(throw new NoSuchClusterException(s"zookeeper cluster:$zookeeperClusterName doesn't exist"))
          }
          .flatMap {
            case (existNodes, newNodes, zkContainers) =>
              if (zkContainers.isEmpty) throw new IllegalArgumentException(s"$clusterName doesn't exist")
              val zookeepers = zkContainers
                .map(c => s"${c.nodeName}:${c.environments(ZookeeperCollie.CLIENT_PORT_KEY).toInt}")
                .mkString(",")

              val existRoute: Map[String, String] = existNodes.map {
                case (node, container) => container.nodeName -> CommonUtils.address(node.name)
              }
              // add route in order to make broker node can connect to each other (and zk node).
              val route: Map[String, String] = newNodes.map {
                case (node, _) =>
                  node.name -> CommonUtils.address(node.name)
              } ++ zkContainers
                .map(zkContainer => zkContainer.nodeName -> CommonUtils.address(zkContainer.nodeName))
                .toMap

              // update the route since we are adding new node to a running broker cluster
              // we don't need to update startup broker list since kafka do the update for us.
              existNodes.foreach {
                case (node, container) => updateRoute(node, container.name, route)
              }

              // the new broker node can't take used id so we find out the max id which is used by current cluster
              val maxId: Int =
                if (existNodes.isEmpty) 0
                else existNodes.values.map(_.environments(BrokerCollie.ID_KEY).toInt).toSet.max + 1

              // ssh connection is slow so we submit request by multi-thread
              Future
                .sequence(newNodes.zipWithIndex.map {
                  case ((node, containerName), index) =>
                    Future {
                      try {
                        dockerCache.exec(
                          node,
                          _.containerCreator()
                            .imageName(imageName)
                            .portMappings(Map(
                              clientPort -> clientPort,
                              exporterPort -> exporterPort
                            ))
                            .hostname(containerName)
                            .envs(Map(
                              BrokerCollie.ID_KEY -> (maxId + index).toString,
                              BrokerCollie.CLIENT_PORT_KEY -> clientPort.toString,
                              BrokerCollie.ZOOKEEPERS_KEY -> zookeepers,
                              BrokerCollie.ADVERTISED_HOSTNAME_KEY -> node.name,
                              BrokerCollie.EXPORTER_PORT_KEY -> exporterPort.toString,
                              BrokerCollie.ADVERTISED_CLIENT_PORT_KEY -> clientPort.toString,
                              ZOOKEEPER_CLUSTER_NAME -> zookeeperClusterName
                            ))
                            .name(containerName)
                            .route(route ++ existRoute)
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
                          LOG.error(s"failed to start $imageName on ${node.name}", e)
                          None
                      }
                    }
                })
                .map(_.flatten.toSeq)
                .map { successfulNodeNames =>
                  if (successfulNodeNames.isEmpty)
                    throw new IllegalArgumentException(s"failed to create $clusterName on $serviceName")
                  clusterCache.requestUpdate()
                  BrokerClusterInfo(
                    name = clusterName,
                    imageName = imageName,
                    zookeeperClusterName = zookeeperClusterName,
                    exporterPort = exporterPort,
                    clientPort = clientPort,
                    nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
                  )
                }
          }
      }
    }

  override protected def doAddNode(
    previousCluster: BrokerClusterInfo,
    previousContainers: Seq[ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = creator()
    .clusterName(previousCluster.name)
    .zookeeperClusterName(previousCluster.zookeeperClusterName)
    .exporterPort(previousCluster.exporterPort)
    .clientPort(previousCluster.clientPort)
    .imageName(previousCluster.imageName)
    .nodeName(newNodeName)
    .create
}
