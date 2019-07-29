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
import com.island.ohara.client.configurator.v0.HadoopApi._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.RouteUtils.{HookOfCreation, HookOfUpdate}
import com.island.ohara.configurator.store.DataStore
import com.island.ohara.kafka.connector.json.ObjectKey

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object HdfsInfoRoute {

  private[this] def hookOfCreation: HookOfCreation[Creation, HdfsInfo] = (group: String, request: Creation) =>
    Future.successful(
      HdfsInfo(group = group,
               name = request.name,
               uri = request.uri,
               lastModified = CommonUtils.current(),
               tags = request.tags))

  private[this] def hookOfUpdate: HookOfUpdate[Creation, Update, HdfsInfo] =
    (key: ObjectKey, update: Update, previous: Option[HdfsInfo]) =>
      Future.successful {
        previous.fold {
          if (update.uri.isEmpty) throw new IllegalArgumentException(RouteUtils.errorMessage(key, "uri"))
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

  def apply(implicit store: DataStore, executionContext: ExecutionContext): server.Route =
    RouteUtils.route[Creation, Update, HdfsInfo](
      root = HDFS_PREFIX_PATH,
      enableGroup = true,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate
    )
}
