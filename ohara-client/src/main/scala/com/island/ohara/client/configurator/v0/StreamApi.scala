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
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Codec

object StreamApi {

  /**
    * StreamApp List Page file "key name" for form-data
    */
  final val INPUT_KEY = "streamapp"

  final val CONTENT_TYPE = MediaTypes.`application/java-archive`

  final val TMP_ROOT = System.getProperty("java.io.tmpdir")

  /**
    *  limit the length of docker container name (&lt; 60).
    */
  final val LIMIT_OF_DOCKER_NAME_LENGTH: Int = 60

  /**
    * StreamApp Docker Image name
    */
  final val IMAGE_NAME_DEFAULT: String = s"oharastream/streamapp:${VersionUtils.VERSION}"

  /**
    * streamApp use same port to do jmx expose: {host_port}:{container_port}. Using random port to avoid possible port conflict
    * from other clusters (zk, bk and wk)
    */
  final val JMX_PORT_DEFAULT: Int = CommonUtils.availablePort()

  val STREAM_PREFIX_PATH: String = "stream"
  val STREAM_LIST_PREFIX_PATH: String = "jars"
  val STREAM_PROPERTY_PREFIX_PATH: String = "property"
  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"
  val STATUS_COMMAND: String = "status"

  // --- data stored in configurator -- //
  //TODO : We can remove this class after #1151 solved...by Sam
  /**
    * the jar info of the uploaded streamApp jar
    *
    * @param workerClusterName which cluster the jar is belong to
    * @param id jar upload unique id
    * @param name jar name
    * @param lastModified the data change time
    */
  final case class StreamJar(workerClusterName: String, id: String, name: String, lastModified: Long) extends Data {
    override def kind: String = "streamJar"
  }

  /**
    * the streamApp description that is kept in ohara stores
    *
    * @param workerClusterName the worker cluster name
    * @param id the streamApp unique id
    * @param name streamApp name in pipeline
    * @param instances numbers of streamApp running container
    * @param jarInfo uploaded jar information
    * @param from the candidate topics for streamApp consume from
    * @param to the candidate topics for streamApp produce to
    * @param state the state of streamApp (stopped streamApp does not have this field)
    * @param error the error message if the state was failed to fetch
    * @param metrics the metrics bean
    * @param lastModified this data change time
    */
  final case class StreamAppDescription(workerClusterName: String,
                                        id: String,
                                        name: String,
                                        instances: Int,
                                        jarInfo: JarApi.JarInfo,
                                        from: Seq[String],
                                        to: Seq[String],
                                        state: Option[String],
                                        error: Option[String],
                                        metrics: Metrics,
                                        lastModified: Long)
      extends Data {
    override def kind: String = "streamApp"
    override def toString: String =
      s"""
          workerClusterName: $workerClusterName,
          id: $id,
          name: $name,
          instances: $instances,
          jarInfo: $jarInfo,
          fromTopics: $from,
          toTopics: $to
      """.stripMargin
  }
  implicit val STREAMAPP_DESCRIPTION_JSON_FORMAT: RootJsonFormat[StreamAppDescription] = jsonFormat11(
    StreamAppDescription)

  final case class StreamClusterCreationRequest(id: String,
                                                name: String,
                                                imageName: String,
                                                from: Seq[String],
                                                to: Seq[String],
                                                jmxPort: Option[Int],
                                                instances: Int,
                                                nodeNames: Set[String])
      extends ClusterCreationRequest {
    override def ports: Set[Int] = Set(jmxPort.getOrElse(JMX_PORT_DEFAULT))
  }

  /**
    * The Stream Cluster Information
    *
    * @param name cluster name
    * @param imageName image name
    * @param nodeNames actual running nodes
    * @param jmxPort  jmx port
    * @param state the state of this cluster (see '''ContainerState''')
    */
  final case class StreamClusterInfo(
    name: String,
    imageName: String,
    nodeNames: Set[String] = Set.empty,
    jmxPort: Int,
    state: Option[String] = None
  ) extends ClusterInfo {
    // We don't care the ports since streamApp communicates by broker
    override def ports: Set[Int] = Set.empty

    override def clone(newNodeNames: Set[String]): StreamClusterInfo = copy(nodeNames = newNodeNames)
  }

  // StreamApp List Request Body
  final case class StreamListRequest(jarName: String)
  implicit val STREAM_LIST_REQUEST_JSON_FORMAT: RootJsonFormat[StreamListRequest] = jsonFormat1(StreamListRequest)

  // StreamApp List Response Body
  implicit val STREAM_JAR_JSON_FORMAT: RootJsonFormat[StreamJar] = jsonFormat4(StreamJar)

  // StreamApp Property Request Body
  final case class StreamPropertyRequest(jarId: String,
                                         name: Option[String],
                                         from: Option[Seq[String]],
                                         to: Option[Seq[String]],
                                         instances: Option[Int])
  implicit val STREAM_PROPERTY_REQUEST_JSON_FORMAT: RootJsonFormat[StreamPropertyRequest] = jsonFormat5(
    StreamPropertyRequest)

  sealed abstract class ActionAccess extends BasicAccess(s"$STREAM_PREFIX_PATH") {

    /**
      *  start a streamApp
      *
      * @param id streamApp component id
      * @param executionContext execution context
      * @return status of streamApp ("RUNNING" if success, "EXITED" if fail)
      */
    def start(id: String)(implicit executionContext: ExecutionContext): Future[StreamAppDescription]

