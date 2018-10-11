package com.island.ohara.configurator.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.{ConnectorClient, ConnectorJson}
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import com.island.ohara.util.SystemUtil
import spray.json.DefaultJsonProtocol._

private[configurator] object SourceRoute extends SprayJsonSupport {

  private[this] def toRes(uuid: String, request: SourceRequest) =
    Source(
      uuid = uuid,
      name = request.name,
      className = request.className,
      schema = request.schema,
      topics = request.topics,
      numberOfTasks = request.numberOfTasks,
      state = None,
      configs = request.configs,
      lastModified = SystemUtil.current()
    )

  private[this] def verify(request: SourceRequest): SourceRequest = {
    if (request.schema.exists(_.order < 1))
      throw new IllegalArgumentException(s"invalid order of column:${request.schema.map(_.order)}")
    if (request.schema.map(_.order).toSet.size != request.schema.size)
      throw new IllegalArgumentException(s"duplicate order:${request.schema.map(_.order)}")
    request
  }

  private[this] def update(store: Store, connectorClient: ConnectorClient, source: Source): Source = {
    val state =
      if (connectorClient.exist(source.uuid)) Some(connectorClient.status(source.uuid).connector.state) else None
    val newOne = source.copy(state = state)
    store.update[Source](newOne)
    newOne
  }

  def apply(implicit store: Store, uuidGenerator: () => String, connectorClient: ConnectorClient): server.Route =
    pathPrefix(SOURCE_PATH) {
      pathEnd {
        // add
        post {
          entity(as[SourceRequest]) { req =>
            val data = toRes(uuidGenerator(), verify(req))
            store.add(data)
            complete(data)
          }
        } ~ get(complete(store.data[Source].map(update(store, connectorClient, _)).toSeq)) // list
      } ~ pathPrefix(Segment) { uuid =>
        // get
        get(complete(update(store, connectorClient, store.data[Source](uuid)))) ~
          // delete
          delete {
            assertNotRelated2Pipeline(uuid)
            if (connectorClient.exist(uuid)) throw new IllegalArgumentException(s"$uuid is not stopped")
            complete(store.remove[Source](uuid))
          } ~
          // update
          put {
            entity(as[SourceRequest]) { req =>
              if (connectorClient.exist(uuid)) throw new IllegalArgumentException(s"$uuid is not stopped")
              val newData = toRes(uuid, verify(req))
              store.update(newData)
              complete(newData)
            }
          } ~ path(START_COMMAND) {
          put {
            val source = store.data[Source](uuid)
            if (connectorClient.exist(source.uuid)) throw new IllegalArgumentException(s"$uuid exists!!!")
            if (source.topics.isEmpty) throw new IllegalArgumentException("topics is required")
            connectorClient
              .connectorCreator()
              .name(source.uuid)
              .disableConverter()
              .connectorClass(sourceAlias(source.className))
              .schema(source.schema)
              .configs(source.configs)
              .topics(source.topics)
              .numberOfTasks(source.numberOfTasks)
              .create()
            // we don't update state of source since it will be updated by GET API
            complete(StatusCodes.OK)
          }
        } ~ path(STOP_COMMAND) {
          put {
            val source = store.data[Source](uuid)
            if (connectorClient.nonExist(source.uuid)) throw new IllegalArgumentException(s"$uuid doesn't exist!!!")
            connectorClient.delete(source.uuid)
            complete(StatusCodes.OK)
          }
        } ~ path(PAUSE_COMMAND) {
          put {
            val source = store.data[Source](uuid)
            if (connectorClient.nonExist(source.uuid)) throw new IllegalArgumentException(s"$uuid doesn't exist!!!")
            connectorClient.pause(source.uuid)
            complete(StatusCodes.OK)
          }
        } ~ path(RESUME_COMMAND) {
          put {
            val source = store.data[Source](uuid)
            if (connectorClient.nonExist(source.uuid)) throw new IllegalArgumentException(s"$uuid doesn't exist!!!")
            val state = connectorClient.status(source.uuid).connector.state
            if (state != ConnectorJson.State.PAUSED) throw new IllegalArgumentException(s"$uuid is in a $state")
            connectorClient.resume(source.uuid)
            complete(StatusCodes.OK)
          }
        }
      }
    }
}
