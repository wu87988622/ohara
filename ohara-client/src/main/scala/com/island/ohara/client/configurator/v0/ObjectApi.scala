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

object ObjectApi {
  val OBJECT_PREFIX_PATH: String = "objects"

  final case class Object(id: String, name: String, lastModified: Long, kind: String) extends Data
  implicit val OBJECT_JSON_FORMAT: RootJsonFormat[Object] = jsonFormat4(Object)

  class Access private[v0] extends BasicAccess(OBJECT_PREFIX_PATH) {
    def get(id: String)(implicit executionContext: ExecutionContext): Future[Object] =
      exec.get[Object, ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
    def list(implicit executionContext: ExecutionContext): Future[Seq[Object]] =
      exec.get[Seq[Object], ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")
  }

  def access(): Access = new Access
}
