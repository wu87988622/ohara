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
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.wharf.{StreamWarehouse, Warehouse}
import com.island.ohara.agent.{Crane, NoSuchClusterException, NodeCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.{ClusterInfo, ContainerApi}
import com.island.ohara.common.util.{Releasable, ReleaseOnce}

import scala.concurrent.{ExecutionContext, Future}

private[agent] class K8sCraneImpl(nodeCollie: NodeCollie, k8sClient: K8SClient) extends ReleaseOnce with Crane {

  //TODO: refactor after #885...by sam
  override def streamWarehouse(): StreamWarehouse = new K8sStreamWarehouseImpl(
    nodeCollie = nodeCollie,
    k8sClient = k8sClient,
    clusterMap = list(ExecutionContext.global)
  )

  override def remove(warehouseName: String)(implicit executionContext: ExecutionContext): Future[ClusterInfo] = {
    get(warehouseName).flatMap {
      case (cluster, containers) =>
        Future.traverse(containers)(container => k8sClient.remove(container.name)).map(_ => cluster)
    }
  }

  override def list(
    implicit executionContext: ExecutionContext): Future[Map[ClusterInfo, Seq[ContainerApi.ContainerInfo]]] = {
    nodeCollie
      .nodes()
      .flatMap {
        Future.traverse(_) { node =>
          k8sClient.containers.map { containers =>
            containers.filter(container =>
              container.nodeName.equals(node.name) &&
                container.name.startsWith(Warehouse.PREFIX_KEY))
          }
        }
      }
      .map(_.flatten)
      .flatMap { allContainers =>
        def parse(
          serviceName: String,
          f: (String, Seq[ContainerInfo]) => Future[ClusterInfo]
        ): Future[Map[ClusterInfo, Seq[ContainerInfo]]] =
          Future
            .sequence(
              allContainers
                .filter(
                  _.name.contains(
                    s"${Warehouse.DIVIDER}$serviceName${Warehouse.DIVIDER}"
                  )
                )
                // form: PREFIX_KEY-SERVICE-CLUSTER_NAME-HASH
                .map(
                  container => container.name.split(Warehouse.DIVIDER)(2) -> container
                )
                .groupBy(_._1)
                .map {
                  case (clusterName, value) => clusterName -> value.map(_._2)
                }
                .map {
                  case (clusterName, containers) =>
                    f(clusterName, containers).map(_ -> containers)
                }
            )
            .map(_.toMap)

        parse(StreamWarehouse.STREAM_SERVICE_NAME, toStreamCluster)
      }
  }

  override def get(warehouseName: String)(
    implicit executionContext: ExecutionContext): Future[(ClusterInfo, Seq[ContainerApi.ContainerInfo])] = {
    list.map {
      _.find(_._1.name == warehouseName).getOrElse(throw new NoSuchClusterException(s"$warehouseName doesn't exist"))
    }
  }

  private[this] def toStreamCluster(clusterName: String, containers: Seq[ContainerInfo]): Future[ClusterInfo] = {
    val first = containers.head
    Future.successful(
      StreamClusterInfo(
        name = clusterName,
        imageName = first.imageName,
        nodeNames = containers.map(_.nodeName),
        state = {
          // we only have two possible results here:
          // 1. only assume cluster is "running" if all of containers are running
          // 2. the cluster state is always "dead" if one of container state was not running
          Some(containers.map(_.state).reduce[String] {
            case (c1, c2) =>
              if (c1 == ContainerState.RUNNING.name) c2
              else ContainerState.DEAD.name
          })
        }
      )
    )
  }

  override protected def doClose(): Unit = Releasable.close(k8sClient)
}
