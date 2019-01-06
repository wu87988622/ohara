package com.island.ohara.client.configurator.v0
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object FtpApi {
  val FTP_PREFIX_PATH: String = "ftp"
  final case class FtpInfoRequest(name: String, hostname: String, port: Int, user: String, password: String)
  implicit val FTP_INFO_REQUEST_JSON_FORMAT: RootJsonFormat[FtpInfoRequest] = jsonFormat5(FtpInfoRequest)

  final case class FtpInfo(id: String,
                           name: String,
                           hostname: String,
                           port: Int,
                           user: String,
                           password: String,
                           lastModified: Long)
      extends Data {
    override def kind: String = "ftp"
  }
  implicit val FTP_INFO_JSON_FORMAT: RootJsonFormat[FtpInfo] = jsonFormat7(FtpInfo)

  def access(): Access[FtpInfoRequest, FtpInfo] =
    new Access[FtpInfoRequest, FtpInfo](FTP_PREFIX_PATH)
}
