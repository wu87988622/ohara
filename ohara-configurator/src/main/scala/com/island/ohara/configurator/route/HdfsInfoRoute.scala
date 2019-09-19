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
import com.island.ohara.client.configurator.v0.HdfsInfoApi._
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook.{HookOfCreation, HookOfGroup, HookOfUpdating}
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object HdfsInfoRoute {

  private[this] def hookOfCreation: HookOfCreation[Creation, HdfsInfo] = (creation: Creation) =>
    Future.successful(
      HdfsInfo(group = creation.group,
               name = creation.name,
               uri = creation.uri,
               lastModified = CommonUtils.current(),
               tags = creation.tags))

  private[this] def HookOfUpdating: HookOfUpdating[Creation, Updating, HdfsInfo] =
    (key: ObjectKey, update: Updating, previous: Option[HdfsInfo]) =>
      Future.successful {
        previous.fold {
          if (update.uri.isEmpty) throw new IllegalArgumentException(errorMessage(key, "uri"))
          HdfsInfo(
            group = key.group,
            name = key.name,
            uri = update.uri.get,
            lastModified = CommonUtils.current(),
            tags = update.tags.getOrElse(Map.empty)
          )
        } { previous =>
          HdfsInfo(
            group = key.group,
            name = key.name,
            uri = update.uri.getOrElse(previous.uri),
            lastModified = CommonUtils.current(),
            tags = update.tags.getOrElse(previous.tags)
          )
        }
    }

  private[this] def hookOfGroup: HookOfGroup = _.getOrElse(GROUP_DEFAULT)

  def apply(implicit store: DataStore, executionContext: ExecutionContext): server.Route =
    route[Creation, Updating, HdfsInfo](
      root = HDFS_PREFIX_PATH,
      hookOfGroup = hookOfGroup,
      hookOfCreation = hookOfCreation,
      HookOfUpdating = HookOfUpdating
    )
}
