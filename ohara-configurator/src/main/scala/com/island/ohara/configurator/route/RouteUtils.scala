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
    * process the data for input Get request
    * @tparam Res data
    */
  trait HookOfGet[Res <: Data] {
    def apply(res: Res): Future[Res]
  }

  /**
    * process the data for input List request
    * @tparam Res data
    */
  trait HookOfList[Res <: Data] {
    def apply(res: Seq[Res]): Future[Seq[Res]]
  }

  /**
    * this hook is invoked after http request is parsed and converted to scala object.
    * @tparam Creation creation object
    * @tparam Res result to response
    */
  trait HookOfCreation[Creation <: CreationRequest, Res <: Data] {
    def apply(group: String, creation: Creation): Future[Res]
  }

  /**
    * this hook is invoked after http request is parsed and converted to scala object.
    *
    * Noted: the update request ought to create a new object if the input (group, key) are not associated to an existent object
    * (it means the previous is not defined).
    * @tparam Creation creation object
    * @tparam Res result to response
    */
  trait HookOfUpdate[Creation <: CreationRequest, Update, Res <: Data] {
    def apply(key: DataKey, update: Update, previous: Option[Res]): Future[Res]
  }

  /**
    * Do something before the objects does be removed from store actually.
    *
    * Noted: the returned (group, name) can differ from input. And the removed object is associated to the returned stuff.
    */
  trait HookBeforeDelete {
    def apply(key: DataKey): Future[Unit]
  }

  /**
    * used to execute START request.
    * Noted, the response will be added to store.
    * @tparam Res data
    */
  trait HookOfStart[Res] {
    def apply(key: DataKey): Future[Res]
  }

  /**
    * used to execute STOP request.
    * Noted, the response will be added to store.
    * @tparam Res data
    */
  trait HookOfStop[Res] {
    def apply(key: DataKey): Future[Res]
  }

  /**
    * used to execute PAUSE request.
    * Noted, the response will be added to store.
    * @tparam Res data
    */
  trait HookOfPause[Res] {
    def apply(key: DataKey): Future[Res]
  }

  /**
    * used to execute RESUME request.
    * Noted, the response will be added to store.
    * @tparam Res data
    */
  trait HookOfResume[Res] {
    def apply(key: DataKey): Future[Res]
  }

  /**
    * generate the error message used to indicate that some fields are miss in the update request.
    * @param key key
    * @param fieldName name of field
    * @return error message
    */
  def errorMessage(key: DataKey, fieldName: String): String =
    s"$key does not exist so there is an new object will be created. Hence, you cannot ignore $fieldName"

  //-------------------- global parameter for route -------------------------//
  val LOG = Logger(RouteUtils.getClass)
  type Id = String

  /** default we restrict the jar size to 50MB */
  final val DEFAULT_FILE_SIZE_BYTES = 50 * 1024 * 1024L
  final val START_COMMAND: String = com.island.ohara.client.configurator.v0.START_COMMAND
  final val STOP_COMMAND: String = com.island.ohara.client.configurator.v0.STOP_COMMAND
  final val PAUSE_COMMAND: String = com.island.ohara.client.configurator.v0.PAUSE_COMMAND
  final val RESUME_COMMAND: String = com.island.ohara.client.configurator.v0.RESUME_COMMAND

  /**
    * a route to custom CREATION and UPDATE resource. It offers default implementation to GET, LIST and DELETE.
    * The CREATION is routed to "POST  /$root"
    * The UPDATE is routed to "PUT /$root/$name"
    * The GET is routed to "GET /$root/$name"
    * The LIST is routed to "GET /$root"
    * The DELETE is routed to "DELETE /$root/$name"
    * @param root the prefix of URL
    * @param enableGroup true if this route accept group. Otherwise, the input group is ignored and the group passed to route is Data.GROUP_DEFAULT
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
  def route[Creation <: CreationRequest, Update, Res <: Data: ClassTag](
    root: String,
    enableGroup: Boolean,
    hookOfCreation: HookOfCreation[Creation, Res],
    hookOfUpdate: HookOfUpdate[Creation, Update, Res])(implicit store: DataStore,
                                                       // normally, update request does not carry the name field,
                                                       // Hence, the check of name have to be executed by format of creation
                                                       // since it must have name field.
                                                       rm: OharaJsonFormat[Creation],
                                                       rm1: RootJsonFormat[Update],
                                                       rm2: RootJsonFormat[Res],
                                                       executionContext: ExecutionContext): server.Route =
    route(
      root = root,
      enableGroup = enableGroup,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfGet = (res: Res) => Future.successful(res),
      hookOfList = (res: Seq[Res]) => Future.successful(res),
      hookBeforeDelete = (_: DataKey) => Future.successful(Unit)
    )

  /**
    *  this is the basic route of all APIs to access ohara's data.
    *  It implements 1) get, 2) list, 3) delete, 4) add and 5) update function.
    * @param root path to root
    * @param enableGroup true if this route accept group. Otherwise, the input group is ignored and the group passed to route is Data.GROUP_DEFAULT
    * @param hookOfCreation used to convert request to response for Add function
    * @param hookOfUpdate used to convert request to response for Update function
    * @param hookOfList used to convert response for List function
    * @param hookOfGet used to convert response for Get function
    * @param hookBeforeDelete used to do something before doing delete operation. For example, validate the name.
    * @param store data store
    * @param rm marshalling of creation
    * @param rm1 marshalling of update
    * @param rm2 marshalling of response
    * @param executionContext thread pool
    * @tparam Creation creation request
    * @tparam Update creation request
    * @tparam Res response
    * @return route
    */
  def route[Creation <: CreationRequest, Update, Res <: Data: ClassTag](
    root: String,
    enableGroup: Boolean,
    hookOfCreation: HookOfCreation[Creation, Res],
    hookOfUpdate: HookOfUpdate[Creation, Update, Res],
    hookOfList: HookOfList[Res],
    hookOfGet: HookOfGet[Res],
    hookBeforeDelete: HookBeforeDelete)(implicit store: DataStore,
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
          parameter(Data.GROUP_KEY ?) { groupOption =>
            val group = if (enableGroup) groupOption.getOrElse(Data.GROUP_DEFAULT) else Data.GROUP_DEFAULT
            complete(hookOfCreation(group, creation).flatMap(res => store.addIfAbsent(res)))
          }
        }) ~
          get(complete(store.values[Res]().flatMap(hookOfList(_))))
      } ~ path(Segment) { name =>
        parameter(Data.GROUP_KEY ?) { groupOption =>
          val group = if (enableGroup) groupOption.getOrElse(Data.GROUP_DEFAULT) else Data.GROUP_DEFAULT
          val key = DataKey(
            group = rm.check(Data.GROUP_KEY, JsString(group)).value,
            name = rm.check(Data.NAME_KEY, JsString(name)).value
          )
          get(complete(store.value[Res](key).flatMap(hookOfGet(_)))) ~
            delete(complete(
              hookBeforeDelete(key).map(_ => key).flatMap(store.remove[Res](_).map(_ => StatusCodes.NoContent)))) ~
            put(
              entity(as[Update])(
                update =>
                  complete(
                    store
                      .get[Res](key)
                      .flatMap(previous => hookOfUpdate(key = key, update = update, previous = previous))
                      .flatMap(store.add))))
        }
      }
    }

  /**
    * this is the basic route of all APIs to access ohara's data.
    * It implements 1) get, 2) list, 3) delete, 4) add, 5) update, 6) start and 7) stop function.
    * The CREATION is routed to "POST  /$root"
    * The UPDATE is routed to "PUT /$root/$name"
    * The GET is routed to "GET /$root/$name"
    * The LIST is routed to "GET /$root"
    * The DELETE is routed to "DELETE /$root/$name"
    * The START is routed to "PUT /$root/$name/start"
    * The STOP is routed to "PUT /$root/$name/stop"
    * @param root path to root
    * @param enableGroup true if this route accept group. Otherwise, the input group is ignored and the group passed to route is Data.GROUP_DEFAULT
    * @param hookOfCreation used to convert request to response for Add function
    * @param hookOfUpdate used to convert request to response for Update function
    * @param hookOfList used to convert response for List function
    * @param hookOfGet used to convert response for Get function
    * @param hookBeforeDelete used to do something before doing delete operation. For example, validate the name.
    * @param hookOfStart used to handle start command
    * @param hookOfStop used to handle stop command
    * @param store data store
    * @param rm marshalling of creation
    * @param rm1 marshalling of update
    * @param rm2 marshalling of response
    * @param executionContext thread pool
    * @tparam Creation creation request
    * @tparam Update creation request
    * @tparam Res response
    * @return route
    */
  def route[Creation <: CreationRequest, Update, Res <: Data: ClassTag](
    root: String,
    enableGroup: Boolean,
    hookOfCreation: HookOfCreation[Creation, Res],
    hookOfUpdate: HookOfUpdate[Creation, Update, Res],
    hookOfList: HookOfList[Res],
    hookOfGet: HookOfGet[Res],
    hookBeforeDelete: HookBeforeDelete,
    hookOfStart: HookOfStart[Res],
    hookOfStop: HookOfStop[Res])(implicit store: DataStore,
                                 // normally, update request does not carry the name field,
                                 // Hence, the check of name have to be executed by format of creation
                                 // since it must have name field.
                                 rm: OharaJsonFormat[Creation],
                                 rm1: RootJsonFormat[Update],
                                 rm2: RootJsonFormat[Res],
                                 executionContext: ExecutionContext): server.Route = route(
    root = root,
    enableGroup = enableGroup,
    hookOfCreation = hookOfCreation,
    hookOfUpdate = hookOfUpdate,
    hookOfList = hookOfList,
    hookOfGet = hookOfGet,
    hookBeforeDelete = hookBeforeDelete
  ) ~ pathPrefix(root / Segment) { name =>
    parameter(Data.GROUP_KEY ?) { groupOption =>
      put {
        val group = if (enableGroup) groupOption.getOrElse(Data.GROUP_DEFAULT) else Data.GROUP_DEFAULT
        val key = DataKey(
          group = group,
          name = name
        )
        path(START_COMMAND)(complete(StatusCodes.Accepted -> hookOfStart(key).flatMap(res => store.add[Res](res)))) ~ path(
          STOP_COMMAND)(complete(StatusCodes.Accepted -> hookOfStop(key).flatMap(res => store.add[Res](res))))
      }
    }
  }

  /**
    * this is the basic route of all APIs to access ohara's data.
    * It implements 1) get, 2) list, 3) delete, 4) add, 5) update, 6) start and 7) stop function.
    * The CREATION is routed to "POST  /$root"
    * The UPDATE is routed to "PUT /$root/$name"
    * The GET is routed to "GET /$root/$name"
    * The LIST is routed to "GET /$root"
    * The DELETE is routed to "DELETE /$root/$name"
    * The START is routed to "PUT /$root/$name/start"
    * The STOP is routed to "PUT /$root/$name/stop"
    * The PAUSE is routed to "PUT /$root/$name/pause"
    * The RESUME is routed to "PUT /$root/$name/resume"
    * @param root path to root
    * @param enableGroup true if this route accept group. Otherwise, the input group is ignored and the group passed to route is Data.GROUP_DEFAULT
    * @param hookOfCreation used to convert request to response for Add function
    * @param hookOfUpdate used to convert request to response for Update function
    * @param hookOfList used to convert response for List function
    * @param hookOfGet used to convert response for Get function
    * @param hookBeforeDelete used to do something before doing delete operation. For example, validate the name.
    * @param hookOfStart used to handle start command
    * @param hookOfStop used to handle stop command
    * @param hookOfPause used to handle pause command
    * @param hookOfResume used to handle resume command
    * @param store data store
    * @param rm marshalling of creation
    * @param rm1 marshalling of update
    * @param rm2 marshalling of response
    * @param executionContext thread pool
    * @tparam Creation creation request
    * @tparam Update creation request
    * @tparam Res response
    * @return route
    */
  def route[Creation <: CreationRequest, Update, Res <: Data: ClassTag](
    root: String,
    enableGroup: Boolean,
    hookOfCreation: HookOfCreation[Creation, Res],
    hookOfUpdate: HookOfUpdate[Creation, Update, Res],
    hookOfList: HookOfList[Res],
    hookOfGet: HookOfGet[Res],
    hookBeforeDelete: HookBeforeDelete,
    hookOfStart: HookOfStart[Res],
    hookOfStop: HookOfStop[Res],
    hookOfPause: HookOfPause[Res],
    hookOfResume: HookOfResume[Res])(implicit store: DataStore,
                                     // normally, update request does not carry the name field,
                                     // Hence, the check of name have to be executed by format of creation
                                     // since it must have name field.
                                     rm: OharaJsonFormat[Creation],
                                     rm1: RootJsonFormat[Update],
                                     rm2: RootJsonFormat[Res],
                                     executionContext: ExecutionContext): server.Route = route(
    root = root,
    enableGroup = enableGroup,
    hookOfCreation = hookOfCreation,
    hookOfUpdate = hookOfUpdate,
    hookOfList = hookOfList,
    hookOfGet = hookOfGet,
    hookBeforeDelete = hookBeforeDelete
  ) ~ pathPrefix(root / Segment) { name =>
    parameter(Data.GROUP_KEY ?) { groupOption =>
      put {
        val group = if (enableGroup) groupOption.getOrElse(Data.GROUP_DEFAULT) else Data.GROUP_DEFAULT
        val key = DataKey(
          group = group,
          name = name
        )
        path(START_COMMAND)(complete(StatusCodes.Accepted -> hookOfStart(key).flatMap(res => store.add[Res](res)))) ~
          path(STOP_COMMAND)(complete(StatusCodes.Accepted -> hookOfStop(key).flatMap(res => store.add[Res](res)))) ~
          path(PAUSE_COMMAND)(complete(StatusCodes.Accepted -> hookOfPause(key).flatMap(res => store.add[Res](res)))) ~
          path(RESUME_COMMAND)(complete(StatusCodes.Accepted -> hookOfResume(key).flatMap(res => store.add[Res](res))))
      }
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
    enableGroup: Boolean,
    collie: Collie[Req, Creator],
    hookOfStart: (Seq[ClusterInfo], Req) => Future[Req],
    hookOfStop: String => Future[String])(implicit store: DataStore,
                                          clusterCollie: ClusterCollie,
                                          nodeCollie: NodeCollie,
                                          rm: OharaJsonFormat[Req],
                                          executionContext: ExecutionContext): server.Route =
    pathPrefix(root / Segment) { clusterName =>
      path(Segment) { remainder =>
        parameter(Data.GROUP_KEY ?) { groupOption =>
          val group = if (enableGroup) groupOption.getOrElse(Data.GROUP_DEFAULT) else Data.GROUP_DEFAULT
          val key = DataKey(
            group = rm.check(Data.GROUP_KEY, JsString(group)).value,
            name = rm.check(Data.NAME_KEY, JsString(clusterName)).value
          )
          remainder match {
            case START_COMMAND =>
              put {
                complete(store.value[Req](key).flatMap { req =>
                  collie.exist(req.name).flatMap {
                    if (_) {
                      // this cluster already exists, return OK
                      Future.successful(StatusCodes.Accepted)
                    } else {
                      basicCheckOfCluster2(nodeCollie, clusterCollie, req)
                        .flatMap(clusters => hookOfStart(clusters, req))
                        .map(_ => StatusCodes.Accepted)
                    }
                  }
                })
              }
            case STOP_COMMAND =>
              put {
                parameter(Data.FORCE_KEY ?)(
                  force =>
                    complete(
                      collie
                        .cluster(clusterName)
                        .flatMap(
                          _ =>
                            hookOfStop(clusterName)
                              .flatMap(_ =>
                                if (force.exists(_.toLowerCase == "true"))
                                  collie.forceRemove(clusterName)
                                else collie.remove(clusterName))
                              .map(_ => StatusCodes.Accepted)
                        )
                  )
                )
              }
            case nodeName =>
              put {
                complete(collie.cluster(clusterName).map(_._1).flatMap { cluster =>
                  if (cluster.nodeNames.contains(nodeName)) Future.successful(cluster)
                  else collie.addNode(clusterName, nodeName)
                })
              } ~ delete {
                complete(collie.clusters().map(_.keys.toSeq).flatMap { clusters =>
                  if (clusters.exists(
                        cluster => cluster.name == clusterName && cluster.nodeNames.contains(nodeName)
                      ))
                    collie.removeNode(clusterName, nodeName).map(_ => StatusCodes.NoContent)
                  else Future.successful(StatusCodes.NoContent)
                })
              }
          }
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
            parameter(Data.FORCE_KEY ?)(force =>
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
