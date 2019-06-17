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

import java.net.URI

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{ClusterCache, ContainerCollie, NodeCollie, StreamCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.common.util.CommonUtils

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
private class StreamCollieImpl(nodeCollie: NodeCollie, dockerCache: DockerClientCache, clusterCache: ClusterCache)
    extends BasicCollieImpl[StreamClusterInfo, StreamCollie.ClusterCreator](nodeCollie, dockerCache, clusterCache)
    with StreamCollie {

  override def creator(): StreamCollie.ClusterCreator =
    (clusterName,
     nodeNames,
     imageName,
     jarUrl,
     instance,
     appId,
     brokerProps,
     fromTopics,
     toTopics,
     jmxPort,
     executionContext) => {
      implicit val exec: ExecutionContext = executionContext
      val clusters = clusterCache.snapshot
      if (clusters.keys.filter(_.isInstanceOf[StreamClusterInfo]).exists(_.name == clusterName))
        Future.failed(new IllegalArgumentException(s"stream cluster:$clusterName exists!"))
      else
        nodeCollie
          .nodes()
          .map { all =>
            if (CommonUtils.isEmpty(nodeNames.asJava)) {
              // Check instance first
              // Here we will check the following conditions:
              // 1. instance should be positive
              // 2. available nodes should be bigger than instance (one node runs one instance)
              if (all.size < instance)
                throw new IllegalArgumentException(s"cannot run streamApp. expect: $instance, actual: ${all.size}")
              Random.shuffle(all).take(CommonUtils.requirePositiveInt(instance))
            } else
              // if require node name is not in nodeCollie, do not take that node
              CommonUtils.requireNonEmpty(all.filter(n => nodeNames.contains(n.name)).asJava).asScala
          }
          .map(_.map(node => node -> ContainerCollie.format(PREFIX_KEY, clusterName, serviceName)).toMap)
          .flatMap { nodes =>
            def urlToHost(url: String): String = new URI(url).getHost

            val route: Map[String, String] = nodes.keys.map { node =>
              node.name -> CommonUtils.address(node.name)
            }.toMap +
              // make sure the streamApp can connect to configurator
              (urlToHost(jarUrl) -> CommonUtils.address(urlToHost(jarUrl)))
            // ssh connection is slow so we submit request by multi-thread
            Future
              .sequence(nodes.map {
                case (node, containerName) =>
                  Future {
                    try {
                      val containerInfo = ContainerInfo(
                        nodeName = node.name,
                        id = "unknown",
                        imageName = imageName,
                        created = "unknown",
                        state = "unknown",
                        kind = "unknown",
                        name = containerName,
                        size = "unknown",
                        portMappings = Seq(
                          PortMapping(
                            hostIp = "unknown",
                            portPairs = Seq(
                              PortPair(
                                hostPort = jmxPort,
                                containerPort = jmxPort
                              )
                            )
                          )),
                        environments = Map(
                          StreamCollie.JARURL_KEY -> jarUrl,
                          StreamCollie.APPID_KEY -> appId,
                          StreamCollie.SERVERS_KEY -> brokerProps,
                          StreamCollie.FROM_TOPIC_KEY -> fromTopics.mkString(","),
                          StreamCollie.TO_TOPIC_KEY -> toTopics.mkString(","),
                          StreamCollie.JMX_PORT_KEY -> jmxPort.toString
                        ),
                        // we should set the hostname to container name in order to avoid duplicate name with other containers
                        hostname = containerName
                      )
                      dockerCache.exec(
                        node,
                        _.containerCreator()
                          .imageName(containerInfo.imageName)
                          .hostname(containerInfo.name)
                          .envs(containerInfo.environments)
                          .name(containerInfo.name)
                          .portMappings(containerInfo.portMappings
                            .flatMap(_.portPairs)
                            .map(pair => pair.hostPort -> pair.containerPort)
                            .toMap)
                          .route(route)
                          .command(String.join(" ",
                                               StreamCollie.formatJMXProperties(node.name, jmxPort).mkString(" "),
                                               StreamCollie.MAIN_ENTRY))
                          .execute()
                      )
                      Some(containerInfo)
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
              .map { successfulContainers =>
                if (successfulContainers.isEmpty)
                  throw new IllegalArgumentException(s"failed to create $clusterName on $serviceName")
                val clusterInfo = StreamClusterInfo(
                  name = clusterName,
                  imageName = imageName,
                  nodeNames = successfulContainers.map(_.nodeName).toSet,
                  jmxPort = jmxPort,
                  // creating cluster success could be applied containers are "running"
                  state = Some(ContainerState.RUNNING.name)
                )
                clusterCache.put(clusterInfo, clusterCache.get(clusterInfo) ++ successfulContainers)
                clusterInfo
              }
          }
    }

  override protected def doRemoveNode(previousCluster: StreamClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("stream collie doesn't support to remove node from a running cluster"))

  override protected def doAddNode(
    previousCluster: StreamClusterInfo,
    previousContainers: Seq[ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] =
    Future.failed(new UnsupportedOperationException("stream collie doesn't support to add node from a running cluster"))
}
