package com.island.ohara.client.configurator.v0
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object DatabaseApi {
  val JDBC_PREFIX_PATH: String = "jdbc"
  final case class JdbcInfoRequest(name: String, url: String, user: String, password: String)
  implicit val JDBC_INFO_REQUEST_JSON_FORMAT: RootJsonFormat[JdbcInfoRequest] = jsonFormat4(JdbcInfoRequest)

  final case class JdbcInfo(id: String, name: String, url: String, user: String, password: String, lastModified: Long)
      extends Data {
    override def kind: String = "jdbc"
  }
  implicit val JDBC_INFO_JSON_FORMAT: RootJsonFormat[JdbcInfo] = jsonFormat6(JdbcInfo)

  def access(): Access[JdbcInfoRequest, JdbcInfo] =
    new Access[JdbcInfoRequest, JdbcInfo](JDBC_PREFIX_PATH)
}
