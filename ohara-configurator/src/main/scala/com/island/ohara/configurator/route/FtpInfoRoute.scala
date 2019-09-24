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
import com.island.ohara.client.configurator.v0.FtpInfoApi._
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook.{HookOfCreation, HookOfUpdating}
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object FtpInfoRoute {

  private[this] def hookOfCreation: HookOfCreation[Creation, FtpInfo] = (creation: Creation) =>
    Future.successful(
      FtpInfo(
        group = creation.group,
        name = creation.name,
        hostname = creation.hostname,
        port = creation.port,
        user = creation.user,
        password = creation.password,
        lastModified = CommonUtils.current(),
        tags = creation.tags
      ))

  private[this] def HookOfUpdating: HookOfUpdating[Creation, Updating, FtpInfo] =
    (key: ObjectKey, update: Updating, previous: Option[FtpInfo]) =>
      Future.successful(previous.fold {
        FtpInfo(
          group = key.group,
          name = key.name,
          hostname = update.hostname.get,
          port = update.port.get,
          user = update.user.get,
          password = update.password.get,
          lastModified = CommonUtils.current(),
          tags = update.tags.getOrElse(Map.empty)
        )
      } { previous =>
        FtpInfo(
          group = key.group,
          name = key.name,
          hostname = update.hostname.getOrElse(previous.hostname),
          port = update.port.getOrElse(previous.port),
          user = update.user.getOrElse(previous.user),
          password = update.password.getOrElse(previous.password),
          lastModified = CommonUtils.current(),
          tags = update.tags.getOrElse(previous.tags)
        )
      })

  def apply(implicit store: DataStore, executionContext: ExecutionContext): server.Route =
    route[Creation, Updating, FtpInfo](
      root = FTP_PREFIX_PATH,
      hookOfCreation = hookOfCreation,
      HookOfUpdating = HookOfUpdating
    )
}
