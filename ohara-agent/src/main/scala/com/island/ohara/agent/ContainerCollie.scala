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

package com.island.ohara.agent

import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

abstract class ContainerCollie[T <: ClusterInfo: ClassTag, Creator <: ClusterCreator[T]](nodeCollie: NodeCollie)
    extends Collie[T, Creator] {

  private[agent] val LENGTH_OF_CONTAINER_NAME_ID: Int = 7

  /**
    * generate unique name for the container.
    * It can be used in setting container's hostname and name
    * @param clusterName cluster name
    * @return a formatted string. form: ${clusterName}-${service}-${index}
    */
  protected def format(prefixKey: String, clusterName: String, serviceName: String): String =
    Seq(
      prefixKey,
      clusterName,
      serviceName,
      CommonUtils.randomString(LENGTH_OF_CONTAINER_NAME_ID)
    ).mkString(ContainerCollie.DIVIDER)

  protected def doAddNodeContainer(previousCluster: T, previousContainers: Seq[ContainerInfo], newNodeName: String)(
    implicit executionContext: ExecutionContext): Future[T]

  override def addNode(clusterName: String, nodeName: String)(implicit executionContext: ExecutionContext): Future[T] =
    nodeCollie
      .node(nodeName) // make sure there is a exist node.
      .flatMap(_ => cluster(clusterName))
      .flatMap {
        case (c, cs) => doAddNodeContainer(c, cs, nodeName)
      }

  protected def doRemoveNode(previousCluster: T, previousContainer: ContainerInfo, removedNodeName: String)(
    implicit executionContext: ExecutionContext): Future[T]

  override def removeNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[T] =
    checkRemoveNode(clusterName, nodeName).flatMap(cluster => doRemoveNode(cluster._1, cluster._2, nodeName))

  protected val serviceName: String =
    if (classTag[T].runtimeClass.isAssignableFrom(classOf[ZookeeperClusterInfo])) ContainerCollie.ZK_SERVICE_NAME
    else if (classTag[T].runtimeClass.isAssignableFrom(classOf[BrokerClusterInfo])) ContainerCollie.BK_SERVICE_NAME
    else if (classTag[T].runtimeClass.isAssignableFrom(classOf[WorkerClusterInfo])) ContainerCollie.WK_SERVICE_NAME
    else throw new IllegalArgumentException(s"Who are you, ${classTag[T].runtimeClass} ???")

  private def checkRemoveNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[(T, ContainerInfo)] =
    cluster(clusterName).map {
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
}

object ContainerCollie {
  val ZK_SERVICE_NAME: String = "zk"
  val BK_SERVICE_NAME: String = "bk"
  val WK_SERVICE_NAME: String = "wk"

  /**
    * used to distinguish the cluster name and service name
    */
  val DIVIDER: String = "-"
}
