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
import com.island.ohara.client.configurator.v0.DatabaseApi._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route.RouteUtils.{Id, TargetCluster}

import scala.concurrent.Future

private[configurator] object JdbcInfoRoute {
  def apply(implicit store: Store): server.Route = RouteUtils.basicRoute[JdbcInfoRequest, JdbcInfo](
    root = JDBC_PREFIX_PATH,
    reqToRes = (_: TargetCluster, id: Id, request: JdbcInfoRequest) =>
      Future.successful(JdbcInfo(id, request.name, request.url, request.user, request.password, CommonUtils.current()))
  )
}
