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
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorDescription
import com.island.ohara.client.configurator.v0.MetricsApi._
import com.island.ohara.client.configurator.v0.PipelineApi._
import com.island.ohara.client.configurator.v0.StreamApi.{StreamAppDescription, StreamClusterInfo}
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.{Data, DataKey}
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.RouteUtils._
import com.island.ohara.configurator.store.{DataStore, MeterCache}

import scala.concurrent.{ExecutionContext, Future}
private[configurator] object PipelineRoute {

  private[this] def toAbstract(data: ConnectorDescription, clusterInfo: WorkerClusterInfo, workerClient: WorkerClient)(
    implicit executionContext: ExecutionContext,
    meterCache: MeterCache): Future[ObjectAbstract] =
    workerClient
      .connectorDefinition(data.className)
      .map(
        connectorDefinition =>
          ObjectAbstract(
            name = data.name,
            kind = connectorDefinition.kind,
            className = Some(data.className),
            state = None,
            error = None,
            // the group of counter is equal to connector's name (this is a part of kafka's core setting)
            // Hence, we filter the connectors having different "name" (we use name instead of name in creating connector)
            metrics = Metrics(meterCache.meters(clusterInfo).getOrElse(data.name, Seq.empty)),
            lastModified = data.lastModified,
            tags = data.tags
        )
      )
      .flatMap { obj =>
        workerClient.exist(obj.name).flatMap {
          if (_) workerClient.status(obj.name).map { connectorInfo =>
            obj.copy(
              state = Some(connectorInfo.connector.state.name),
              error = connectorInfo.connector.trace,
            )
          } else Future.successful(obj)
        }
      }

  private[this] def toAbstract(data: TopicInfo, clusterInfo: BrokerClusterInfo)(
    implicit meterCache: MeterCache): Future[ObjectAbstract] =
    Future.successful(
      ObjectAbstract(
        name = data.name,
        kind = data.kind,
        className = None,
        state = None,
        error = None,
        // noted we create a topic with name rather than name
        metrics = Metrics(meterCache.meters(clusterInfo).getOrElse(data.name, Seq.empty)),
        lastModified = data.lastModified,
        tags = data.tags
      ))

  private[this] def toAbstract(data: StreamAppDescription, clusterInfo: StreamClusterInfo): Future[ObjectAbstract] =
    Future.successful(
      ObjectAbstract(
        name = data.name,
        kind = data.kind,
        className = None,
        state = clusterInfo.state,
        error = None,
        metrics = Metrics(Seq.empty),
        lastModified = data.lastModified,
        tags = data.tags
      ))

  private[this] def toAbstract(data: Data, error: Option[String]): Future[ObjectAbstract] = Future.successful(
    ObjectAbstract(name = data.name,
                   kind = data.kind,
                   className = None,
                   state = None,
                   error = error,
                   metrics = Metrics(Seq.empty),
                   lastModified = data.lastModified,
                   tags = data.tags))

  private[this] def toAbstract(obj: Data)(implicit clusterCollie: ClusterCollie,
                                          executionContext: ExecutionContext,
                                          meterCache: MeterCache): Future[ObjectAbstract] = obj match {
    case data: ConnectorDescription =>
      clusterCollie.workerCollie.workerClient(data.workerClusterName).flatMap {
        case (workerClusterInfo, workerClient) => toAbstract(data, workerClusterInfo, workerClient)
      }
    case data: TopicInfo =>
      clusterCollie.brokerCollie.cluster(data.brokerClusterName).map(_._1).flatMap(toAbstract(data, _))
    case data: StreamAppDescription =>
      clusterCollie.streamCollie.cluster(data.name).map(_._1).flatMap(toAbstract(data, _))
    case _ => toAbstract(obj, None)
  }

  /**
    * collect the abstract for all objects in flow. This is a expensive operation since it invokes a bunch of threads
    * to retrieve the information from many remote nodes.
    * @param pipeline pipeline
    * @param store data store
    * @param clusterCollie cluster collie
    * @param executionContext thread pool
    * @param meterCache meter cache
    * @return updated pipeline
    */
  private[this] def updateObjects(pipeline: Pipeline)(implicit store: DataStore,
                                                      clusterCollie: ClusterCollie,
                                                      executionContext: ExecutionContext,
                                                      meterCache: MeterCache): Future[Pipeline] =
    Future
      .traverse(pipeline.flows.flatMap(flow => flow.to ++ Set(flow.from)))(store.raws)
      .map(_.flatten)
      .flatMap(Future.traverse(_) { obj =>
        toAbstract(obj).recoverWith {
          case e: Throwable => toAbstract(obj, Some(e.getMessage))
        }
      })
      .map(objects => pipeline.copy(objects = objects))

  private[this] def hookOfGet(implicit store: DataStore,
                              clusterCollie: ClusterCollie,
                              executionContext: ExecutionContext,
                              meterCache: MeterCache): HookOfGet[Pipeline] = updateObjects(_)

  private[this] def hookOfList(implicit store: DataStore,
                               clusterCollie: ClusterCollie,
                               executionContext: ExecutionContext,
                               meterCache: MeterCache): HookOfList[Pipeline] = Future.traverse(_)(updateObjects)

  private[this] def hookOfCreation(implicit store: DataStore,
                                   clusterCollie: ClusterCollie,
                                   executionContext: ExecutionContext,
                                   meterCache: MeterCache): HookOfCreation[Creation, Pipeline] =
    (_: String, creation: Creation) =>
      updateObjects(
        Pipeline(
          name = creation.name,
          flows = creation.flows,
          workerClusterName = creation.workerClusterName,
          objects = Seq.empty,
          lastModified = CommonUtils.current(),
          tags = creation.tags
        ))

  private[this] def hookOfUpdate(implicit store: DataStore,
                                 clusterCollie: ClusterCollie,
                                 executionContext: ExecutionContext,
                                 meterCache: MeterCache): HookOfUpdate[Creation, Update, Pipeline] =
    (key: DataKey, update: Update, previous: Option[Pipeline]) =>
      updateObjects(
        Pipeline(
          name = key.name,
          flows = update.flows.getOrElse(previous.map(_.flows).getOrElse(Seq.empty)),
          workerClusterName = update.workerClusterName.orElse(previous.flatMap(_.workerClusterName)),
          objects = previous.map(_.objects).getOrElse(Seq.empty),
          lastModified = CommonUtils.current(),
          tags = update.tags.getOrElse(previous.map(_.tags).getOrElse(Map.empty))
        ))

  private[this] def hookBeforeDelete: HookBeforeDelete = Future.successful(_)

  def apply(implicit store: DataStore,
            clusterCollie: ClusterCollie,
            executionContext: ExecutionContext,
            meterCache: MeterCache): server.Route =
    RouteUtils.basicRoute[Creation, Update, Pipeline](
      root = PIPELINES_PREFIX_PATH,
      enableGroup = true,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookBeforeDelete = hookBeforeDelete
    )
}
