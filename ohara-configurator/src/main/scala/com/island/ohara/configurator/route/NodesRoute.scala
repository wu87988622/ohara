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
import com.typesafe.scalalogging.Logger

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
object NodesRoute {
  private[this] lazy val LOG = Logger(NodesRoute.getClass)

  /**
    * all exceptions are swallowed since the ssh info may be wrong.
    */
  private[this] def wrapExceptionToEmpty[T](f: => Future[Seq[T]]): Seq[T] = try Await.result(f, 30 seconds)
  catch {
    case e: Throwable =>
      LOG.error("there is a invalid node!!!", e)
      Seq.empty
  }
  private[this] def update(res: Node)(implicit clusterCollie: ClusterCollie): Node = res.copy(
    services = Seq(
      NodeService(
        name = "zookeeper",
        clusterNames = wrapExceptionToEmpty(
          clusterCollie
            .zookeepersCollie()
            .clusters()
            .map(_.filter(_._1.nodeNames.contains(res.name)).map(_._1.name).toSeq))
      ),
      NodeService(
        name = "broker",
        clusterNames = wrapExceptionToEmpty(
          clusterCollie.brokerCollie().clusters().map(_.filter(_._1.nodeNames.contains(res.name)).map(_._1.name).toSeq))
      ),
      NodeService(
        name = "connect-worker",
        clusterNames = wrapExceptionToEmpty(
          clusterCollie.workerCollie().clusters().map(_.filter(_._1.nodeNames.contains(res.name)).map(_._1.name).toSeq))
      )
    )
  )

  def apply(implicit store: Store, clusterCollie: ClusterCollie): server.Route =
    RouteUtil.basicRoute[NodeCreationRequest, Node](
      root = NODES_PREFIX_PATH,
      hookOfAdd = (_: String, request: NodeCreationRequest) => {
        if (request.name.isEmpty) throw new IllegalArgumentException(s"name is required")
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
      hookOfUpdate = (name: String, request: NodeCreationRequest, previous: Node) => {
        if (request.name.exists(_ != name))
          throw new IllegalArgumentException(
            s"the name from request is conflict with previous setting:${previous.name}")
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
      hookOfGet = (response: Node) => response,
      hookOfList = (responses: Seq[Node]) => responses,
      hookBeforeDelete = (id: String) => id,
      hookOfDelete = (response: Node) => response
    )
}
