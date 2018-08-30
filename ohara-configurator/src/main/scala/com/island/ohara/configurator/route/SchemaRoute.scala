package com.island.ohara.configurator.route
import akka.http.scaladsl.server
import BasicRoute._
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.configurator.Configurator.Store

private[configurator] object SchemaRoute {

  private[this] def toRes(uuid: String, request: SchemaRequest) = {
    if (request.columns.map(_.order).toSet.size != request.columns.size)
      throw new IllegalArgumentException(s"duplicate order in ${request.columns}")
    if (request.columns.map(_.name).toSet.size != request.columns.size)
      throw new IllegalArgumentException(s"duplicate name in ${request.columns}")
    if (request.columns.map(_.order).exists(_.equals(0))) throw new IllegalArgumentException("0 is invalid order")
    (1 to request.columns.size).foreach(
      index =>
        if (!request.columns.map(_.order).exists(_.equals(index)))
          throw new IllegalArgumentException(s"there are ${request.columns.size} columns but $index doesn't exist"))
    Schema(uuid, request.name, request.columns, request.disabled, System.currentTimeMillis())
  }

  def apply(implicit store: Store, uuidGenerator: () => String): server.Route =
    pathPrefix(SCHEMA_PATH) {
      pathEnd {
        // add
        post {
          entity(as[SchemaRequest]) { req =>
            val data = toRes(uuidGenerator(), req)
            store.add(data.uuid, data)
            complete(data)
          }
        } ~ get(complete(store.data[Schema].toSeq)) // list
      } ~ path(Segment) { uuid =>
        // get
        get(complete(store.data[Schema](uuid))) ~
          // delete
          delete {
            complete(store.remove[Schema](uuid))
          } ~
          // update
          put {
            entity(as[SchemaRequest]) { req =>
              val newData = toRes(uuid, req)
              store.update(uuid, newData)
              complete(newData)
            }
          }
      }
    }
}
