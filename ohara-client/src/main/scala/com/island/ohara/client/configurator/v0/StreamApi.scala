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
import java.nio.charset.CodingErrorAction

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.FileInfo
import akka.util.ByteString
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.{Codec, Source}

object StreamApi {

  /**
    * StreamApp List Page max acceptable upload file size (1 MB currently)
    */
  final val MAX_FILE_SIZE = 1 * 1024 * 1024L

  /**
    * StreamApp List Page "key name" for form-data
    */
  final val INPUT_KEY = "streamapp"

  final val CONTENT_TYPE = MediaTypes.`application/java-archive`

  final val TMP_ROOT = System.getProperty("java.io.tmpdir")

  /**
    * the only entry for ohara streamApp
    */
  final val MAIN_ENTRY = "com.island.ohara.streams.StreamApp"

  /**
    *  limit the length of docker container name (&lt; 60).
    */
  final val LIMIT_OF_DOCKER_NAME_LENGTH: Int = 60

  private[this] def assertLength(s: String): String = if (s.length > LIMIT_OF_DOCKER_NAME_LENGTH)
    throw new IllegalArgumentException(s"limit of length is $LIMIT_OF_DOCKER_NAME_LENGTH. actual: ${s.length}")
  else s

  final val PREFIX_KEY = "ost"

  final val DIVIDER: String = "-"

  /**
    * format unique applicationId for the streamApp.
    * It can be used in setting container's hostname and name
    * @param streamId the streamApp id
    * @return a formatted string. form: ${prefix}-${streamId}
    */
  def formatAppId(streamId: String): String =
    assertLength(
      Seq(
        PREFIX_KEY,
        streamId
      ).mkString(DIVIDER))

  final val JARURL_KEY: String = "STREAMAPP_JARURL"
  final val APPID_KEY: String = "STREAMAPP_APPID"
  final val SERVERS_KEY: String = "STREAMAPP_SERVERS"
  final val FROM_TOPIC_KEY: String = "STREAMAPP_FROMTOPIC"
  final val TO_TOPIC_KEY: String = "STREAMAPP_TOTOPIC"

  /**
    * StreamApp Docker Image name
    */
  final val STREAMAPP_IMAGE: String = s"oharastream/streamapp:${VersionUtils.VERSION}"

  /**
    * create temp file(with suffix .tmp) inside temp folder
    *
    * @param fileInfo the request file
    * @return the tmp file
    */
  def saveTmpFile(fileInfo: FileInfo): File = CommonUtils.createTempFile(fileInfo.fileName)

  val STREAM_PREFIX_PATH: String = "stream"
  val STREAM_LIST_PREFIX_PATH: String = "jars"
  val STREAM_PROPERTY_PREFIX_PATH: String = "property"
  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"

  /**
    * the streamApp data keeps into ohara Stores
    *
    * @param pipelineId the ohara pipeline id
    * @param id the streamApp unique id
    * @param name streamApp name in pipeline
    * @param instances streamApp running configuration : num.stream.threads
    * @param jarInfo saving jar information
    * @param fromTopics the candidate topics for streamApp consume from
    * @param toTopics the candidate topics for streamApp produce to
    * @param lastModified this data change time
    * @param state this streamApp current state
    * @param nodes the streamApp running nodes
    */
  final case class StreamApp(pipelineId: String,
                             id: String,
                             name: String,
                             instances: Int,
                             jarInfo: JarApi.JarInfo,
                             fromTopics: Seq[String],
                             toTopics: Seq[String],
                             lastModified: Long,
                             state: Option[ConnectorState] = None,
                             nodes: Seq[NodeApi.Node] = Seq.empty)
      extends Data {
    override def kind: String = "streamApp"
  }

  // StreamApp Action Response Body
  final case class StreamActionResponse(id: String, state: Option[ContainerApi.ContainerState])
  implicit val STREAM_ACTION_RESPONSE_JSON_FORMAT: RootJsonFormat[StreamActionResponse] = jsonFormat2(
    StreamActionResponse)

  // StreamApp List Request Body
  final case class StreamListRequest(jarName: String)
  implicit val STREAM_LIST_REQUEST_JSON_FORMAT: RootJsonFormat[StreamListRequest] = jsonFormat1(StreamListRequest)

  // StreamApp List Response Body
  final case class StreamListResponse(id: String, name: String, jarName: String, lastModified: Long)
  implicit val STREAM_JAR_JSON_FORMAT: RootJsonFormat[StreamListResponse] =
    jsonFormat4(StreamListResponse)

  // StreamApp Property Request Body
  final case class StreamPropertyRequest(name: String, fromTopics: Seq[String], toTopics: Seq[String], instances: Int)
  implicit val STREAM_PROPERTY_REQUEST_JSON_FORMAT: RootJsonFormat[StreamPropertyRequest] = jsonFormat4(
    StreamPropertyRequest)

