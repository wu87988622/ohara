package com.island.ohara.configurator.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.ConnectorClient
import com.island.ohara.common.data.connector.ConnectorState
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import spray.json.DefaultJsonProtocol._

private[configurator] object SinkRoute extends SprayJsonSupport {

  // TODO: hard code...by chia
  private[this] def sinkAlias(name: String): String = name.toLowerCase match {
    case "hdfs" => "com.island.ohara.connector.hdfs.HDFSSinkConnector"
    case "ftp"  => "com.island.ohara.connector.ftp.FtpSink"
    case _      => name
  }

  private[this] def toRes(uuid: String, request: SinkRequest) =
    Sink(
      id = uuid,
      name = request.name,
      className = sinkAlias(request.className),
      schema = request.schema,
      topics = request.topics,
      numberOfTasks = request.numberOfTasks,
      state = None,
      configs = request.configs,
      lastModified = CommonUtil.current()
    )

  private[this] def verify(request: SinkRequest): SinkRequest = {
    if (request.schema.exists(_.order < 1))
      throw new IllegalArgumentException(s"invalid order from column:${request.schema.map(_.order)}")
    if (request.schema.map(_.order).toSet.size != request.schema.size)
      throw new IllegalArgumentException(s"duplicate order:${request.schema.map(_.order)}")
    request
  }

  private[this] def update(sink: Sink)(implicit store: Store, connectorClient: ConnectorClient): Sink = {
    val state =
      if (connectorClient.exist(sink.id)) Some(connectorClient.status(sink.id).connector.state) else None
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
        pathEnd {
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
            }
        } ~ path(START_COMMAND) {
          put {
            val sink = store.data[Sink](uuid)
            if (connectorClient.nonExist(uuid)) {
              if (sink.topics.isEmpty) throw new IllegalArgumentException("topics is required")
              sink.topics.foreach(t =>
                if (store.nonExist[TopicInfo](t)) throw new IllegalArgumentException(s"$t does not exist in ohara"))
              connectorClient
                .connectorCreator()
                .name(sink.id)
                .disableConverter()
                .connectorClass(sink.className)
                .schema(sink.schema)
                .configs(sink.configs)
                .topics(sink.topics)
                .numberOfTasks(sink.numberOfTasks)
                .create()
              // update the stats manually. Connector request is executed async so we can't get the "real-time" state from
              // connector from kafka
              store.update[Sink](sink.copy(state = Some(ConnectorState.RUNNING)))
            }
            complete(StatusCodes.OK)
          }
        } ~ path(STOP_COMMAND) {
          put {
            val sink = store.data[Sink](uuid)
            if (connectorClient.exist(uuid)) {
              connectorClient.delete(sink.id)
              // update the stats manually. Connector request is executed async so we can't get the "real-time" state from
              // connector from kafka
              store.update[Sink](sink.copy(state = None))
            }
            complete(StatusCodes.OK)
          }
        } ~ path(PAUSE_COMMAND) {
          put {
            val sink = store.data[Sink](uuid)
            if (connectorClient.nonExist(uuid))
              throw new IllegalArgumentException(
                s"Connector is not running , using start command first . UUID: $uuid !!!")
            connectorClient.pause(sink.id)
            // update the stats manually. Connector request is executed async so we can't get the "real-time" state from
            // connector from kafka
            store.update[Sink](sink.copy(state = Some(ConnectorState.PAUSED)))
            complete(StatusCodes.OK)
          }
        } ~ path(RESUME_COMMAND) {
          put {
            val sink = store.data[Sink](uuid)
            if (connectorClient.nonExist(uuid))
              throw new IllegalArgumentException(
                s"Connector is not running , using start command first . UUID: $uuid !!!")
            connectorClient.resume(sink.id)
            // update the stats manually. Connector request is executed async so we can't get the "real-time" state from
            // connector from kafka
            store.update[Sink](sink.copy(state = Some(ConnectorState.RUNNING)))
            complete(StatusCodes.OK)
          }
        }
      }
    }
}
