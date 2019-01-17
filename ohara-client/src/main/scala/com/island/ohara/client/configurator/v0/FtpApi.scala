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
