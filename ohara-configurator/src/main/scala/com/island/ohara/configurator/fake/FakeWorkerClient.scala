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
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorState
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
import com.island.ohara.kafka.connector.json._
import com.island.ohara.kafka.connector.{RowSinkConnector, RowSourceConnector}
import org.apache.kafka.connect.runtime.AbstractHerder
import org.apache.kafka.connect.sink.SinkConnector
import org.apache.kafka.connect.source.SourceConnector
import org.reflections.Reflections
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeWorkerClient extends WorkerClient {
  private[this] val cachedConnectors = new ConcurrentHashMap[String, Map[String, String]]()
  private[this] val cachedConnectorsState = new ConcurrentHashMap[String, ConnectorState]()

  override def connectorCreator(): WorkerClient.Creator = (_, creation) => {
    if (cachedConnectors.contains(creation.id()))
      Future.failed(new IllegalStateException(s"the connector:${creation.id()} exists!"))
    else {
      import scala.collection.JavaConverters._
      cachedConnectors.put(creation.id(), creation.configs().asScala.toMap)
      cachedConnectorsState.put(creation.id(), ConnectorState.RUNNING)
      Future.successful(ConnectorCreationResponse(creation.id(), creation.configs().asScala.toMap, Seq.empty))
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
    if (!cachedConnectors.containsKey(name))
      Future.failed(new IllegalArgumentException(s"connector:$name doesn't exist"))
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
    // TODO: this implementation use kafka private APIs ... by chia
    (executionContext, validation) =>
      Future {
        val instance = Class.forName(validation.className).newInstance()
        val (connectorType, configDef, values) = instance match {
          case c: SourceConnector => ("source", c.config(), c.config().validate(validation.settings))
          case c: SinkConnector   => ("sink", c.config(), c.config().validate(validation.settings))
          case _                  => throw new IllegalArgumentException(s"who are you ${validation.className} ???")
        }
        SettingInfo.of(
          AbstractHerder.generateResult(connectorType, configDef.configKeys(), values, Collections.emptyList()))
      }(executionContext)

  override def connectors(implicit executionContext: ExecutionContext): Future[Seq[ConnectorDefinitions]] =
    Future.successful {
      val reflections = new Reflections()
      (reflections.getSubTypesOf(classOf[RowSourceConnector]).asScala.map { clz =>
        ConnectorDefinitions(
          className = clz.getName,
          definitions = clz.newInstance().definitions().asScala
        )
      } ++ reflections.getSubTypesOf(classOf[RowSinkConnector]).asScala.map { clz =>
        ConnectorDefinitions(
          className = clz.getName,
          definitions = clz.newInstance().definitions().asScala
        )
      }).toSeq
    }
}
