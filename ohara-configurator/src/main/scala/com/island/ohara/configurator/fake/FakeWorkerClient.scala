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

package com.island.ohara.configurator.fake

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.client.configurator.v0.WorkerApi.ConnectorDefinitions
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.client.kafka.WorkerClient.Validator
import com.island.ohara.client.kafka.WorkerJson.{
  ConnectorConfig,
  ConnectorCreationResponse,
  ConnectorInfo,
  ConnectorStatus,
  Plugin,
  TaskStatus
}
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.kafka.connector.json._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeWorkerClient extends WorkerClient {
  private[this] val cachedConnectors = new ConcurrentHashMap[String, Map[String, String]]()
  private[this] val cachedConnectorsState = new ConcurrentHashMap[String, ConnectorState]()

  override def connectorCreator(): WorkerClient.Creator = (executionContext,
                                                           name,
                                                           className,
                                                           topicNames,
                                                           numberOfTasks,
                                                           columns,
                                                           converterTypeOfKey,
                                                           converterTypeOfValue,
                                                           configs) => {
    if (cachedConnectors.contains(name))
      Future.failed(new IllegalStateException(s"the connector:$name exists!"))
    else {
      cachedConnectors.put(name, configs)
      cachedConnectorsState.put(name, ConnectorState.RUNNING)
      Future.successful(ConnectorCreationResponse(name, configs, Seq.empty))
    }
  }

  override def delete(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    try if (cachedConnectors.remove(name) == null)
      Future.failed(new IllegalStateException(s"the connector:$name doesn't exist!"))
    else Future.successful(())
    finally cachedConnectorsState.remove(name)
  import scala.collection.JavaConverters._
  // TODO; does this work? by chia
  override def plugins(implicit executionContext: ExecutionContext): Future[Seq[Plugin]] =
    Future.successful(cachedConnectors.keys.asScala.map(Plugin(_, "unknown", "unknown")).toSeq)
  override def activeConnectors(implicit executionContext: ExecutionContext): Future[Seq[String]] =
    Future.successful(cachedConnectors.keys.asScala.toSeq)
  override def connectionProps: String = "Unknown"
  override def status(name: String)(implicit executionContext: ExecutionContext): Future[ConnectorInfo] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else
      Future.successful(
        ConnectorInfo(name, ConnectorStatus(cachedConnectorsState.get(name), "fake id", None), Seq.empty))

  override def config(name: String)(implicit executionContext: ExecutionContext): Future[ConnectorConfig] = {
    val map = cachedConnectors.get(name)
    if (map == null) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(map.toJson.convertTo[ConnectorConfig])
  }

  override def taskStatus(name: String, id: Int)(implicit executionContext: ExecutionContext): Future[TaskStatus] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(TaskStatus(0, cachedConnectorsState.get(name), "worker_id", None))

  override def pause(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(cachedConnectorsState.put(name, ConnectorState.PAUSED))

  override def resume(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(cachedConnectorsState.put(name, ConnectorState.RUNNING))

  override def connectorValidator(): Validator =
    (executionContext, className, settings) =>
      Future.successful(SettingInfo.of(SettingDefinitions.DEFINITIONS_DEFAULT.asScala.map { definition =>
        Setting.of(
          definition,
          SettingValue.of(
            definition.key(),
            if (definition.key() == SettingDefinition.CONNECTOR_CLASS_DEFINITION.key()) className
            else if (definition.key() == SettingDefinition.TOPIC_NAMES_DEFINITION.key())
              settings.getOrElse(SettingDefinition.TOPIC_NAMES_DEFINITION.key(), null)
            else if (definition.key() == SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key())
              settings.getOrElse(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key(), null)
            else if (definition.key() == SettingDefinition.COLUMNS_DEFINITION.key())
              settings.getOrElse(SettingDefinition.COLUMNS_DEFINITION.key(), null)
            else if (definition.key() == SettingDefinition.KEY_CONVERTER_DEFINITION.key())
              settings.getOrElse(SettingDefinition.KEY_CONVERTER_DEFINITION.key(), null)
            else if (definition.key() == SettingDefinition.VALUE_CONVERTER_DEFINITION.key())
              settings.getOrElse(SettingDefinition.VALUE_CONVERTER_DEFINITION.key(), null)
            else if (definition.key() == SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())
              settings.getOrElse(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key(), null)
            else null,
            Collections.emptyList()
          )
        )
      }.asJava))

  override def connectors(implicit executionContext: ExecutionContext): Future[Seq[ConnectorDefinitions]] =
    Future.successful(cachedConnectors.keys.asScala.map(c => ConnectorDefinitions(c, Seq.empty)).toSeq)
}
