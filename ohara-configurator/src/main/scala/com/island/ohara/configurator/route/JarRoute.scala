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

import java.io.File

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.FileInfo
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorDescription
import com.island.ohara.client.configurator.v0.JarApi._
import com.island.ohara.client.configurator.v0.Parameters
import com.island.ohara.client.configurator.v0.StreamApi.StreamAppDescription
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.configurator.store.DataStore
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
private[configurator] object JarRoute {

  /**
    * Check the specific jar is not used in pipeline.
    * will check same group (worker cluster) only.
    * @param jarInfo the jar
    * @param executionContext execution context
    * @param store data store
    * @return true if the jar is in used, false otherwise.
    */
  private[this] def check(jarInfo: JarInfo)(implicit executionContext: ExecutionContext,
                                            store: DataStore): Future[Boolean] = {
    store
      .raws()
      .map(_.exists {
        case d: StreamAppDescription =>
          if (d.jarInfo.id.equals(jarInfo.id) && d.workerClusterName.equals(jarInfo.group)) true
          else false
        //TODO : does connector need checking the jar is used before deleting ??...by Sam
        case _: ConnectorDescription => false
        // other data type do nothing
        case _ => false
      })
  }

  private[this] def tempDestination(fileInfo: FileInfo): File =
    File.createTempFile(fileInfo.fileName, ".tmp")

  def apply(implicit jarStore: JarStore, dataStore: DataStore, executionContext: ExecutionContext): server.Route =
    pathPrefix(JAR_PREFIX_PATH) {
      withSizeLimit(RouteUtils.DEFAULT_JAR_SIZE_BYTES) {
        //see https://github.com/akka/akka-http/issues/1216#issuecomment-311973943
        toStrictEntity(1.seconds) {
          formFields(Parameters.GROUP_NAME.?) { wkName =>
            storeUploadedFile("jar", tempDestination) {
              case (metadata, file) =>
                complete(if (wkName.isDefined) jarStore.add(file, metadata.fileName, wkName.get)
                else jarStore.add(file, metadata.fileName))
            }
          }
        } ~ path(Segment) { name =>
          parameter(Parameters.GROUP_NAME) { wkName =>
            get(complete(jarStore.jarInfo(wkName, name))) ~ delete(
              complete(
                jarStore
                  .exist(wkName, name)
                  .flatMap {
                    // if jar exists, we do checking is in used or not
                    if (_)
                      jarStore
                        .jarInfo(wkName, name)
                        .flatMap(check(_))
                        .flatMap(exists => {
                          if (exists) throw new RuntimeException(s"Cannot delete jar [$name] which is in used")
                          else jarStore.remove(wkName, name)
                        })
                    // do nothing
                    else Future.successful(false)
                  }
                  .map(_ => StatusCodes.NoContent))
            )
          }
        } ~ pathEnd {
          get {
            parameter(Parameters.GROUP_NAME.?) { wkName =>
              complete(if (wkName.isDefined) jarStore.jarInfos(wkName.get)
              else jarStore.jarInfos())
            }
          }
        }
      }
    }
}
