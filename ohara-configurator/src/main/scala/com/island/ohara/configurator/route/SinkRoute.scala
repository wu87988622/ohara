package com.island.ohara.configurator.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import spray.json.DefaultJsonProtocol._

private[configurator] object SinkRoute extends SprayJsonSupport {

  private[this] def toRes(uuid: String, request: SinkRequest) =
    Sink(uuid, request.name, request.className, request.schema, request.configs, System.currentTimeMillis())

  private[this] def verify(request: SinkRequest): SinkRequest = {
    if (request.schema.exists(_.order < 1))
      throw new IllegalArgumentException(s"invalid order of column:${request.schema.map(_.order)}")
    if (request.schema.map(_.order).toSet.size != request.schema.size)
      throw new IllegalArgumentException(s"duplicate order:${request.schema.map(_.order)}")
    request
  }

  def apply(implicit store: Store, uuidGenerator: () => String): server.Route = pathPrefix(SINK_PATH) {
    pathEnd {
      // add
      post {
        entity(as[SinkRequest]) { req =>
          val data = toRes(uuidGenerator(), verify(req))
          store.add(data.uuid, data)
          complete(data)
        }
      } ~ get(complete(store.data[Sink].toSeq)) // list
    } ~ path(Segment) { uuid =>
      // get
      get(complete(store.data[Sink](uuid))) ~
        // delete
        delete {
          assertNotRelated2Pipeline(uuid)
          complete(store.remove[Sink](uuid))
        } ~
        // update
        put {
          entity(as[SinkRequest]) { req =>
            assertNotRelated2RunningPipeline(uuid)
            val newData = toRes(uuid, verify(req))
            store.update(uuid, newData)
            complete(newData)
          }
        }
    }
  }
}
