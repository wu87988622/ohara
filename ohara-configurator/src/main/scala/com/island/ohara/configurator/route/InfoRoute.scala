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
import akka.http.scaladsl.server.Directives.{complete, get, path}
import com.island.ohara.client.WorkerClient
import com.island.ohara.client.ConnectorJson.Plugin
import com.island.ohara.client.configurator.v0.InfoApi._
import com.island.ohara.common.data.DataType
import com.island.ohara.common.util.VersionUtil
import akka.http.scaladsl.server.Directives._
import com.island.ohara.kafka.BrokerClient
object InfoRoute extends SprayJsonSupport {

  private[this] val SUPPORTED_DATABASES = Seq("mysql")

  def apply(implicit brokerClient: BrokerClient, workerClient: WorkerClient): server.Route =
    // TODO: OHARA-1212 should remove "cluster" ... by chia
    path(INFO_PREFIX_PATH | "cluster") {
      get {
        val plugins = workerClient.plugins()

        def toConnectorInfo(plugin: Plugin): ConnectorVersion = {
          val (version, revision) = try {
            // see com.island.ohara.kafka.connection.Version for the format from "kafka's version"
            val index = plugin.version.lastIndexOf("_")
            if (index < 0 || index >= plugin.version.length - 1) (plugin.version, "unknown")
            else (plugin.version.substring(0, index), plugin.version.substring(index + 1))
          } catch {
            case _: Throwable => (plugin.version, "unknown")
          }
          ConnectorVersion(plugin.className, version, revision)
        }
        import scala.collection.JavaConverters._
        complete(
          ConfiguratorInfo(
            brokerClient.brokers,
            workerClient.workers,
            plugins.filter(_.typeName.toLowerCase == "source").map(toConnectorInfo),
            plugins.filter(_.typeName.toLowerCase == "sink").map(toConnectorInfo),
            SUPPORTED_DATABASES,
            DataType.all.asScala,
            ConfiguratorVersion(
              version = VersionUtil.VERSION,
              user = VersionUtil.USER,
              revision = VersionUtil.REVISION,
              date = VersionUtil.DATE
            )
          ))
      }
    }
}
