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

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}
object JarApi {
  val JAR_PREFIX_PATH: String = "jars"

  implicit val URL_FORMAT: RootJsonFormat[URL] = new RootJsonFormat[URL] {
    override def read(json: JsValue): URL = new URL(json.asInstanceOf[JsString].value)
    override def write(obj: URL): JsValue = JsString(obj.toString)
  }

  /**
    * jar information
    * @param name jar file name
    * @param group group name
    * @param size file size
    * @param url download url
    * @param lastModified last modified time
    */
  final case class JarInfo(name: String, group: String, size: Long, url: URL, lastModified: Long) extends Data {
    override def kind: String = "jar"
  }

  implicit val JAR_INFO_JSON_FORMAT: RootJsonFormat[JarInfo] = jsonFormat5(JarInfo)

  // this class used to identify jar "primary key"
  final case class JarKey(group: String, name: String)
  implicit val JAR_KEY_JSON_FORMAT: RootJsonFormat[JarKey] =
    JsonRefiner[JarKey].format(jsonFormat2(JarKey)).rejectEmptyString().refine

  trait Request {
    @Optional("default will use file name")
    def newName(newName: String): Request
    @Optional("default will use random string")
    def group(group: String): Request

    // TODO this is a temporary solution for introducing group in jar api
    // TODO please refactor this part after we design the best solution for "group" in ohara...by Sam
    def upload(file: File)(implicit executionContext: ExecutionContext): Future[JarInfo]
    def get(name: String)(implicit executionContext: ExecutionContext): Future[JarInfo]
    def delete(name: String)(implicit executionContext: ExecutionContext): Future[Unit]
    def list()(implicit executionContext: ExecutionContext): Future[Seq[JarInfo]]

  }

  final class Access private[v0] extends BasicAccess(JAR_PREFIX_PATH) {
    def request: Request = new Request {
      private[this] var newName: Option[String] = None
      private[this] var group: Option[String] = None

      override def newName(newName: String): Request = {
        this.newName = Some(CommonUtils.requireNonEmpty(newName))
        this
      }
      override def group(group: String): Request = {
        this.group = Some(CommonUtils.requireNonEmpty(group))
        this
      }

      override def get(name: String)(implicit executionContext: ExecutionContext): Future[JarInfo] =
        exec.get[JarInfo, ErrorApi.Error](
          Parameters.appendTargetGroup(
            s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$name",
            // TODO : for 0.6, GET method should pass the group to filter ; remove this after refactor
            CommonUtils.requireNonEmpty(group.getOrElse(""))
          ))

      override def delete(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
        exec.delete[ErrorApi.Error](
          Parameters.appendTargetGroup(
            s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$name",
            // TODO : for 0.6, DELETE method should pass the group to filter ; remove this after refactor
            CommonUtils.requireNonEmpty(group.getOrElse(""))
          ))

      override def list()(implicit executionContext: ExecutionContext): Future[Seq[JarInfo]] =
        exec.get[Seq[JarInfo], ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")

      override def upload(file: File)(implicit executionContext: ExecutionContext): Future[JarInfo] = {
        request(s"http://${_hostname}:${_port}/${_version}/${_prefixPath}", file, newName, group)
          .flatMap(exec.request[JarInfo, ErrorApi.Error])
      }

      private[this] def formData(file: File, newName: String, group: Option[String]) = {
        var parts = Seq(
          Multipart.FormData.BodyPart(
            "jar",
            HttpEntity.fromFile(MediaTypes.`application/octet-stream`, file),
            Map("filename" -> newName)
          ))
        if (group.isDefined) parts :+= Multipart.FormData.BodyPart(Parameters.GROUP_NAME, group.get)
        Multipart.FormData(parts: _*)
      }

      private[this] def request(target: String, file: File, newName: Option[String], group: Option[String])(
        implicit executionContext: ExecutionContext): Future[HttpRequest] =
        Marshal(formData(file, newName.getOrElse(file.getName), group))
          .to[RequestEntity]
          .map(e => HttpRequest(HttpMethods.POST, uri = target, entity = e))
    }
  }

  def access: Access = new Access
}
