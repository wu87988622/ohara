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
import com.island.ohara.client.configurator.v0.DataKey
import com.island.ohara.client.configurator.v0.NodeApi._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.RouteUtils._
import com.island.ohara.configurator.store.DataStore
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
object NodeRoute {
  private[this] lazy val LOG = Logger(NodeRoute.getClass)

  private[this] def updateServices(node: Node)(implicit clusterCollie: ClusterCollie,
                                               executionContext: ExecutionContext): Future[Node] =
    updateServices(Seq(node)).map(_.head)

  private[this] def updateServices(nodes: Seq[Node])(implicit clusterCollie: ClusterCollie,
                                                     executionContext: ExecutionContext): Future[Seq[Node]] =
    clusterCollie.fetchServices(nodes).recover {
      case e: Throwable =>
        LOG.error("failed to seek cluster information", e)
        nodes
    }

  private[this] def hookOfGet(implicit clusterCollie: ClusterCollie,
                              executionContext: ExecutionContext): HookOfGet[Node] = updateServices(_)

  private[this] def hookOfList(implicit clusterCollie: ClusterCollie,
                               executionContext: ExecutionContext): HookOfList[Node] =
    Future.traverse(_)(updateServices)

  private[this] def hookOfCreation(implicit clusterCollie: ClusterCollie,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, Node] =
    (_: String, creation: Creation) =>
      updateServices(
        Node(
          hostname = creation.hostname,
          port = creation.port,
          user = creation.user,
          password = creation.password,
          services = Seq.empty,
          lastModified = CommonUtils.current(),
          validationReport = None,
          tags = creation.tags
        ))

  private[this] def hookOfUpdate(implicit clusterCollie: ClusterCollie,
                                 executionContext: ExecutionContext): HookOfUpdate[Creation, Update, Node] =
    (key: DataKey, update: Update, previous: Option[Node]) =>
      updateServices(
        Node(
          hostname = key.name,
          port = if (update.port.isDefined) update.port else previous.flatMap(_.port),
          user = if (update.user.isDefined) update.user else previous.flatMap(_.user),
          password = if (update.password.isDefined) update.password else previous.flatMap(_.password),
          services = Seq.empty,
          lastModified = CommonUtils.current(),
          // clear the report for update request
          validationReport = None,
          tags = update.tags.getOrElse(previous.map(_.tags).getOrElse(Map.empty))
        ))

  private[this] def hookBeforeDelete(implicit store: DataStore,
                                     clusterCollie: ClusterCollie,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: DataKey) =>
    store
      .get[Node](key)
      .flatMap(_.map {
        updateServices(_).map { node =>
          if (node.services.map(_.clusterNames.size).sum != 0) {
            throw new IllegalStateException(
              s"${node.name} is running ${node.services.filter(_.clusterNames.nonEmpty).map(s => s"${s.name}:${s.clusterNames.mkString(".")}").mkString(" ")}. " +
                s"Please stop all services before deleting")
          }
          key
        }
      }.getOrElse(Future.successful(key)))

  def apply(implicit store: DataStore, clusterCollie: ClusterCollie, executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRoute[Creation, Update, Node](
      root = NODES_PREFIX_PATH,
      enableGroup = false,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookBeforeDelete = hookBeforeDelete
    )

}
