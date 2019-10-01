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
import com.island.ohara.agent.ServiceCollie
import com.island.ohara.client.configurator.v0.NodeApi._
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook._
import com.island.ohara.configurator.store.DataStore
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
object NodeRoute {
  private[this] lazy val LOG = Logger(NodeRoute.getClass)

  private[this] def updateServices(node: Node)(implicit serviceCollie: ServiceCollie,
                                               executionContext: ExecutionContext): Future[Node] =
    updateServices(Seq(node)).map(_.head)

  private[this] def updateServices(nodes: Seq[Node])(implicit serviceCollie: ServiceCollie,
                                                     executionContext: ExecutionContext): Future[Seq[Node]] =
    serviceCollie.fetchServices(nodes).recover {
      case e: Throwable =>
        LOG.error("failed to seek cluster information", e)
        nodes
    }

  private[this] def hookOfGet(implicit serviceCollie: ServiceCollie,
                              executionContext: ExecutionContext): HookOfGet[Node] = updateServices(_)

  private[this] def hookOfList(implicit serviceCollie: ServiceCollie,
                               executionContext: ExecutionContext): HookOfList[Node] =
    Future.traverse(_)(updateServices)

  private[this] def hookOfCreation(implicit serviceCollie: ServiceCollie,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, Node] =
    (creation: Creation) =>
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

  private[this] def HookOfUpdating(implicit serviceCollie: ServiceCollie,
                                   executionContext: ExecutionContext): HookOfUpdating[Creation, Updating, Node] =
    (key: ObjectKey, update: Updating, previous: Option[Node]) =>
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
                                     serviceCollie: ServiceCollie,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: ObjectKey) =>
    store
      .get[Node](key)
      .flatMap(_.map {
        updateServices(_).flatMap { node =>
          if (node.services.map(_.clusterKeys.size).sum != 0)
            Future.failed(new IllegalStateException(
              s"${node.name} is running ${node.services.filter(_.clusterKeys.nonEmpty).map(s => s"${s.name}:${s.clusterKeys.mkString(".")}").mkString(" ")}. " +
                s"Please stop all services before deleting"))
          else Future.unit
        }
      }.getOrElse(Future.unit))

  def apply(implicit store: DataStore, serviceCollie: ServiceCollie, executionContext: ExecutionContext): server.Route =
    route[Creation, Updating, Node](
      root = NODES_PREFIX_PATH,
      hookOfCreation = hookOfCreation,
      HookOfUpdating = HookOfUpdating,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookBeforeDelete = hookBeforeDelete
    )

}
