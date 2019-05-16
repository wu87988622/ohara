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
import com.island.ohara.client.configurator.v0.JarApi
import com.island.ohara.client.configurator.v0.JarApi._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.jar.JarStore
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
private[configurator] object JarsRoute {

  def tempDestination(fileInfo: FileInfo): File =
    File.createTempFile(fileInfo.fileName, ".tmp")

  def pathToJar(id: String): String = s"${JarApi.DOWNLOAD_JAR_PREFIX_PATH}/${CommonUtils.requireNonEmpty(id)}.jar"

  def apply(implicit jarStore: JarStore, executionContext: ExecutionContext): server.Route =
    pathPrefix(JAR_PREFIX_PATH) {
      storeUploadedFile("jar", tempDestination) {
        case (metadata, file) =>
          complete(jarStore.add(file, metadata.fileName))
      } ~ get(complete(jarStore.jarInfos)) ~ path(Segment) { id =>
        delete(complete(jarStore.remove(id).map(_ => StatusCodes.NoContent))) ~ get(complete(jarStore.jarInfo(id)))
      }
    } ~ pathPrefix(DOWNLOAD_JAR_PREFIX_PATH / Segment) { idWithExtension =>
      // We force all url end with .jar
      if (!idWithExtension.endsWith(".jar")) complete(StatusCodes.NotFound -> s"$idWithExtension doesn't exist")
      else {
        val id = idWithExtension.substring(0, idWithExtension.indexOf(".jar"))
        // TODO: how to use future in getFromFile???
        getFromFile(Await.result(jarStore.toFile(id), 30 seconds))
      }
    }
}
