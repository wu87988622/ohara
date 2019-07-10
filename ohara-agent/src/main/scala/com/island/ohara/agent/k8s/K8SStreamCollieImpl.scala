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

package com.island.ohara.agent.k8s

import java.net.URI

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{ContainerCollie, NodeCollie, StreamCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.common.util.CommonUtils
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}

private class K8SStreamCollieImpl(nodeCollie: NodeCollie, k8sClient: K8SClient)
    extends K8SBasicCollieImpl[StreamClusterInfo, StreamCollie.ClusterCreator](nodeCollie, k8sClient)
    with StreamCollie {
  private[this] val log = Logger(classOf[K8SStreamCollieImpl])

  override def creator: StreamCollie.ClusterCreator =
    (clusterName,
     nodeNames,
     imageName,
     jarUrl,
     appId,
     brokerProps,
     fromTopics,
     toTopics,
     jmxPort,
     enableExactlyOnce,
     executionContext) => {
      implicit val exec: ExecutionContext = executionContext
      exist(clusterName).flatMap {
        if (_) Future.failed(new IllegalArgumentException(s"stream cluster:$clusterName exists!"))
        else
          nodeCollie
            .nodes(nodeNames)
            .map(_.map(node => node -> ContainerCollie.format(PREFIX_KEY, clusterName, serviceName)).toMap)
            .flatMap { nodes =>
              def urlToHost(url: String): String = new URI(url).getHost

              val route: Map[String, String] = nodes.keys.map { node =>
                node.name -> CommonUtils.address(node.name)
              }.toMap +
                // make sure the streamApp can connect to configurator
                (urlToHost(jarUrl).toLowerCase -> CommonUtils.address(urlToHost(jarUrl)))
              Future
                .sequence(nodes.map {
                  case (node, podName) =>
                    k8sClient
                      .containerCreator()
                      .imageName(imageName)
                      .nodeName(node.name)
                      .hostname(podName)
                      .name(podName)
                      .labelName(OHARA_LABEL)
                      .domainName(K8S_DOMAIN_NAME)
                      .portMappings(Map(
                        jmxPort -> jmxPort
                      ))
                      .routes(route)
                      .envs(
                        Map(
                          StreamCollie.JARURL_KEY -> jarUrl,
                          StreamCollie.APPID_KEY -> appId,
                          StreamCollie.SERVERS_KEY -> brokerProps,
                          StreamCollie.FROM_TOPIC_KEY -> fromTopics.mkString(","),
                          StreamCollie.TO_TOPIC_KEY -> toTopics.mkString(","),
                          StreamCollie.JMX_PORT_KEY -> jmxPort.toString,
                          StreamCollie.EXACTLY_ONCE -> enableExactlyOnce.toString
                        )
                      )
                      .args(StreamCollie.formatJMXProperties(node.name, jmxPort) :+ StreamCollie.MAIN_ENTRY)
                      .threadPool(executionContext)
                      .create()
                      .recover {
                        case e: Throwable =>
                          log.error(s"failed to start $clusterName", e)
                          None
                      }
                })
                .map(_.flatten.toSeq.map(_.nodeName))
                .map { successfulNodeNames =>
                  if (successfulNodeNames.isEmpty) {
                    throw new IllegalArgumentException(s"failed to create $clusterName")
                  }
                  StreamClusterInfo(
                    name = clusterName,
                    imageName = imageName,
                    nodeNames = successfulNodeNames.toSet,
                    deadNodes = Set.empty,
                    jmxPort = jmxPort,
                    // creating cluster success could be applied containers are "running"
                    state = Some(ContainerState.RUNNING.name)
                  )
                }
            }
      }
    }

  override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[StreamClusterInfo] =
    toStreamCluster(clusterName, containers)

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
