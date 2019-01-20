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

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{FileIO, Source}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
object JarApi {
  val JAR_PREFIX_PATH: String = "jars"
  final case class JarInfo(id: String, name: String, size: Long, lastModified: Long) extends Data {
    override def kind: String = "jar"
  }
  implicit val JAR_JSON_FORMAT: RootJsonFormat[JarInfo] = jsonFormat4(JarInfo)

  sealed abstract class Access extends BasicAccess(JAR_PREFIX_PATH) {
    def upload(f: File): Future[JarInfo] = upload(f, f.getName)
    def upload(f: File, newName: String): Future[JarInfo]
    def delete(id: String): Future[JarInfo]
    def list(): Future[Seq[JarInfo]]
  }

  def access(): Access = new Access {
    private[this] def request(target: String, file: File, newName: String): Future[HttpRequest] =
      Marshal(
        Multipart.FormData(
          Source.single(
            Multipart.FormData.BodyPart(
              "jar",
              HttpEntity(MediaTypes.`application/octet-stream`, file.length(), FileIO.fromPath(file.toPath)),
              Map("filename" -> newName)))))
        .to[RequestEntity]
        .map(e => HttpRequest(HttpMethods.POST, uri = target, entity = e))

    override def upload(f: File, newName: String): Future[JarInfo] =
      request(s"http://${_hostname}:${_port}/${_version}/${_prefixPath}", f, newName)
        .flatMap(exec.request[JarInfo, ErrorApi.Error])
    override def delete(id: String): Future[JarInfo] =
      exec.delete[JarInfo, ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
    override def list(): Future[Seq[JarInfo]] =
      exec.get[Seq[JarInfo], ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")
  }
}
