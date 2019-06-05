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
import akka.stream.scaladsl.{FileIO, Source}
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
    * @param id unique id
    * @param name jar file name
    * @param group group name
    * @param size file size
    * @param url download url
    * @param lastModified last modified time
    */
  final case class JarInfo(id: String, name: String, group: String, size: Long, url: URL, lastModified: Long)
      extends Data {
    override def kind: String = "jar"
  }

  implicit val JAR_INFO_JSON_FORMAT: RootJsonFormat[JarInfo] = jsonFormat6(JarInfo)

  sealed abstract class Access extends BasicAccess(JAR_PREFIX_PATH) {
    def upload(f: File)(implicit executionContext: ExecutionContext): Future[JarInfo] = upload(f, f.getName)
    def upload(f: File, newName: String)(implicit executionContext: ExecutionContext): Future[JarInfo]
    def delete(id: String)(implicit executionContext: ExecutionContext): Future[Unit]
    def list(implicit executionContext: ExecutionContext): Future[Seq[JarInfo]]
    def get(id: String)(implicit executionContext: ExecutionContext): Future[JarInfo]
  }

  def access(): Access = new Access {
    private[this] def formData(file: File, newName: String) = Multipart.FormData(
      Source.single(
        Multipart.FormData.BodyPart(
          "jar",
          HttpEntity(MediaTypes.`application/octet-stream`, file.length(), FileIO.fromPath(file.toPath)),
          Map("filename" -> newName))))

    private[this] def request(target: String, file: File, newName: String)(
      implicit executionContext: ExecutionContext): Future[HttpRequest] =
      Marshal(formData(file, newName))
        .to[RequestEntity]
        .map(e => HttpRequest(HttpMethods.POST, uri = target, entity = e))

    override def upload(f: File, newName: String)(implicit executionContext: ExecutionContext): Future[JarInfo] =
      request(s"http://${_hostname}:${_port}/${_version}/${_prefixPath}", f, newName)
        .flatMap(exec.request[JarInfo, ErrorApi.Error])
    override def delete(id: String)(implicit executionContext: ExecutionContext): Future[Unit] =
      exec.delete[ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
    override def list(implicit executionContext: ExecutionContext): Future[Seq[JarInfo]] =
      exec.get[Seq[JarInfo], ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")
    override def get(id: String)(implicit executionContext: ExecutionContext): Future[JarInfo] =
      exec.get[JarInfo, ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
  }
}
