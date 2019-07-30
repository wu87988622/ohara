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
import com.island.ohara.agent.{NoSuchClusterException, WorkerCollie}
import com.island.ohara.client.configurator.v0.ConnectorApi._
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.RouteUtils._
import com.island.ohara.configurator.store.{DataStore, MeterCache}
import com.island.ohara.kafka.connector.json.{ConnectorKey, ObjectKey, SettingDefinition}
import com.typesafe.scalalogging.Logger
import spray.json.{JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}
private[configurator] object ConnectorRoute extends SprayJsonSupport {

  private[this] lazy val LOG = Logger(ConnectorRoute.getClass)

  private[this] def toRes(request: Creation) =
    ConnectorDescription(
      settings = request.settings ++ request.workerClusterName.fold[Map[String, JsValue]](Map.empty)(s =>
        Map(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key() -> JsString(s))),
      // we don't need to fetch connector from kafka since it has not existed in kafka.
      state = None,
      error = None,
      metrics = Metrics(Seq.empty),
      lastModified = CommonUtils.current()
    )

  private[this] def update(connectorConfig: ConnectorDescription,
                           workerClusterInfo: WorkerClusterInfo,
                           workerClient: WorkerClient)(implicit store: DataStore,
                                                       executionContext: ExecutionContext,
                                                       meterCache: MeterCache): Future[ConnectorDescription] =
    workerClient
      .statusOrNone(connectorConfig.key)
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
            error = trace
          )
      }
      .flatMap { data =>
        store.addIfPresent[ConnectorDescription](
          name = data.name,
          updater = (previous: ConnectorDescription) =>
            previous.copy(
              // Absent worker will be filled by workerClient, we should update it here
              settings = previous.settings ++ Map(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key() ->
                JsString(previous.workerClusterName.getOrElse(workerClusterInfo.name))),
              state = data.state,
              error = data.error,
              metrics = Metrics(
                meterCache.meters(workerClusterInfo).getOrElse(connectorConfig.key.connectorNameOnKafka, Seq.empty))
          )
        )
      }

  private[this] def hookOfGet(implicit store: DataStore,
                              workerCollie: WorkerCollie,
                              executionContext: ExecutionContext,
                              meterCache: MeterCache): HookOfGet[ConnectorDescription] =
    (connectorDescription: ConnectorDescription) =>
      CollieUtils.workerClient(connectorDescription.workerClusterName).flatMap {
        case (cluster, wkClient) =>
          update(connectorDescription, cluster, wkClient)
    }

  private[this] def hookOfList(implicit store: DataStore,
                               workerCollie: WorkerCollie,
                               executionContext: ExecutionContext,
                               meterCache: MeterCache): HookOfList[ConnectorDescription] =
    (connectorDescriptions: Seq[ConnectorDescription]) =>
      Future.sequence(connectorDescriptions.map { connectorDescription =>
        CollieUtils.workerClient(connectorDescription.workerClusterName).flatMap {
          case (cluster, wkClient) =>
            update(connectorDescription, cluster, wkClient)
        }
      })

  private[this] def hookOfCreation: HookOfCreation[Creation, ConnectorDescription] =
    (creation: Creation) => Future.successful(toRes(creation))

  private[this] def hookOfUpdate: HookOfUpdate[Creation, Update, ConnectorDescription] =
    (key: ObjectKey, update: Update, previous: Option[ConnectorDescription]) => {
      // merge the settings from previous one
      val newSettings = Creation(
        previous
          .map(old =>
            if (old.state.isDefined) throw new IllegalArgumentException(s"Connector:${key.name} is not stopped")
            else old.settings ++ update.settings)
          .getOrElse(update.settings) ++
          // Update request may not carry the name via payload so we copy the name from url to payload manually
          Map(SettingDefinition.CONNECTOR_NAME_DEFINITION.key() -> JsString(key.name),
              Data.GROUP_KEY -> JsString(key.group)))
      Future.successful(toRes(newSettings))
    }

  private[this] def hookBeforeDelete(implicit store: DataStore,
                                     workerCollie: WorkerCollie,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: ObjectKey) =>
    store
      .get[ConnectorDescription](key)
      .flatMap(_.map { connectorDescription =>
        CollieUtils
          .workerClient(connectorDescription.workerClusterName)
          .flatMap {
            case (_, wkClient) =>
              wkClient.exist(connectorDescription.key).flatMap {
                if (_) wkClient.delete(connectorDescription.key)
                else Future.unit
              }
          }
          .recoverWith {
            // Connector can't live without cluster...
            case _: NoSuchClusterException => Future.unit
          }
      }.getOrElse(Future.unit))

  private[this] def hookOfStart(implicit store: DataStore,
                                workerCollie: WorkerCollie,
                                meterCache: MeterCache,
                                executionContext: ExecutionContext): HookOfStart[ConnectorDescription] =
    (key: ObjectKey) =>
      store.value[ConnectorDescription](key).flatMap { connectorDesc =>
        CollieUtils
          .workerClient(connectorDesc.workerClusterName)
          .flatMap {
            case (cluster, wkClient) =>
              store
                .values[TopicInfo]()
                .map(topics =>
                  (cluster, wkClient, topics.filter(t => t.brokerClusterName == cluster.brokerClusterName)))
          }
          .flatMap {
            case (cluster, wkClient, topicInfos) =>
              connectorDesc.topicKeys.foreach(
                key =>
                  if (!topicInfos.exists(_.key == key))
                    throw new NoSuchElementException(
                      s"$key doesn't exist. actual:${topicInfos.map(_.name).mkString(",")}"))
              if (connectorDesc.topicKeys.isEmpty) throw new IllegalArgumentException("topics are required")
              wkClient.exist(connectorDesc.key).flatMap {
                if (_) update(connectorDesc, cluster, wkClient)
                else
                  wkClient
                    .connectorCreator()
                    .settings(connectorDesc.plain)
                    // always override the name
                    .connectorKey(connectorDesc.key)
                    .threadPool(executionContext)
                    .topicKeys(connectorDesc.topicKeys)
                    .create()
                    .map(res =>
                      connectorDesc.copy(state =
                        if (res.tasks.isEmpty) Some(ConnectorState.UNASSIGNED)
                        else Some(ConnectorState.RUNNING)))
                    .flatMap(update(_, cluster, wkClient))
              }
          }
    }

  private[this] def hookOfStop(implicit store: DataStore,
                               workerCollie: WorkerCollie,
                               meterCache: MeterCache,
                               executionContext: ExecutionContext): HookOfStop[ConnectorDescription] =
    (key: ObjectKey) =>
      store.value[ConnectorDescription](key).flatMap { connectorConfig =>
        CollieUtils.workerClient(connectorConfig.workerClusterName).flatMap {
          case (cluster, wkClient) =>
            wkClient.exist(ConnectorKey.of(key.group, key.name)).flatMap {
              if (_)
                wkClient
                  .delete(ConnectorKey.of(key.group, key.name))
                  .flatMap(_ => update(connectorConfig, cluster, wkClient))
              else update(connectorConfig, cluster, wkClient)
            }
        }
    }

  private[this] def hookOfPause(implicit store: DataStore,
                                workerCollie: WorkerCollie,
                                executionContext: ExecutionContext): HookOfPause[ConnectorDescription] =
    (key: ObjectKey) =>
      store.value[ConnectorDescription](key).flatMap { connectorConfig =>
        CollieUtils.workerClient(connectorConfig.workerClusterName).flatMap {
          case (_, wkClient) =>
            wkClient.status(ConnectorKey.of(key.group, key.name)).map(_.connector.state).flatMap {
              case ConnectorState.PAUSED =>
                Future.successful(connectorConfig.copy(state = Some(ConnectorState.PAUSED)))
              case _ =>
                wkClient
                  .pause(ConnectorKey.of(key.group, key.name))
                  .map(_ => connectorConfig.copy(state = Some(ConnectorState.PAUSED)))
            }
        }
    }

  private[this] def hookOfResume(implicit store: DataStore,
                                 workerCollie: WorkerCollie,
                                 executionContext: ExecutionContext): HookOfResume[ConnectorDescription] =
    (key: ObjectKey) =>
      store.value[ConnectorDescription](key).flatMap { connectorConfig =>
        CollieUtils.workerClient(connectorConfig.workerClusterName).flatMap {
          case (_, wkClient) =>
            wkClient.status(ConnectorKey.of(key.group, key.name)).map(_.connector.state).flatMap {
              case ConnectorState.PAUSED =>
                wkClient
                  .resume(ConnectorKey.of(key.group, key.name))
                  .map(_ => connectorConfig.copy(state = Some(ConnectorState.RUNNING)))
              case s => Future.successful(connectorConfig.copy(state = Some(s)))
            }
        }
    }

  def apply(implicit store: DataStore,
            workerCollie: WorkerCollie,
            executionContext: ExecutionContext,
            meterCache: MeterCache): server.Route =
    RouteUtils.route[Creation, Update, ConnectorDescription](
      root = CONNECTORS_PREFIX_PATH,
      enableGroup = false,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookBeforeDelete = hookBeforeDelete,
      hookOfStart = hookOfStart,
      hookOfStop = hookOfStop,
      hookOfPause = hookOfPause,
      hookOfResume = hookOfResume
    )
}
