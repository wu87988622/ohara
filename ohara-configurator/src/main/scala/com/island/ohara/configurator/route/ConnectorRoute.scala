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
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil._
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
private[configurator] object ConnectorRoute extends SprayJsonSupport {
  private[this] lazy val LOG = Logger(ConnectorRoute.getClass)

  private[this] def toRes(wkClusterName: String, id: Id, name: String, request: ConnectorCreationRequest) =
    ConnectorInfo(
      id = id,
      name = name,
      className = request.className,
      schema = request.schema,
      topics = request.topics,
      numberOfTasks = request.numberOfTasks,
      workerClusterName = wkClusterName,
      state = None,
      error = None,
      configs = request.configs,
      lastModified = CommonUtil.current()
    )

  private[this] def verify(request: ConnectorCreationRequest): ConnectorCreationRequest = {
    if (request.schema.exists(_.order < 1))
      throw new IllegalArgumentException(s"invalid order from column:${request.schema.map(_.order)}")
    if (request.schema.map(_.order).toSet.size != request.schema.size)
      throw new IllegalArgumentException(s"duplicate order:${request.schema.map(_.order)}")
    request
  }

  private[route] def errorMessage(state: Option[ConnectorState]): Option[String] = state
    .filter(_ == ConnectorState.FAILED)
    .map(_ => "Some terrible things happen on your connector... Please use LOG APIs to see more details")
  private[this] def update(connectorConfig: ConnectorInfo, workerClient: WorkerClient): Future[ConnectorInfo] =
    workerClient
      .status(connectorConfig.id)
      .map(s => Some(s.connector.state))
      .recover {
        case e: Throwable =>
          LOG.error(s"failed to fetch stats for $connectorConfig", e)
          None
      }
      .map { state =>
        connectorConfig.copy(state = state, error = errorMessage(state))
      }

  def apply(implicit store: Store, workerCollie: WorkerCollie): server.Route =
    RouteUtil.basicRoute[ConnectorCreationRequest, ConnectorInfo](
      root = CONNECTORS_PREFIX_PATH,
      hookOfAdd = (targetCluster: TargetCluster, id: Id, request: ConnectorCreationRequest) =>
        /**
          * TODO: remove TargetCluster. see https://github.com/oharastream/ohara/issues/206
          */
        CollieUtils
          .workerClient(if (request.workerClusterName.isEmpty) targetCluster else request.workerClusterName)
          .map {
            case (cluster, _) =>
              toRes(cluster.name,
                    id,
                    request.name.getOrElse(throw new NoSuchElementException("name is required")),
                    verify(request))

        },
      hookOfUpdate = (id: Id, request: ConnectorCreationRequest, previous: ConnectorInfo) =>
        CollieUtils.workerClient(Some(previous.workerClusterName)).flatMap {
          case (_, wkClient) =>
            wkClient.exist(id).map {
              if (_) throw new IllegalArgumentException(s"$id is not stopped")
              else toRes(previous.workerClusterName, id, request.name.getOrElse(previous.name), verify(request))
            }
      },
      hookOfGet = (response: ConnectorInfo) =>
        CollieUtils.workerClient(Some(response.workerClusterName)).flatMap {
          case (_, wkClient) =>
            update(response, wkClient)
      },
      hookOfList = (responses: Seq[ConnectorInfo]) =>
        Future.sequence(responses.map { response =>
          CollieUtils.workerClient(Some(response.workerClusterName)).flatMap {
            case (_, wkClient) =>
              update(response, wkClient)
          }
        }),
      hookOfDelete = (response: ConnectorInfo) =>
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
      pathPrefix((CONNECTORS_PREFIX_PATH) / Segment) { id =>
        path(START_COMMAND) {
          put {
            onSuccess(store.value[ConnectorInfo](id).flatMap { connectorConfig =>
              CollieUtils
                .workerClient(Some(connectorConfig.workerClusterName))
                .flatMap {
                  case (cluster, wkClient) =>
                    store
                      .values[TopicInfo]
                      .map(topics => (wkClient, topics.filter(t => t.brokerClusterName == cluster.brokerClusterName)))
                }
                .flatMap {
                  case (wkClient, topicInfos) =>
                    connectorConfig.topics.foreach(t =>
                      if (!topicInfos.exists(_.id == t))
                        throw new NoSuchElementException(
                          s"$t doesn't exist. actual:${topicInfos.map(_.id).mkString(",")}"))
                    if (connectorConfig.topics.isEmpty) throw new IllegalArgumentException("topics are required")
                    wkClient.exist(connectorConfig.id).flatMap {
                      if (_) update(connectorConfig, wkClient)
                      else
                        wkClient
                          .connectorCreator()
                          .name(connectorConfig.id)
                          .disableConverter()
                          .connectorClass(connectorConfig.className)
                          .schema(connectorConfig.schema)
                          .configs(connectorConfig.configs)
                          .topicNames(connectorConfig.topics)
                          .numberOfTasks(connectorConfig.numberOfTasks)
                          .create()
                          .flatMap(_ => Future.successful(connectorConfig.copy(state = Some(ConnectorState.RUNNING))))
                    }
                }
            })(complete(_))
          }
        } ~ path(STOP_COMMAND) {
          put {
            onSuccess(store.value[ConnectorInfo](id).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).flatMap {
                case (_, wkClient) =>
                  wkClient.exist(id).flatMap {
                    if (_) wkClient.delete(id).flatMap(_ => update(connectorConfig, wkClient))
                    else update(connectorConfig, wkClient)
                  }
              }
            })(complete(_))
          }
        } ~ path(PAUSE_COMMAND) {
          put {
            onSuccess(store.value[ConnectorInfo](id).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).flatMap {
                case (_, wkClient) =>
                  wkClient.status(id).map(_.connector.state).flatMap {
                    case ConnectorState.PAUSED =>
                      Future.successful(connectorConfig.copy(state = Some(ConnectorState.PAUSED)))
                    case _ => wkClient.pause(id).map(_ => connectorConfig.copy(state = Some(ConnectorState.PAUSED)))
                  }
              }
            })(complete(_))
          }
        } ~ path(RESUME_COMMAND) {
          put {
            onSuccess(store.value[ConnectorInfo](id).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).map {
                case (_, wkClient) =>
                  wkClient.status(id).map(_.connector.state).flatMap {
                    case ConnectorState.PAUSED =>
                      wkClient.resume(id).map(_ => connectorConfig.copy(state = Some(ConnectorState.RUNNING)))
                    case s => Future.successful(connectorConfig.copy(state = Some(s)))
                  }
              }
            })(complete(_))
          }
        }
      }
}
