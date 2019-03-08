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

import java.net.URL

import com.island.ohara.agent.{BrokerCollie, NoSuchClusterException, NodeCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.common.util.CommonUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

private class WorkerCollieImpl(nodeCollie: NodeCollie,
                               dockerCache: DockerClientCache,
                               clusterCache: Cache[Map[ClusterInfo, Seq[ContainerInfo]]])
    extends BasicCollieImpl[WorkerClusterInfo, WorkerCollie.ClusterCreator](nodeCollie, dockerCache, clusterCache)
    with WorkerCollie {

  /**
    * This is a complicated process. We must address following issues.
    * 1) check the existence of cluster
    * 2) check the existence of nodes
    * 3) Each worker container has got to export exporter port and client port
    * 4) Each worker container should assign "docker host name/port" to advertised name/port
    * 5) add broker routes to all worker containers (worker needs to connect to broker cluster)
    * 6) Add worker routes to all worker containers
    * 7) update existed containers (if we are adding new node into a running cluster)
    * @return description of worker cluster
    */
  override def creator(): WorkerCollie.ClusterCreator = (clusterName,
                                                         imageName,
                                                         brokerClusterName,
                                                         clientPort,
                                                         groupId,
                                                         offsetTopicName,
                                                         offsetTopicReplications,
                                                         offsetTopicPartitions,
                                                         statusTopicName,
                                                         statusTopicReplications,
                                                         statusTopicPartitions,
                                                         configTopicName,
                                                         configTopicReplications,
                                                         jarUrls,
                                                         nodeNames) =>
    clusterCache.get().flatMap { clusters =>
      clusters
        .filter(_._1.isInstanceOf[WorkerClusterInfo])
        .map {
          case (cluster, containers) => cluster.asInstanceOf[WorkerClusterInfo] -> containers
        }
        .find(_._1.name == clusterName)
        .map(_._2)
        .map(containers =>
          nodeCollie
            .nodes(containers.map(_.nodeName))
            .map(_.map(node => node -> containers.find(_.nodeName == node.name).get).toMap))
        .getOrElse(Future.successful(Map.empty))
        .flatMap(existNodes =>
          nodeCollie.nodes(nodeNames).map(_.map(node => node -> format(clusterName)).toMap).map((existNodes, _)))
        .map {
          case (existNodes, newNodes) =>
            existNodes.keys.foreach(node =>
              if (newNodes.keys.exists(_.name == node.name))
                throw new IllegalArgumentException(s"${node.name} has run the worker service for $clusterName"))
            clusters
              .filter(_._1.isInstanceOf[BrokerClusterInfo])
              .find(_._1.name == brokerClusterName)
              .map(_._2)
              .map((existNodes, newNodes, _))
              .getOrElse(throw new NoSuchClusterException(s"broker cluster:$brokerClusterName doesn't exist"))
        }
        .flatMap {
          case (existNodes, newNodes, brokerContainers) =>
            if (brokerContainers.isEmpty)
              throw new IllegalArgumentException(s"broker cluster:$brokerClusterName doesn't exist")
            val brokers = brokerContainers
              .map(c => s"${c.nodeName}:${c.environments(BrokerCollie.CLIENT_PORT_KEY).toInt}")
              .mkString(",")

            val existRoute: Map[String, String] = existNodes.map {
              case (node, container) => container.hostname -> CommonUtil.address(node.name)
            }
            // add route in order to make broker node can connect to each other (and broker node).
            val route: Map[String, String] = newNodes.map {
              case (node, _) =>
                node.name -> CommonUtil.address(node.name)
            } ++ brokerContainers
              .map(brokerContainer => brokerContainer.nodeName -> CommonUtil.address(brokerContainer.nodeName))
              .toMap

            // update the route since we are adding new node to a running worker cluster
            // we don't need to update startup broker list (WorkerCollie.BROKERS_KEY) since kafka do the update for us.
            existNodes.foreach {
              case (node, container) => updateRoute(node, container.name, route)
            }

            // ssh connection is slow so we submit request by multi-thread
            Future
              .sequence(newNodes.map {
                case (node, containerName) =>
                  Future {
                    try {
                      dockerCache.exec(
                        node,
                        _.containerCreator()
                          .imageName(imageName)
                          // In --network=host mode, we don't need to export port for containers.
//                          .portMappings(Map(clientPort -> clientPort))
                          .hostname(containerName)
                          .envs(Map(
                            WorkerCollie.CLIENT_PORT_KEY -> clientPort.toString,
                            WorkerCollie.BROKERS_KEY -> brokers,
                            WorkerCollie.GROUP_ID_KEY -> groupId,
                            WorkerCollie.OFFSET_TOPIC_KEY -> offsetTopicName,
                            WorkerCollie.OFFSET_TOPIC_PARTITIONS_KEY -> offsetTopicPartitions.toString,
                            WorkerCollie.OFFSET_TOPIC_REPLICATIONS_KEY -> offsetTopicReplications.toString,
                            WorkerCollie.CONFIG_TOPIC_KEY -> configTopicName,
                            WorkerCollie.CONFIG_TOPIC_REPLICATIONS_KEY -> configTopicReplications.toString,
                            WorkerCollie.STATUS_TOPIC_KEY -> statusTopicName,
                            WorkerCollie.STATUS_TOPIC_PARTITIONS_KEY -> statusTopicPartitions.toString,
                            WorkerCollie.STATUS_TOPIC_REPLICATIONS_KEY -> statusTopicReplications.toString,
                            WorkerCollie.ADVERTISED_HOSTNAME_KEY -> node.name,
                            WorkerCollie.ADVERTISED_CLIENT_PORT_KEY -> clientPort.toString,
                            WorkerCollie.PLUGINS_KEY -> jarUrls.mkString(","),
                            BROKER_CLUSTER_NAME -> brokerClusterName
                          ))
                          .name(containerName)
                          .route(route ++ existRoute)
                          // we use --network=host for worker cluster since the connectors run on worker cluster may need to
                          // access external system to request data. In ssh mode, dns service "may" be not deployed.
                          // In order to simplify their effort, we directly mount host's route on the container.
                          // This is not a normal case I'd say. However, we always meet special case which must be addressed
                          // by this "special" solution...
                          .networkDriver(NETWORK_DRIVER)
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
                        LOG.error(s"failed to start $imageName", e)
                        None
                    }
                  }
              })
              .map(_.flatten.toSeq)
              .map { successfulNodeNames =>
                if (successfulNodeNames.isEmpty)
                  throw new IllegalArgumentException(s"failed to create $clusterName on $serviceName")
                val nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
                WorkerClusterInfo(
                  name = clusterName,
                  imageName = imageName,
                  brokerClusterName = brokerClusterName,
                  clientPort = clientPort,
                  groupId = groupId,
                  offsetTopicName = offsetTopicName,
                  offsetTopicPartitions = offsetTopicPartitions,
                  offsetTopicReplications = offsetTopicReplications,
                  configTopicName = configTopicName,
                  configTopicPartitions = 1,
                  configTopicReplications = configTopicReplications,
                  statusTopicName = statusTopicName,
                  statusTopicPartitions = statusTopicPartitions,
                  statusTopicReplications = statusTopicReplications,
                  jarNames = jarUrls.map(_.getFile),
                  sources = Seq.empty,
                  sinks = Seq.empty,
                  nodeNames = nodeNames
                )
              }
        }
  }

  override protected def doAddNode(previousCluster: WorkerClusterInfo,
                                   previousContainers: Seq[ContainerInfo],
                                   newNodeName: String): Future[WorkerClusterInfo] = {
    creator()
      .clusterName(previousCluster.name)
      .brokerClusterName(previousCluster.brokerClusterName)
      .clientPort(previousCluster.clientPort)
      .groupId(previousCluster.groupId)
      .offsetTopicName(previousCluster.offsetTopicName)
      .statusTopicName(previousCluster.statusTopicName)
      .configTopicName(previousCluster.configTopicName)
      .imageName(previousCluster.imageName)
      .jarUrls(
        previousContainers.head
          .environments(WorkerCollie.PLUGINS_KEY)
          .split(",")
          .filter(_.nonEmpty)
          .map(s => new URL(s)))
      .nodeName(newNodeName)
      .create()
  }
}
