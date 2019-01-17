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
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import com.island.ohara.agent.{Collie, NodeCollie}
import com.island.ohara.client.configurator.v0.PipelineApi.Pipeline
import com.island.ohara.client.configurator.v0.{ClusterCreationRequest, ClusterInfo, Data, ErrorApi}
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
private[configurator] object RouteUtil {
  def rejectNonexistentUuid(uuid: String): StandardRoute = complete(
    StatusCodes.BadRequest -> ErrorApi.of(new IllegalArgumentException(s"Failed to find a schema mapping to $uuid")))

  def assertNotRelated2Pipeline(id: String)(implicit store: Store): Unit =
    if (Await.result(
          store
            .values[Pipeline]
            .map(_.exists(pipeline =>
              pipeline.id == id || pipeline.rules.keys.toSet.contains(id) || pipeline.rules.values.toSet.contains(id))),
          60 seconds
        ))
      throw new IllegalArgumentException(s"The id:$id is used by pipeline")

  private[this] def routeOfAdd[Req, Res <: Data](
    hook: (String, Req) => Res)(implicit store: Store, rm: RootJsonFormat[Req], rm2: RootJsonFormat[Res]) = post {
    entity(as[Req]) { req =>
      onSuccess(store.add(hook(CommonUtil.uuid(), req)))(value => complete(value))
    }
  }
  private[this] def routeOfList[Req, Res <: Data: ClassTag](hook: Seq[Res] => Seq[Res])(implicit store: Store,
                                                                                        rm: RootJsonFormat[Res]) = get(
    onSuccess(store.values[Res])(values => complete(hook(values))))

  private[this] def routeOfGet[Req, Res <: Data: ClassTag](id: String, hook: Res => Res)(implicit store: Store,
                                                                                         rm: RootJsonFormat[Res]) = get(
    onSuccess(store.value[Res](id))(value => complete(hook(value))))

  private[this] def routeOfDelete[Req, Res <: Data: ClassTag](id: String, hook: String => String, hook1: Res => Res)(
    implicit store: Store,
    rm: RootJsonFormat[Res]) =
    delete(onSuccess(store.remove[Res](hook(id)))(value => complete(hook1(value))))

  private[this] def routeOfUpdate[Req, Res <: Data: ClassTag](
    id: String,
    hook: (String, Req, Res) => Res)(implicit store: Store, rm: RootJsonFormat[Req], rm2: RootJsonFormat[Res]) = put {
    entity(as[Req])(req =>
      onSuccess(store.update(id, (previous: Res) => hook(id, req, previous)))(value => complete(value)))
  }

  /**
    *  this is the basic route of all APIs to access ohara's data.
    *  It implements 1) get, 2) list, 3) delete, 4) add and 5) update function.
    * @param root root path of APIs
    * @param reqToRes used in updating and adding. You have to convert the request to response
    * @param resToRes used in getting, listing and deleting. You can change the response by this function
    * @tparam Req Request
    * @tparam Res Response
    * @return route
    */
  def basicRoute[Req, Res <: Data: ClassTag](root: String,
                                             reqToRes: (String, Req) => Res,
                                             resToRes: Res => Res = (r: Res) => r)(
    implicit store: Store,
    rm: RootJsonFormat[Req],
    rm2: RootJsonFormat[Res]): server.Route = basicRoute(
    root = root,
    hookOfAdd = reqToRes,
    hookOfUpdate = (id: String, req: Req, _: Res) => reqToRes(id, req),
    hookOfList = (r: Seq[Res]) => r.map(resToRes),
    hookOfGet = (r: Res) => resToRes(r),
    hookBeforeDelete = (id: String) => id,
    hookOfDelete = (r: Res) => resToRes(r)
  )

  /**
    *  this is the basic route of all APIs to access ohara's data.
    *  It implements 1) get, 2) list, 3) delete, 4) add and 5) update function.
    * @param root path to root
    * @param hookOfAdd used to convert request to response for Add function
    * @param hookOfUpdate used to convert request to response for Update function
    * @param hookOfList used to convert response for List function
    * @param hookOfGet used to convert response for Get function
    * @param hookBeforeDelete used to check the id of data before doing delete
    * @param hookOfDelete used to convert response for Delete function
    * @tparam Req request
    * @tparam Res response
    * @return route
    */
  def basicRoute[Req, Res <: Data: ClassTag](
    root: String,
    hookOfAdd: (String, Req) => Res,
    hookOfUpdate: (String, Req, Res) => Res,
    hookOfList: Seq[Res] => Seq[Res],
    hookOfGet: Res => Res,
    hookBeforeDelete: String => String,
    hookOfDelete: Res => Res)(implicit store: Store, rm: RootJsonFormat[Req], rm2: RootJsonFormat[Res]): server.Route =
    pathPrefix(root) {
      pathEnd {
        routeOfAdd[Req, Res](hookOfAdd) ~ routeOfList[Req, Res](hookOfList)
      } ~ path(Segment) { id =>
        routeOfGet[Req, Res](id, hookOfGet) ~ routeOfDelete[Req, Res](id, hookBeforeDelete, hookOfDelete) ~
          routeOfUpdate[Req, Res](id, hookOfUpdate)
      }
    }

  // TODO: remove this method after we resolve OHARA-1201 ... by chia
  def basicRoute2[Req, Res <: Data: ClassTag](
    hookOfAdd: (String, Req) => Res,
    hookOfUpdate: (String, Req, Res) => Res,
    hookOfList: Seq[Res] => Seq[Res],
    hookOfGet: Res => Res,
    hookBeforeDelete: String => String,
    hookOfDelete: Res => Res)(implicit store: Store, rm: RootJsonFormat[Req], rm2: RootJsonFormat[Res]): server.Route =
    pathEnd {
      routeOfAdd[Req, Res](hookOfAdd) ~ routeOfList[Req, Res](hookOfList)
    } ~ path(Segment) { id =>
      routeOfGet[Req, Res](id, hookOfGet) ~ routeOfDelete[Req, Res](id, hookBeforeDelete, hookOfDelete) ~
        routeOfUpdate[Req, Res](id, hookOfUpdate)
    }

  def basicRouteOfCluster[Req <: ClusterCreationRequest, Res <: ClusterInfo](root: String,
                                                                             hookOfCreation: Req => Future[Res])(
    implicit collie: Collie[Res],
    nodeCollie: NodeCollie,
    rm: RootJsonFormat[Req],
    rm1: RootJsonFormat[Res]): server.Route =
    pathPrefix(root) {
      pathEnd {
        // create cluster
        post {
          entity(as[Req]) { req =>
            if (req.nodeNames.isEmpty) throw new IllegalArgumentException(s"You are too poor to buy any server?")
            onSuccess(collie.nonExists(req.name).flatMap {
              if (_) nodeCollie.nodes(req.nodeNames).flatMap(_ => hookOfCreation(req))
              else Future.failed(new IllegalArgumentException(s"${req.name} is already running"))
            })(complete(_))
          }
        } ~ get(onSuccess(collie.clusters()) { clusters =>
          complete(clusters.keys)
        })
      } ~ pathPrefix(Segment) { clusterName =>
        path(Segment) { nodeName =>
          post {
            onSuccess(collie.addNode(clusterName, nodeName))(complete(_))
          } ~ delete {
            onSuccess(collie.removeNode(clusterName, nodeName))(complete(_))
          }
        } ~ pathEnd {
          delete {
            onComplete(collie.remove(clusterName))(cluster => complete(cluster))
          } ~ get {
            complete(collie.containers(clusterName))
          }
        }
      }
    }
}
