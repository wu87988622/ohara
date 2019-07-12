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
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object HdfsInfoRoute {
  def apply(implicit store: DataStore, executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRoute[Creation, Update, HdfsInfo](
      root = HDFS_PREFIX_PATH,
      hookOfCreation = (request: Creation) =>
        Future.successful(
          HdfsInfo(name = request.name, uri = request.uri, lastModified = CommonUtils.current(), request.tags)),
      hookOfUpdate = (name: String, request: Update, previousOption: Option[HdfsInfo]) =>
        Future.successful {
          previousOption.fold {
            if (request.uri.isEmpty) throw new IllegalArgumentException(RouteUtils.errorMessage(name, "uri"))
            HdfsInfo(
              name = name,
              uri = request.uri.get,
              lastModified = CommonUtils.current(),
              tags = request.tags.getOrElse(Map.empty)
            )
          } { previous =>
            HdfsInfo(
              name = name,
              uri = request.uri.getOrElse(previous.uri),
              lastModified = CommonUtils.current(),
              tags = request.tags.getOrElse(previous.tags)
            )
          }
      }
    )
}
