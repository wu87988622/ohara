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
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{ClusterCollie, Collie, NodeCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0._
import com.island.ohara.configurator.store.DataStore
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}
private[route] object RouteUtils {

  /**
    * generate the error message used to indicate that some fields are miss in the update request.
    * @param name name
    * @param fieldName name of field
    * @return error message
    */
  def errorMessage(name: String, fieldName: String): String =
    s"$name does not exist so there is an new object will be created. Hence, you cannot ignore $fieldName"

  //-------------------- global parameter for route -------------------------//
  val LOG = Logger(RouteUtils.getClass)
  type Id = String

  /** default we restrict the jar size to 50MB */
  final val DEFAULT_FILE_SIZE_BYTES = 50 * 1024 * 1024L
  final val START_COMMAND: String = "start"
  final val STOP_COMMAND: String = "stop"
  final val PAUSE_COMMAND: String = "pause"
  final val RESUME_COMMAND: String = "resume"

  /**
    * a route to custom CREATION and UPDATE resource. It offers default implementation to GET, LIST and DELETE.
    * The CREATION is routed to "POST  /$root"
    * The UPDATE is routed to "PUT /$root/$name"
    * The GET is routed to "GET /$root/$name"
    * The LIST is routed to "GET /$root"
    * The DELETE is routed to "DELETE /$root/$name"
    * @param root the prefix of URL
    * @param hookOfCreation custom action for CREATION. the name is either user-defined request or random string
    * @param hookOfUpdate custom action for UPDATE. the name from URL is must equal to name in payload
    * @param store data store
    * @param rm used to marshal request
    * @param rm2 used to marshal response
    * @param executionContext thread pool
    * @tparam Creation request type
    * @tparam Res response type
    * @return route
    */
  def basicRoute[Creation <: CreationRequest, Update, Res <: Data: ClassTag](
    root: String,
    hookOfCreation: Creation => Future[Res],
    hookOfUpdate: (String, Update, Option[Res]) => Future[Res])(implicit store: DataStore,
                                                                // normally, update request does not carry the name field,
                                                                // Hence, the check of name have to be executed by format of creation
                                                                // since it must have name field.
                                                                rm: OharaJsonFormat[Creation],
                                                                rm1: RootJsonFormat[Update],
                                                                rm2: RootJsonFormat[Res],
                                                                executionContext: ExecutionContext): server.Route =
    basicRoute(
      root = root,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfList = (res: Seq[Res]) => Future.successful(res),
      hookOfGet = (res: Res) => Future.successful(res),
      hookBeforeDelete = (name: String) => Future.successful(name)
    )

  /**
    *  this is the basic route of all APIs to access ohara's data.
    *  It implements 1) get, 2) list, 3) delete, 4) add and 5) update function.
    * @param root path to root
    * @param hookOfCreation used to convert request to response for Add function
    * @param hookOfUpdate used to convert request to response for Update function
    * @param hookOfList used to convert response for List function
    * @param hookOfGet used to convert response for Get function
    * @param hookBeforeDelete used to do something before doing delete operation. For example, validate the name.
    * @tparam Creation creation request
    * @tparam Update creation request
    * @tparam Res response
    * @return route
    */
  def basicRoute[Creation <: CreationRequest, Update, Res <: Data: ClassTag](
    root: String,
    hookOfCreation: Creation => Future[Res],
    hookOfUpdate: (String, Update, Option[Res]) => Future[Res],
    hookOfList: Seq[Res] => Future[Seq[Res]],
    hookOfGet: Res => Future[Res],
    hookBeforeDelete: String => Future[String])(implicit store: DataStore,
                                                // normally, update request does not carry the name field,
                                                // Hence, the check of name have to be executed by format of creation
                                                // since it must have name field.
                                                rm: OharaJsonFormat[Creation],
                                                rm1: RootJsonFormat[Update],
                                                rm2: RootJsonFormat[Res],
                                                executionContext: ExecutionContext): server.Route =
    pathPrefix(root) {
      pathEnd {
        post(entity(as[Creation]) { creation =>
          complete(hookOfCreation(creation).flatMap(res => store.addIfAbsent(res)))
        }) ~
          get(complete(store.values[Res]().flatMap(hookOfList)))
      } ~ path(Segment) { name =>
        get(complete(store.value[Res](name).flatMap(hookOfGet))) ~
          delete(
            complete(hookBeforeDelete(name).flatMap(name => store.remove[Res](name).map(_ => StatusCodes.NoContent)))) ~
          put(entity(as[Update])(update =>
            complete(store.get[Res](rm.check("name", JsString(name)).value).flatMap { previous =>
              hookOfUpdate(name, update, previous).flatMap(res => store.add(name, res))
            })))
      }
    }

  /**
    * Append cluster actions to current route.
    * For example:
    * if the current path is: /v0/api/zookeeper
    * we will add the following actions:
    *
    * - Operate current cluster: PUT /v0/api/zookeeper/xxx/[start|stop]
    * - Manage nodes of current cluster: [PUT / DELETE] /v0/api/zookeeper/xxx/{nodeName}
    */
  def appendRouteOfClusterAction[Req <: ClusterInfo with Data: ClassTag, Creator <: ClusterCreator[Req]](
    root: String,
    collie: Collie[Req, Creator],
    hookOfStart: (Seq[ClusterInfo], Req) => Future[Req],
    hookOfStop: String => Future[String])(implicit store: DataStore,
                                          clusterCollie: ClusterCollie,
                                          nodeCollie: NodeCollie,
                                          rm: RootJsonFormat[Req],
                                          executionContext: ExecutionContext): server.Route =
    pathPrefix(root / Segment) { clusterName =>
      path(Segment) {
        case START_COMMAND =>
          put {
            complete(store
              .value[Req](clusterName)
              .flatMap(req =>
                if (req.nodeNames.isEmpty) throw new IllegalArgumentException(s"You are too poor to buy any server?")
                else basicCheckOfCluster2(nodeCollie, clusterCollie, req).map(clusters => hookOfStart(clusters, req))))
          }
        case STOP_COMMAND =>
          put {
            parameter(Parameters.FORCE_REMOVE ?)(
              force =>
                complete(
                  collie
                    .exist(clusterName)
                    .flatMap(
                      if (_)
                        hookOfStop(clusterName)
                        // we don't use boolean convert since we don't want to see the convert exception
                          .flatMap(_ =>
                            if (force.exists(_.toLowerCase == "true")) collie.forceRemove(clusterName)
                            else collie.remove(clusterName))
                          .map(_ => None -> None)
                          .recover {
                            case e: Throwable =>
                              Some(ContainerState.DEAD.name) -> Some(e.getMessage)
                          }
                          .flatMap {
                            case (state, error) =>
                              store.addIfPresent[Req](
                                clusterName,
                                data => Future.successful(data.clone2(state, error).asInstanceOf[Req]))
                          } else
                        store.addIfPresent[Req](clusterName,
                                                data => Future.successful(data.clone2(None, None).asInstanceOf[Req]))
                    )))
          }
        case nodeName =>
          put {
            complete(collie.cluster(clusterName).map(_._1).flatMap { cluster =>
              if (cluster.nodeNames.contains(nodeName)) Future.successful(cluster)
              else collie.addNode(clusterName, nodeName)
            })
          } ~ delete {
            complete(collie.clusters().map(_.keys.toSeq).flatMap { clusters =>
              if (clusters.exists(cluster => cluster.name == clusterName && cluster.nodeNames.contains(nodeName)))
                collie.removeNode(clusterName, nodeName).map(_ => StatusCodes.NoContent)
              else Future.successful(StatusCodes.NoContent)
            })
          }
      }
    }

  // TODO deprecated, should be removed after finish #1544...by Sam
  def basicRouteOfCluster[Req <: ClusterCreationRequest, Res <: ClusterInfo: ClassTag, Creator <: ClusterCreator[Res]](
    collie: Collie[Res, Creator],
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
            complete(basicCheckOfCluster[Req, Res](nodeCollie, clusterCollie, req).map(clusters =>
              hookOfCreation(clusters, req)))
          }
        } ~ get(complete(collie.clusters().map(_.keys)))
      } ~ pathPrefix(Segment) { clusterName =>
        path(Segment) { nodeName =>
          put {
            complete(collie.cluster(clusterName).map(_._1).flatMap { cluster =>
              if (cluster.nodeNames.contains(nodeName)) Future.successful(cluster)
              else collie.addNode(clusterName, nodeName)
            })
          } ~ delete {
            complete(collie.clusters().map(_.keys.toSeq).flatMap { clusters =>
              if (clusters.exists(cluster => cluster.name == clusterName && cluster.nodeNames.contains(nodeName)))
                collie.removeNode(clusterName, nodeName).map(_ => StatusCodes.NoContent)
              else Future.successful(StatusCodes.NoContent)
            })
          }
        } ~ pathEnd {
          delete {
            parameter(Parameters.FORCE_REMOVE ?)(force =>
              // we must list ALL clusters !!!
              complete(clusterCollie.clusters().map(_.keys.toSeq).flatMap { clusters =>
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

  //------------------------------ helper methods ---------------------------//
  /**
    * Test whether this cluster satisfied the following rules:
    * <p>
    * 1) cluster image in all nodes
    * 2) name should not conflict
    * 3) port should not conflict
    *
    * @param nodeCollie nodeCollie instance
    * @param clusterCollie clusterCollie instance
    * @param req cluster creation request
    * @param executionContext execution context
    * @tparam Req type of request
    * @return clusters that fitted the requires
    */
  private[route] def basicCheckOfCluster2[Req <: ClusterInfo: ClassTag](
    nodeCollie: NodeCollie,
    clusterCollie: ClusterCollie,
    req: Req)(implicit executionContext: ExecutionContext): Future[Seq[ClusterInfo]] = {
    // nodeCollie.nodes(req.nodeNames) is used to check the existence of node names of request
    nodeCollie
      .nodes(req.nodeNames)
      .flatMap(clusterCollie.images)
      // check the docker images
      .map { nodesImages =>
        val image = req.imageName
        nodesImages
          .filterNot(_._2.contains(image))
          .keys
          .map(_.name)
          .foreach(n => throw new IllegalArgumentException(s"$n doesn't have docker image:$image"))
        nodesImages
      }
      .flatMap(_ => clusterCollie.clusters().map(_.keys.toSeq))
      .map { clusters =>
        def serviceName(cluster: ClusterInfo): String = cluster match {
          case _: ZookeeperClusterInfo => s"zookeeper cluster:${cluster.name}"
          case _: BrokerClusterInfo    => s"broker cluster:${cluster.name}"
          case _: WorkerClusterInfo    => s"worker cluster:${cluster.name}"
          case _: StreamClusterInfo    => s"stream cluster:${cluster.name}"
          case _                       => s"cluster:${cluster.name}"
        }
        // check name conflict
        clusters
          .filter(c => classTag[Req].runtimeClass.isInstance(c))
          .map(_.asInstanceOf[Req])
          .find(_.name == req.name)
          .foreach(conflictCluster => throw new IllegalArgumentException(s"${serviceName(conflictCluster)} is running"))

        // check port conflict
        Some(clusters
          .flatMap { cluster =>
            val conflictPorts = cluster.ports.intersect(req.ports)
            if (conflictPorts.isEmpty) None
            else Some(cluster -> conflictPorts)
          }
          .map {
            case (cluster, conflictPorts) =>
              s"ports:${conflictPorts.mkString(",")} are used by ${serviceName(cluster)} (the port is generated randomly if it is ignored from request)"
          }
          .mkString(";")).filter(_.nonEmpty).foreach(s => throw new IllegalArgumentException(s))
        clusters
      }
  }

  // TODO deprecated, should be removed after finish #1544...by Sam
  private[route] def basicCheckOfCluster[Req <: ClusterCreationRequest, Res <: ClusterInfo: ClassTag](
    nodeCollie: NodeCollie,
    clusterCollie: ClusterCollie,
    req: Req)(implicit executionContext: ExecutionContext): Future[Seq[ClusterInfo]] = {
    // nodeCollie.nodes(req.nodeNames) is used to check the existence of node names of request
    nodeCollie
      .nodes(req.nodeNames)
      .flatMap(clusterCollie.images)
      // check the docker images
      .map { nodesImages =>
        val image = req.imageName
        nodesImages
          .filterNot(_._2.contains(image))
          .keys
          .map(_.name)
          .foreach(n => throw new IllegalArgumentException(s"$n doesn't have docker image:$image"))
        nodesImages
      }
      .flatMap(_ => clusterCollie.clusters().map(_.keys.toSeq))
      .map { clusters =>
        def serviceName(cluster: ClusterInfo): String = cluster match {
          case _: ZookeeperClusterInfo => s"zookeeper cluster:${cluster.name}"
          case _: BrokerClusterInfo    => s"broker cluster:${cluster.name}"
          case _: WorkerClusterInfo    => s"worker cluster:${cluster.name}"
          case _: StreamClusterInfo    => s"stream cluster:${cluster.name}"
          case _                       => s"cluster:${cluster.name}"
        }
        // check name conflict
        clusters
          .filter(c => classTag[Res].runtimeClass.isInstance(c))
          .map(_.asInstanceOf[Res])
          .find(_.name == req.name)
          .foreach(conflictCluster => throw new IllegalArgumentException(s"${serviceName(conflictCluster)} is running"))

        // check port conflict
        Some(clusters
          .flatMap { cluster =>
            val conflictPorts = cluster.ports.intersect(req.ports)
            if (conflictPorts.isEmpty) None
            else Some(cluster -> conflictPorts)
          }
          .map {
            case (cluster, conflictPorts) =>
              s"ports:${conflictPorts.mkString(",")} are used by ${serviceName(cluster)} (the port is generated randomly if it is ignored from request)"
          }
          .mkString(";")).filter(_.nonEmpty).foreach(s => throw new IllegalArgumentException(s))
        clusters
      }
  }
}
