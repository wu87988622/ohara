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

package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.client.configurator.v0.FtpApi._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store

private[configurator] object FtpInfoRoute {

  def apply(implicit store: Store): server.Route = RouteUtil.basicRoute[FtpInfoRequest, FtpInfo](
    root = FTP_PREFIX_PATH,
    reqToRes = (id: String, request: FtpInfoRequest) => {
      validateField(request)
      FtpInfo(id, request.name, request.hostname, request.port, request.user, request.password, CommonUtil.current())
    }
  )

  private[route] def validateField(ftpInfo: FtpInfoRequest): Unit = {
    val FtpInfoRequest(name, hostname, port, user, password) = ftpInfo
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
