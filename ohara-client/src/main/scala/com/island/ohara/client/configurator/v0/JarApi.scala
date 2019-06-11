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

    /**
      * upload jar to jar store
      * @param f the file
      * @param newName file new name
      * @param group the group name for file to group by (we use worker cluster name as this value for now)
      * @param executionContext execution context
      * @return uploaded jar information
      */
    def upload(f: File, newName: String, group: Option[String])(
      implicit executionContext: ExecutionContext): Future[JarInfo]
    def upload(f: File, group: Option[String])(implicit executionContext: ExecutionContext): Future[JarInfo] =
      upload(f, f.getName, group)
    def delete(id: String)(implicit executionContext: ExecutionContext): Future[Unit]
    def list(implicit executionContext: ExecutionContext): Future[Seq[JarInfo]]
    def get(id: String)(implicit executionContext: ExecutionContext): Future[JarInfo]
  }

  def access(): Access = new Access {
    private[this] def formData(file: File, newName: String, group: Option[String]) = {
      var parts = Seq(
        Multipart.FormData.BodyPart(
          "jar",
          HttpEntity.fromFile(MediaTypes.`application/octet-stream`, file),
          Map("filename" -> newName)
        ))
      if (group.isDefined) parts :+= Multipart.FormData.BodyPart(Parameters.CLUSTER_NAME, group.get)
      Multipart.FormData(parts: _*)
    }

    private[this] def request(target: String, file: File, newName: String, group: Option[String])(
      implicit executionContext: ExecutionContext): Future[HttpRequest] =
      Marshal(formData(file, newName, group))
        .to[RequestEntity]
        .map(e => HttpRequest(HttpMethods.POST, uri = target, entity = e))

    override def upload(f: File, newName: String, group: Option[String])(
      implicit executionContext: ExecutionContext): Future[JarInfo] =
      request(s"http://${_hostname}:${_port}/${_version}/${_prefixPath}", f, newName, group)
        .flatMap(exec.request[JarInfo, ErrorApi.Error])
    override def delete(id: String)(implicit executionContext: ExecutionContext): Future[Unit] =
      exec.delete[ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
    override def list(implicit executionContext: ExecutionContext): Future[Seq[JarInfo]] =
      exec.get[Seq[JarInfo], ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")
    override def get(id: String)(implicit executionContext: ExecutionContext): Future[JarInfo] =
      exec.get[JarInfo, ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
  }
}
