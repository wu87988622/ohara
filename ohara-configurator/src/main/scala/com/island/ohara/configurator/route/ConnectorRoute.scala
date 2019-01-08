package com.island.ohara.configurator.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.configurator.v0.ConnectorApi._
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtil._

import scala.concurrent.Await
import scala.concurrent.duration._

private[configurator] object ConnectorRoute extends SprayJsonSupport {

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
    implicit connectorClient: ConnectorClient): ConnectorConfiguration = {
    val state =
      if (connectorClient.exist(connectorConfig.id)) Some(connectorClient.status(connectorConfig.id).connector.state)
      else None
    val newOne = connectorConfig.copy(state = state)
    newOne
  }

  def apply(implicit store: Store, connectorClient: ConnectorClient): server.Route =
    // TODO: OHARA-1201 should remove the "sources" and "sinks" ... by chia
    pathPrefix(CONNECTORS_PREFIX_PATH | "sources" | "sinks") {
      RouteUtil.basicRoute2[ConnectorConfigurationRequest, ConnectorConfiguration](
        hookOfAdd = (id: String, request: ConnectorConfigurationRequest) => toRes(id, verify(request)),
        hookOfUpdate = (id: String, request: ConnectorConfigurationRequest, _: ConnectorConfiguration) => {
          if (connectorClient.exist(id)) throw new IllegalArgumentException(s"$id is not stopped")
          toRes(id, verify(request))
        },
        hookOfGet = update(_),
        hookOfList = (responses: Seq[ConnectorConfiguration]) => responses.map(update),
        hookBeforeDelete = (id: String) => {
          assertNotRelated2Pipeline(id)
          if (connectorClient.exist(id)) throw new IllegalArgumentException(s"$id is not stopped")
          id
        },
        hookOfDelete = (response: ConnectorConfiguration) => response
      )
    } ~
      // TODO: OHARA-1201 should remove the "sources" and "sinks" ... by chia
      pathPrefix((CONNECTORS_PREFIX_PATH | "sources" | "sinks") / Segment) { id =>
        path(START_COMMAND) {
          put {
            if (connectorClient.nonExist(id)) onSuccess(store.value[ConnectorConfiguration](id)) { source =>
              if (source.topics.isEmpty) throw new IllegalArgumentException("topics is required")
              val invalidTopics =
                source.topics.filterNot(t => Await.result(store.exist[TopicInfo](t), 30 seconds))
              if (invalidTopics.nonEmpty) throw new IllegalArgumentException(s"$invalidTopics doesn't exist in ohara")
              connectorClient
                .connectorCreator()
                .name(source.id)
                .disableConverter()
                .connectorClass(source.className)
                .schema(source.schema)
                .configs(source.configs)
                .topics(source.topics)
                .numberOfTasks(source.numberOfTasks)
                .create()
              complete(StatusCodes.OK)
            } else complete(StatusCodes.OK)
          }
        } ~ path(STOP_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id)) { _ =>
              if (connectorClient.exist(id)) connectorClient.delete(id)
              complete(StatusCodes.OK)
            }
          }
        } ~ path(PAUSE_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id)) { _ =>
              if (connectorClient.nonExist(id))
                throw new IllegalArgumentException(s"Connector is not running , using start command first . id:$id !!!")
              connectorClient.pause(id)
              complete(StatusCodes.OK)
            }
          }
        } ~ path(RESUME_COMMAND) {
          put {
            onSuccess(store.value[ConnectorConfiguration](id)) { _ =>
              if (connectorClient.nonExist(id))
                throw new IllegalArgumentException(s"Connector is not running , using start command first . id:$id !!!")
              connectorClient.resume(id)
              complete(StatusCodes.OK)
            }
          }
        }
      }
}
