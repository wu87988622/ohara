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
