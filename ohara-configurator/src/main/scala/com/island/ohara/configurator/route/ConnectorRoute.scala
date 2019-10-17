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
import com.island.ohara.agent.{BrokerCollie, NoSuchClusterException, WorkerCollie}
import com.island.ohara.client.configurator.v0.ConnectorApi._
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.common.setting.{ConnectorKey, ObjectKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook._
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
private[configurator] object ConnectorRoute extends SprayJsonSupport {

  private[this] lazy val LOG = Logger(ConnectorRoute.getClass)

  private[this] def creationToConnectorInfo(
    creation: Creation)(implicit store: DataStore, executionContext: ExecutionContext): Future[ConnectorInfo] =
    store.exist[WorkerClusterInfo](creation.workerClusterKey).map {
      if (_)
        ConnectorInfo(
          settings = creation.settings,
          // we don't need to fetch connector from kafka since it has not existed in kafka.
          status = None,
          tasksStatus = Seq.empty,
          metrics = Metrics.EMPTY,
          lastModified = CommonUtils.current()
        )
      else throw new IllegalArgumentException(s"worker cluster:${creation.workerClusterKey} does not exist")
    }

  private[this] def updateState(connectorDescription: ConnectorInfo)(implicit executionContext: ExecutionContext,
                                                                     workerCollie: WorkerCollie,
                                                                     store: DataStore,
                                                                     meterCache: MeterCache): Future[ConnectorInfo] =
    CollieUtils
      .workerClient(connectorDescription.workerClusterKey)
      .flatMap {
        case (cluster, workerClient) =>
          workerClient.status(connectorDescription.key).map { connectorInfoFromKafka =>
            connectorDescription.copy(
              status = Some(Status(
                state = State.forName(connectorInfoFromKafka.connector.state),
                error = connectorInfoFromKafka.connector.trace,
                nodeName = connectorInfoFromKafka.connector.workerHostname
              )),
              tasksStatus = connectorInfoFromKafka.tasks.map { taskStatus =>
                Status(
                  state = State.forName(taskStatus.state),
                  error = taskStatus.trace,
                  nodeName = taskStatus.workerHostname
                )
              },
              metrics =
                Metrics(meterCache.meters(cluster).getOrElse(connectorDescription.key.connectorNameOnKafka, Seq.empty))
            )
          }
      }
      .recover {
        case e: Throwable =>
          LOG.debug(s"failed to fetch stats for $connectorDescription", e)
          connectorDescription.copy(
            status = None,
            tasksStatus = Seq.empty,
            metrics = Metrics.EMPTY
          )
      }

  private[this] def hookOfGet(implicit workerCollie: WorkerCollie,
                              store: DataStore,
                              executionContext: ExecutionContext,
                              meterCache: MeterCache): HookOfGet[ConnectorInfo] = updateState

  private[this] def hookOfList(implicit workerCollie: WorkerCollie,
                               store: DataStore,
                               executionContext: ExecutionContext,
                               meterCache: MeterCache): HookOfList[ConnectorInfo] =
    (connectorDescriptions: Seq[ConnectorInfo]) => Future.sequence(connectorDescriptions.map(updateState))

  private[this] def hookOfCreation(implicit store: DataStore,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, ConnectorInfo] =
    creationToConnectorInfo(_)

  private[this] def HookOfUpdating(implicit workerCollie: WorkerCollie,
                                   store: DataStore,
                                   executionContext: ExecutionContext,
                                   meterCache: MeterCache): HookOfUpdating[Creation, Updating, ConnectorInfo] =
    (key: ObjectKey, updating: Updating, previousOption: Option[ConnectorInfo]) =>
      if (previousOption.isEmpty) creationToConnectorInfo(access.request.settings(updating.settings).creation)
      else {
        // we have to check whether connector is running on previous cluster
        CollieUtils.workerClient(previousOption.get.workerClusterKey).flatMap {
          case (cluster, client) =>
            client
              .activeConnectors()
              .map(_.contains(ConnectorKey.of(key.group(), key.name()).connectorNameOnKafka()))
              .flatMap {
                if (_)
                  throw new IllegalStateException(
                    "the connector is working now. Please stop it before updating the properties")
                else
                  // 1) fill the previous settings (if exists)
                  // 2) overwrite previous settings by updated settings
                  // 3) fill the ignored settings by creation
                  creationToConnectorInfo(
                    access.request
                      .settings(previousOption.map(_.settings).getOrElse(Map.empty))
                      .settings(updating.settings)
                      // the key is not in update's settings so we have to add it to settings
                      .name(key.name)
                      .group(key.group)
                      .creation)
              }
        }
    }

  private[this] def hookBeforeDelete(implicit store: DataStore,
                                     meterCache: MeterCache,
                                     workerCollie: WorkerCollie,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: ObjectKey) =>
    store
      .get[ConnectorInfo](key)
      .flatMap(_.map { connectorDescription =>
        CollieUtils
          .workerClient(connectorDescription.workerClusterKey)
          .flatMap {
            case (_, wkClient) =>
              wkClient.exist(connectorDescription.key).flatMap {
                if (_)
                  throw new IllegalStateException(
                    "the connector is working now. Please stop it before deleting the properties")
                else Future.unit
              }
          }
          .recoverWith {
            // Connector can't live without cluster...
            case _: NoSuchClusterException => Future.unit
          }
      }.getOrElse(Future.unit))

  private[this] def hookOfStart(implicit store: DataStore,
                                meterCache: MeterCache,
                                adminCleaner: AdminCleaner,
                                brokerCollie: BrokerCollie,
                                workerCollie: WorkerCollie,
                                executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _, _) =>
      store.value[ConnectorInfo](key).flatMap { connectorDesc =>
        CollieUtils
          .both(connectorDesc.workerClusterKey)
          .flatMap {
            case (brokerClusterInfo, topicAdmin, _, wkClient) =>
              topicAdmin.topics().map(topics => (brokerClusterInfo, wkClient, topics))
          }
          .flatMap {
            case (brokerClusterInfo, wkClient, topicInfos) =>
              connectorDesc.topicKeys.foreach { key =>
                if (!topicInfos.exists(_.name == key.topicNameOnKafka()))
                  throw new NoSuchElementException(
                    s"topic:$key is not running on broker cluster:${brokerClusterInfo.key}")
              }
              if (connectorDesc.topicKeys.isEmpty) throw new IllegalArgumentException("topics are required")
              wkClient.exist(connectorDesc.key).flatMap {
                if (_) Future.unit
                else
                  wkClient
                    .connectorCreator()
                    .settings(connectorDesc.plain)
                    // always override the name
                    .connectorKey(connectorDesc.key)
                    .threadPool(executionContext)
                    .topicKeys(connectorDesc.topicKeys)
                    .create()
                    .map(_ => Unit)
              }
          }
    }

  private[this] def hookOfStop(implicit store: DataStore,
                               meterCache: MeterCache,
                               workerCollie: WorkerCollie,
                               executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _, _) =>
      store.value[ConnectorInfo](key).flatMap { connectorDescription =>
        CollieUtils.workerClient(connectorDescription.workerClusterKey).flatMap {
          case (_, wkClient) =>
            wkClient.exist(connectorDescription.key).flatMap {
              if (_) wkClient.delete(connectorDescription.key).map(_ => Unit)
              else Future.unit
            }
        }
    }

  private[this] def hookOfPause(implicit store: DataStore,
                                meterCache: MeterCache,
                                workerCollie: WorkerCollie,
                                executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _, _) =>
      store.value[ConnectorInfo](key).flatMap { connectorDescription =>
        CollieUtils.workerClient(connectorDescription.workerClusterKey).flatMap {
          case (_, wkClient) =>
            wkClient.status(ConnectorKey.of(key.group, key.name)).map(_.connector.state).flatMap {
              case State.PAUSED.name => Future.unit
              case _ =>
                wkClient.pause(ConnectorKey.of(key.group, key.name)).map(_ => Unit)
            }
        }
    }

  private[this] def hookOfResume(implicit store: DataStore,
                                 meterCache: MeterCache,
                                 workerCollie: WorkerCollie,
                                 executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _, _) =>
      store.value[ConnectorInfo](key).flatMap { connectorDescription =>
        CollieUtils.workerClient(connectorDescription.workerClusterKey).flatMap {
          case (_, wkClient) =>
            wkClient.status(ConnectorKey.of(key.group, key.name)).map(_.connector.state).flatMap {
              case State.PAUSED.name =>
                wkClient.resume(ConnectorKey.of(key.group, key.name)).map(_ => Unit)
              case _ => Future.unit
            }
        }
    }

  def apply(implicit store: DataStore,
            adminCleaner: AdminCleaner,
            brokerCollie: BrokerCollie,
            workerCollie: WorkerCollie,
            executionContext: ExecutionContext,
            meterCache: MeterCache): server.Route =
    route[Creation, Updating, ConnectorInfo](
      root = CONNECTORS_PREFIX_PATH,
      hookOfCreation = hookOfCreation,
      HookOfUpdating = HookOfUpdating,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookBeforeDelete = hookBeforeDelete,
      hookOfStart = hookOfStart,
      hookOfStop = hookOfStop,
      hookOfPause = hookOfPause,
      hookOfResume = hookOfResume
    )
}
