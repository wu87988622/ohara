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
import com.island.ohara.client.configurator.v0.DataKey
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.RouteUtils.{
  PAUSE_COMMAND => _,
  RESUME_COMMAND => _,
  START_COMMAND => _,
  STOP_COMMAND => _,
  _
}
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.island.ohara.kafka.connector.json.SettingDefinition
import com.typesafe.scalalogging.Logger
import spray.json.JsString

import scala.concurrent.{ExecutionContext, Future}
private[configurator] object ConnectorRoute extends SprayJsonSupport {

  private[this] lazy val LOG = Logger(ConnectorRoute.getClass)

  private[this] def toRes(wkClusterName: String, request: Creation) =
    ConnectorDescription(
      settings = request.settings ++ Map(
        SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key() -> JsString(wkClusterName)),
      // we don't need to fetch connector from kafka since it has not existed in kafka.
      state = None,
      error = None,
      metrics = Metrics(Seq.empty),
      lastModified = CommonUtils.current()
    )

  private[this] def update(connectorConfig: ConnectorDescription,
                           workerClusterInfo: WorkerClusterInfo,
                           workerClient: WorkerClient)(implicit executionContext: ExecutionContext,
                                                       meterCache: MeterCache): Future[ConnectorDescription] =
    workerClient
      .statusOrNone(connectorConfig.name)
      .map(statusOption => statusOption.map(_.connector))
      .map(connectorOption =>
        connectorOption.map(connector => Some(connector.state) -> connector.trace).getOrElse(None -> None))
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
            metrics = Metrics(meterCache.meters(workerClusterInfo).getOrElse(connectorConfig.name, Seq.empty))
          )
      }

  private[this] def hookOfGet(implicit workerCollie: WorkerCollie,
                              executionContext: ExecutionContext,
                              meterCache: MeterCache): HookOfGet[ConnectorDescription] =
    (connectorDescription: ConnectorDescription) =>
      CollieUtils.workerClient(Some(connectorDescription.workerClusterName)).flatMap {
        case (cluster, wkClient) =>
          update(connectorDescription, cluster, wkClient)
    }

  private[this] def hookOfList(implicit workerCollie: WorkerCollie,
                               executionContext: ExecutionContext,
                               meterCache: MeterCache): HookOfList[ConnectorDescription] =
    (connectorDescriptions: Seq[ConnectorDescription]) =>
      Future.sequence(connectorDescriptions.map { connectorDescription =>
        CollieUtils.workerClient(Some(connectorDescription.workerClusterName)).flatMap {
          case (cluster, wkClient) =>
            update(connectorDescription, cluster, wkClient)
        }
      })

  private[this] def hookOfCreation(implicit workerCollie: WorkerCollie,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, ConnectorDescription] =
    (_: String, creation: Creation) =>
      CollieUtils.workerClient(creation.workerClusterName).map {
        case (cluster, _) => toRes(cluster.name, creation)
    }

  private[this] def hookOfUpdate(
    implicit clusterCollie: WorkerCollie,
    executionContext: ExecutionContext): HookOfUpdate[Creation, Update, ConnectorDescription] =
    (key: DataKey, update: Update, previous: Option[ConnectorDescription]) => {
      // merge the settings from previous one
      val newSettings = Creation(previous
        .map { previous =>
          if (update.workerClusterName.exists(_ != previous.workerClusterName))
            throw new IllegalArgumentException(
              s"It is illegal to change worker cluster for connector. previous:${previous.workerClusterName} new:${update.workerClusterName.get}")
          previous.settings
        }
        .map { previousSettings =>
          previousSettings ++
            update.settings
        }
        .getOrElse(update.settings) ++
        // Update request may not carry the name via payload so we copy the name from url to payload manually
        Map(SettingDefinition.CONNECTOR_NAME_DEFINITION.key() -> JsString(key.name)))

      CollieUtils.workerClient(newSettings.workerClusterName).flatMap {
        case (cluster, wkClient) =>
          wkClient.exist(key.name).map {
            if (_) throw new IllegalArgumentException(s"Connector:${key.name} is not stopped")
            else toRes(cluster.name, newSettings)
          }
      }
    }

  private[this] def hookBeforeDelete(implicit store: DataStore,
                                     workerCollie: WorkerCollie,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: DataKey) =>
    store
      .get[ConnectorDescription](key)
      .flatMap(_.map { connectorDescription =>
        CollieUtils
          .workerClient(Some(connectorDescription.workerClusterName))
          .flatMap {
            case (_, wkClient) =>
              wkClient.exist(connectorDescription.name).flatMap {
                if (_)
                  wkClient.delete(connectorDescription.name).map(_ => key)
                else Future.successful(key)
              }
          }
          .recover {
            // Connector can't live without cluster...
            case _: NoSuchClusterException => key
          }
      }.getOrElse(Future.successful(key)))

  def apply(implicit store: DataStore,
            workerCollie: WorkerCollie,
            executionContext: ExecutionContext,
            meterCache: MeterCache): server.Route =
    RouteUtils.basicRoute[Creation, Update, ConnectorDescription](
      root = CONNECTORS_PREFIX_PATH,
      enableGroup = false,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookBeforeDelete = hookBeforeDelete
    ) ~
      pathPrefix(CONNECTORS_PREFIX_PATH / Segment) { name =>
        path(START_COMMAND) {
          put {
            complete(store.value[ConnectorDescription](name).flatMap { connectorDesc =>
              CollieUtils
                .workerClient(Some(connectorDesc.workerClusterName))
                .flatMap {
                  case (cluster, wkClient) =>
                    store
                      .values[TopicInfo]()
                      .map(topics =>
                        (cluster, wkClient, topics.filter(t => t.brokerClusterName == cluster.brokerClusterName)))
                }
                .flatMap {
                  case (cluster, wkClient, topicInfos) =>
                    connectorDesc.topicNames.foreach(t =>
                      if (!topicInfos.exists(_.name == t))
                        throw new NoSuchElementException(
                          s"$t doesn't exist. actual:${topicInfos.map(_.name).mkString(",")}"))
                    if (connectorDesc.topicNames.isEmpty) throw new IllegalArgumentException("topics are required")
                    wkClient.exist(connectorDesc.name).flatMap {
                      if (_) update(connectorDesc, cluster, wkClient)
                      else
                        wkClient
                          .connectorCreator()
                          .settings(connectorDesc.plain)
                          // always override the name
                          .name(connectorDesc.name)
                          .threadPool(executionContext)
                          .create()
                          .flatMap(res =>
                            Future.successful(connectorDesc.copy(state =
                              if (res.tasks.isEmpty) Some(ConnectorState.UNASSIGNED)
                              else Some(ConnectorState.RUNNING))))
                    }
                }
            })
          }
        } ~ path(STOP_COMMAND) {
          put {
            complete(store.value[ConnectorDescription](name).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).flatMap {
                case (cluster, wkClient) =>
                  wkClient.exist(name).flatMap {
                    if (_)
                      wkClient.delete(name).flatMap(_ => update(connectorConfig, cluster, wkClient))
                    else update(connectorConfig, cluster, wkClient)
                  }
              }
            })
          }
        } ~ path(PAUSE_COMMAND) {
          put {
            complete(store.value[ConnectorDescription](name).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).flatMap {
                case (_, wkClient) =>
                  wkClient.status(name).map(_.connector.state).flatMap {
                    case ConnectorState.PAUSED =>
                      Future.successful(connectorConfig.copy(state = Some(ConnectorState.PAUSED)))
                    case _ => wkClient.pause(name).map(_ => connectorConfig.copy(state = Some(ConnectorState.PAUSED)))
                  }
              }
            })
          }
        } ~ path(RESUME_COMMAND) {
          put {
            complete(store.value[ConnectorDescription](name).flatMap { connectorConfig =>
              CollieUtils.workerClient(Some(connectorConfig.workerClusterName)).map {
                case (_, wkClient) =>
                  wkClient.status(name).map(_.connector.state).flatMap {
                    case ConnectorState.PAUSED =>
                      wkClient.resume(name).map(_ => connectorConfig.copy(state = Some(ConnectorState.RUNNING)))
                    case s => Future.successful(connectorConfig.copy(state = Some(s)))
                  }
              }
            })
          }
        }
      }
}
