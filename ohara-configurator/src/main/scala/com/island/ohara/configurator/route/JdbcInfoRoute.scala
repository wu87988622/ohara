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
import com.island.ohara.client.configurator.v0.JdbcInfoApi._
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook.{HookOfCreation, HookOfUpdating}
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object JdbcInfoRoute {

  private[this] def hookOfCreation: HookOfCreation[Creation, JdbcInfo] = (creation: Creation) =>
    Future.successful(
      JdbcInfo(
        group = creation.group,
        name = creation.name,
        url = creation.url,
        user = creation.user,
        password = creation.password,
        lastModified = CommonUtils.current(),
        tags = creation.tags
      ))

  private[this] def hookOfUpdating: HookOfUpdating[Updating, JdbcInfo] =
    (key: ObjectKey, update: Updating, previous: Option[JdbcInfo]) =>
      Future.successful {
        previous.fold {
          JdbcInfo(
            group = key.group,
            name = key.name,
            url = update.url.get,
            user = update.user.get,
            password = update.password.get,
            CommonUtils.current(),
            tags = update.tags.getOrElse(Map.empty)
          )
        } { previous =>
          JdbcInfo(
            group = key.group,
            name = key.name,
            url = update.url.getOrElse(previous.url),
            user = update.user.getOrElse(previous.user),
            password = update.password.getOrElse(previous.password),
            CommonUtils.current(),
            tags = update.tags.getOrElse(previous.tags)
          )
        }
    }

  def apply(implicit store: DataStore, executionContext: ExecutionContext): server.Route =
    RouteBuilder[Creation, Updating, JdbcInfo]()
      .root(JDBC_PREFIX_PATH)
      .hookOfCreation(hookOfCreation)
      .hookOfUpdating(hookOfUpdating)
      .build()
}
