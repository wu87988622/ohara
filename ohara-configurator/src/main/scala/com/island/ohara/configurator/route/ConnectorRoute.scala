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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.{NoSuchClusterException, WorkerCollie}
import com.island.ohara.client.configurator.v0.ConnectorApi._
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.RouteUtils._
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.island.ohara.kafka.connector.json.SettingDefinition
import com.typesafe.scalalogging.Logger
import spray.json.JsString

import scala.concurrent.{ExecutionContext, Future}
private[configurator] object ConnectorRoute extends SprayJsonSupport {
  private[this] lazy val LOG = Logger(ConnectorRoute.getClass)

  private[this] def toRes(wkClusterName: String, id: Id, request: ConnectorCreationRequest) = {
    if (request.workerClusterName.exists(_ != wkClusterName))
      throw new IllegalArgumentException(
        s"It is illegal to change worker cluster for connector. previous:$wkClusterName new:${request.workerClusterName.get}")
    ConnectorDescription(
      id = id,
      settings = request.settings ++ Map(
        SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key() -> JsString(wkClusterName)),
      // we don't need to fetch connector from kafka since it has not existed in kafka.
      state = None,
      error = None,
      metrics = Metrics(Seq.empty),
      lastModified = CommonUtils.current()
    )
  }

  private[this] def verify(request: ConnectorCreationRequest): ConnectorCreationRequest = {
    if (request.columns.exists(_.order < 1))
      throw new IllegalArgumentException(s"invalid order from column:${request.columns.map(_.order)}")
    if (request.columns.map(_.order).toSet.size != request.columns.size)
      throw new IllegalArgumentException(s"duplicate order:${request.columns.map(_.order)}")
    request
  }

  private[this] def update(connectorConfig: ConnectorDescription,
                           workerClusterInfo: WorkerClusterInfo,
                           workerClient: WorkerClient)(implicit executionContext: ExecutionContext,
                                                       meterCache: MeterCache): Future[ConnectorDescription] =
    workerClient
      .status(connectorConfig.id)
      .map(s => Some(s.connector.state) -> s.connector.trace)
      .recover {
        case e: Throwable =>
          val message = s"failed to fetch stats for $connectorConfig"
          LOG.error(message, e)
          None -> None
      }
      .map {
        case (state, trace) =>
          connectorConfig.copy(
            state = state,
            error = trace,
            metrics = Metrics(meterCache.meters(workerClusterInfo).getOrElse(connectorConfig.id, Seq.empty))
          )
      }

  def apply(implicit store: DataStore,
            workerCollie: WorkerCollie,
            executionContext: ExecutionContext,
            meterCache: MeterCache): server.Route =
    RouteUtils.basicRoute[ConnectorCreationRequest, ConnectorDescription](
      root = CONNECTORS_PREFIX_PATH,
      hookOfAdd = (targetCluster: TargetCluster, id: Id, request: ConnectorCreationRequest) =>
        // TODO: remove TargetCluster. see https://github.com/oharastream/ohara/issues/206
        CollieUtils
          .workerClient(if (request.workerClusterName.isEmpty) targetCluster else request.workerClusterName)
          .map {
            case (cluster, _) =>
              toRes(cluster.name, id, verify(request))

        },
      hookOfUpdate = (id: Id, request: ConnectorCreationRequest, previous: ConnectorDescription) =>
        CollieUtils.workerClient(Some(previous.workerClusterName)).flatMap {
          case (_, wkClient) =>
            wkClient.exist(id).map {
              if (_) throw new IllegalArgumentException(s"$id is not stopped")
              else toRes(previous.workerClusterName, id, verify(request))
            }
      },
      hookOfGet = (response: ConnectorDescription) =>
        CollieUtils.workerClient(Some(response.workerClusterName)).flatMap {
          case (cluster, wkClient) =>
            update(response, cluster, wkClient)
      },
      hookOfList = (responses: Seq[ConnectorDescription]) =>
        Future.sequence(responses.map { response =>
          CollieUtils.workerClient(Some(response.workerClusterName)).flatMap {
            case (cluster, wkClient) =>
              update(response, cluster, wkClient)
          }
        }),
      hookOfDelete = (response: ConnectorDescription) =>
        CollieUtils
          .workerClient(Some(response.workerClusterName))
          .flatMap {
            case (_, wkClient) =>
              wkClient.exist(response.id).flatMap {
                if (_)
                  wkClient
                    .delete(response.id)
                    .recover {
                      case e: Throwable =>
                        LOG.info(s"Failed to remove connector:${response.id}", e)
                    }
                    .map(_ => response)
                else Future.successful(response)
              }
          }
          .recover {
            case _: NoSuchClusterException => response
        }
    ) ~
      pathPrefix(CONNECTORS_PREFIX_PATH / Segment) { id =>
        path(START_COMMAND) {
          put {
            complete(store.value[ConnectorDescription](id).flatMap { connectorDesc =>
              CollieUtils
                .workerClient(Some(connectorDesc.workerClusterName))
                .flatMap {
                  case (cluster, wkClient) =>
                    store
                      .values[TopicInfo]
                      .map(topics =>
                        (cluster, wkClient, topics.filter(t => t.brokerClusterName == cluster.brokerClusterName)))
                }
                .flatMap {
                  case (cluster, wkClient, topicInfos) =>
                    connectorDesc.topicNames.foreach(t =>
                      if (!topicInfos.exists(_.id == t))
                        throw new NoSuchElementException(
                          s"$t doesn't exist. actual:${topicInfos.map(_.id).mkString(",")}"))
                    if (connectorDesc.topicNames.isEmpty) throw new IllegalArgumentException("topics are required")
                    wkClient.exist(connectorDesc.id).flatMap {
                      if (_) update(connectorDesc, cluster, wkClient)
                      else
                        wkClient
                          .connectorCreator()
                          .settings(connectorDesc.plain)
                          // always override the id
                          .id(connectorDesc.id)
                          .create
                          .flatMap(_ => Future.successful(connectorDesc.copy(state = Some(ConnectorState.RUNNING))))
                    }
                }
            })
          }
        } ~ path(STOP_COMMAND) {
          put {
            complete(store.value[ConnectorDescription](id).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).flatMap {
                case (cluster, wkClient) =>
                  wkClient.exist(id).flatMap {
                    if (_)
                      wkClient.delete(id).flatMap(_ => update(connectorConfig, cluster, wkClient))
                    else update(connectorConfig, cluster, wkClient)
                  }
              }
            })
          }
        } ~ path(PAUSE_COMMAND) {
          put {
            complete(store.value[ConnectorDescription](id).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).flatMap {
                case (_, wkClient) =>
                  wkClient.status(id).map(_.connector.state).flatMap {
                    case ConnectorState.PAUSED =>
                      Future.successful(connectorConfig.copy(state = Some(ConnectorState.PAUSED)))
                    case _ => wkClient.pause(id).map(_ => connectorConfig.copy(state = Some(ConnectorState.PAUSED)))
                  }
              }
            })
          }
        } ~ path(RESUME_COMMAND) {
          put {
            complete(store.value[ConnectorDescription](id).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).map {
                case (_, wkClient) =>
                  wkClient.status(id).map(_.connector.state).flatMap {
                    case ConnectorState.PAUSED =>
                      wkClient.resume(id).map(_ => connectorConfig.copy(state = Some(ConnectorState.RUNNING)))
                    case s => Future.successful(connectorConfig.copy(state = Some(s)))
                  }
              }
            })
          }
        }
      }
}
