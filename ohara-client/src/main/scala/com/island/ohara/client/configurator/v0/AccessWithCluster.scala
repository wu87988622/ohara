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

package com.island.ohara.client.configurator.v0

import spray.json.RootJsonFormat

import scala.concurrent.Future

/**
  * Some APIs can specify target cluster so this class adds a variant "add" to carry the target cluster name.
  * @param prefixPath path to data
  * @param rm0 formatter of request
  * @param rm1 formatter of response
  * @tparam Req type of request
  * @tparam Res type of Response
  */
class AccessWithCluster[Req, Res] private[v0] (prefixPath: String)(implicit rm0: RootJsonFormat[Req],
                                                                   rm1: RootJsonFormat[Res])
    extends Access(prefixPath)(rm0, rm1) {
  def add(targetCluster: String, request: Req): Future[Res] =
    exec.post[Req, Res, ErrorApi.Error](
      Parameters.appendTargetCluster(s"http://${_hostname}:${_port}/${_version}/${_prefixPath}", targetCluster),
      request)
}
