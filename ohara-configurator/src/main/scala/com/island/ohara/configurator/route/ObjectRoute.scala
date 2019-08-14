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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.configurator.Data
import com.island.ohara.client.configurator.v0.ObjectApi._
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.configurator.store.DataStore
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext
private[configurator] object ObjectRoute {
  private[this] def toObject(data: Data): Object = Object(
    group = data.group,
    name = data.name,
    lastModified = data.lastModified,
    kind = data.kind,
    tags = data.tags
  )
  def apply(implicit store: DataStore, executionContext: ExecutionContext): server.Route =
    pathPrefix(OBJECT_PREFIX_PATH) {
      pathEnd(get(complete(store.raws().map(_.map(toObject))))) ~ path(Segment) { name =>
        parameter(GROUP_KEY ?) { groupOption =>
          val group = groupOption.getOrElse(GROUP_DEFAULT)
          get(complete(store.raws(ObjectKey.of(group, name)).map(_.map(toObject))))
        }
      }
    }
}
