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

import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.client.kafka.WorkerClient.Validator
import com.island.ohara.client.kafka.WorkerJson.{
  ConfigValidationResponse,
  ConnectorConfig,
  ConnectorInfo,
  ConnectorStatus,
  ConnectorCreationResponse,
  Plugin,
  TaskStatus
}
import com.island.ohara.common.data.ConnectorState

import scala.concurrent.Future
import spray.json.DefaultJsonProtocol._
import spray.json._

private[configurator] class FakeWorkerClient extends WorkerClient {
  private[this] val cachedConnectors = new ConcurrentHashMap[String, Map[String, String]]()
  private[this] val cachedConnectorsState = new ConcurrentHashMap[String, ConnectorState]()

  override def connectorCreator(): WorkerClient.Creator = request => {
    if (cachedConnectors.contains(request.name))
      Future.failed(new IllegalStateException(s"the connector:${request.name} exists!"))
    else {
      cachedConnectors.put(request.name, request.configs)
      cachedConnectorsState.put(request.name, ConnectorState.RUNNING)
      Future.successful(ConnectorCreationResponse(request.name, request.configs, Seq.empty))
    }
  }

  override def delete(name: String): Future[Unit] = try if (cachedConnectors.remove(name) == null)
    Future.failed(new IllegalStateException(s"the connector:$name doesn't exist!"))
  else Future.successful(())
  finally cachedConnectorsState.remove(name)
  import scala.collection.JavaConverters._
  // TODO; does this work? by chia
  override def plugins(): Future[Seq[Plugin]] =
    Future.successful(cachedConnectors.keys.asScala.map(Plugin(_, "unknown", "unknown")).toSeq)
  override def activeConnectors(): Future[Seq[String]] = Future.successful(cachedConnectors.keys.asScala.toSeq)
  override def connectionProps: String = "Unknown"
  override def status(name: String): Future[ConnectorInfo] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else
      Future.successful(
        ConnectorInfo(name, ConnectorStatus(cachedConnectorsState.get(name), "fake id", None), Seq.empty))

  override def config(name: String): Future[ConnectorConfig] = {
    val map = cachedConnectors.get(name)
    if (map == null) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(map.toJson.convertTo[ConnectorConfig])
  }

  override def taskStatus(name: String, id: Int): Future[TaskStatus] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(TaskStatus(0, cachedConnectorsState.get(name), "worker_id", None))

  override def pause(name: String): Future[Unit] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(cachedConnectorsState.put(name, ConnectorState.PAUSED))

  override def resume(name: String): Future[Unit] =
    if (!cachedConnectors.containsKey(name)) Future.failed(new IllegalArgumentException(s"$name doesn't exist"))
    else Future.successful(cachedConnectorsState.put(name, ConnectorState.RUNNING))

  override def connectorValidator(): Validator = (className, _) =>
    Future.successful(
      ConfigValidationResponse(
        className = className,
        definitions = Seq.empty,
        validatedValues = Seq.empty
      )
  )
}
