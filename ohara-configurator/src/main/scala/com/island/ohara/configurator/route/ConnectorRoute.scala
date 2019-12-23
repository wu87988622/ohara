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
import com.island.ohara.client.configurator.v0.ConnectorApi
import com.island.ohara.client.configurator.v0.ConnectorApi._
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.common.setting.{ConnectorKey, ObjectKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.ObjectChecker.Condition.{RUNNING, STOPPED}
import com.island.ohara.configurator.route.ObjectChecker.ObjectCheckException
import com.island.ohara.configurator.route.hook._
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.island.ohara.kafka.connector.json.ConnectorDefUtils
import com.typesafe.scalalogging.Logger
import spray.json.DeserializationException

import scala.concurrent.{ExecutionContext, Future}
private[configurator] object ConnectorRoute {
  private[this] lazy val LOG = Logger(ConnectorRoute.getClass)

  private[this] def creationToConnectorInfo(
    creation: Creation
  )(
    implicit objectChecker: ObjectChecker,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext
  ): Future[ConnectorInfo] =
    objectChecker.checkList
      .workerCluster(creation.workerClusterKey)
      .topics(creation.topicKeys)
      .check()
      .map(_.workerClusterInfos.head)
      // if the worker cluster is running, we try to fetch definitions and add them to the settings for the ignored key-values.
      .flatMap {
        case (workerClusterInfo, condition) =>
          condition match {
            case RUNNING =>
              try workerCollie.workerClient(workerClusterInfo).flatMap(_.connectorDefinitions())
              catch {
                case e: Throwable =>
                  LOG.error(s"failed to get definitions from worker cluster:${workerClusterInfo.key}", e)
                  Future.successful(Seq.empty)
              }
            case STOPPED => Future.successful(Seq.empty)
          }
      }
      .map(_.find(_.className == creation.className).map(_.settingDefinitions).getOrElse(Seq.empty))
      .map { definitions =>
        ConnectorInfo(
          settings = extractDefaultValues(definitions) ++ creation.settings,
          // we don't need to fetch connector from kafka since it has not existed in kafka.
          status = None,
          tasksStatus = Seq.empty,
          metrics = Metrics.EMPTY,
          lastModified = CommonUtils.current()
        )
      }

  private[this] def updateState(connectorInfo: ConnectorInfo)(
    implicit executionContext: ExecutionContext,
    workerCollie: WorkerCollie,
    objectChecker: ObjectChecker,
    meterCache: MeterCache
  ): Future[ConnectorInfo] =
    objectChecker.checkList
      .workerCluster(connectorInfo.workerClusterKey)
      .check()
      .map(_.workerClusterInfos.head)
      .flatMap {
        case (workerClusterInfo, condition) =>
          condition match {
            case STOPPED =>
              Future.successful(
                connectorInfo.copy(
                  status = None,
                  tasksStatus = Seq.empty,
                  metrics = Metrics.EMPTY
                )
              )
            case RUNNING =>
              workerCollie.workerClient(workerClusterInfo).flatMap { workerClient =>
                workerClient.status(connectorInfo.key).map { connectorInfoFromKafka =>
                  connectorInfo.copy(
                    status = Some(
                      Status(
                        state = State.forName(connectorInfoFromKafka.connector.state),
                        error = connectorInfoFromKafka.connector.trace,
                        nodeName = connectorInfoFromKafka.connector.workerHostname
                      )
                    ),
                    tasksStatus = connectorInfoFromKafka.tasks.map { taskStatus =>
                      Status(
                        state = State.forName(taskStatus.state),
                        error = taskStatus.trace,
                        nodeName = taskStatus.workerHostname
                      )
                    },
                    metrics = Metrics(
                      meterCache.meters(workerClusterInfo).getOrElse(connectorInfo.key.connectorNameOnKafka, Seq.empty)
                    )
                  )
                }
              }
          }
      }
      .recover {
        case e: Throwable =>
          LOG.debug(s"failed to fetch stats for $connectorInfo", e)
          connectorInfo.copy(
            status = None,
            tasksStatus = Seq.empty,
            metrics = Metrics.EMPTY
          )
      }

  private[this] def hookOfGet(
    implicit workerCollie: WorkerCollie,
    objectChecker: ObjectChecker,
    executionContext: ExecutionContext,
    meterCache: MeterCache
  ): HookOfGet[ConnectorInfo] = updateState

  private[this] def hookOfList(
    implicit workerCollie: WorkerCollie,
    objectChecker: ObjectChecker,
    executionContext: ExecutionContext,
    meterCache: MeterCache
  ): HookOfList[ConnectorInfo] =
    (connectorDescriptions: Seq[ConnectorInfo]) => Future.sequence(connectorDescriptions.map(updateState))

  private[this] def hookOfCreation(
    implicit objectChecker: ObjectChecker,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext
  ): HookOfCreation[Creation, ConnectorInfo] =
    creationToConnectorInfo(_)

  private[this] def hookOfUpdating(
    implicit objectChecker: ObjectChecker,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext
  ): HookOfUpdating[Updating, ConnectorInfo] =
    (key: ObjectKey, updating: Updating, previousOption: Option[ConnectorInfo]) =>
      previousOption match {
        case None =>
          creationToConnectorInfo(
            access.request
              .settings(updating.settings)
              // the key is not in update's settings so we have to add it to settings
              .name(key.name)
              .group(key.group)
              .creation
          )
        case Some(previous) =>
          objectChecker.checkList.connector(previous.key, STOPPED).check().flatMap { _ =>
            // 1) fill the previous settings (if exists)
            // 2) overwrite previous settings by updated settings
            // 3) fill the ignored settings by creation
            creationToConnectorInfo(
              access.request
                .settings(previous.settings)
                .settings(keepEditableFields(updating.settings, ConnectorApi.DEFINITIONS))
                // the key is not in update's settings so we have to add it to settings
                .name(key.name)
                .group(key.group)
                .creation
            )
          }
      }

  private[this] def hookBeforeDelete(
    implicit objectChecker: ObjectChecker,
    executionContext: ExecutionContext
  ): HookBeforeDelete =
    (key: ObjectKey) =>
      objectChecker.checkList
        .connector(ConnectorKey.of(key.group(), key.name()), STOPPED)
        .check()
        .recover {
          // the duplicate deletes are legal to ohara
          case e: ObjectCheckException if e.nonexistent.contains(key) => Unit
        }
        .map(_ => Unit)

  private[this] def hookOfStart(
    implicit objectChecker: ObjectChecker,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext
  ): HookOfAction[ConnectorInfo] =
    (connectorInfo: ConnectorInfo, _, _) =>
      objectChecker.checkList
        .connector(connectorInfo.key)
        .topics(
          // our UI needs to create a connector without topics so the connector info may has no topics...
          if (connectorInfo.topicKeys.isEmpty)
            throw DeserializationException(
              s"${ConnectorDefUtils.TOPIC_KEYS_DEFINITION.key()} can't be empty",
              fieldNames = List(ConnectorDefUtils.TOPIC_KEYS_DEFINITION.key())
            )
          else connectorInfo.topicKeys,
          RUNNING
        )
        .workerCluster(connectorInfo.workerClusterKey, RUNNING)
        .check()
        .map(report => (report.connectorInfos.head._2, report.runningWorkers.head, report.runningTopics))
        .flatMap {
          case (condition, workerClusterInfo, topicInfos) =>
            condition match {
              case RUNNING => Future.unit
              case STOPPED =>
                topicInfos.filter(_.brokerClusterKey != workerClusterInfo.brokerClusterKey).foreach { topicInfo =>
                  throw new IllegalArgumentException(
                    s"Connector app counts on broker cluster:${workerClusterInfo.brokerClusterKey} " +
                      s"but topic:${topicInfo.key} is on another broker cluster:${topicInfo.brokerClusterKey}"
                  )
                }
                workerCollie.workerClient(workerClusterInfo).flatMap {
                  _.connectorCreator()
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

  private[this] def hookOfStop(
    implicit objectChecker: ObjectChecker,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext
  ): HookOfAction[ConnectorInfo] =
    (connectorInfo: ConnectorInfo, _, _) =>
      objectChecker.checkList.connector(connectorInfo.key).check().map(_.connectorInfos.head._2).flatMap {
        case STOPPED => Future.unit
        case RUNNING =>
          objectChecker.checkList
            .workerCluster(connectorInfo.workerClusterKey, RUNNING)
            .check()
            .map(_.runningWorkers.head)
            .flatMap(workerCollie.workerClient)
            .flatMap(workerClient => workerClient.delete(connectorInfo.key))
      }

  private[this] def hookOfPause(
    implicit objectChecker: ObjectChecker,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext
  ): HookOfAction[ConnectorInfo] =
    (connectorInfo: ConnectorInfo, _, _) =>
      objectChecker.checkList
        .workerCluster(connectorInfo.workerClusterKey, RUNNING)
        .connector(connectorInfo.key, RUNNING)
        .check()
        .map(_.runningWorkers.head)
        .flatMap(workerCollie.workerClient)
        .map { wkClient =>
          wkClient.status(connectorInfo.key).map(_.connector.state).flatMap {
            case State.PAUSED.name => Future.unit
            case _                 => wkClient.pause(connectorInfo.key).map(_ => Unit)
          }
        }

  private[this] def hookOfResume(
    implicit objectChecker: ObjectChecker,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext
  ): HookOfAction[ConnectorInfo] =
    (connectorInfo: ConnectorInfo, _, _) =>
      objectChecker.checkList
        .workerCluster(connectorInfo.workerClusterKey, RUNNING)
        .connector(connectorInfo.key, RUNNING)
        .check()
        .map(_.runningWorkers.head)
        .flatMap(workerCollie.workerClient)
        .map { wkClient =>
          wkClient.status(connectorInfo.key).map(_.connector.state).flatMap {
            case State.RUNNING.name => Future.unit
            case _                  => wkClient.resume(connectorInfo.key).map(_ => Unit)
          }
        }

  def apply(
    implicit store: DataStore,
    objectChecker: ObjectChecker,
    workerCollie: WorkerCollie,
    executionContext: ExecutionContext,
    meterCache: MeterCache
  ): server.Route =
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
