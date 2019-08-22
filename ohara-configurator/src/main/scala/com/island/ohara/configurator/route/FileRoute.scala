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
import com.island.ohara.client.configurator.Data
import com.island.ohara.client.configurator.v0.FileInfoApi
import com.island.ohara.client.configurator.v0.FileInfoApi._
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.configurator.file.FileStore
import com.island.ohara.configurator.store.DataStore
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
private[configurator] object FileRoute {

  /**
    * find the objects accessing the input file.
    * @param fileInfo the file
    * @param executionContext execution context
    * @param store data store
    * @return list of objects
    */
  private[this] def accessBy(fileInfo: FileInfo)(implicit executionContext: ExecutionContext,
                                                 store: DataStore): Future[Seq[Data]] =
    store
      .raws()
      .map(_.filter {
        case info: StreamClusterInfo => info.jarKey == fileInfo.key
        case info: WorkerClusterInfo => info.jarInfos.map(_.key).contains(fileInfo.key)
        // other data type do nothing
        case _ => false
      })

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
        parameter(GROUP_KEY ?) { groupOption =>
          val key = ObjectKey.of(groupOption.getOrElse(GROUP_DEFAULT), name)
          get(complete(fileStore.fileInfo(key))) ~ delete(
            complete(fileStore
              .exist(key)
              .flatMap {
                // if jar exists, we do checking is in used or not
                if (_)
                  fileStore
                    .fileInfo(key)
                    .flatMap(accessBy(_))
                    .flatMap(objects => {
                      if (objects.nonEmpty)
                        throw new RuntimeException(
                          s"Cannot delete jar [$name] since it is used by ${objects.map(o => s"${o.kind}:${o.name}").mkString(",")}")
                      else fileStore.remove(key)
                    })
                // do nothing
                else Future.successful(false)
              }
              .map(_ => StatusCodes.NoContent))
          ) ~ put {
            // update the tags for an existent file
            entity(as[Update]) { update =>
              val fileInfo: Future[FileInfo] =
                update.tags.map(fileStore.updateTags(key, _)).getOrElse(fileStore.fileInfo(key))
              complete(fileInfo)
            }
          }
        }
      } ~ pathEnd {
        withSizeLimit(DEFAULT_FILE_SIZE_BYTES) {
          //see https://github.com/akka/akka-http/issues/1216#issuecomment-311973943
          toStrictEntity(1.seconds) {
            formFields((GROUP_KEY ?, TAGS_KEY ?)) {
              case (group, tagsString) =>
                storeUploadedFile(fieldName, tempDestination) {
                  case (metadata, file) =>
                    complete(
                      fileStore.fileInfoCreator
                        .group(group.getOrElse(GROUP_DEFAULT))
                        .name(metadata.fileName)
                        .file(file)
                        .tags(tagsString.map(FileInfoApi.toTags).getOrElse(Map.empty))
                        .threadPool(executionContext)
                        .create())
                }
            }
          }
        } ~ get {
          parameter(GROUP_KEY ?) { groupOption =>
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
