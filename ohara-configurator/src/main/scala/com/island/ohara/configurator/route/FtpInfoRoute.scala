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
import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object FtpInfoRoute {

  def apply(implicit store: DataStore, executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRoute2[Creation, Update, FtpInfo](
      root = FTP_PREFIX_PATH,
      hookOfCreate = (request: Creation) => {
        validateField(request)
        Future.successful(
          FtpInfo(request.name, request.hostname, request.port, request.user, request.password, CommonUtils.current()))
      },
      hookOfUpdate = (name: String, request: Update) => {
        validateField(request)
        Future.successful(
          FtpInfo(name, request.hostname, request.port, request.user, request.password, CommonUtils.current()))
      }
    )

  @VisibleForTesting
  private[route] def validateField(request: Creation): Unit = {
    CommonUtils.requireNonEmpty(request.name)
    CommonUtils.requireNonEmpty(request.hostname)
    CommonUtils.requireNonEmpty(request.user)
    CommonUtils.requireNonEmpty(request.password)
    CommonUtils.requireConnectionPort(request.port)
  }

  @VisibleForTesting
  private[route] def validateField(request: Update): Unit = {
    CommonUtils.requireNonEmpty(request.hostname)
    CommonUtils.requireNonEmpty(request.user)
    CommonUtils.requireNonEmpty(request.password)
    CommonUtils.requirePositiveInt(request.port)
  }

}
