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
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil.{Id, TargetCluster}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
object NodesRoute {
  private[this] lazy val LOG = Logger(NodesRoute.getClass)

  private[this] def update(res: Node)(implicit clusterCollie: ClusterCollie): Future[Node] = clusterCollie
    .zookeepersCollie()
    .clusters()
    .map(_.filter(_._1.nodeNames.contains(res.name)).map(_._1.name).toSeq)
    .flatMap { zks =>
      clusterCollie
        .brokerCollie()
        .clusters()
        .map(_.filter(_._1.nodeNames.contains(res.name)).map(_._1.name).toSeq)
        .map { bks =>
          (zks, bks)
        }
    }
    .flatMap {
      case (zks, bks) =>
        clusterCollie
          .workerCollie()
          .clusters()
          .map(_.filter(_._1.nodeNames.contains(res.name)).map(_._1.name).toSeq)
          .map { wks =>
            (zks, bks, wks)
          }
    }
    .recover {
      // all exceptions are swallowed since the ssh info may be wrong.
      case e: Throwable =>
        LOG.error("there is an invalid node!!!", e)
        (Seq.empty, Seq.empty, Seq.empty)
    }
    .map {
      case (zks, bks, wks) =>
        res.copy(
          services = Seq(
            NodeService(
              name = "zookeeper",
              clusterNames = zks
            ),
            NodeService(
              name = "broker",
              clusterNames = bks
            ),
            NodeService(
              name = "connect-worker",
              clusterNames = wks
            )
          )
        )
    }

  def apply(implicit store: Store, clusterCollie: ClusterCollie): server.Route =
    RouteUtil.basicRoute[NodeCreationRequest, Node](
      root = NODES_PREFIX_PATH,
      hookOfAdd = (_: TargetCluster, _: Id, request: NodeCreationRequest) => {
        if (request.name.isEmpty) Future.failed(new IllegalArgumentException(s"name is required"))
        else
          update(
            Node(
              name = request.name.get,
              port = request.port,
              user = request.user,
              password = request.password,
              services = Seq.empty,
              lastModified = CommonUtil.current()
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
              password = request.password,
              services = Seq.empty,
              lastModified = CommonUtil.current()
            ))
      },
      hookOfGet = (response: Node) => update(response),
      hookOfList = (responses: Seq[Node]) => Future.sequence(responses.map(update)),
      hookOfDelete = (response: Node) => Future.successful(response)
    )
}
