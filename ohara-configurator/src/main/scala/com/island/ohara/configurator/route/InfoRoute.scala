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
import com.island.ohara.agent.StreamCollie
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.InfoApi._
import com.island.ohara.client.configurator.v0.{BrokerApi, StreamApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.VersionUtils
import com.island.ohara.configurator.Configurator.Mode
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.ExecutionContext
object InfoRoute extends SprayJsonSupport {

  private[this] def routeToConfiguratorInf(mode: Mode) = get {
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

  def apply(mode: Mode)(implicit dataStore: DataStore,
                        streamCollie: StreamCollie,
                        executionContext: ExecutionContext): server.Route =
    pathPrefix(INFO_PREFIX_PATH) {
      path(STREAM_PREFIX_PATH / Segment) { fileName =>
        parameter(GROUP_KEY ?) { groupOption =>
          complete(
            dataStore
              .value[FileInfo](ObjectKey.of(groupOption.getOrElse(GROUP_DEFAULT), fileName))
              .map(_.url)
              .flatMap(streamCollie.loadDefinition)
              .map { definition =>
                ServiceDefinition(
                  imageName = StreamApi.IMAGE_NAME_DEFAULT,
                  settingDefinitions = definition.settingDefinitions
                )
              })
        }
      } ~ path(ZOOKEEPER_PREFIX_PATH) {
        get {
          complete(
            ServiceDefinition(
              imageName = ZookeeperApi.IMAGE_NAME_DEFAULT,
              settingDefinitions = ZookeeperApi.DEFINITIONS
            ))
        }
      } ~ path(BROKER_PREFIX_PATH) {
        get {
          complete(
            ServiceDefinition(
              imageName = BrokerApi.IMAGE_NAME_DEFAULT,
              settingDefinitions = BrokerApi.DEFINITIONS
            ))
        }
      } ~ path(WORKER_PREFIX_PATH) {
        get {
          complete(
            ServiceDefinition(
              imageName = WorkerApi.IMAGE_NAME_DEFAULT,
              settingDefinitions = WorkerApi.DEFINITIONS
            ))
        }
      } ~ path(CONFIGURATOR_PREFIX_PATH) {
        routeToConfiguratorInf(mode)
      } ~ pathEnd {
        // TODO: remove this route (see https://github.com/oharastream/ohara/issues/3093)
        routeToConfiguratorInf(mode)
      }
    }
}
