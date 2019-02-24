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
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.client.configurator.v0.ObjectApi._
import com.island.ohara.configurator.Configurator.Store
import spray.json.DefaultJsonProtocol._
private[configurator] object ObjectRoute {
  private[this] def toObject(data: Data): Object = Object(
    name = data.name,
    id = data.id,
    lastModified = data.lastModified,
    kind = data.kind
  )
  def apply(implicit store: Store): server.Route = pathPrefix(OBJECT_PREFIX_PATH) {
    pathEnd(get(onSuccess(store.values[Data])(ds => complete(ds.map(toObject))))) ~ path(Segment)(id =>
      get(onSuccess(store.value[Data](id))(d => complete(toObject(d)))))
  }
}
