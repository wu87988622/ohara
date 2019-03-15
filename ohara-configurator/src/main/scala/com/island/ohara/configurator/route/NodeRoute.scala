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
import com.island.ohara.client.configurator.v0.NodeApi._
import com.island.ohara.configurator.route.RouteUtils.{Id, TargetCluster}
import com.island.ohara.configurator.store.DataStore
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
object NodeRoute {
  private[this] lazy val LOG = Logger(NodeRoute.getClass)

  private[this] def update(node: Node)(implicit clusterCollie: ClusterCollie,
                                       executionContext: ExecutionContext): Future[Node] =
    update(Seq(node)).map(_.head)

  private[this] def update(nodes: Seq[Node])(implicit clusterCollie: ClusterCollie,
                                             executionContext: ExecutionContext): Future[Seq[Node]] =
    clusterCollie.fetchServices(nodes).recover {
      case e: Throwable =>
        LOG.error("failed to seek cluster information", e)
        nodes
    }

  def apply(implicit store: DataStore, clusterCollie: ClusterCollie, executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRoute[NodeCreationRequest, Node](
      root = NODES_PREFIX_PATH,
      hookOfAdd = (_: TargetCluster, _: Id, request: NodeCreationRequest) => {
        if (request.name.isEmpty) Future.failed(new IllegalArgumentException(s"name is required"))
        else
          update(
            Node(
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
            Node(
              name = name,
              port = request.port,
              user = request.user,
              password = request.password
            ))
      },
      hookOfGet = (response: Node) => update(response),
      hookOfList = (responses: Seq[Node]) => update(responses),
      hookBeforeDelete = (id: String) =>
        store
          .value[Node](id)
          .flatMap(update)
          .map { node =>
            def toString(services: Seq[NodeService]): String = services
              .filter(_.clusterNames.nonEmpty)
              .map(s => s"${s.name}:${s.clusterNames.mkString(".")}")
              .mkString(" ")
            if (node.services.map(_.clusterNames.size).sum != 0)
              throw new IllegalStateException(
                s"${node.name} is running ${toString(node.services)}. Please stop all services before deleting")
            node.id
        },
      // we don't need to update node in deleting
      hookOfDelete = (response: Node) => Future.successful(response)
    )
}
