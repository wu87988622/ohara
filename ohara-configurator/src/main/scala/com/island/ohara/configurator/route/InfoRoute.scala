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
import com.island.ohara.agent.WorkerCollie
import com.island.ohara.client.configurator.v0.InfoApi
import com.island.ohara.client.configurator.v0.InfoApi._
import com.island.ohara.common.data.DataType
import com.island.ohara.common.util.VersionUtils

import scala.concurrent.{ExecutionContext, Future}
object InfoRoute extends SprayJsonSupport {

  private[this] val SUPPORTED_DATABASES = Seq("mysql")

  def apply(implicit workerCollie: WorkerCollie, executionContext: ExecutionContext): server.Route =
    path(INFO_PREFIX_PATH) {
      get {
        import scala.collection.JavaConverters._
        onSuccess(workerCollie.clusters.flatMap { clusters =>
          clusters.size match {
            case 1 => workerCollie.workerClient(clusters.head._1.name).flatMap(_._2.connectors)
            case _ => Future.successful(Seq.empty)
          }
        }) { plugins =>
          complete(
            ConfiguratorInfo(
              connectors = plugins,
              // TODO: remove this (https://github.com/oharastream/ohara/issues/518) by chia
              sources = plugins.map(InfoApi.toConnectorVersion).filter(_.typeName == "source"),
              // TODO: remove this (https://github.com/oharastream/ohara/issues/518) by chia
              sinks = plugins.map(InfoApi.toConnectorVersion).filter(_.typeName == "sink"),
              supportedDatabases = SUPPORTED_DATABASES,
              supportedDataTypes = DataType.all.asScala,
              versionInfo = ConfiguratorVersion(
                version = VersionUtils.VERSION,
                user = VersionUtils.USER,
                revision = VersionUtils.REVISION,
                date = VersionUtils.DATE
              )
            ))
        }
      }
    }
}
