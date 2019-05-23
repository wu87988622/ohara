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
import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.agent.{ClusterCollie, Collie, NodeCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.store.DataStore
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}
private[route] object RouteUtils {
  val LOG = Logger(RouteUtils.getClass)
  type Id = String

  private[this] def routeOfAdd[Req, Res <: Data](hook: (Id, Req) => Future[Res])(implicit store: DataStore,
                                                                                 rm: RootJsonFormat[Req],
                                                                                 rm2: RootJsonFormat[Res],
                                                                                 executionContext: ExecutionContext) =
    post {
      entity(as[Req])(req => complete(hook(CommonUtils.uuid(), req).flatMap(store.add)))
    }

  private[this] def routeOfList[Res <: Data: ClassTag](hook: Seq[Res] => Future[Seq[Res]])(
    implicit store: DataStore,
    rm: RootJsonFormat[Res],
    executionContext: ExecutionContext) = get(complete(store.values[Res]().flatMap(values => hook(values))))

  private[this] def routeOfGet[Res <: Data: ClassTag](
    id: Id,
    hook: Res => Future[Res])(implicit store: DataStore, rm: RootJsonFormat[Res], executionContext: ExecutionContext) =
    get(complete(store.value[Res](id).flatMap(value => hook(value))))

  private[this] def routeOfDelete[Res <: Data: ClassTag](id: Id, hookBeforeDelete: String => Future[String])(
    implicit store: DataStore,
    executionContext: ExecutionContext) =
    delete(complete(hookBeforeDelete(id).flatMap(id => store.remove[Res](id).map(_ => StatusCodes.NoContent))))

  private[this] def routeOfUpdate[Req, Res <: Data: ClassTag](id: Id, hook: (Id, Req, Res) => Future[Res])(
    implicit store: DataStore,
    rm: RootJsonFormat[Req],
    rm2: RootJsonFormat[Res],
    executionContext: ExecutionContext) =
    put {
      entity(as[Req])(req => complete(store.update(id, (previous: Res) => hook(id, req, previous))))
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
                                             reqToRes: (Id, Req) => Future[Res],
                                             resToRes: Res => Future[Res] = (r: Res) => Future.successful(r))(
    implicit store: DataStore,
    rm: RootJsonFormat[Req],
    rm2: RootJsonFormat[Res],
    executionContext: ExecutionContext): server.Route = basicRoute(
    root = root,
    hookOfAdd = reqToRes,
    hookOfUpdate = (id: Id, req: Req, _: Res) => reqToRes(id, req),
    hookOfList = (r: Seq[Res]) => Future.traverse(r)(resToRes),
    hookOfGet = (r: Res) => resToRes(r)
  )

  def basicRoute[Req, Res <: Data: ClassTag](root: String,
                                             hookOfAdd: (Id, Req) => Future[Res],
                                             hookOfUpdate: (Id, Req, Res) => Future[Res],
                                             hookOfList: Seq[Res] => Future[Seq[Res]],
                                             hookOfGet: Res => Future[Res])(
    implicit store: DataStore,
    rm: RootJsonFormat[Req],
    rm2: RootJsonFormat[Res],
    executionContext: ExecutionContext): server.Route =
    basicRoute(
      root = root,
      hookOfAdd = hookOfAdd,
      hookOfUpdate = hookOfUpdate,
      hookOfList = hookOfList,
      hookOfGet = hookOfGet,
      hookBeforeDelete = id => Future.successful(id)
    )

  /**
    *  this is the basic route of all APIs to access ohara's data.
    *  It implements 1) get, 2) list, 3) delete, 4) add and 5) update function.
    * @param root path to root
    * @param hookOfAdd used to convert request to response for Add function
    * @param hookOfUpdate used to convert request to response for Update function
    * @param hookOfList used to convert response for List function
    * @param hookOfGet used to convert response for Get function
    * @param hookBeforeDelete used to do something before doing delete operation. For example, validate the id.
    * @tparam Req request
    * @tparam Res response
    * @return route
    */
  def basicRoute[Req, Res <: Data: ClassTag](root: String,
                                             hookOfAdd: (Id, Req) => Future[Res],
                                             hookOfUpdate: (Id, Req, Res) => Future[Res],
                                             hookOfList: Seq[Res] => Future[Seq[Res]],
                                             hookOfGet: Res => Future[Res],
                                             hookBeforeDelete: String => Future[String])(
    implicit store: DataStore,
    rm: RootJsonFormat[Req],
    rm2: RootJsonFormat[Res],
    executionContext: ExecutionContext): server.Route =
    pathPrefix(root) {
      pathEnd {
        routeOfAdd[Req, Res](hookOfAdd) ~ routeOfList[Res](hookOfList)
      } ~ path(Segment) { id =>
        routeOfGet[Res](id, hookOfGet) ~ routeOfDelete[Res](id, hookBeforeDelete) ~
          routeOfUpdate[Req, Res](id, hookOfUpdate)
      }
    }

  def basicRouteOfCluster[Req <: ClusterCreationRequest, Res <: ClusterInfo: ClassTag, Creator <: ClusterCreator[Res]](
    collie: Collie[Res, Creator],
    defaultImage: String,
    root: String,
    hookBeforeDelete: (Seq[ClusterInfo], String) => Future[String],
    hookOfCreation: (Seq[ClusterInfo], Req) => Future[Res])(implicit clusterCollie: ClusterCollie,
                                                            nodeCollie: NodeCollie,
                                                            rm: RootJsonFormat[Req],
                                                            rm1: RootJsonFormat[Res],
                                                            executionContext: ExecutionContext): server.Route =
    pathPrefix(root) {
      pathEnd {
        // create cluster
        post {
          entity(as[Req]) { req =>
            if (req.nodeNames.isEmpty) throw new IllegalArgumentException(s"You are too poor to buy any server?")
            // nodeCollie.nodes(req.nodeNames) is used to check the existence of node names of request
            complete(
              nodeCollie
                .nodes(req.nodeNames)
                .flatMap(clusterCollie.images)
                // check the docker images
                .map { nodesImages =>
                  val image = req.imageName.getOrElse(defaultImage)
                  nodesImages
                    .filterNot(_._2.contains(image))
                    .keys
                    .map(_.name)
                    .foreach(n => throw new IllegalArgumentException(s"$n doesn't have docker image:$image"))
                  nodesImages
                }
                .flatMap(_ => clusterCollie.clusters.map(_.keys.toSeq))
                .flatMap { clusters =>
                  def serviceName(cluster: ClusterInfo): String = cluster match {
                    case _: ZookeeperClusterInfo => s"zookeeper cluster:${cluster.name}"
                    case _: BrokerClusterInfo    => s"broker cluster:${cluster.name}"
                    case _: WorkerClusterInfo    => s"worker cluster:${cluster.name}"
                    case _                       => s"cluster:${cluster.name}"
                  }
                  // check name conflict
                  clusters
                    .filter(c => classTag[Res].runtimeClass.isInstance(c))
                    .map(_.asInstanceOf[Res])
                    .find(_.name == req.name)
                    .foreach(conflictCluster =>
                      throw new IllegalArgumentException(s"${serviceName(conflictCluster)} is running"))

                  // check port conflict
                  Some(clusters
                    .flatMap { cluster =>
                      val conflictPorts = cluster.ports.intersect(req.ports)
                      if (conflictPorts.isEmpty) None
                      else Some(cluster -> conflictPorts)
                    }
                    .map {
                      case (cluster, conflictPorts) =>
                        s"ports:${conflictPorts.mkString(",")} are used by ${serviceName(cluster)}"
                    }
                    .mkString(";")).filter(_.nonEmpty).foreach(s => throw new IllegalArgumentException(s))

                  hookOfCreation(clusters, req)
                })
          }
        } ~ get(complete(collie.clusters.map(_.keys)))
      } ~ pathPrefix(Segment) { clusterName =>
        path(Segment) { nodeName =>
          post {
            complete(collie.addNode(clusterName, nodeName))
          } ~ delete {
            complete(collie.clusters.map(_.keys.toSeq).flatMap { clusters =>
              if (clusters.exists(cluster => cluster.name == clusterName && cluster.nodeNames.contains(nodeName)))
                collie.removeNode(clusterName, nodeName).map(_ => StatusCodes.NoContent)
              else Future.successful(StatusCodes.NoContent)
            })
          }
        } ~ pathEnd {
          delete {
            parameter(Parameters.FORCE_REMOVE ?)(force =>
              // we must list ALL clusters !!!
              complete(clusterCollie.clusters.map(_.keys.toSeq).flatMap { clusters =>
                if (clusters.exists(_.name == clusterName))
                  hookBeforeDelete(clusters, clusterName)
                  // we don't use boolean convert since we don't want to see the convert exception
                    .flatMap(_ =>
                      if (force.exists(_.toLowerCase == "true")) collie.forceRemove(clusterName)
                      else collie.remove(clusterName))
                    .map(_ => StatusCodes.NoContent)
                else Future.successful(StatusCodes.NoContent)
              }))
          } ~ get {
            complete(collie.cluster(clusterName).map(_._1))
          }
        }
      }
    }
}
