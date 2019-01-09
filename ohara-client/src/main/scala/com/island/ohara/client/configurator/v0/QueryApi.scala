package com.island.ohara.client.configurator.v0

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future

object QueryApi {
  val QUERY_PREFIX_PATH: String = "query"
  val RDB_PREFIX_PATH: String = "rdb"
  final case class RdbColumn(name: String, dataType: String, pk: Boolean)
  implicit val RDB_COLUMN_JSON_FORMAT: RootJsonFormat[RdbColumn] = jsonFormat3(RdbColumn)
  final case class RdbTable(catalogPattern: Option[String],
                            schemaPattern: Option[String],
                            name: String,
                            schema: Seq[RdbColumn])
  implicit val RDB_TABLE_JSON_FORMAT: RootJsonFormat[RdbTable] = jsonFormat4(RdbTable)

  final case class RdbQuery(url: String,
                            user: String,
                            password: String,
                            catalogPattern: Option[String],
                            schemaPattern: Option[String],
                            tableName: Option[String])
  implicit val RDB_QUERY_JSON_FORMAT: RootJsonFormat[RdbQuery] = jsonFormat6(RdbQuery)

  final case class RdbInfo(name: String, tables: Seq[RdbTable])
  implicit val RDB_INFO_JSON_FORMAT: RootJsonFormat[RdbInfo] = jsonFormat2(RdbInfo)

  sealed abstract class Access extends BasicAccess(QUERY_PREFIX_PATH) {
    def query(q: RdbQuery): Future[RdbInfo]
  }

  def access(): Access = new Access {
    override def query(q: RdbQuery): Future[RdbInfo] =
      exec.post[RdbQuery, RdbInfo](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$RDB_PREFIX_PATH", q)
  }
}
