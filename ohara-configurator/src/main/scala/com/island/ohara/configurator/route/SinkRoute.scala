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
    Sink(uuid, request.name, request.className, request.configs, System.currentTimeMillis())

  def apply(implicit store: Store, uuidGenerator: () => String): server.Route = pathPrefix(SINK_PATH) {
    pathEnd {
      // add
      post {
        entity(as[SinkRequest]) { req =>
          val data = toRes(uuidGenerator(), req)
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
            val newData = toRes(uuid, req)
            store.update(uuid, newData)
            complete(newData)
          }
        }
    }
  }
}
