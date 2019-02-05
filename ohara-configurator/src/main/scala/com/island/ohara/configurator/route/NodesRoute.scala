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

package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.agent.ClusterCollie
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi._
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil.{Id, TargetCluster}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
object NodesRoute {
  private[this] lazy val LOG = Logger(NodesRoute.getClass)

  private[this] def update(node: Node)(implicit clusterCollie: ClusterCollie): Future[Node] =
    update(Seq(node)).map(_.head)

  private[this] def update(nodes: Seq[Node])(implicit clusterCollie: ClusterCollie): Future[Seq[Node]] = clusterCollie
    .clusters()
    .map(_.keys.toSeq)
    .map { clusters =>
      nodes.map { node =>
        node.copy(
          services = Seq(
            NodeService(
              name = "zookeeper",
              clusterNames = clusters
                .filter(_.isInstanceOf[ZookeeperClusterInfo])
                .map(_.asInstanceOf[ZookeeperClusterInfo])
                .filter(_.nodeNames.contains(node.name))
                .map(_.name)
            ),
            NodeService(
              name = "broker",
              clusterNames = clusters
                .filter(_.isInstanceOf[BrokerClusterInfo])
                .map(_.asInstanceOf[BrokerClusterInfo])
                .filter(_.nodeNames.contains(node.name))
                .map(_.name)
            ),
            NodeService(
              name = "connect-worker",
              clusterNames = clusters
                .filter(_.isInstanceOf[WorkerClusterInfo])
                .map(_.asInstanceOf[WorkerClusterInfo])
                .filter(_.nodeNames.contains(node.name))
                .map(_.name)
            )
          )
        )
      }
    }
    .recover {
      case e: Throwable =>
        LOG.error("failed to seek cluster information", e)
        nodes
    }

  def apply(implicit store: Store, clusterCollie: ClusterCollie): server.Route =
    RouteUtil.basicRoute[NodeCreationRequest, Node](
      root = NODES_PREFIX_PATH,
      hookOfAdd = (_: TargetCluster, _: Id, request: NodeCreationRequest) => {
        if (request.name.isEmpty) Future.failed(new IllegalArgumentException(s"name is required"))
        else
          update(
            NodeApi.node(
              name = request.name.get,
              port = request.port,
              user = request.user,
              password = request.password
            ))
      },
      hookOfUpdate = (name: Id, request: NodeCreationRequest, previous: Node) => {
        if (request.name.exists(_ != name))
          Future.failed(
            new IllegalArgumentException(s"the name from request is conflict with previous setting:${previous.name}"))
        else
          update(
            NodeApi.node(
              name = name,
              port = request.port,
              user = request.user,
              password = request.password
            ))
      },
      hookOfGet = (response: Node) => update(response),
      hookOfList = (responses: Seq[Node]) => update(responses),
      hookOfDelete = (response: Node) => Future.successful(response)
    )
}
