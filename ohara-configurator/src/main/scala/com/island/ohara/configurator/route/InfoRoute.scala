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
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import com.island.ohara.client.configurator.v0.InfoApi._
import com.island.ohara.common.util.VersionUtils
import com.island.ohara.configurator.Configurator.Mode
object InfoRoute extends SprayJsonSupport {

  def apply(mode: Mode): server.Route =
    path(INFO_PREFIX_PATH) {
      get {
        complete(
          ConfiguratorInfo(
            versionInfo = ConfiguratorVersion(
              version = VersionUtils.VERSION,
              branch = VersionUtils.BRANCH,
              user = VersionUtils.USER,
              revision = VersionUtils.REVISION,
              date = VersionUtils.DATE
            ),
            mode = mode.toString
          ))
      }
    }
}
