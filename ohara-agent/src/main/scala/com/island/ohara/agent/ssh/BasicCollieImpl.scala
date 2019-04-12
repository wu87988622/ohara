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

import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.agent.{Collie, NoSuchClusterException, NodeCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}
private abstract class BasicCollieImpl[T <: ClusterInfo: ClassTag, Creator <: ClusterCreator[T]](
  nodeCollie: NodeCollie,
  dockerCache: DockerClientCache,
  clusterCache: Cache[Map[ClusterInfo, Seq[ContainerInfo]]])
    extends Collie[T, Creator] {

  final override def clusters(implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] =
    clusterCache.get.map {
      _.filter(entry => classTag[T].runtimeClass.isInstance(entry._1)).map {
        case (cluster, containers) => cluster.asInstanceOf[T] -> containers
      }
    }

  final override def cluster(name: String)(
    implicit executionContext: ExecutionContext): Future[(T, Seq[ContainerInfo])] =
    clusterCache.get.map {
      _.filter(entry => classTag[T].runtimeClass.isInstance(entry._1))
        .map {
          case (cluster, containers) => cluster.asInstanceOf[T] -> containers
        }
        .find(_._1.name == name)
        .getOrElse(throw new NoSuchClusterException(s"$name doesn't exist"))
    }

  val serviceName: String =
    if (classTag[T].runtimeClass.isAssignableFrom(classOf[ZookeeperClusterInfo])) ZK_SERVICE_NAME
    else if (classTag[T].runtimeClass.isAssignableFrom(classOf[BrokerClusterInfo])) BK_SERVICE_NAME
    else if (classTag[T].runtimeClass.isAssignableFrom(classOf[WorkerClusterInfo])) WK_SERVICE_NAME
    else throw new IllegalArgumentException(s"Who are you, ${classTag[T].runtimeClass} ???")

  def updateRoute(node: Node, containerName: String, route: Map[String, String]): Unit =
    dockerCache.exec(node,
                     _.containerInspector(containerName)
                       .asRoot()
                       .append("/etc/hosts", route.map {
                         case (hostname, ip) => s"$ip $hostname"
                       }.toSeq))

  /**
    * generate unique name for the container.
    * It can be used in setting container's hostname and name
    * @param clusterName cluster name
    * @return a formatted string. form: ${clusterName}-${service}-${index}
    */
  def format(clusterName: String): String =
    Seq(
      PREFIX_KEY,
      clusterName,
      serviceName,
      CommonUtils.randomString(LENGTH_OF_CONTAINER_NAME_ID)
    ).mkString(DIVIDER)

  override def forceRemove(clusterName: String)(implicit executionContext: ExecutionContext): Future[T] =
    remove(clusterName, true)

  override def remove(clusterName: String)(implicit executionContext: ExecutionContext): Future[T] =
    remove(clusterName, false)

  private[this] def remove(clusterName: String, force: Boolean)(
    implicit executionContext: ExecutionContext): Future[T] =
    cluster(clusterName).flatMap {
      case (cluster, containerInfos) =>
        Future
          .traverse(containerInfos) { containerInfo =>
            nodeCollie
              .node(containerInfo.nodeName)
              .map(
                node =>
                  dockerCache.exec(node,
                                   client =>
                                     if (force) client.forceRemove(containerInfo.name)
                                     else {
                                       client.stop(containerInfo.name)
                                       client.remove(containerInfo.name)
                                   }))
          }
          .map(_ => cluster)
    }

  override def logs(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] = nodeCollie
    .nodes()
    .flatMap(Future.traverse(_)(
      dockerCache.exec(_, _.containers(_.startsWith(s"$PREFIX_KEY$DIVIDER$clusterName$DIVIDER$serviceName")))))
    .map(_.flatten)
    .flatMap { containers =>
      Future
        .sequence(containers.map { container =>
          nodeCollie.node(container.nodeName).map { node =>
            container -> dockerCache.exec(node, _.log(container.name))
          }
        })
        .map(_.toMap)
    }

  override def removeNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[T] = cluster(clusterName)
    .map {
      case (cluster, runningContainers) =>
        runningContainers.size match {
          case 0 => throw new IllegalArgumentException(s"$clusterName doesn't exist")
          case 1 if runningContainers.map(_.nodeName).contains(nodeName) =>
            throw new IllegalArgumentException(
              s"$clusterName is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead")
          case _ =>
            cluster -> runningContainers
              .find(_.nodeName == nodeName)
              .getOrElse(throw new IllegalArgumentException(s"$nodeName doesn't exist on cluster:$clusterName"))
        }
    }
    .flatMap {
      case (cluster, container) =>
        nodeCollie.node(container.nodeName).map { node =>
          dockerCache.exec(node, _.stop(container.name))
          clusterCache.requestUpdate()
          // TODO: why we need to use match pattern? please refactor it...by chia
          (cluster match {
            case c: ZookeeperClusterInfo =>
              ZookeeperClusterInfo(
                name = c.name,
                imageName = c.imageName,
                clientPort = c.clientPort,
                peerPort = c.peerPort,
                electionPort = c.electionPort,
                nodeNames = c.nodeNames.filter(_ != nodeName)
              )
            case c: BrokerClusterInfo =>
              BrokerClusterInfo(
                name = c.name,
                imageName = c.imageName,
                clientPort = c.clientPort,
                exporterPort = c.exporterPort,
                zookeeperClusterName = c.zookeeperClusterName,
                nodeNames = c.nodeNames.filter(_ != nodeName)
              )
            case c: WorkerClusterInfo =>
              WorkerClusterInfo(
                name = c.name,
                imageName = c.imageName,
                brokerClusterName = c.brokerClusterName,
                clientPort = c.clientPort,
                jmxPort = c.jmxPort,
                groupId = c.groupId,
                statusTopicName = c.statusTopicName,
                statusTopicPartitions = c.statusTopicPartitions,
                statusTopicReplications = c.statusTopicReplications,
                configTopicName = c.configTopicName,
                configTopicPartitions = c.configTopicPartitions,
                configTopicReplications = c.configTopicReplications,
                offsetTopicName = c.offsetTopicName,
                offsetTopicPartitions = c.offsetTopicPartitions,
                offsetTopicReplications = c.offsetTopicReplications,
                connectors = c.connectors,
                jarNames = c.jarNames,
                nodeNames = c.nodeNames.filter(_ != nodeName)
              )
          }).asInstanceOf[T]
        }
    }
  protected def doAddNode(previousCluster: T, previousContainers: Seq[ContainerInfo], newNodeName: String)(
    implicit executionContext: ExecutionContext): Future[T]

  override def addNode(clusterName: String, nodeName: String)(implicit executionContext: ExecutionContext): Future[T] =
    nodeCollie
      .node(nodeName) // make sure there is a exist node.
      .flatMap(_ => cluster(clusterName))
      .flatMap {
        case (c, cs) => doAddNode(c, cs, nodeName)
      }
}
