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
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

abstract class ContainerCollie[T <: ClusterInfo: ClassTag, Creator <: ClusterCreator[T]](nodeCollie: NodeCollie)
    extends Collie[T, Creator] {

  protected def doRemoveNode(previousCluster: T, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean]

  override final def removeNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[Boolean] = clusters.flatMap(
    _.find(_._1.name == clusterName)
      .filter(_._1.nodeNames.contains(nodeName))
      .filter(_._2.exists(_.nodeName == nodeName))
      .map {
        case (cluster, runningContainers) =>
          runningContainers.size match {
            case 1 =>
              Future.failed(new IllegalArgumentException(
                s"$clusterName is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead"))
            case _ =>
              doRemoveNode(
                cluster,
                runningContainers
                  .find(_.nodeName == nodeName)
                  .getOrElse(throw new IllegalArgumentException(
                    s"This should not be happen!!! $nodeName doesn't exist on cluster:$clusterName"))
              )
          }
      }
      .getOrElse(Future.successful(false)))

  protected def doAddNode(previousCluster: T, previousContainers: Seq[ContainerInfo], newNodeName: String)(
    implicit executionContext: ExecutionContext): Future[T]

  override final def addNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[T] = {
    nodeCollie
      .node(nodeName) // make sure there is a exist node.
      .flatMap(_ => cluster(clusterName))
      .flatMap {
        case (cluster, containers) =>
          if (clusterName.isEmpty || nodeName.isEmpty)
            Future.failed(new IllegalArgumentException("cluster and node name can't empty"))
          else if (CommonUtils.hasUpperCase(nodeName))
            Future.failed(new IllegalArgumentException("Your node name can't uppercase"))
          else if (cluster.nodeNames.contains(nodeName))
            // the new node is running so we don't need to do anything for this method
            Future.successful(cluster)
          else doAddNode(cluster, containers, nodeName)
      }
  }

  protected def serviceName: String =
    if (classTag[T].runtimeClass.isAssignableFrom(classOf[ZookeeperClusterInfo])) ContainerCollie.ZK_SERVICE_NAME
    else if (classTag[T].runtimeClass.isAssignableFrom(classOf[BrokerClusterInfo])) ContainerCollie.BK_SERVICE_NAME
    else if (classTag[T].runtimeClass.isAssignableFrom(classOf[WorkerClusterInfo])) ContainerCollie.WK_SERVICE_NAME
    else if (classTag[T].runtimeClass.isAssignableFrom(classOf[StreamClusterInfo])) ContainerCollie.STREAM_SERVICE_NAME
    else throw new IllegalArgumentException(s"Who are you, ${classTag[T].runtimeClass} ???")

  override final def forceRemove(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusterWithAllContainers.flatMap(
      _.find(_._1.name == clusterName)
        .map {
          case (cluster, containerInfos) => doForceRemove(cluster, containerInfos)
        }
        .getOrElse(Future.successful(false)))

  override final def remove(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    clusterWithAllContainers.flatMap(
      _.find(_._1.name == clusterName)
        .map {
          case (cluster, containerInfos) => doRemove(cluster, containerInfos)
        }
        .getOrElse(Future.successful(false)))

  protected def doRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean]

  // default implementation of "force" remove is "gracefully" remove
  protected def doForceRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    doRemove(clusterInfo, containerInfos)

}

object ContainerCollie {

  val ZK_SERVICE_NAME: String = "zk"
  val BK_SERVICE_NAME: String = "bk"
  val WK_SERVICE_NAME: String = "wk"

  /**
    * container name is controlled by streamRoute, the service name here use five words was ok.
    */
  val STREAM_SERVICE_NAME: String = "stream"

  /**
    * used to distinguish the cluster name and service name
    */
  val DIVIDER: String = "-"
  val UNKNOWN: String = "unknown"

  private[agent] val LENGTH_OF_CONTAINER_NAME_ID: Int = 7

  /**
    * generate unique name for the container.
    * It can be used in setting container's hostname and name
    * @param clusterName cluster name
    * @return a formatted string. form: ${clusterName}-${service}-${index}
    */
  def format(prefixKey: String, clusterName: String, serviceName: String): String =
    Seq(
      prefixKey,
      clusterName,
      serviceName,
      CommonUtils.randomString(LENGTH_OF_CONTAINER_NAME_ID)
    ).mkString(ContainerCollie.DIVIDER)

  protected[agent] def preSettingEnvironment(
    existNodes: Map[Node, ContainerInfo],
    newNodes: Map[Node, String],
    zkContainers: Seq[ContainerInfo],
    resolveHostName: String => String,
    hookUpdate: (Node, ContainerInfo, Map[String, String]) => Unit): Map[String, String] = {
    val existRoute: Map[String, String] = existNodes.map {
      case (node, container) => container.nodeName -> resolveHostName(node.name)
    }
    // add route in order to make broker node can connect to each other (and zk node).
    val route: Map[String, String] = newNodes.map {
      case (node, _) =>
        node.name -> resolveHostName(node.name)
    } ++ zkContainers.map(zkContainer => zkContainer.nodeName -> resolveHostName(zkContainer.nodeName)).toMap

    // update the route since we are adding new node to a running broker cluster
    // we don't need to update startup broker list since kafka do the update for us.
    existNodes.foreach {
      case (node, container) => hookUpdate(node, container, route)
    }
    existRoute ++ route
  }

}
