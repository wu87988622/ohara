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
import com.island.ohara.agent.WorkerCollie
import com.island.ohara.client.configurator.v0.ConnectorApi._
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.common.setting.{ConnectorKey, ObjectKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.ObjectChecker.Condition.{RUNNING, STOPPED}
import com.island.ohara.configurator.route.ObjectChecker.ObjectCheckException
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

  private[this] def hookOfUpdating(implicit objectChecker: ObjectChecker,
                                   store: DataStore,
                                   executionContext: ExecutionContext): HookOfUpdating[Updating, ConnectorInfo] =
    (key: ObjectKey, updating: Updating, previousOption: Option[ConnectorInfo]) =>
      previousOption match {
        case None => creationToConnectorInfo(access.request.settings(updating.settings).creation)
        case Some(previous) =>
          objectChecker.checkList.connector(previous.key, STOPPED).check().flatMap { _ =>
            // 1) fill the previous settings (if exists)
            // 2) overwrite previous settings by updated settings
            // 3) fill the ignored settings by creation
            creationToConnectorInfo(
              access.request
                .settings(previous.settings)
                .settings(updating.settings)
                // the key is not in update's settings so we have to add it to settings
                .name(key.name)
                .group(key.group)
                .creation)
          }
    }

  private[this] def hookBeforeDelete(implicit objectChecker: ObjectChecker,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: ObjectKey) =>
    objectChecker.checkList
      .connector(ConnectorKey.of(key.group(), key.name()), STOPPED)
      .check()
      .recover {
        // the duplicate deletes are legal to ohara
        case e: ObjectCheckException if e.nonexistent.contains(key) => Unit
      }
      .map(_ => Unit)

  private[this] def hookOfStart(implicit store: DataStore,
                                objectChecker: ObjectChecker,
                                meterCache: MeterCache,
                                workerCollie: WorkerCollie,
                                executionContext: ExecutionContext): HookOfAction[ConnectorInfo] =
    (connectorInfo: ConnectorInfo, _, _) =>
      objectChecker.checkList
        .connector(connectorInfo.key)
        .topics(connectorInfo.topicKeys, RUNNING)
        .workerCluster(connectorInfo.workerClusterKey, RUNNING)
        .check()
        .map(_.connectorInfos.head)
        .flatMap {
          case (connectorInfo, condition) =>
            condition match {
              case RUNNING => Future.unit
              case STOPPED =>
                CollieUtils.workerClient(connectorInfo.workerClusterKey).map {
                  case (_, wkClient) =>
                    wkClient
                      .connectorCreator()
                      .settings(connectorInfo.plain)
                      // always override the name
                      .connectorKey(connectorInfo.key)
                      .threadPool(executionContext)
                      .topicKeys(connectorInfo.topicKeys)
                      .create()
                      .map(_ => Unit)
                }
            }
      }

  private[this] def hookOfStop(implicit store: DataStore,
                               objectChecker: ObjectChecker,
                               meterCache: MeterCache,
                               workerCollie: WorkerCollie,
                               executionContext: ExecutionContext): HookOfAction[ConnectorInfo] =
    (connectorInfo: ConnectorInfo, _, _) =>
      objectChecker.checkList.connector(connectorInfo.key).check().map(_.connectorInfos.head).flatMap {
        case (connectorInfo, condition) =>
          condition match {
            case STOPPED => Future.unit
            case RUNNING =>
              CollieUtils.workerClient(connectorInfo.workerClusterKey).map {
                case (_, wkClient) => wkClient.delete(connectorInfo.key)
              }
          }
    }

  private[this] def hookOfPause(implicit store: DataStore,
                                objectChecker: ObjectChecker,
                                meterCache: MeterCache,
                                workerCollie: WorkerCollie,
                                executionContext: ExecutionContext): HookOfAction[ConnectorInfo] =
    (connectorInfo: ConnectorInfo, _, _) =>
      objectChecker.checkList
        .connector(connectorInfo.key, RUNNING)
        .check()
        .flatMap(_ => CollieUtils.workerClient(connectorInfo.workerClusterKey))
        .map {
          case (_, wkClient) =>
            wkClient.status(connectorInfo.key).map(_.connector.state).flatMap {
              case State.PAUSED.name => Future.unit
              case _                 => wkClient.pause(connectorInfo.key).map(_ => Unit)
            }
      }

  private[this] def hookOfResume(implicit store: DataStore,
                                 objectChecker: ObjectChecker,
                                 meterCache: MeterCache,
                                 workerCollie: WorkerCollie,
                                 executionContext: ExecutionContext): HookOfAction[ConnectorInfo] =
    (connectorInfo: ConnectorInfo, _, _) =>
      objectChecker.checkList
        .connector(connectorInfo.key, RUNNING)
        .check()
        .flatMap(_ => CollieUtils.workerClient(connectorInfo.workerClusterKey))
        .map {
          case (_, wkClient) =>
            wkClient.status(connectorInfo.key).map(_.connector.state).flatMap {
              case State.RUNNING.name => Future.unit
              case _                  => wkClient.resume(connectorInfo.key).map(_ => Unit)
            }
      }

  def apply(implicit store: DataStore,
            objectChecker: ObjectChecker,
            workerCollie: WorkerCollie,
            executionContext: ExecutionContext,
            meterCache: MeterCache): server.Route =
    RouteBuilder[Creation, Updating, ConnectorInfo]()
      .root(CONNECTORS_PREFIX_PATH)
      .hookOfCreation(hookOfCreation)
      .hookOfUpdating(hookOfUpdating)
      .hookOfGet(hookOfGet)
      .hookOfList(hookOfList)
      .hookBeforeDelete(hookBeforeDelete)
      .hookOfPutAction(START_COMMAND, hookOfStart)
      .hookOfPutAction(STOP_COMMAND, hookOfStop)
      .hookOfPutAction(PAUSE_COMMAND, hookOfPause)
      .hookOfPutAction(RESUME_COMMAND, hookOfResume)
      .build()
}
