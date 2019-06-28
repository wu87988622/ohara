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
import com.island.ohara.common.util.CommonUtils
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
    RouteUtils.basicRoute[Creation, Update, Node](
      root = NODES_PREFIX_PATH,
      hookOfAdd = (request: Creation) => {
        if (request.name.isEmpty) Future.failed(new IllegalArgumentException(s"name is required"))
        else
          update(
            Node(
              name = request.name,
              port = request.port,
              user = request.user,
              password = request.password,
              services = Seq.empty,
              lastModified = CommonUtils.current()
            ))
      },
      hookOfUpdate = (name: String, request: Update, previous: Option[Node]) => {
        update(
          Node(
            name = name,
            port = request.port.getOrElse(
              previous.map(_.port).getOrElse(throw new NoSuchElementException(RouteUtils.errorMessage(name, "port")))),
            user = request.user.getOrElse(
              previous.map(_.user).getOrElse(throw new NoSuchElementException(RouteUtils.errorMessage(name, "user")))),
            password = request.password.getOrElse(
              previous
                .map(_.password)
                .getOrElse(throw new NoSuchElementException(RouteUtils.errorMessage(name, "password")))),
            services = Seq.empty,
            lastModified = CommonUtils.current()
          ))
      },
      hookOfGet = (response: Node) => update(response),
      hookOfList = (responses: Seq[Node]) => update(responses),
      hookBeforeDelete = (id: String) =>
        store
          .get[Node](id)
          .flatMap(_.map {
            update(_).map { node =>
              if (node.services.map(_.clusterNames.size).sum != 0) {
                throw new IllegalStateException(
                  s"${node.name} is running ${node.services.filter(_.clusterNames.nonEmpty).map(s => s"${s.name}:${s.clusterNames.mkString(".")}").mkString(" ")}. " +
                    s"Please stop all services before deleting")
              }
              id
            }
          }.getOrElse(Future.successful(id)))
    )

}
