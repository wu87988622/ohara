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

import java.io.File
import java.net.URL
import java.util.Objects

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import com.island.ohara.client.configurator.Data
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsString, JsValue, RootJsonFormat}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
object FileInfoApi {

  /**
    * The default value of group for this API.
    */
  val GROUP_DEFAULT: String = com.island.ohara.client.configurator.v0.GROUP_DEFAULT
  val FILE_PREFIX_PATH: String = "files"

  /**
    * the key used in formData to point out the data.
    */
  val FIELD_NAME: String = "file"
  def toString(tags: Map[String, JsValue]): String = JsObject(tags).toString

  /**
    * parse the input string to json representation for tags
    * @param string input string
    * @return json representation of tags
    */
  def toTags(string: String): Map[String, JsValue] = string.parseJson.asJsObject.fields

  implicit val URL_FORMAT: RootJsonFormat[URL] = new RootJsonFormat[URL] {
    override def read(json: JsValue): URL = new URL(json.asInstanceOf[JsString].value)
    override def write(obj: URL): JsValue = JsString(obj.toString)
  }

  case class Updating(tags: Option[Map[String, JsValue]])
  final implicit val FILE_UPDATING_FORMAT: RootJsonFormat[Updating] = jsonFormat1(Updating)

  /**
    * file information
    * @param name file name
    * @param group group name
    * @param size file size
    * @param url download url
    * @param lastModified last modified time
    */
  final case class FileInfo(group: String,
                            name: String,
                            size: Long,
                            url: URL,
                            lastModified: Long,
                            tags: Map[String, JsValue])
      extends Data {
    override def kind: String = "file"
  }

  implicit val FILE_INFO_JSON_FORMAT: RootJsonFormat[FileInfo] = jsonFormat6(FileInfo)

  sealed trait Request {
    private[this] var group: String = GROUP_DEFAULT
    private[this] var name: String = _
    private[this] var file: File = _
    private[this] var tags: Map[String, JsValue] = _

    @Optional("default group is GROUP_DEFAULT")
    def group(group: String): Request = {
      this.group = CommonUtils.requireNonEmpty(group)
      this
    }

    @Optional("default will use file name")
    def name(name: String): Request = {
      this.name = CommonUtils.requireNonEmpty(name)
      this
    }

    @Optional("This field is useless in updating")
    def file(file: File): Request = {
      this.file = CommonUtils.requireFile(file)
      this
    }

    @Optional("default is empty tags in creating. And default value is null in updating.")
    def tags(tags: Map[String, JsValue]): Request = {
      this.tags = Objects.requireNonNull(tags)
      this
    }

    def upload()(implicit executionContext: ExecutionContext): Future[FileInfo] = doUpload(
      group = CommonUtils.requireNonEmpty(group),
      file = CommonUtils.requireFile(file),
      name = if (name == null) file.getName else name,
      tags = if (tags == null) Map.empty else tags
    )

    def update()(implicit executionContext: ExecutionContext): Future[FileInfo] = doUpdate(
      group = CommonUtils.requireNonEmpty(group),
      name = CommonUtils.requireNonEmpty(name),
      update = Updating(tags = Option(tags))
    )

    protected def doUpload(group: String, name: String, file: File, tags: Map[String, JsValue])(
      implicit executionContext: ExecutionContext): Future[FileInfo]

    protected def doUpdate(group: String, name: String, update: Updating)(
      implicit executionContext: ExecutionContext): Future[FileInfo]
  }

  final class Access private[v0] extends BasicAccess(FILE_PREFIX_PATH) {

    def list()(implicit executionContext: ExecutionContext): Future[Seq[FileInfo]] =
      exec.get[Seq[FileInfo], ErrorApi.Error](url)

    /**
      * get file info mapped to specific name. The group is ${GROUP_DEFAULT} by default.
      * @param key file key
      * @param executionContext thread pool
      * @return file info
      */
    def get(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[FileInfo] =
      exec.get[FileInfo, ErrorApi.Error](url(key))

    /**
      * delete file info mapped to specific name. The group is ${GROUP_DEFAULT} by default.
      * @param key file key
      * @param executionContext thread pool
      * @return file info
      */
    def delete(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Unit] =
      exec.delete[ErrorApi.Error](url(key))

    /**
      * start a progress to upload file to remote Configurator
      * @return request to process upload
      */
    def request: Request = new Request {
      override protected def doUpload(group: String, name: String, file: File, tags: Map[String, JsValue])(
        implicit executionContext: ExecutionContext): Future[FileInfo] =
        Marshal(
          Multipart.FormData(
            // add file
            Multipart.FormData.BodyPart(FIELD_NAME,
                                        HttpEntity.fromFile(MediaTypes.`application/octet-stream`, file),
                                        Map("filename" -> name)),
            // add group
            Multipart.FormData.BodyPart(GROUP_KEY, group),
            // add tags
            Multipart.FormData.BodyPart(TAGS_KEY, FileInfoApi.toString(tags))
          ))
          .to[RequestEntity]
          .map(e => HttpRequest(HttpMethods.POST, uri = url, entity = e))
          .flatMap(exec.request[FileInfo, ErrorApi.Error])

      override protected def doUpdate(group: String, name: String, update: Updating)(
        implicit executionContext: ExecutionContext): Future[FileInfo] =
        exec.put[Updating, FileInfo, ErrorApi.Error](url(ObjectKey.of(group, name)), update)
    }
  }

  def access: Access = new Access
}
