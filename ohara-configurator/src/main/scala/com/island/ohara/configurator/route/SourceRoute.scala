package com.island.ohara.configurator.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.ConnectorJson.State
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import com.island.ohara.util.SystemUtil
import spray.json.DefaultJsonProtocol._

private[configurator] object SourceRoute extends SprayJsonSupport {

  // TODO: hard code...by chia
  private[this] def sourceAlias(name: String): String = name.toLowerCase match {
    case "jdbc" => "com.island.ohara.connector.jdbc.JDBCSourceConnector"
    case "ftp"  => "com.island.ohara.connector.ftp.FtpSource"
    case _      => name
  }

  private[this] def toRes(uuid: String, request: SourceRequest) =
    Source(
      uuid = uuid,
      name = request.name,
      className = sourceAlias(request.className),
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

  private[this] def update(source: Source)(implicit store: Store, connectorClient: ConnectorClient): Source = {
    val state =
      if (connectorClient.exist(source.uuid)) Some(connectorClient.status(source.uuid).connector.state) else None
    val newOne = source.copy(state = state)
    store.update(newOne)
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
        } ~ get(complete(store.data[Source].map(update(_)).toSeq)) // list
      } ~ pathPrefix(Segment) { uuid =>
        pathEnd {
          // get
          get(complete(update(store.data[Source](uuid)))) ~
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
            }
        } ~ path(START_COMMAND) {
          put {
            val source = store.data[Source](uuid)
            if (connectorClient.nonExist(uuid)) {
              if (source.topics.isEmpty) throw new IllegalArgumentException("topics is required")
              source.topics.foreach(t =>
                if (store.nonExist[TopicInfo](t)) throw new IllegalArgumentException(s"$t does not exist in ohara"))
              connectorClient
                .connectorCreator()
                .name(source.uuid)
                .disableConverter()
                .connectorClass(source.className)
                .schema(source.schema)
                .configs(source.configs)
                .topics(source.topics)
                .numberOfTasks(source.numberOfTasks)
                .create()
              // update the stats manually. Connector request is executed async so we can't get the "real-time" state of
              // connector from kafka
              store.update[Source](source.copy(state = Some(State.RUNNING)))
            }
            complete(StatusCodes.OK)
          }
        } ~ path(STOP_COMMAND) {
          put {
            val source = store.data[Source](uuid)
            if (connectorClient.exist(uuid)) {
              connectorClient.delete(source.uuid)
              // update the stats manually. Connector request is executed async so we can't get the "real-time" state of
              // connector from kafka
              store.update[Source](source.copy(state = None))
            }
            complete(StatusCodes.OK)
          }
        } ~ path(PAUSE_COMMAND) {
          put {
            val source = store.data[Source](uuid)
            if (connectorClient.nonExist(uuid))
              throw new IllegalArgumentException(
                s"Connector is not running , using start command first . UUID: $uuid !!!")
            connectorClient.pause(source.uuid)
            // update the stats manually. Connector request is executed async so we can't get the "real-time" state of
            // connector from kafka
            store.update[Source](source.copy(state = Some(State.PAUSED)))
            complete(StatusCodes.OK)
          }
        } ~ path(RESUME_COMMAND) {
          put {
            val source = store.data[Source](uuid)
            if (connectorClient.nonExist(uuid))
              throw new IllegalArgumentException(
                s"Connector is not running , using start command first . UUID: $uuid !!!")
            connectorClient.resume(source.uuid)
            // update the stats manually. Connector request is executed async so we can't get the "real-time" state of
            // connector from kafka
            store.update[Source](source.copy(state = Some(State.RUNNING)))
            complete(StatusCodes.OK)
          }
        }
      }
    }
}
