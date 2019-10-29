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
import com.island.ohara.common.setting.{ObjectKey, SettingDef}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

object InfoApi {
  val INFO_PREFIX_PATH: String = "info"

  val CONFIGURATOR_PREFIX_PATH: String = "configurator"
  val ZOOKEEPER_PREFIX_PATH: String = "zookeeper"
  val BROKER_PREFIX_PATH: String = "broker"
  val WORKER_PREFIX_PATH: String = "worker"
  val STREAM_PREFIX_PATH: String = "stream"

  final case class ConfiguratorVersion(version: String, branch: String, user: String, revision: String, date: String)
  implicit val CONFIGURATOR_VERSION_JSON_FORMAT: RootJsonFormat[ConfiguratorVersion] = jsonFormat5(ConfiguratorVersion)

  final case class ConfiguratorInfo(versionInfo: ConfiguratorVersion, mode: String)

  implicit val CONFIGURATOR_INFO_JSON_FORMAT: RootJsonFormat[ConfiguratorInfo] = jsonFormat2(ConfiguratorInfo)

  case class ServiceDefinition(imageName: String, settingDefinitions: Seq[SettingDef])

  implicit val SERVICE_DEFINITION_FORMAT: RootJsonFormat[ServiceDefinition] = jsonFormat2(ServiceDefinition)

  class Access private[v0] extends BasicAccess(INFO_PREFIX_PATH) {
    def configuratorInfo()(implicit executionContext: ExecutionContext): Future[ConfiguratorInfo] =
      exec.get[ConfiguratorInfo, ErrorApi.Error](s"$url/$CONFIGURATOR_PREFIX_PATH")

    def zookeeperInfo()(implicit executionContext: ExecutionContext): Future[ServiceDefinition] =
      exec.get[ServiceDefinition, ErrorApi.Error](s"$url/$ZOOKEEPER_PREFIX_PATH")

    def brokerInfo()(implicit executionContext: ExecutionContext): Future[ServiceDefinition] =
      exec.get[ServiceDefinition, ErrorApi.Error](s"$url/$BROKER_PREFIX_PATH")

    def workerInfo()(implicit executionContext: ExecutionContext): Future[ServiceDefinition] =
      exec.get[ServiceDefinition, ErrorApi.Error](s"$url/$WORKER_PREFIX_PATH")

    def streamInfo(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[ServiceDefinition] =
      exec.get[ServiceDefinition, ErrorApi.Error](s"$url/$STREAM_PREFIX_PATH/${key.name()}?$GROUP_KEY=${key.group()}")
  }

  def access: Access = new Access
}
