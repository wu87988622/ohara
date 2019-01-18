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
import akka.util.ByteString
import com.island.ohara.client.StreamClient
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

object StreamApi {
  val STREAM_PREFIX_PATH: String = "stream"
  val STREAM_LIST_PREFIX_PATH: String = "jars"
  val STREAM_PROPERTY_PREFIX_PATH: String = "property"
  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"

  /**
    * the streamapp data keeps into ohara Stores
    *
    * @param pipelineId the ohara pipeline id
    * @param id the streamapp unique id
    * @param name streamapp running configuration : application.id
    * @param instances streamapp running configuration : num.stream.threads
    * @param jarInfo saving jar inrfomation
    * @param fromTopics the candidate topics for streamapp consume from
    * @param toTopics the candidate topics for streamapp produce to
    * @param lastModified this data change time
    */
  final case class StreamApp(pipelineId: String,
                             id: String,
                             name: String,
                             instances: Int,
                             jarInfo: JarApi.JarInfo,
                             fromTopics: Seq[String],
                             toTopics: Seq[String],
                             lastModified: Long)
      extends Data {
    override def kind: String = "streamApp"
  }

  // StreamApp List Request Body
  final case class StreamListRequest(jarName: String)
  implicit val STREAM_LIST_REQUEST_JSON_FORMAT: RootJsonFormat[StreamListRequest] = jsonFormat1(StreamListRequest)

  // StreamApp List Page Response Body
  final case class StreamListResponse(id: String, jarName: String, lastModified: Long)
  implicit val STREAM_JAR_JSON_FORMAT: RootJsonFormat[StreamListResponse] = jsonFormat3(StreamListResponse)

  // StreamApp Property Request Body
  final case class StreamPropertyRequest(name: String, fromTopics: Seq[String], toTopics: Seq[String], instances: Int)
  implicit val STREAM_PROPERTY_REQUEST_JSON_FORMAT: RootJsonFormat[StreamPropertyRequest] = jsonFormat4(
    StreamPropertyRequest)

  // StreamApp Property Page Response Body
  final case class StreamPropertyResponse(id: String,
                                          jarName: String,
                                          name: String,
                                          fromTopics: Seq[String],
                                          toTopics: Seq[String],
                                          instances: Int,
                                          lastModified: Long)
  implicit val STREAM_PROPERTY_RESPONSE_JSON_FORMAT: RootJsonFormat[StreamPropertyResponse] = jsonFormat7(
    StreamPropertyResponse)

  sealed abstract class ListAccess extends BasicAccess(s"$STREAM_PREFIX_PATH/$STREAM_LIST_PREFIX_PATH") {
    def list(pipeline_id: String): Future[Seq[StreamListResponse]]
    def upload(pipeline_id: String, filePaths: Seq[String]): Future[Seq[StreamListResponse]] =
      upload(pipeline_id: String, filePaths: Seq[String], StreamClient.INPUT_KEY, StreamClient.CONTENT_TYPE)
    def upload(pipeline_id: String,
               filePaths: Seq[String],
               inputKey: String,
               contentType: ContentType): Future[Seq[StreamListResponse]]
    def delete(jar_id: String): Future[StreamListResponse]
    def update(jar_id: String, request: StreamListRequest): Future[StreamListResponse]
  }

  def accessOfList(): ListAccess = new ListAccess {
    private[this] def request(target: String,
                              inputKey: String,
                              contentType: ContentType,
                              filePaths: Seq[String]): Future[HttpRequest] =
      Marshal(Multipart.FormData(filePaths.map(filePath => {
        Multipart.FormData.BodyPart.Strict(
          inputKey,
          HttpEntity(contentType, ByteString(Source.fromFile(filePath).mkString)),
          Map("filename" -> new File(filePath).getName)
        )
      }): _*)).to[RequestEntity].map(entity => HttpRequest(HttpMethods.POST, uri = target, entity = entity))

    override def list(pipeline_id: String): Future[Seq[StreamListResponse]] =
      exec.get[Seq[StreamListResponse]](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$pipeline_id")

    override def upload(pipeline_id: String,
                        filePaths: Seq[String],
                        inputKey: String,
                        contentType: ContentType): Future[Seq[StreamListResponse]] = {
      request(s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$pipeline_id", inputKey, contentType, filePaths)
        .flatMap(exec.request[Seq[StreamListResponse]])
    }
    override def delete(jar_id: String): Future[StreamListResponse] =
      exec.delete[StreamListResponse](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$jar_id")
    override def update(jar_id: String, request: StreamListRequest): Future[StreamListResponse] =
      exec.put[StreamListRequest, StreamListResponse](
        s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$jar_id",
        request)
  }

  sealed trait PropertyAccess {
    def hostname(hostname: String): PropertyAccess
    def port(port: Int): PropertyAccess
    def get(id: String): Future[StreamPropertyResponse]
    def update(id: String, request: StreamPropertyRequest): Future[StreamPropertyResponse]
  }

  def accessOfProperty(): PropertyAccess = new PropertyAccess {
    private[this] val access: Access[StreamPropertyRequest, StreamPropertyResponse] =
      new Access[StreamPropertyRequest, StreamPropertyResponse](s"$STREAM_PREFIX_PATH/$STREAM_PROPERTY_PREFIX_PATH")

    override def hostname(hostname: String): PropertyAccess = {
      access.hostname(hostname)
      this
    }
    override def port(port: Int): PropertyAccess = {
      access.port(port)
      this
    }

    override def get(id: String): Future[StreamPropertyResponse] = access.get(id)
    override def update(id: String, request: StreamPropertyRequest): Future[StreamPropertyResponse] =
      access.update(id, request)
  }
}
