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
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorDescription
import com.island.ohara.client.configurator.v0.FileApi._
import com.island.ohara.client.configurator.v0.StreamApi.StreamAppDescription
import com.island.ohara.client.configurator.v0.{Data, FileApi, Parameters}
import com.island.ohara.configurator.file.FileStore
import com.island.ohara.configurator.store.DataStore
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
private[configurator] object FileRoute {

  private[this] val DEFAULT_GROUP = Data.DEFAULT_GROUP

  /**
    * Check the specific jar is not used in pipeline.
    * will check same group (worker cluster) only.
    * @param fileInfo the file
    * @param executionContext execution context
    * @param store data store
    * @return true if the jar is in used, false otherwise.
    */
  private[this] def check(fileInfo: FileInfo)(implicit executionContext: ExecutionContext,
                                              store: DataStore): Future[Boolean] = {
    store
      .raws()
      .map(_.exists {
        case streamAppDesc: StreamAppDescription =>
          streamAppDesc.jar.group == fileInfo.group && streamAppDesc.jar.name == fileInfo.name
        //TODO : does connector need checking the jar is used before deleting ??...by Sam
        case _: ConnectorDescription => false
        // other data type do nothing
        case _ => false
      })
  }

  private[this] def tempDestination(fileInfo: akka.http.scaladsl.server.directives.FileInfo): File =
    File.createTempFile(fileInfo.fileName, ".tmp")

  /**
    * generate the route for file route.
    * This method enables us to switch the root name and field name in uploading file. In https://github.com/oharastream/ohara/issues/1711 we change
    * the "jars" to "files" and "jar" to "file". For compatibility, we generate two routes - first route is for new APIs and another is for stale APIs.
    * @param root root of url
    * @param fieldName field name indicating the new new for uploaded file
    * @param fileStore file store
    * @param dataStore data store
    * @param executionContext thread pool
    * @return route
    */
  private[this] def route(root: String, fieldName: String)(implicit fileStore: FileStore,
                                                           dataStore: DataStore,
                                                           executionContext: ExecutionContext): server.Route =
    pathPrefix(root) {
      path(Segment) { name =>
        parameter(Parameters.GROUP_NAME.?) { groupOption =>
          get(complete(fileStore.fileInfo(groupOption.getOrElse(DEFAULT_GROUP), name))) ~ delete(
            complete(
              fileStore
                .exist(groupOption.getOrElse(DEFAULT_GROUP), name)
                .flatMap {
                  // if jar exists, we do checking is in used or not
                  if (_)
                    fileStore
                      .fileInfo(groupOption.getOrElse(DEFAULT_GROUP), name)
                      .flatMap(check(_))
                      .flatMap(exists => {
                        if (exists) throw new RuntimeException(s"Cannot delete jar [$name] which is in used")
                        else fileStore.remove(groupOption.getOrElse(DEFAULT_GROUP), name)
                      })
                  // do nothing
                  else Future.successful(false)
                }
                .map(_ => StatusCodes.NoContent))
          ) ~ put {
            // update the tags for an existent file
            entity(as[Update]) { update =>
              parameter(Parameters.GROUP_NAME.?) { groupOption =>
                val fileInfo: Future[FileInfo] = update.tags
                  .map(fileStore.updateTags(groupOption.getOrElse(DEFAULT_GROUP), name, _))
                  .getOrElse(fileStore.fileInfo(groupOption.getOrElse(DEFAULT_GROUP), name))
                complete(fileInfo)
              }
            }
          }
        }
      } ~ pathEnd {
        withSizeLimit(RouteUtils.DEFAULT_FILE_SIZE_BYTES) {
          //see https://github.com/akka/akka-http/issues/1216#issuecomment-311973943
          toStrictEntity(1.seconds) {
            formFields((Parameters.GROUP_NAME.?, Parameters.TAGS_NAME.?)) {
              case (group, tagsString) =>
                storeUploadedFile(fieldName, tempDestination) {
                  case (metadata, file) =>
                    complete(
                      fileStore.fileInfoCreator
                        .group(group.getOrElse(DEFAULT_GROUP))
                        .name(metadata.fileName)
                        .file(file)
                        .tags(tagsString.map(FileApi.toTags).getOrElse(Map.empty))
                        .threadPool(executionContext)
                        .create())
                }
            }
          }
        } ~ get {
          parameter(Parameters.GROUP_NAME.?) { groupOption =>
            val fileInfos: Future[Seq[FileInfo]] = groupOption.map(fileStore.fileInfos).getOrElse(fileStore.fileInfos())
            complete(fileInfos)
          }
        }
      }
    }

  def apply(implicit fileStore: FileStore, dataStore: DataStore, executionContext: ExecutionContext): server.Route =
    // TODO: remove the stale "jars" and "jar"
    route(root = FILE_PREFIX_PATH, fieldName = "file") ~ route(root = "jars", fieldName = "jar")
}
