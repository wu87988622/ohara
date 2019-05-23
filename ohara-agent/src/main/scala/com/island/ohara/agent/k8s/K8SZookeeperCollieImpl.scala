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

import com.island.ohara.agent.{NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

private class K8SZookeeperCollieImpl(val nodeCollie: NodeCollie, val k8sClient: K8SClient)
    extends K8SBasicCollieImpl[ZookeeperClusterInfo, ZookeeperCollie.ClusterCreator](nodeCollie, k8sClient)
    with ZookeeperCollie {

  private[this] val LOG = Logger(classOf[K8SZookeeperCollieImpl])
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  override def creator(): ZookeeperCollie.ClusterCreator =
    (executionContext, clusterName, imageName, clientPort, peerPort, electionPort, nodeNames) => {
      implicit val exec: ExecutionContext = executionContext
      exist(clusterName)
        .flatMap(if (_) Future.failed(new IllegalArgumentException(s"zookeeper cluster:$clusterName exists!"))
        else nodeCollie.nodes(nodeNames))
        .map(_.map(node => node -> s"${format(PREFIX_KEY, clusterName, serviceName)}$DIVIDER${node.name}").toMap)
        .map { nodes =>
          val zkServers: String = nodes.values.mkString(" ")
          val successfulNodeNames: Seq[String] = nodes.zipWithIndex
            .flatMap {
              case ((node, hostname), index) =>
                try {
                  val creatorContainerInfo: Future[Option[ContainerInfo]] = k8sClient
                    .containerCreator()
                    .imageName(imageName)
                    .portMappings(
                      Map(
                        clientPort -> clientPort,
                        peerPort -> peerPort,
                        electionPort -> electionPort
                      ))
                    .nodename(node.name)
                    .hostname(hostname)
                    .labelName(OHARA_LABEL)
                    .domainName(K8S_DOMAIN_NAME)
                    .envs(Map(
                      ZookeeperCollie.ID_KEY -> index.toString,
                      ZookeeperCollie.CLIENT_PORT_KEY -> clientPort.toString,
                      ZookeeperCollie.PEER_PORT_KEY -> peerPort.toString,
                      ZookeeperCollie.ELECTION_PORT_KEY -> electionPort.toString,
                      ZookeeperCollie.SERVERS_KEY -> s"$zkServers-${node.name}"
                    ))
                    .name(hostname)
                    .run()
                  Await.result(creatorContainerInfo, TIMEOUT)
                } catch {
                  case e: Throwable =>
                    LOG.error(s"failed to start $clusterName", e)
                    None
                }
            }
            .map(_.nodeName)
            .toSeq
          if (successfulNodeNames.isEmpty)
            throw new IllegalArgumentException(s"failed to create $clusterName on Zookeeper")
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

  override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
    toZookeeperCluster(clusterName, containers)

  override protected def doRemoveNode(previousCluster: ZookeeperClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

  override protected def doAddNode(
    previousCluster: ZookeeperClusterInfo,
    previousContainers: Seq[ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = Future.failed(
    new UnsupportedOperationException("zookeeper collie doesn't support to add node from a running cluster"))
}
