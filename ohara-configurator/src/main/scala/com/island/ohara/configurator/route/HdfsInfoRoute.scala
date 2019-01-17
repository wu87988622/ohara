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
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store

private[configurator] object HdfsInfoRoute {
  def apply(implicit store: Store): server.Route = RouteUtil.basicRoute[HdfsInfoRequest, HdfsInfo](
    root = HDFS_PREFIX_PATH,
    reqToRes = (id: String, request: HdfsInfoRequest) => HdfsInfo(id, request.name, request.uri, CommonUtil.current())
  )
}
