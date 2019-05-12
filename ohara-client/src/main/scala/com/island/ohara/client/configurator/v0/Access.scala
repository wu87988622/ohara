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
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

/**
  * A general class used to access data of configurator. The protocol is based on http (restful APIs), and this implementation is built by akka
  * http. All data in ohara have same APIs so we extract this layer to make our life easily.
  * @param prefixPath path to data
  * @param rm0 formatter of request
  * @param rm1 formatter of response
  * @tparam Req type of request
  * @tparam Res type of Response
  */
class Access[Req, Res] private[v0] (prefixPath: String)(implicit rm0: RootJsonFormat[Req], rm1: RootJsonFormat[Res])
    extends BasicAccess(prefixPath) {
  def get(name: String)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.get[Res, ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$name")
  def delete(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$name")
  def list(implicit executionContext: ExecutionContext): Future[Seq[Res]] =
    exec.get[Seq[Res], ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")
  def add(request: Req)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.post[Req, Res, ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}", request)
  def update(name: String, request: Req)(implicit executionContext: ExecutionContext): Future[Res] =
    exec.put[Req, Res, ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$name", request)
}