  // StreamApp Property Response Body
  final case class StreamPropertyResponse(id: String,
                                          jarName: String,
                                          name: String,
                                          fromTopics: Seq[String],
                                          toTopics: Seq[String],
                                          instances: Int,
                                          lastModified: Long)
  implicit val STREAM_PROPERTY_RESPONSE_JSON_FORMAT: RootJsonFormat[StreamPropertyResponse] = jsonFormat7(
    StreamPropertyResponse
  )

  sealed abstract class ActionAccess extends BasicAccess(s"$STREAM_PREFIX_PATH") {
    def start(id: String): Future[StreamActionResponse]
    def stop(id: String): Future[StreamActionResponse]
  }
  def accessOfAction(): ActionAccess = new ActionAccess {
    private[this] def url(id: String, action: String): String =
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id/$action"
    override def start(id: String): Future[StreamActionResponse] =
      exec.put[StreamActionResponse, ErrorApi.Error](url(id, START_COMMAND))
    override def stop(id: String): Future[StreamActionResponse] =
      exec.put[StreamActionResponse, ErrorApi.Error](url(id, STOP_COMMAND))
  }

  sealed abstract class ListAccess extends BasicAccess(s"$STREAM_PREFIX_PATH/$STREAM_LIST_PREFIX_PATH") {
    def list(pipeline_id: String): Future[Seq[StreamListResponse]]
    def upload(pipeline_id: String, filePaths: Seq[String]): Future[Seq[StreamListResponse]] =
      upload(
        pipeline_id: String,
        filePaths: Seq[String],
        INPUT_KEY,
        CONTENT_TYPE
      )
    def upload(pipeline_id: String,
               filePaths: Seq[String],
               inputKey: String,
               contentType: ContentType): Future[Seq[StreamListResponse]]
    def delete(jar_id: String): Future[StreamListResponse]
    def update(jar_id: String, request: StreamListRequest): Future[StreamListResponse]
  }

  // To avoid different charset handle, replace the malformedInput and unMappable char
  implicit val codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
  def accessOfList(): ListAccess = new ListAccess {
    private[this] def request(target: String,
                              inputKey: String,
                              contentType: ContentType,
                              filePaths: Seq[String]): Future[HttpRequest] =
      Marshal(Multipart.FormData(filePaths.map(filePath => {
        Multipart.FormData.BodyPart.Strict(
          inputKey,
          HttpEntity(
            contentType,
            ByteString(Source.fromFile(filePath).mkString)
          ),
          Map("filename" -> new File(filePath).getName)
        )
      }): _*))
        .to[RequestEntity]
        .map(
          entity => HttpRequest(HttpMethods.POST, uri = target, entity = entity)
        )

    override def list(pipeline_id: String): Future[Seq[StreamListResponse]] =
      exec.get[Seq[StreamListResponse], ErrorApi.Error](
        s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$pipeline_id")

    override def upload(pipeline_id: String,
                        filePaths: Seq[String],
                        inputKey: String,
                        contentType: ContentType): Future[Seq[StreamListResponse]] = {
      request(s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$pipeline_id", inputKey, contentType, filePaths)
        .flatMap(exec.request[Seq[StreamListResponse], ErrorApi.Error])
    }
    override def delete(jar_id: String): Future[StreamListResponse] =
      exec.delete[StreamListResponse, ErrorApi.Error](
        s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$jar_id")
    override def update(jar_id: String, request: StreamListRequest): Future[StreamListResponse] =
      exec.put[StreamListRequest, StreamListResponse, ErrorApi.Error](
        s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$jar_id",
        request
      )
  }

  sealed trait PropertyAccess {
    def hostname(hostname: String): PropertyAccess
    def port(port: Int): PropertyAccess
    def get(id: String): Future[StreamPropertyResponse]
    def update(id: String, request: StreamPropertyRequest): Future[StreamPropertyResponse]
  }

  def accessOfProperty(): PropertyAccess = new PropertyAccess {
    private[this] val access: Access[StreamPropertyRequest, StreamPropertyResponse] =
      new Access[StreamPropertyRequest, StreamPropertyResponse](
        s"$STREAM_PREFIX_PATH/$STREAM_PROPERTY_PREFIX_PATH"
      )

    override def hostname(hostname: String): PropertyAccess = {
      access.hostname(hostname)
      this
    }
    override def port(port: Int): PropertyAccess = {
      access.port(port)
      this
    }

    override def get(id: String): Future[StreamPropertyResponse] =
      access.get(id)
    override def update(
      id: String,
      request: StreamPropertyRequest
    ): Future[StreamPropertyResponse] =
      access.update(id, request)
  }
}
