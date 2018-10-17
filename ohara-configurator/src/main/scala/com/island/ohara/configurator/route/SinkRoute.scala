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

private[configurator] object SinkRoute extends SprayJsonSupport {

  private[this] def toRes(uuid: String, request: SinkRequest) =
    Sink(
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

  private[this] def verify(request: SinkRequest): SinkRequest = {
    if (request.schema.exists(_.order < 1))
      throw new IllegalArgumentException(s"invalid order of column:${request.schema.map(_.order)}")
    if (request.schema.map(_.order).toSet.size != request.schema.size)
      throw new IllegalArgumentException(s"duplicate order:${request.schema.map(_.order)}")
    request
  }

  private[this] def update(sink: Sink)(implicit store: Store, connectorClient: ConnectorClient): Sink = {
    val state =
      if (connectorClient.exist(sink.uuid)) Some(connectorClient.status(sink.uuid).connector.state) else None
    val newOne = sink.copy(state = state)
    store.update(newOne)
    newOne
  }

  def apply(implicit store: Store, uuidGenerator: () => String, connectorClient: ConnectorClient): server.Route =
    pathPrefix(SINK_PATH) {
      pathEnd {
        // add
        post {
          entity(as[SinkRequest]) { req =>
            val data = toRes(uuidGenerator(), verify(req))
            store.add(data)
            complete(data)
          }
        } ~ get(complete(store.data[Sink].map(update(_)).toSeq)) // list
      } ~ pathPrefix(Segment) { uuid =>
        // get
        get(complete(update(store.data[Sink](uuid)))) ~
          // delete
          delete {
            assertNotRelated2Pipeline(uuid)
            if (connectorClient.exist(uuid)) throw new IllegalArgumentException(s"$uuid is not stopped")
            complete(store.remove[Sink](uuid))
          } ~
          // update
          put {
            entity(as[SinkRequest]) { req =>
              if (connectorClient.exist(uuid)) throw new IllegalArgumentException(s"$uuid is not stopped")
              val newData = toRes(uuid, verify(req))
              store.update(newData)
              complete(newData)
            }
          } ~ path(START_COMMAND) {
          put {
            if (connectorClient.nonExist(uuid)) {
              val sink = store.data[Sink](uuid)
              if (sink.topics.isEmpty) throw new IllegalArgumentException("topics is required")
              connectorClient
                .connectorCreator()
                .name(sink.uuid)
                .disableConverter()
                .connectorClass(sinkAlias(sink.className))
                .schema(sink.schema)
                .configs(sink.configs)
                .topics(sink.topics)
                .numberOfTasks(sink.numberOfTasks)
                .create()
              // update the stats manually. Connector request is executed async so we can't get the "real-time" state of
              // connector from kafka
              store.update[Sink](sink.copy(state = Some(State.RUNNING)))
            }
            complete(StatusCodes.OK)
          }
        } ~ path(STOP_COMMAND) {
          put {
            if (connectorClient.exist(uuid)) {
              val sink = store.data[Sink](uuid)
              connectorClient.delete(sink.uuid)
              // update the stats manually. Connector request is executed async so we can't get the "real-time" state of
              // connector from kafka
              store.update[Sink](sink.copy(state = None))
            }
            complete(StatusCodes.OK)
          }
        } ~ path(PAUSE_COMMAND) {
          put {
            if (connectorClient.nonExist(uuid)) throw new IllegalArgumentException(s"$uuid doesn't exist!!!")
            val sink = store.data[Sink](uuid)
            connectorClient.pause(sink.uuid)
            // update the stats manually. Connector request is executed async so we can't get the "real-time" state of
            // connector from kafka
            store.update[Sink](sink.copy(state = Some(State.PAUSED)))
            complete(StatusCodes.OK)
          }
        } ~ path(RESUME_COMMAND) {
          put {
            if (connectorClient.nonExist(uuid)) throw new IllegalArgumentException(s"$uuid doesn't exist!!!")
            val sink = store.data[Sink](uuid)
            connectorClient.resume(sink.uuid)
            // update the stats manually. Connector request is executed async so we can't get the "real-time" state of
            // connector from kafka
            store.update[Sink](sink.copy(state = Some(State.RUNNING)))
            complete(StatusCodes.OK)
          }
        }
      }
    }
}
