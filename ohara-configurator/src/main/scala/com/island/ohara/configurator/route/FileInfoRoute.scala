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

import java.net.URL
import java.nio.file.Files

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.ContentTypeResolver
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.Materializer
import akka.util.ByteString
import com.island.ohara.agent.ServiceCollie
import com.island.ohara.client.configurator.v0.FileInfoApi._
import com.island.ohara.client.configurator.v0.{BasicCreation, JsonRefiner, OharaJsonFormat}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook.{HookBeforeDelete, HookOfUpdating}
import com.island.ohara.configurator.store.DataStore
import com.typesafe.scalalogging.Logger
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
private[configurator] object FileInfoRoute {

  private[this] val LOG = Logger(FileInfoRoute.getClass)

  private[this] def hookBeforeDelete(implicit objectChecker: ObjectChecker,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: ObjectKey) =>
    objectChecker.checkList
      .allStreamApps()
      .allWorkers()
      .check()
      .map(report => (report.workerClusterInfos.keys, report.streamClusterInfos.keys))
      .map {
        case (workerClusterInfos, streamClusterInfos) =>
          workerClusterInfos.foreach { workerClusterInfo =>
            if (workerClusterInfo.pluginKeys.contains(key))
              throw new IllegalArgumentException(s"file:$key is used by worker cluster:${workerClusterInfo.key}")
          }
          streamClusterInfos.foreach { streamClusterInfo =>
            if (streamClusterInfo.jarKey == key)
              throw new IllegalArgumentException(s"file:$key is used by stream cluster:${streamClusterInfo.key}")
          }
    }

  /**
    * This is a specific prefix which enables user to download binary of file
    */
  private[this] val DOWNLOAD_FILE_PREFIX_PATH: String = "downloadFiles"

  def routeToDownload(implicit store: DataStore,
                      executionContext: ExecutionContext,
                      resolver: ContentTypeResolver): Route =
    pathPrefix(DOWNLOAD_FILE_PREFIX_PATH / Segment / Segment) {
      case (group, name) =>
        complete(store.value[FileInfo](ObjectKey.of(group, name)).map { fileInfo =>
          HttpResponse(
            entity = HttpEntity.Strict(contentType = resolver(fileInfo.name), data = ByteString(fileInfo.bytes)))
        })
    }

  /**
    * we assume the tags in query parameter is written to tags={"a":"b", "b":111 ...}
    */
  private[this] val tagsUnmarshaller = new Unmarshaller[String, JsObject] {
    override def apply(value: String)(implicit ec: ExecutionContext, materializer: Materializer): Future[JsObject] =
      Future.successful(value.parseJson.asJsObject)
  }

  private[this] def customPost(hostname: String, port: Int, version: String)(
    implicit store: DataStore,
    serviceCollie: ServiceCollie,
    executionContext: ExecutionContext): () => Route = () =>
    withSizeLimit(DEFAULT_FILE_SIZE_BYTES) {
      // We need to convert the request entity to strict entity in order to fetch the "form fields",
      // The timeout here used seconds by the formula (for a worse case):
      // timeout = DEFAULT_FILE_SIZE_BYTES(50MB) / 10Mbps upload = 40 seconds
      //see https://github.com/akka/akka-http/issues/1216#issuecomment-311973943
      toStrictEntity(40.seconds) {
        formFields((GROUP_KEY ? GROUP_DEFAULT, TAGS_KEY.as(tagsUnmarshaller) ? JsObject.empty)) {
          case (group, tags) =>
            storeUploadedFile(FIELD_NAME, fileInfo => CommonUtils.createTempFile(fileInfo.getFileName, ".jar")) {
              case (metadata, file) =>
                val name = metadata.fileName
                val key = ObjectKey.of(group, name)
                complete(store.exist[FileInfo](key).flatMap {
                  if (_) throw new IllegalArgumentException(s"file:$key exists!!!")
                  else {
                    serviceCollie
                      .fileContent(Seq(file.toURI.toURL))
                      .map(_.classInfos)
                      .recover {
                        case e: Throwable if !name.endsWith(".jar") =>
                          LOG.debug(s"the file:$name seems not a jar file", e)
                          Seq.empty
                      }
                      .map { classInfos =>
                        new FileInfo(
                          group = group,
                          name = name,
                          url = new URL(s"http://$hostname:$port/$version/$DOWNLOAD_FILE_PREFIX_PATH/$group/$name"),
                          lastModified = CommonUtils.current(),
                          bytes = try Files.readAllBytes(file.toPath)
                          finally file.delete(),
                          classInfos = classInfos,
                          tags = tags.fields
                        )
                      }
                      .flatMap(store.add)
                  }
                })
            }
        }
      }
  }

  private[this] def hookOfUpdating: HookOfUpdating[Updating, FileInfo] =
    (_: ObjectKey, updating: Updating, previousOption: Option[FileInfo]) =>
      previousOption match {
        case None => throw new IllegalArgumentException(s"Updating an nonexistent file is not allowed")
        case Some(previous) =>
          Future.successful(
            new FileInfo(
              group = previous.group,
              name = previous.name,
              url = previous.url,
              lastModified = CommonUtils.current(),
              bytes = previous.bytes,
              classInfos = previous.classInfos,
              tags = updating.tags.getOrElse(previous.tags)
            ))
    }

  /**
    * FileInfo route does not use Creation so there is no creation in FileInfo APIs.
    * However, our route builder requires us to define a creation. Putting fake stuff is ok since we don't use
    * default route of creation. We have defined a custom route to replace the default one.
    */
  private[this] class FakeCreation extends BasicCreation {
    override def group: String = throw new UnsupportedOperationException
    override def name: String = throw new UnsupportedOperationException
    override def tags: Map[String, JsValue] = throw new UnsupportedOperationException
  }

  private[this] implicit val FAKE_FORMAT: OharaJsonFormat[FakeCreation] = JsonRefiner[FakeCreation]
    .format(new RootJsonFormat[FakeCreation] {
      override def read(json: JsValue): FakeCreation = throw new UnsupportedOperationException
      override def write(obj: FakeCreation): JsValue = throw new UnsupportedOperationException
    })
    .refine

  /**
    * @param version the version is a part of generated URL. This is important stuff since we may reject the deprecated URL in the future.
    */
  def apply(hostname: String, port: Int, version: String)(implicit store: DataStore,
                                                          serviceCollie: ServiceCollie,
                                                          objectChecker: ObjectChecker,
                                                          executionContext: ExecutionContext): server.Route =
    RouteBuilder[FakeCreation, Updating, FileInfo]()
      .root(FILE_PREFIX_PATH)
      .customPost(customPost(hostname, port, version))
      .hookOfUpdating(hookOfUpdating)
      .hookBeforeDelete(hookBeforeDelete)
      .build() ~ routeToDownload

}