    /**
      * stop a streamApp
      *
      * @param id streamApp component id
      * @param executionContext execution context
      * @return status of streamApp (None if stop successful, or throw exception)
      */
    def stop(id: String)(implicit executionContext: ExecutionContext): Future[StreamAppDescription]
  }
  def accessOfAction(): ActionAccess = new ActionAccess {
    private[this] def url(id: String, action: String): String =
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id/$action"
    override def start(id: String)(implicit executionContext: ExecutionContext): Future[StreamAppDescription] =
      exec.put[StreamAppDescription, ErrorApi.Error](url(id, START_COMMAND))
    override def stop(id: String)(implicit executionContext: ExecutionContext): Future[StreamAppDescription] =
      exec.put[StreamAppDescription, ErrorApi.Error](url(id, STOP_COMMAND))
  }

  sealed abstract class ListAccess extends BasicAccess(s"$STREAM_PREFIX_PATH/$STREAM_LIST_PREFIX_PATH") {

    /**
      * list jar information of uploading streamApp jars
      *
      * @param wkName query parameter to filter by assigned worker cluster name
      * @param executionContext execution context
      * @return jar list in the worker cluster name, or all jars if not specify
      */
    def list(wkName: Option[String])(implicit executionContext: ExecutionContext): Future[Seq[StreamJar]]

    /**
      * upload streamApp jars to worker cluster,
      * will try to find pre-defined worker cluster if not assigned worker cluster name
      *
      * @param filePaths jar path list
      * @param wkName uploaded worker cluster name
      * @param executionContext execution context
      * @return upload jars to assigned worker cluster, or pre-defined worker cluster
      */
    def upload(filePaths: Seq[String], wkName: Option[String])(
      implicit executionContext: ExecutionContext): Future[Seq[StreamJar]] =
      upload(
        filePaths: Seq[String],
        wkName,
        INPUT_KEY,
        CONTENT_TYPE
      )
    def upload(filePaths: Seq[String], wkName: Option[String], inputKey: String, contentType: ContentType)(
      implicit executionContext: ExecutionContext): Future[Seq[StreamJar]]

    /**
      * delete streamApp jar by id
      *
      * @param id streamApp id
      * @param executionContext execution context
      * @return the deleted jar
      */
    def delete(id: String)(implicit executionContext: ExecutionContext): Future[Unit]

    /**
      * update jar information
      *
      * @param id streamApp id
      * @param request update request
      * @param executionContext execution context
      * @return the updated jar
      */
    def update(id: String, request: StreamListRequest)(implicit executionContext: ExecutionContext): Future[StreamJar]
  }
  // To avoid different charset handle, replace the malformedInput and unMappable char
  implicit val codec: Codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
  def accessOfList(): ListAccess = new ListAccess {
    private[this] def request(
      target: String,
      inputKey: String,
      contentType: ContentType,
      filePaths: Seq[String],
      wkName: Option[String])(implicit executionContext: ExecutionContext): Future[HttpRequest] = {
      var parts = filePaths.map(filePath => {
        Multipart.FormData.BodyPart(
          inputKey,
          HttpEntity.fromFile(contentType, new File(filePath)),
          Map("filename" -> new File(filePath).getName)
        )
      })
      if (wkName.isDefined) parts :+= Multipart.FormData.BodyPart(Parameters.CLUSTER_NAME, wkName.get)

      Marshal(Multipart.FormData(parts: _*))
        .to[RequestEntity]
        .map(
          entity => HttpRequest(HttpMethods.POST, uri = target, entity = entity)
        )
    }

    override def list(wkName: Option[String])(implicit executionContext: ExecutionContext): Future[Seq[StreamJar]] =
      exec.get[Seq[StreamJar], ErrorApi.Error](
        Parameters.appendTargetCluster(s"http://${_hostname}:${_port}/${_version}/${_prefixPath}",
                                       wkName.getOrElse("")))

    override def upload(filePaths: Seq[String], wkName: Option[String], inputKey: String, contentType: ContentType)(
      implicit executionContext: ExecutionContext): Future[Seq[StreamJar]] = {
      request(s"http://${_hostname}:${_port}/${_version}/${_prefixPath}", inputKey, contentType, filePaths, wkName)
        .flatMap(exec.request[Seq[StreamJar], ErrorApi.Error])
    }
    override def delete(id: String)(implicit executionContext: ExecutionContext): Future[Unit] =
      exec.delete[ErrorApi.Error](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
    override def update(id: String, request: StreamListRequest)(
      implicit executionContext: ExecutionContext): Future[StreamJar] =
      exec.put[StreamListRequest, StreamJar, ErrorApi.Error](
        s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id",
        request
      )
  }

  sealed trait PropertyAccess {
    def hostname(hostname: String)(implicit executionContext: ExecutionContext): PropertyAccess
    def port(port: Int)(implicit executionContext: ExecutionContext): PropertyAccess
    def post(request: StreamPropertyRequest)(implicit executionContext: ExecutionContext): Future[StreamAppDescription]
    def get(id: String)(implicit executionContext: ExecutionContext): Future[StreamAppDescription]
    def update(id: String, request: StreamPropertyRequest)(
      implicit executionContext: ExecutionContext): Future[StreamAppDescription]
    def delete(id: String)(implicit executionContext: ExecutionContext): Future[Unit]
  }

  def accessOfProperty(): Access[StreamPropertyRequest, StreamAppDescription] =
    new Access[StreamPropertyRequest, StreamAppDescription](s"$STREAM_PREFIX_PATH/$STREAM_PROPERTY_PREFIX_PATH")
}
