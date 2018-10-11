package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.BasicRoute._
import com.island.ohara.util.SystemUtil
import spray.json.DefaultJsonProtocol._

private[configurator] object FtpInformationRoute {

  private[this] def toRes(uuid: String, request: FtpInformationRequest) = {
    validateField(request)
    FtpInformation(uuid, request.name, request.ip, request.port, request.user, request.password, SystemUtil.current())
  }
  def apply(implicit store: Store, uuidGenerator: () => String): server.Route =
    pathPrefix(FTP_PATH) {
      pathEnd {
        // add
        post {
          entity(as[FtpInformationRequest]) { req =>
            val data = toRes(uuidGenerator(), req)
            store.add(data.uuid, data)
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
              store.update(uuid, newData)
              complete(newData)
            }
          }
      }
    }

  val IP_REGEX =
    raw"^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$$"

  def validateField(ftpInfo: FtpInformationRequest): Unit = {
    val FtpInformationRequest(name, ip, port, user, password) = ftpInfo
    val msg =
      if (!ip.matches(IP_REGEX))
        s"ip can not parse ,ip: $ip "
      else if (!(port.exists(_ <= 65535) || port.isEmpty))
        s"port needs below 65535 or be Empty ,port: $port"
      else if (user.isEmpty)
        s"user is empty"
      else ""

    if (!msg.isEmpty())
      throw new IllegalArgumentException(s"validate error - $msg")
  }

}
