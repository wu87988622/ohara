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

package com.island.ohara.configurator

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.{BrokerClusterInfo, BrokerClusterStatus}
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi.{StreamClusterInfo, StreamClusterStatus}
import com.island.ohara.client.configurator.v0.WorkerApi.{WorkerClusterInfo, WorkerClusterStatus}
import com.island.ohara.client.configurator.v0.ZookeeperApi.{ZookeeperClusterInfo, ZookeeperClusterStatus}
import com.island.ohara.client.configurator.v0.{
  ClusterCreation,
  ClusterInfo,
  ClusterStatus,
  ClusterUpdating,
  ErrorApi,
  OharaJsonFormat
}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.VersionUtils
import com.island.ohara.configurator.route.hook._
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import spray.json.{JsArray, JsString, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

package object route {

  /**
    * generate the error message used to indicate that some fields are miss in the update request.
    * @param key key
    * @param fieldName name of field
    * @return error message
    */
  def errorMessage(key: ObjectKey, fieldName: String): String =
    s"$key does not exist so there is an new object will be created. Hence, you cannot ignore $fieldName"

  /** default we restrict the jar size to 50MB */
  private[route] val DEFAULT_FILE_SIZE_BYTES = 50 * 1024 * 1024L
  private[route] val NAME_KEY: String = com.island.ohara.client.configurator.v0.NAME_KEY
  private[route] val GROUP_KEY: String = com.island.ohara.client.configurator.v0.GROUP_KEY
  private[route] val GROUP_DEFAULT: String = com.island.ohara.client.configurator.v0.GROUP_DEFAULT
  private[route] val TAGS_KEY: String = com.island.ohara.client.configurator.v0.TAGS_KEY
  private[this] val FORCE_KEY: String = com.island.ohara.client.configurator.v0.FORCE_KEY
  private[route] val START_COMMAND: String = com.island.ohara.client.configurator.v0.START_COMMAND
  private[route] val STOP_COMMAND: String = com.island.ohara.client.configurator.v0.STOP_COMMAND
  private[route] val PAUSE_COMMAND: String = com.island.ohara.client.configurator.v0.PAUSE_COMMAND
  private[route] val RESUME_COMMAND: String = com.island.ohara.client.configurator.v0.RESUME_COMMAND

  /**
    * this is a variety to basic route of all APIs to access ohara's "cluster" data.
    * It implements 1) get, 2) list, 3) delete, 4) add, 5) update, 6) start and 7) stop function.
    * The CREATION is routed to "POST  /$root"
    * The UPDATE is routed to "PUT /$root/$name"
    * The GET is routed to "GET /$root/$name"
    * The LIST is routed to "GET /$root"
    * The DELETE is routed to "DELETE /$root/$name"
    * The START is routed to "PUT /$root/$name/start"
    * The STOP is routed to "PUT /$root/$name/stop"
    *
    * The following routes are added autmatically.
    * The GET/LIST route auto-update the state of cluster
    * The DELETE route reject the request to a running cluster
    * @param root path to root
    * @param hookOfCreation used to convert request to response for Add function
    * @param hookOfUpdating used to convert request to response for Update function
    * @param hookOfStart used to handle start command
    * @param hookBeforeStop used to perform checks before stopping cluster
    * @param store data store
    * @param rm marshalling of creation
    * @param rm1 marshalling of update
    * @param rm2 marshalling of response
    * @param executionContext thread pool
    * @tparam Creation creation request for cluster resources
    * @tparam Updating creation request
    * @tparam Cluster cluster info
    * @return route
    */
  def clusterRoute[Cluster <: ClusterInfo: ClassTag,
                   Status <: ClusterStatus,
                   Creation <: ClusterCreation,
                   Updating <: ClusterUpdating](root: String,
                                                metricsKey: Option[String],
                                                hookOfCreation: HookOfCreation[Creation, Cluster],
                                                hookOfUpdating: HookOfUpdating[Updating, Cluster],
                                                hookOfStart: HookOfAction,
                                                hookBeforeStop: HookOfAction)(
    implicit store: DataStore,
    meterCache: MeterCache,
    collie: Collie[Status],
    serviceCollie: ServiceCollie,
    zookeeperCollie: ZookeeperCollie,
    brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie,
    streamCollie: StreamCollie,
    dataCollie: DataCollie,
    rm: OharaJsonFormat[Creation],
    rm1: RootJsonFormat[Updating],
    rm2: RootJsonFormat[Cluster],
    executionContext: ExecutionContext): server.Route =
    RouteBuilder[Creation, Updating, Cluster]()
      .root(root)
      .hookOfCreation(hookOfCreation)
      .hookOfUpdating(hookOfUpdating)
      .hookOfGet(updateState[Cluster, Status](_, metricsKey))
      .hookOfList((clusters: Seq[Cluster]) => Future.traverse(clusters)(updateState[Cluster, Status](_, metricsKey)))
      .hookBeforeDelete((key: ObjectKey) =>
        store.get[Cluster](key).flatMap {
          _.fold(Future.unit) { info =>
            updateState[Cluster, Status](info, metricsKey).flatMap { data =>
              if (data.state.isEmpty) Future.unit
              else
                Future.failed(new RuntimeException(
                  s"You cannot delete a non-stopped ${classTag[Cluster].runtimeClass.getSimpleName} :$key"))
            }
          }
      })
      .hookOfPutAction(
        START_COMMAND,
        (key: ObjectKey, subName: String, params: Map[String, String]) =>
          store.value[Cluster](key).flatMap { req =>
            collie.exist(key).flatMap {
              if (_) {
                // this cluster already exists, return OK
                Future.unit
              } else {
                checkResourcesConflict(dataCollie, req).flatMap(_ => hookOfStart(key, subName, params))
              }
            }
        }
      )
      .hookOfPutAction(
        STOP_COMMAND,
        (key: ObjectKey, subName: String, params: Map[String, String]) =>
          hookBeforeStop(key, subName, params).flatMap(
            _ =>
              collie
                .clusters()
                .flatMap { clusters =>
                  if (!clusters.map(_._1.key).exists(_ == key)) Future.unit
                  else if (params.get(FORCE_KEY).exists(_.toLowerCase == "true")) collie.forceRemove(key)
                  else collie.remove(key)
                }
                .flatMap(_ => Future.unit)
        )
      )
      .hookOfFinalPutAction((key: ObjectKey, nodeName: String, _: Map[String, String]) =>
        store.value[Cluster](key).flatMap { cluster =>
          // A BIT hard-code here to reuse the checker :(
          rm.check[JsArray]("nodeNames", JsArray(JsString(nodeName)))
          collie.creator
            .settings(cluster.settings)
            .nodeName(nodeName)
            .create()
            // we have to update the nodeNames of stored cluster info. Otherwise, the following Get/List request
            // will see the out-of-date nodeNames
            .flatMap(_ => store.add(cluster.newNodeNames(cluster.nodeNames + nodeName)))
            .flatMap(_ => Future.unit)
      })
      .hookOfFinalDeleteAction((key: ObjectKey, nodeName: String, _: Map[String, String]) =>
        store.get[Cluster](key).flatMap { clusterOption =>
          clusterOption
            .filter(_.nodeNames.contains(nodeName))
            .map { cluster =>
              collie
                .removeNode(key, nodeName)
                .flatMap(_ =>
                  store.addIfPresent(cluster.newNodeNames(cluster.nodeNames - nodeName).asInstanceOf[Cluster]))
                .flatMap(_ => Future.unit)
            }
            .getOrElse(Future.unit)
      })
      .build()

  /**
    * the url to official APIs documentation.
    * @return url string
    */
  def apiUrl: String = {
    val docVersion = if (VersionUtils.BRANCH == "master") "latest" else VersionUtils.BRANCH
    s"https://ohara.readthedocs.io/en/$docVersion/rest_interface.html"
  }

  private[this] def errorWithOfficialApis(inputPath: String): ErrorApi.Error = ErrorApi.Error(
    code = s"Unsupported API: $inputPath",
    message = s"please see link to find the available APIs. input url:$inputPath",
    stack = "N/A",
    apiUrl = Some(apiUrl)
  )

  def routeToOfficialUrl(inputPath: String): server.Route = complete(
    StatusCodes.NotFound -> errorWithOfficialApis(inputPath))

  /**
    * Test whether this cluster satisfied the following rules:
    * <p>
    * 1) cluster image in all nodes
    * 2) name should not conflict
    * 3) port should not conflict
    *
    * @param dataCollie dataCollie instance
    * @param serviceCollie serviceCollie instance
    * @param req cluster creation request
    * @param executionContext execution context
    * @tparam Cluster type of request
    * @return clusters that fitted the requires
    */
  private[this] def checkResourcesConflict[Cluster <: ClusterInfo: ClassTag](dataCollie: DataCollie, req: Cluster)(
    implicit executionContext: ExecutionContext,
    serviceCollie: ServiceCollie,
    store: DataStore,
    zookeeperCollie: ZookeeperCollie,
    brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie,
    streamCollie: StreamCollie,
    meterCache: MeterCache): Future[Unit] =
    // dataCollie.nodes(req.nodeNames) is used to check the existence of node names of request
    dataCollie
      .valuesByNames[Node](req.nodeNames)
      .flatMap(serviceCollie.images)
      // check the docker images
      .map { nodesImages =>
        nodesImages
          .filterNot(_._2.contains(req.imageName))
          .keys
          .map(_.name)
          .foreach(n => throw new IllegalArgumentException(s"$n doesn't have docker image:${req.imageName}"))
      }
      .flatMap { _ =>
        for {
          zks <- runningZookeeperClusters()
          bks <- runningBrokerClusters()
          wks <- runningWorkerClusters()
          ss <- runningStreamClusters()
        } yield zks ++ bks ++ wks ++ ss
      }
      // the non-running clusters should NOT block other clusters since they are NOT using the resources.
      .map(_.filter(_.state.nonEmpty))
      .flatMap { clusters =>
        def serviceName(cluster: ClusterInfo): String = cluster match {
          case _: ZookeeperClusterInfo => s"zookeeper cluster:${cluster.key}"
          case _: BrokerClusterInfo    => s"broker cluster:${cluster.key}"
          case _: WorkerClusterInfo    => s"worker cluster:${cluster.key}"
          case _: StreamClusterInfo    => s"stream cluster:${cluster.key}"
          case _                       => s"cluster:${cluster.key}"
        }
        // check name conflict
        clusters
          .filter(c => classTag[Cluster].runtimeClass.isInstance(c))
          .map(_.asInstanceOf[Cluster])
          .find(_.key == req.key)
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
        Future.unit
      }

  def runningZookeeperClusters()(implicit meterCache: MeterCache,
                                 store: DataStore,
                                 collie: ZookeeperCollie,
                                 executionContext: ExecutionContext): Future[Seq[ZookeeperClusterInfo]] =
    clusters[ZookeeperClusterInfo, ZookeeperClusterStatus]()

  def runningBrokerClusters()(implicit meterCache: MeterCache,
                              store: DataStore,
                              collie: BrokerCollie,
                              executionContext: ExecutionContext): Future[Seq[BrokerClusterInfo]] =
    clusters[BrokerClusterInfo, BrokerClusterStatus]()

  def runningWorkerClusters()(implicit meterCache: MeterCache,
                              store: DataStore,
                              collie: WorkerCollie,
                              executionContext: ExecutionContext): Future[Seq[WorkerClusterInfo]] =
    clusters[WorkerClusterInfo, WorkerClusterStatus]()

  def runningStreamClusters()(implicit meterCache: MeterCache,
                              store: DataStore,
                              collie: StreamCollie,
                              executionContext: ExecutionContext): Future[Seq[StreamClusterInfo]] =
    clusters[StreamClusterInfo, StreamClusterStatus]()

  /**
    * return the running clusters. All runtime information is up-to-date.
    */
  private[this] def clusters[Cluster <: ClusterInfo: ClassTag, Status <: ClusterStatus]()(
    implicit meterCache: MeterCache,
    store: DataStore,
    collie: Collie[Status],
    executionContext: ExecutionContext): Future[Seq[Cluster]] = store
    .values[Cluster]()
    .flatMap(clusters => Future.sequence(clusters.map(cluster => updateState[Cluster, Status](cluster, None))))
    .map(_.filter(_.state.nonEmpty))

  private[this] def updateState[Cluster <: ClusterInfo: ClassTag, Status <: ClusterStatus](cluster: Cluster,
                                                                                           metricsKey: Option[String])(
    implicit meterCache: MeterCache,
    collie: Collie[Status],
    executionContext: ExecutionContext): Future[Cluster] =
    collie
      .clusters()
      .map(
        _.keys
          .find(_.key == cluster.key)
          .map {
            // the casting is a bit ugly but it is safe since we use the type-binding to restrict the input collie
            case status: ZookeeperClusterStatus =>
              cluster.asInstanceOf[ZookeeperClusterInfo].update(status)
            case status: BrokerClusterStatus =>
              cluster.asInstanceOf[BrokerClusterInfo].update(status)
            case status: WorkerClusterStatus =>
              cluster.asInstanceOf[WorkerClusterInfo].update(status)
            case status: StreamClusterStatus =>
              cluster
                .asInstanceOf[StreamClusterInfo]
                .update(status)
                .copy(metrics =
                  Metrics(metricsKey.flatMap(key => meterCache.meters(cluster).get(key)).getOrElse(Seq.empty)))
          }
          .getOrElse {
            // no running cluster. It means no state and no dead nodes.
            // noted that the failed containers should still exist and we can "get" the cluster from collie.
            // the case of getting nothing from collie is only one that there is absolutely no containers and
            // we assume the cluster is NOT running.
            cluster match {
              case c: ZookeeperClusterInfo =>
                c.copy(
                  aliveNodes = Set.empty,
                  state = None,
                  error = None,
                )
              case c: BrokerClusterInfo =>
                c.copy(
                  aliveNodes = Set.empty,
                  state = None,
                  error = None,
                )
              case c: WorkerClusterInfo =>
                c.copy(
                  aliveNodes = Set.empty,
                  state = None,
                  error = None,
                )
              case c: StreamClusterInfo =>
                c.copy(
                  aliveNodes = Set.empty,
                  state = None,
                  error = None,
                  // the cluster is stooped (all containers are gone) so we don't need to fetch metrics.
                  metrics = Metrics.EMPTY
                )
            }
          }
          // the actual type is erased since the clone method returns the ClusterInfo type.
          // However, it is safe to case the type to the input type since all sub classes of ClusterInfo should work well.
          .asInstanceOf[Cluster]
      )
}
