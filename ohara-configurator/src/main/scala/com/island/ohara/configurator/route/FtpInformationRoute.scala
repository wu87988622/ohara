package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import spray.json.DefaultJsonProtocol._

private[configurator] object FtpInformationRoute {

  private[this] def toRes(uuid: String, request: FtpInformationRequest) = {
    validateField(request)
    FtpInformation(uuid,
                   request.name,
                   request.hostname,
                   request.port,
                   request.user,
                   request.password,
                   CommonUtil.current())
  }
  def apply(implicit store: Store, uuidGenerator: () => String): server.Route =
    pathPrefix(FTP_PATH) {
      pathEnd {
        // add
        post {
          entity(as[FtpInformationRequest]) { req =>
            val data = toRes(uuidGenerator(), req)
            store.add(data)
            complete(data)
          }
        } ~ get(complete(store.data[FtpInformation].toSeq)) // list
      } ~ path(Segment) { uuid =>
        // get
        get(complete(store.data[FtpInformation](uuid))) ~
          // delete
          delete(complete {
            store.remove[FtpInformation](uuid)
          }) ~
          // update
          put {
            entity(as[FtpInformationRequest]) { req =>
              val newData = toRes(uuid, req)
              store.update(newData)
              complete(newData)
            }
          }
      }
    }

  private[route] def validateField(ftpInfo: FtpInformationRequest): Unit = {
    val FtpInformationRequest(name, hostname, port, user, password) = ftpInfo
    val msg =
      if (name.isEmpty) "empty name is illegal"
      else if (port <= 0 || port > 65535) s"illegal port:$port"
      else if (hostname.isEmpty) "empty hostname is illegal"
      else if (user.isEmpty) "empty user is illegal"
      else if (password.isEmpty) "empty password is illegal"
      else ""
    if (msg.nonEmpty) throw new IllegalArgumentException(s"validate error - $msg")
  }

}
