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
import com.island.ohara.agent.{BrokerCollie, StreamCollie, WorkerCollie}
import com.island.ohara.client.configurator.Data
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorInfo
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.MetricsApi._
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.ObjectApi.ObjectInfo
import com.island.ohara.client.configurator.v0.PipelineApi._
import com.island.ohara.client.configurator.v0.ShabondiApi.ShabondiDescription
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0.{
  BrokerApi,
  ConnectorApi,
  FileInfoApi,
  NodeApi,
  ObjectApi,
  PipelineApi,
  ShabondiApi,
  StreamApi,
  TopicApi,
  WorkerApi,
  ZookeeperApi
}
import com.island.ohara.client.kafka.{TopicAdmin, WorkerClient}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook._
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
private[configurator] object PipelineRoute {
  private[this] lazy val LOG = Logger(PipelineRoute.getClass)

  private[this] def toAbstract(data: ConnectorInfo, clusterInfo: WorkerClusterInfo, workerClient: WorkerClient)(
    implicit executionContext: ExecutionContext,
    meterCache: MeterCache
  ): Future[ObjectAbstract] =
    workerClient
      .connectorDefinition(data.className)
      .map(
        classInfo =>
          ObjectAbstract(
            group = data.group,
            name = data.name,
            kind = classInfo.classType,
            className = Some(data.className),
            state = None,
            error = None,
            // the group of counter is equal to connector's name (this is a part of kafka's core setting)
            // Hence, we filter the connectors having different "name" (we use name instead of name in creating connector)
            metrics = Metrics(meterCache.meters(clusterInfo).getOrElse(data.key.connectorNameOnKafka, Seq.empty)),
            lastModified = data.lastModified,
            tags = data.tags
          )
      )
      .flatMap { obj =>
        workerClient
          .status(data.key)
          .map { connectorInfo =>
            obj.copy(
              state = Some(connectorInfo.connector.state),
              error = connectorInfo.connector.trace
            )
          }
          // we don't put this recovery in the final chain since we want to keep the definitions fetched from kafka
          .recover {
            case e: Throwable =>
              LOG.debug(s"failed to fetch obj for $data", e)
              obj
          }
      }

  private[this] def toAbstract(data: TopicInfo, clusterInfo: BrokerClusterInfo, topicAdmin: TopicAdmin)(
    implicit meterCache: MeterCache,
    executionContext: ExecutionContext
  ): Future[ObjectAbstract] = {
    topicAdmin
      .exist(data.key)
      .map(
        try if (_) Some(TopicApi.State.RUNNING) else None
        finally topicAdmin.close()
      )
      .map(_.map(_.name))
      .map(
        state =>
          ObjectAbstract(
            group = data.group,
            name = data.name,
            kind = data.kind,
            className = None,
            state = state,
            error = None,
            // noted we create a topic with name rather than name
            metrics = Metrics(meterCache.meters(clusterInfo).getOrElse(data.topicNameOnKafka, Seq.empty)),
            lastModified = data.lastModified,
            tags = data.tags
          )
      )
  }

  private[this] def toAbstract(obj: Data)(
    implicit dataStore: DataStore,
    brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie,
    streamCollie: StreamCollie,
    adminCleaner: AdminCleaner,
    executionContext: ExecutionContext,
    meterCache: MeterCache
  ): Future[ObjectAbstract] = obj match {
    case data: ConnectorInfo =>
      workerClient(data.workerClusterKey).flatMap {
        case (workerClusterInfo, workerClient) => toAbstract(data, workerClusterInfo, workerClient)
      }
    case data: TopicInfo =>
      topicAdmin(data.brokerClusterKey).flatMap {
        case (cluster, admin) => toAbstract(data, cluster, admin)
      }
    case data: StreamClusterInfo =>
      streamCollie
        .cluster(data.key)
        .map { status =>
          ObjectAbstract(
            group = data.group,
            name = data.name,
            kind = data.kind,
            className = Some(data.className),
            state = status.state,
            error = status.error,
            metrics = Metrics(meterCache.meters(data).getOrElse(StreamRoute.STREAM_GROUP, Seq.empty)),
            lastModified = data.lastModified,
            tags = data.tags
          )
        }
    case _ =>
      Future.successful(
        ObjectAbstract(
          group = obj.group,
          name = obj.name,
          kind = obj.kind,
          className = None,
          state = None,
          error = None,
          metrics = Metrics.EMPTY,
          lastModified = obj.lastModified,
          tags = obj.tags
        )
      )
  }

  /**
    * collect the abstract for all objects in endpoints. This is a expensive operation since it invokes a bunch of threads
    * to retrieve the information from many remote nodes.
    * @param pipeline pipeline
    * @param store data store
    * @param executionContext thread pool
    * @param meterCache meter cache
    * @return updated pipeline
    */
  private[this] def updateObjectsAndJarKeys(pipeline: Pipeline)(
    implicit brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie,
    streamCollie: StreamCollie,
    adminCleaner: AdminCleaner,
    store: DataStore,
    executionContext: ExecutionContext,
    meterCache: MeterCache
  ): Future[Pipeline] =
    Future
      .traverse(pipeline.endpoints.map(_.key))(store.raws)
      .map(_.flatten.toSet)
      .flatMap(Future.traverse(_) { obj =>
        toAbstract(obj).recover {
          case e: Throwable =>
            ObjectAbstract(
              group = obj.group,
              name = obj.name,
              kind = obj.kind,
              className = obj match {
                case d: ConnectorInfo     => Some(d.className)
                case d: StreamClusterInfo => Some(d.className)
                case _                    => None
              },
              state = None,
              error = Some(e.getMessage),
              metrics = Metrics.EMPTY,
              lastModified = obj.lastModified,
              tags = obj.tags
            )
        }
      })
      .map(objects => pipeline.copy(objects = objects))
      // update the jar keys used by the objects
      .flatMap { pipeline =>
        Future
          .traverse(pipeline.objects.map(obj => ObjectKey.of(obj.group, obj.name)))(store.raws)
          .map(_.flatten)
          .map(_.map {
            case connectorInfo: ConnectorInfo =>
              store
                .get[WorkerClusterInfo](connectorInfo.workerClusterKey)
                .map(_.map(w => w.sharedJarKeys ++ w.pluginKeys).getOrElse(Set.empty))
            case streamClusterInfo: StreamClusterInfo => Future.successful(Set(streamClusterInfo.jarKey))
            case _                                    => Future.successful(Set.empty[ObjectKey])
          })
          .flatMap(s => Future.sequence(s))
          .map(_.flatten)
          .map(jarKeys => pipeline.copy(jarKeys = jarKeys))
      }

  private[this] def hookOfGet(
    implicit brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie,
    streamCollie: StreamCollie,
    adminCleaner: AdminCleaner,
    store: DataStore,
    executionContext: ExecutionContext,
    meterCache: MeterCache
  ): HookOfGet[Pipeline] = updateObjectsAndJarKeys(_)

  private[this] def hookOfList(
    implicit brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie,
    streamCollie: StreamCollie,
    adminCleaner: AdminCleaner,
    store: DataStore,
    executionContext: ExecutionContext,
    meterCache: MeterCache
  ): HookOfList[Pipeline] =
    Future.traverse(_)(updateObjectsAndJarKeys)

  private[this] def hookOfCreation(
    implicit brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie,
    streamCollie: StreamCollie,
    adminCleaner: AdminCleaner,
    store: DataStore,
    executionContext: ExecutionContext,
    meterCache: MeterCache
  ): HookOfCreation[Creation, Pipeline] =
    (creation: Creation) =>
      updateObjectsAndJarKeys(
        Pipeline(
          group = creation.group,
          name = creation.name,
          endpoints = creation.endpoints,
          objects = Set.empty,
          jarKeys = Set.empty,
          lastModified = CommonUtils.current(),
          tags = creation.tags
        )
      )

  private[this] def hookOfUpdating(
    implicit brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie,
    streamCollie: StreamCollie,
    adminCleaner: AdminCleaner,
    store: DataStore,
    executionContext: ExecutionContext,
    meterCache: MeterCache
  ): HookOfUpdating[Updating, Pipeline] =
    (key: ObjectKey, update: Updating, previous: Option[Pipeline]) =>
      updateObjectsAndJarKeys(
        Pipeline(
          group = key.group,
          name = key.name,
          endpoints = update.endpoints.getOrElse(previous.map(_.endpoints).getOrElse(Set.empty)),
          objects = previous.map(_.objects).getOrElse(Set.empty),
          jarKeys = previous.map(_.jarKeys).getOrElse(Set.empty),
          lastModified = CommonUtils.current(),
          tags = update.tags.getOrElse(previous.map(_.tags).getOrElse(Map.empty))
        )
      )

  private[this] def hookBeforeDelete: HookBeforeDelete = _ => Future.unit

  private[this] def refreshEndpoints(
    pipeline: Pipeline
  )(implicit store: DataStore, executionContext: ExecutionContext): Future[Pipeline] =
    Future
      .traverse(pipeline.endpoints)(
        endpoint =>
          (endpoint.kind match {
            case TopicApi.KIND     => store.get[TopicInfo](endpoint.key)
            case ConnectorApi.KIND => store.get[ConnectorInfo](endpoint.key)
            case FileInfoApi.KIND  => store.get[FileInfo](endpoint.key)
            case NodeApi.KIND      => store.get[Node](endpoint.key)
            case ObjectApi.KIND    => store.get[ObjectInfo](endpoint.key)
            case PipelineApi.KIND  => store.get[Pipeline](endpoint.key)
            case ZookeeperApi.KIND => store.get[ZookeeperClusterInfo](endpoint.key)
            case BrokerApi.KIND    => store.get[BrokerClusterInfo](endpoint.key)
            case WorkerApi.KIND    => store.get[WorkerClusterInfo](endpoint.key)
            case StreamApi.KIND    => store.get[StreamClusterInfo](endpoint.key)
            case ShabondiApi.KIND  => store.get[ShabondiDescription](endpoint.key)
            case _                 => Future.successful(None)
          }).map {
            case None    => Seq.empty
            case Some(o) => Seq(o)
          }
      )
      .map(_.flatten)
      .map(
        existedData =>
          pipeline.copy(
            endpoints = pipeline.endpoints
              .filter(
                endpoint =>
                  existedData.exists(
                    data =>
                      data.group == endpoint.group
                        && data.name == endpoint.name
                        && endpoint.kind.contains(data.kind)
                  )
              )
          )
      )

  private[this] def hookOfRefresh(
    implicit store: DataStore,
    executionContext: ExecutionContext
  ): HookOfAction[Pipeline] =
    (pipeline: Pipeline, _, _) =>
      refreshEndpoints(pipeline)
        .flatMap(pipeline => store.add[Pipeline](pipeline))
        .map(_ => Unit)

  def apply(
    implicit brokerCollie: BrokerCollie,
    workerCollie: WorkerCollie,
    streamCollie: StreamCollie,
    adminCleaner: AdminCleaner,
    store: DataStore,
    executionContext: ExecutionContext,
    meterCache: MeterCache
  ): server.Route =
    RouteBuilder[Creation, Updating, Pipeline]()
      .root(PIPELINES_PREFIX_PATH)
      .hookOfCreation(hookOfCreation)
      .hookOfUpdating(hookOfUpdating)
      .hookOfGet(hookOfGet)
      .hookOfList(hookOfList)
      .hookBeforeDelete(hookBeforeDelete)
      .hookOfPutAction(REFRESH_COMMAND, hookOfRefresh)
      .build()
}
