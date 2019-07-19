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
      hookOfCreation = (request: Creation) =>
        update(
          Node(
            hostname = request.hostname,
            port = request.port,
            user = request.user,
            password = request.password,
            services = Seq.empty,
            lastModified = CommonUtils.current(),
            validationReport = None,
            tags = request.tags
          )),
      hookOfUpdate = (hostname: String, request: Update, previous: Option[Node]) => {
        update(
          Node(
            hostname = hostname,
            port = if (request.port.isDefined) request.port else previous.flatMap(_.port),
            user = if (request.user.isDefined) request.user else previous.flatMap(_.user),
            password = if (request.password.isDefined) request.password else previous.flatMap(_.password),
            services = Seq.empty,
            lastModified = CommonUtils.current(),
            // clear the report for update request
            validationReport = None,
            tags = request.tags.getOrElse(previous.map(_.tags).getOrElse(Map.empty))
          ))
      },
      hookOfGet = (response: Node) => update(response),
      hookOfList = (responses: Seq[Node]) => update(responses),
      hookBeforeDelete = (name: String) =>
        store
          .get[Node](name)
          .flatMap(_.map {
            update(_).map { node =>
              if (node.services.map(_.clusterNames.size).sum != 0) {
                throw new IllegalStateException(
                  s"${node.name} is running ${node.services.filter(_.clusterNames.nonEmpty).map(s => s"${s.name}:${s.clusterNames.mkString(".")}").mkString(" ")}. " +
                    s"Please stop all services before deleting")
              }
              name
            }
          }.getOrElse(Future.successful(name)))
    )

}
