package com.island.ohara.configurator.route
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import spray.json.DefaultJsonProtocol._

private[configurator] object HdfsInformationRoute {

  private[this] def toRes(uuid: String, request: HdfsInformationRequest) =
    HdfsInformation(uuid, request.name, request.uri, System.currentTimeMillis())

  def apply(implicit store: Store, uuidGenerator: () => String): server.Route =
    pathPrefix(HDFS_PATH) {
      pathEnd {
        // add
        post {
          entity(as[HdfsInformationRequest]) { req =>
            val data = toRes(uuidGenerator(), req)
            store.add(data.uuid, data)
            complete(data)
          }
        } ~ get(complete(store.data[HdfsInformation].toSeq)) // list
      } ~ path(Segment) { uuid =>
        // get
        get(complete(store.data[HdfsInformation](uuid))) ~
          // delete
          delete(complete {
            store.remove[HdfsInformation](uuid)
          }) ~
          // update
          put {
            entity(as[HdfsInformationRequest]) { req =>
              val newData = toRes(uuid, req)
              store.update(uuid, newData)
              complete(newData)
            }
          }
      }
    }
}
