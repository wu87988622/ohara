package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import com.island.ohara.util.SystemUtil
import spray.json.DefaultJsonProtocol._

private[configurator] object JdbcInformationRoute {

  private[this] def toRes(uuid: String, request: JdbcInformationRequest) = {
    JdbcInformation(uuid, request.name, request.url, request.user, request.password, SystemUtil.current())
  }
  def apply(implicit store: Store, uuidGenerator: () => String): server.Route =
    pathPrefix(JDBC_PATH) {
      pathEnd {
        // add
        post {
          entity(as[JdbcInformationRequest]) { req =>
            val data = toRes(uuidGenerator(), req)
            store.add(data)
            complete(data)
          }
        } ~ get(complete(store.data[JdbcInformation].toSeq)) // list
      } ~ path(Segment) { uuid =>
        // get
        get(complete(store.data[JdbcInformation](uuid))) ~
          // delete
          delete(complete {
            store.remove[JdbcInformation](uuid)
          }) ~
          // update
          put {
            entity(as[JdbcInformationRequest]) { req =>
              val newData = toRes(uuid, req)
              store.update(newData)
              complete(newData)
            }
          }
      }
    }
}
