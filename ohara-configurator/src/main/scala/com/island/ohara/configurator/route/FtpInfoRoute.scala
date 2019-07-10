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
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object FtpInfoRoute {

  def apply(implicit store: DataStore, executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRoute[Creation, Update, FtpInfo](
      root = FTP_PREFIX_PATH,
      hookOfCreation = (request: Creation) =>
        Future.successful(
          FtpInfo(
            name = request.name,
            hostname = request.hostname,
            port = request.port,
            user = request.user,
            password = request.password,
            lastModified = CommonUtils.current(),
            tags = request.tags
          )),
      hookOfUpdate = (name: String, request: Update, previousOption: Option[FtpInfo]) =>
        Future.successful(previousOption.fold {
          if (request.hostname.isEmpty)
            throw new IllegalArgumentException(RouteUtils.errorMessage(name, "hostname"))
          if (request.port.isEmpty) throw new IllegalArgumentException(RouteUtils.errorMessage(name, "port"))
          if (request.user.isEmpty) throw new IllegalArgumentException(RouteUtils.errorMessage(name, "user"))
          if (request.password.isEmpty)
            throw new IllegalArgumentException(RouteUtils.errorMessage(name, "password"))
          FtpInfo(
            name = name,
            hostname = request.hostname.get,
            port = request.port.get,
            user = request.user.get,
            password = request.password.get,
            lastModified = CommonUtils.current(),
            tags = request.tags.getOrElse(Set.empty)
          )
        } { previous =>
          FtpInfo(
            name = name,
            hostname = request.hostname.getOrElse(previous.hostname),
            port = request.port.getOrElse(previous.port),
            user = request.user.getOrElse(previous.user),
            password = request.password.getOrElse(previous.password),
            lastModified = CommonUtils.current(),
            tags = request.tags.getOrElse(previous.tags)
          )
        })
    )
}
