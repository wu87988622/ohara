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
import com.island.ohara.agent.WorkerCollie
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

  private[this] def toRes(wkClusterName: String, id: Id, request: ConnectorConfigurationRequest) =
    ConnectorConfiguration(
      id = id,
      name = request.name,
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

  private[this] def verify(request: ConnectorConfigurationRequest): ConnectorConfigurationRequest = {
    if (request.schema.exists(_.order < 1))
      throw new IllegalArgumentException(s"invalid order from column:${request.schema.map(_.order)}")
    if (request.schema.map(_.order).toSet.size != request.schema.size)
      throw new IllegalArgumentException(s"duplicate order:${request.schema.map(_.order)}")
    request
  }

  private[route] def errorMessage(state: Option[ConnectorState]): Option[String] = state
    .filter(_ == ConnectorState.FAILED)
    .map(_ => "Some terrible things happen on your connector... Please use LOG APIs to see more details")
  private[this] def update(connectorConfig: ConnectorConfiguration,
                           workerClient: WorkerClient): ConnectorConfiguration = {
    val state = try if (workerClient.exist(connectorConfig.id))
      Some(workerClient.status(connectorConfig.id).connector.state)
    else None
    catch {
      case e: Throwable =>
        LOG.error(s"failed to fetch stats for $connectorConfig", e)
        None
    }
    val newOne = connectorConfig.copy(state = state, error = errorMessage(state))
    newOne
  }

  def apply(implicit store: Store, workerCollie: WorkerCollie): server.Route =
    // TODO: OHARA-1201 should remove the "sources" and "sinks" ... by chia
    pathPrefix(CONNECTORS_PREFIX_PATH | "sources" | "sinks") {
      RouteUtil.basicRoute2[ConnectorConfigurationRequest, ConnectorConfiguration](
        hookOfAdd = (targetCluster: TargetCluster, id: Id, request: ConnectorConfigurationRequest) =>
          CollieUtils.workerClient(targetCluster).map {
            case (cluster, _) =>
              toRes(cluster.name, id, verify(request))

        },
        hookOfUpdate = (id: Id, request: ConnectorConfigurationRequest, previous: ConnectorConfiguration) =>
          CollieUtils.workerClient(Some(previous.workerClusterName)).map {
            case (_, wkClient) =>
              if (wkClient.exist(id)) throw new IllegalArgumentException(s"$id is not stopped")
              else toRes(previous.workerClusterName, id, verify(request))
        },
        hookOfGet = (response: ConnectorConfiguration) =>
          CollieUtils.workerClient(Some(response.workerClusterName)).map {
            case (_, wkClient) =>
              update(response, wkClient)
        },
        hookOfList = (responses: Seq[ConnectorConfiguration]) =>
          Future.sequence(responses.map { response =>
            CollieUtils.workerClient(Some(response.workerClusterName)).map {
              case (_, wkClient) =>
                update(response, wkClient)
            }
          }),
        hookOfDelete = (response: ConnectorConfiguration) =>
          CollieUtils.workerClient(Some(response.workerClusterName)).map {
            case (_, wkClient) =>
              if (wkClient.exist(response.id)) wkClient.delete(response.id)
              response
        },
        hookBeforeDelete = (id: Id) =>
          store.value[ConnectorConfiguration](id).flatMap { config =>
            CollieUtils.workerClient(Some(config.workerClusterName)).map {
              case (_, wkClient) =>
                val lastConfig = update(config, wkClient)
                if (lastConfig.state.isEmpty) lastConfig.id
                else throw new IllegalArgumentException(s"Please stop connector:${lastConfig.id} first")
            }
        }
      )
    } ~
      // TODO: OHARA-1201 should remove the "sources" and "sinks" ... by chia
      pathPrefix((CONNECTORS_PREFIX_PATH | "sources" | "sinks") / Segment) { id =>
        path(START_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id).flatMap { connectorConfig =>
              CollieUtils
                .workerClient(Some(connectorConfig.workerClusterName))
                .flatMap {
                  case (cluster, wkClient) =>
                    store
                      .values[TopicInfo]
                      .map(
                        topics =>
                          (wkClient,
                           topics
                           // filter out nonexistent topics
                             .filter(t => connectorConfig.topics.contains(t.id))
                             // filter out topics having unmatched bk cluster
                             .filter(t => t.brokerClusterName == cluster.brokerClusterName)))
                }
                .map {
                  case (wkClient, topicInfos) =>
                    connectorConfig.topics.foreach(t =>
                      if (!topicInfos.exists(_.id == t))
                        throw new IllegalArgumentException(
                          s"$t is invalid. actual:${topicInfos.map(_.id).mkString(",")}"))
                    if (connectorConfig.topics.isEmpty) throw new IllegalArgumentException("topics are required")
                    if (wkClient.nonExist(connectorConfig.id)) {
                      wkClient
                        .connectorCreator()
                        .name(connectorConfig.id)
                        .disableConverter()
                        .connectorClass(connectorConfig.className)
                        .schema(connectorConfig.schema)
                        .configs(connectorConfig.configs)
                        .topics(connectorConfig.topics)
                        .numberOfTasks(connectorConfig.numberOfTasks)
                        .create()
                    }
                    update(connectorConfig, wkClient)
                }
            })(complete(_))
          }
        } ~ path(STOP_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).map {
                case (_, wkClient) =>
                  if (wkClient.exist(id)) wkClient.delete(id)
                  update(connectorConfig, wkClient)
              }
            })(complete(_))
          }
        } ~ path(PAUSE_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).map {
                case (_, wkClient) =>
                  if (wkClient.nonExist(id))
                    throw new IllegalArgumentException(
                      s"Connector is not running , using start command first . id:$id !!!")
                  wkClient.pause(id)
                  update(connectorConfig, wkClient)
              }
            })(complete(_))
          }
        } ~ path(RESUME_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).map {
                case (_, wkClient) =>
                  if (wkClient.nonExist(id))
                    throw new IllegalArgumentException(
                      s"Connector is not running , using start command first . id:$id !!!")
                  wkClient.resume(id)
                  update(connectorConfig, wkClient)
              }
            })(complete(_))
          }
        }
      }
}
