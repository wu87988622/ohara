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
import com.island.ohara.agent.WorkerCollie
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfiguration
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.client.configurator.v0.PipelineApi._
import com.island.ohara.client.configurator.v0.StreamApi.StreamApp
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil.{Id, TargetCluster}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
private[configurator] object PipelineRoute {

  private[this] def toRes(targetCluster: TargetCluster, id: String, request: PipelineCreationRequest)(
    implicit store: Store,
    workerCollie: WorkerCollie): Future[Pipeline] = CollieUtils.workerClient(targetCluster).flatMap {
    case (cluster, client) =>
      abstracts(cluster, request, client).map { abs =>
        Pipeline(id, request.name, request.rules, abs, cluster.name, CommonUtil.current())
      }
  }

  private[this] def abstracts(cluster: WorkerClusterInfo, request: PipelineCreationRequest, workerClient: WorkerClient)(
    implicit store: Store): Future[List[ObjectAbstract]] =
    verifyRules(cluster, request)
      .flatMap { rules =>
        Future.sequence(
          rules
            .flatMap {
              case (k, v) => Seq(k, v)
            }
            .filterNot(_ == UNKNOWN)
            .toSet
            .map(id => store.value[Data](id)))
      }
      .map {
        _.map {
          case data: ConnectorConfiguration =>
            ObjectAbstract(data.id,
                           data.name,
                           data.kind,
                           if (workerClient.exist(data.id)) Some(workerClient.status(data.id).connector.state)
                           else None,
                           data.lastModified)
          case data => ObjectAbstract(data.id, data.name, data.kind, None, data.lastModified)
        }.toList // NOTED: we have to return a "serializable" list!!!
      }

  /**
    * we should accept following data type only
    * [ConnectorConfiguration, TopicInfo, StreamApp]
    */
  private[this] def verifyRules(cluster: WorkerClusterInfo, request: PipelineCreationRequest)(
    implicit store: Store): Future[Map[String, String]] = {
    def verify(id: String): Future[String] = if (id != UNKNOWN) {
      store.raw(id).map {
        case d: ConnectorConfiguration =>
          if (d.workerClusterName != cluster.name)
            throw new IllegalArgumentException(
              s"connector:${d.name} is run by ${d.workerClusterName} so it can't be placed at pipeline:${request.name} which is placed at worker cluster:${cluster.name}")
          else id
        case d: TopicInfo =>
          if (d.brokerClusterName != cluster.brokerClusterName)
            throw new IllegalArgumentException(
              s"topic:${d.name} is run by ${d.brokerClusterName} so it can't be placed at pipeline:${request.name} which is placed at broker cluster:${cluster.brokerClusterName}")
          else id
        case _: StreamApp => id
        case raw          => throw new IllegalArgumentException(s"${raw.getClass.getName} can't be placed at pipeline")
      }
    } else Future.successful(id)

    Future
      .sequence(request.rules.map {
        case (k, v) =>
          if (k == v) Future.failed(new IllegalArgumentException(s"the from:$k can't be equals to to:$v"))
          else
            verify(k).flatMap { k =>
              verify(v).map(v => (k, v))
            }
      })
      .map(_.toMap)
  }

  private[this] def update(pipeline: Pipeline)(implicit store: Store, workerCollie: WorkerCollie): Future[Pipeline] =
    toRes(Some(pipeline.workerClusterName),
          pipeline.id,
          PipelineCreationRequest(
            name = pipeline.name,
            rules = pipeline.rules
          ))

  def apply(implicit store: Store, workerCollie: WorkerCollie): server.Route =
    RouteUtil.basicRoute[PipelineCreationRequest, Pipeline](
      root = PIPELINES_PREFIX_PATH,
      hookOfAdd = (t: TargetCluster, id: Id, request: PipelineCreationRequest) => toRes(t, id, request),
      hookOfUpdate = (id: Id, request: PipelineCreationRequest, previous: Pipeline) =>
        toRes(Some(previous.workerClusterName), id, request),
      hookOfGet = (response: Pipeline) => update(response),
      hookOfList = (responses: Seq[Pipeline]) => Future.sequence(responses.map(update)),
      hookOfDelete = (response: Pipeline) => Future.successful(response)
    )
}
