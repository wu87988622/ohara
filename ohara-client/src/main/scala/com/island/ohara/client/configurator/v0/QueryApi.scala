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

import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.util.CommonUtils
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

  final case class RdbQuery private[QueryApi] (url: String,
                                               user: String,
                                               password: String,
                                               workerClusterName: Option[String],
                                               catalogPattern: Option[String],
                                               schemaPattern: Option[String],
                                               tableName: Option[String])
  implicit val RDB_QUERY_JSON_FORMAT: OharaJsonFormat[RdbQuery] =
    JsonRefiner[RdbQuery].format(jsonFormat7(RdbQuery)).rejectEmptyString().refine

  final case class RdbInfo(name: String, tables: Seq[RdbTable])
  implicit val RDB_INFO_JSON_FORMAT: RootJsonFormat[RdbInfo] = jsonFormat2(RdbInfo)

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {
    def jdbcUrl(url: String): Request

    @Optional("server will match a broker cluster for you if the wk name is ignored")
    def workerClusterName(workerClusterName: String): Request

    def user(user: String): Request

    def password(password: String): Request

    @Optional("default is null")
    def catalogPattern(catalogPattern: String): Request

    @Optional("default is null")
    def schemaPattern(schemaPattern: String): Request

    @Optional("default is null")
    def tableName(tableName: String): Request

    @VisibleForTesting
    private[v0] def query: RdbQuery

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def query()(implicit executionContext: ExecutionContext): Future[RdbInfo]
  }

  final class Access private[QueryApi] extends BasicAccess(QUERY_PREFIX_PATH) {
    def request: Request = new Request {
      private[this] var jdbcUrl: String = _
      private[this] var user: String = _
      private[this] var password: String = _
      private[this] var workerClusterName: String = _
      private[this] var catalogPattern: String = _
      private[this] var schemaPattern: String = _
      private[this] var tableName: String = _

      override def jdbcUrl(jdbcUrl: String): Request = {
        this.jdbcUrl = CommonUtils.requireNonEmpty(jdbcUrl)
        this
      }

      override def workerClusterName(workerClusterName: String): Request = {
        this.workerClusterName = CommonUtils.requireNonEmpty(workerClusterName)
        this
      }

      override def user(user: String): Request = {
        this.user = CommonUtils.requireNonEmpty(user)
        this
      }

      override def password(password: String): Request = {
        this.password = CommonUtils.requireNonEmpty(password)
        this
      }

      override def catalogPattern(catalogPattern: String): Request = {
        this.catalogPattern = CommonUtils.requireNonEmpty(catalogPattern)
        this
      }

      override def schemaPattern(schemaPattern: String): Request = {
        this.schemaPattern = CommonUtils.requireNonEmpty(schemaPattern)
        this
      }

      override def tableName(tableName: String): Request = {
        this.tableName = CommonUtils.requireNonEmpty(tableName)
        this
      }

      override private[v0] def query: RdbQuery = RdbQuery(
        url = CommonUtils.requireNonEmpty(jdbcUrl),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        workerClusterName = Option(workerClusterName).map(CommonUtils.requireNonEmpty),
        catalogPattern = Option(catalogPattern).map(CommonUtils.requireNonEmpty),
        schemaPattern = Option(schemaPattern).map(CommonUtils.requireNonEmpty),
        tableName = Option(tableName).map(CommonUtils.requireNonEmpty)
      )

      override def query()(implicit executionContext: ExecutionContext): Future[RdbInfo] =
        exec.post[RdbQuery, RdbInfo, ErrorApi.Error](s"$url/$RDB_PREFIX_PATH", query)
    }
  }

  def access: Access = new Access
}
