/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.client.configurator.v0

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

object QueryApi {
  val QUERY_PREFIX_PATH: String = "query"
  val RDB_PREFIX_PATH: String = "rdb"
  final case class RdbColumn(name: String, dataType: String, pk: Boolean)
  implicit val RDB_COLUMN_JSON_FORMAT: RootJsonFormat[RdbColumn] = jsonFormat3(RdbColumn)
  final case class RdbTable(catalogPattern: Option[String],
                            schemaPattern: Option[String],
                            name: String,
                            columns: Seq[RdbColumn])
  implicit val RDB_TABLE_JSON_FORMAT: RootJsonFormat[RdbTable] = jsonFormat4(RdbTable)

  final case class RdbQuery(url: String,
                            user: String,
                            password: String,
                            workerClusterName: Option[String],
                            catalogPattern: Option[String],
                            schemaPattern: Option[String],
                            tableName: Option[String])
  implicit val RDB_QUERY_JSON_FORMAT: RootJsonFormat[RdbQuery] = jsonFormat7(RdbQuery)

  final case class RdbInfo(name: String, tables: Seq[RdbTable])
  implicit val RDB_INFO_JSON_FORMAT: RootJsonFormat[RdbInfo] = jsonFormat2(RdbInfo)

  sealed abstract class Access extends BasicAccess(QUERY_PREFIX_PATH) {
    def query(q: RdbQuery)(implicit executionContext: ExecutionContext): Future[RdbInfo]
  }

  def access(): Access = new Access {
    override def query(q: RdbQuery)(implicit executionContext: ExecutionContext): Future[RdbInfo] =
      exec.post[RdbQuery, RdbInfo, ErrorApi.Error](
        s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$RDB_PREFIX_PATH",
        q)
  }
}
