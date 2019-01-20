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
import com.island.ohara.client.configurator.v0.ConnectorApi._
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil._
import com.typesafe.scalalogging.Logger

import scala.concurrent.Await
import scala.concurrent.duration._

private[configurator] object ConnectorRoute extends SprayJsonSupport {
  private[this] lazy val LOG = Logger(ConnectorRoute.getClass)

  private[this] def toRes(id: String, request: ConnectorConfigurationRequest) =
    ConnectorConfiguration(
      id = id,
      name = request.name,
      className = request.className,
      schema = request.schema,
      topics = request.topics,
      numberOfTasks = request.numberOfTasks,
      state = None,
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

  private[this] def update(connectorConfig: ConnectorConfiguration)(
    implicit workerClient: WorkerClient): ConnectorConfiguration = {
    val state = try if (workerClient.exist(connectorConfig.id))
      Some(workerClient.status(connectorConfig.id).connector.state)
    else None
    catch {
      case e: Throwable =>
        LOG.error(s"failed to fetch stats for $connectorConfig", e)
        None
    }
    val newOne = connectorConfig.copy(state = state)
    newOne
  }

  def apply(implicit store: Store, workerClient: WorkerClient): server.Route =
    // TODO: OHARA-1201 should remove the "sources" and "sinks" ... by chia
    pathPrefix(CONNECTORS_PREFIX_PATH | "sources" | "sinks") {
      RouteUtil.basicRoute2[ConnectorConfigurationRequest, ConnectorConfiguration](
        hookOfAdd = (id: String, request: ConnectorConfigurationRequest) => toRes(id, verify(request)),
        hookOfUpdate = (id: String, request: ConnectorConfigurationRequest, _: ConnectorConfiguration) => {
          if (workerClient.exist(id)) throw new IllegalArgumentException(s"$id is not stopped")
          toRes(id, verify(request))
        },
        hookOfGet = update(_),
        hookOfList = (responses: Seq[ConnectorConfiguration]) => responses.map(update),
        hookBeforeDelete = (id: String) => {
          assertNotRelated2Pipeline(id)
          if (workerClient.exist(id)) throw new IllegalArgumentException(s"$id is not stopped")
          id
        },
        hookOfDelete = (response: ConnectorConfiguration) => response
      )
    } ~
      // TODO: OHARA-1201 should remove the "sources" and "sinks" ... by chia
      pathPrefix((CONNECTORS_PREFIX_PATH | "sources" | "sinks") / Segment) { id =>
        path(START_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id)) { connector =>
              if (workerClient.nonExist(connector.id)) {
                if (connector.topics.isEmpty) throw new IllegalArgumentException("topics is required")
                val invalidTopics =
                  connector.topics.filterNot(t => Await.result(store.exist[TopicInfo](t), 30 seconds))
                if (invalidTopics.nonEmpty) throw new IllegalArgumentException(s"$invalidTopics doesn't exist in ohara")
                workerClient
                  .connectorCreator()
                  .name(connector.id)
                  .disableConverter()
                  .connectorClass(connector.className)
                  .schema(connector.schema)
                  .configs(connector.configs)
                  .topics(connector.topics)
                  .numberOfTasks(connector.numberOfTasks)
                  .create()
              }
              complete(update(connector))
            }
          }
        } ~ path(STOP_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id)) { connector =>
              if (workerClient.exist(id)) workerClient.delete(id)
              complete(update(connector))
            }
          }
        } ~ path(PAUSE_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id)) { connector =>
              if (workerClient.nonExist(id))
                throw new IllegalArgumentException(s"Connector is not running , using start command first . id:$id !!!")
              workerClient.pause(id)
              complete(update(connector))
            }
          }
        } ~ path(RESUME_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id)) { connector =>
              if (workerClient.nonExist(id))
                throw new IllegalArgumentException(s"Connector is not running , using start command first . id:$id !!!")
              workerClient.resume(id)
              complete(update(connector))
            }
          }
        }
      }
}
