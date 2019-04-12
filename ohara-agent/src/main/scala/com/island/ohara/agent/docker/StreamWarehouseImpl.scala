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

package com.island.ohara.agent.docker

import com.island.ohara.agent.ssh.{Cache, DockerClientCache}
import com.island.ohara.agent.wharf.StreamWarehouse
import com.island.ohara.agent.{NoSuchClusterException, NodeCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.{ClusterInfo, StreamApi}
import com.island.ohara.common.util.CommonUtils
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

private class StreamWarehouseImpl(nodeCollie: NodeCollie,
                                  dockerCache: DockerClientCache,
                                  clusterCache: Cache[Map[ClusterInfo, Seq[ContainerInfo]]])
    extends StreamWarehouse {
  private[this] val log = Logger(classOf[StreamWarehouse])

  override def creator(): StreamWarehouse.StreamCreator = {
    (clusterName, nodeNames, imageName, jarUrl, instance, appId, brokerProps, fromTopics, toTopics, executionContext) =>
      {
        implicit val exec: ExecutionContext = executionContext
        clusterCache.get.flatMap { clusters =>
          if (clusters.keys.filter(_.isInstanceOf[StreamClusterInfo]).exists(_.name == clusterName))
            Future.failed(
              new IllegalArgumentException(
                s"container cluster:$clusterName exists!"
              )
            )
          else
            nodeCollie
              .nodes()
              .map { all =>
                if (CommonUtils.isEmpty(nodeNames.asJava)) {
                  // Check instance first
                  // if no enough node exists, throw exception
                  CommonUtils
                    .requireNonEmpty(Random.shuffle(all).take(CommonUtils.requirePositiveInt(instance)).asJava)
                    .asScala
                } else
                  // if require node name is not in nodeCollie, do not take that node
                  CommonUtils.requireNonEmpty(all.filter(n => nodeNames.contains(n.name)).asJava).asScala
              }
              .map(_.map(node => node -> format(StreamWarehouse.STREAM_SERVICE_NAME, clusterName)).toMap)
              .flatMap { nodes =>
                // add route in order to make each node can connect to others.
//                val route: Map[String, String] = nodes.map {
//                  case (node, _) =>
//                    node.name -> CommonUtils.address(node.name)
//                }
                // ssh connection is slow so we submit request by multi-thread
                Future
                  .sequence(nodes.map {
                    case (node, name) =>
                      Future {
                        try {
                          dockerCache.exec(
                            node,
                            _.containerCreator()
                              .imageName(imageName)
                              // we should set the hostname to identify container location
                              .hostname(node.name)
                              .envs(
                                Map(
                                  StreamApi.JARURL_KEY -> jarUrl,
                                  StreamApi.APPID_KEY -> appId,
                                  StreamApi.SERVERS_KEY -> brokerProps,
                                  StreamApi.FROM_TOPIC_KEY -> fromTopics.mkString(","),
                                  StreamApi.TO_TOPIC_KEY -> toTopics.mkString(",")
                                )
                              )
                              .name(name)
//                              .route(route)
                              .execute()
                          )
                          Some(node.name)
                        } catch {
                          case e: Throwable =>
                            try dockerCache.exec(node, _.forceRemove(name))
                            catch {
                              case _: Throwable =>
                              // do nothing
                            }
                            log.error(s"failed to start $clusterName", e)
                            None
                        }
                      }
                  })
                  .map(_.flatten.toSeq)
                  .map { successfulNodeNames =>
                    if (successfulNodeNames.isEmpty)
                      throw new IllegalArgumentException(
                        s"failed to create $clusterName"
                      )
                    clusterCache.requestUpdate()
                    StreamClusterInfo(
                      name = clusterName,
                      imageName = imageName,
                      jarUrl = jarUrl,
                      brokerProps = brokerProps,
                      fromTopics = fromTopics,
                      toTopics = toTopics,
                      nodeNames = successfulNodeNames
                    )
                  }
              }
        }
      }
  }

  override def containers(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = {
    clusterCache.get.map {
      _.filter(entry => entry._1.isInstanceOf[StreamClusterInfo])
        .find(_._1.name == clusterName)
        .getOrElse(throw new NoSuchClusterException(s"$clusterName doesn't exist"))
        ._2
    }
  }
}
