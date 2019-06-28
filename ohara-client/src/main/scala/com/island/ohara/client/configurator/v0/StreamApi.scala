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
import java.util.Objects

import com.island.ohara.client.configurator.v0.JarApi.JarKey
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object StreamApi {

  /**
    * StreamApp Docker Image name
    */
  final val IMAGE_NAME_DEFAULT: String = s"oharastream/streamapp:${VersionUtils.VERSION}"

  val STREAM_PREFIX_PATH: String = "stream"
  val STREAM_PROPERTY_PREFIX_PATH: String = "property"
  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"

  // --- data stored in configurator -- //
  /**
    * the streamApp description that is kept in ohara stores
    *
    * @param name streamApp name in pipeline
    * @param imageName streamApp image name
    * @param instances numbers of streamApp running container
    * @param nodeNames node list of streamApp running container
    * @param deadNodes dead node list of the exited containers from this cluster
    * @param jar uploaded jar key
    * @param from the candidate topics for streamApp consume from
    * @param to the candidate topics for streamApp produce to
    * @param state the state of streamApp (stopped streamApp does not have this field)
    * @param error the error message if the state was failed to fetch
    * @param jmxPort the expose jmx port
    * @param metrics the metrics bean
    * @param exactlyOnce enable exactly once
    * @param lastModified this data change time
    */
  final case class StreamAppDescription(name: String,
                                        imageName: String,
                                        instances: Int,
                                        nodeNames: Set[String],
                                        deadNodes: Set[String],
                                        jar: JarKey,
                                        from: Set[String],
                                        to: Set[String],
                                        state: Option[String],
                                        error: Option[String],
                                        jmxPort: Int,
                                        metrics: Metrics,
                                        // TODO remove this default value after we could handle from UI
                                        exactlyOnce: Boolean = false,
                                        lastModified: Long)
      extends Data {
    override def id: String = name
    override def kind: String = "streamApp"
  }
  implicit val STREAMAPP_DESCRIPTION_JSON_FORMAT: RootJsonFormat[StreamAppDescription] = jsonFormat14(
    StreamAppDescription)

  final case class Creation(name: String,
                            imageName: String,
                            jar: JarKey,
                            from: Set[String],
                            to: Set[String],
                            jmxPort: Int,
                            instances: Int,
                            nodeNames: Set[String])
      extends ClusterCreationRequest {
    override def ports: Set[Int] = Set(jmxPort)
  }
  implicit val STREAM_CREATION_JSON_FORMAT: RootJsonFormat[Creation] =
    JsonRefiner[Creation]
      .format(jsonFormat8(Creation))
      // the default value
      .nullToString("imageName", IMAGE_NAME_DEFAULT)
      .nullToEmptyArray("from")
      .nullToEmptyArray("to")
      .nullToRandomPort("jmxPort")
      .nullToInt("instances", 1)
      .nullToEmptyArray("nodeNames")
      // the instances cannot by negative (zero number will be reject in StreamRoute start api)
      .rejectNegativeNumber()
      .rejectEmptyString()
      .requireBindPort("jmxPort")
      .refine

  final case class Update(imageName: Option[String],
                          from: Option[Set[String]],
                          to: Option[Set[String]],
                          jar: Option[JarKey],
                          jmxPort: Option[Int],
                          instances: Option[Int],
                          nodeNames: Option[Set[String]])
  implicit val STREAM_UPDATE_JSON_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update]
      .format(jsonFormat7(Update))
      // the instances cannot by negative (zero number will be reject in StreamRoute start api)
      .rejectNegativeNumber()
      .rejectEmptyString()
      // the node names cannot be empty
      .rejectEmptyArray()
      .requireBindPort("jmxPort")
      .refine

  /**
    * The Stream Cluster Information
    *
    * @param name cluster name
    * @param imageName image name
    * @param nodeNames actual running nodes
    * @param deadNodes dead nodes of dead containers from this cluster
    * @param jmxPort  jmx port
    * @param state the state of this cluster (see '''ContainerState''')
    */
  final case class StreamClusterInfo(
    name: String,
    imageName: String,
    nodeNames: Set[String],
    deadNodes: Set[String],
    jmxPort: Int,
    state: Option[String] = None
  ) extends ClusterInfo {
    override def ports: Set[Int] = Set(jmxPort)

    override def clone(newNodeNames: Set[String]): StreamClusterInfo = copy(nodeNames = newNodeNames)
  }

  sealed trait Request {
    def name(name: String): Request
    @Optional("the default image is IMAGE_NAME_DEFAULT")
    def imageName(imageName: String): Request
    def jar(jar: JarKey): Request
    def from(from: Set[String]): Request
    def to(to: Set[String]): Request
    @Optional("the default port is random")
    def jmxPort(jmxPort: Int): Request
    @Optional("this parameter has lower priority than nodeNames")
    def instances(instances: Int): Request
    @Optional("this parameter has higher priority than instances")
    def nodeNames(nodeNames: Set[String]): Request

    /**
      * generate POST request
      *
      * @param executionContext execution context
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[StreamAppDescription]

    /**
      * generate the PUT request
      *
      * @param executionContext execution context
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[StreamAppDescription]

    /**
      * for testing only
      * @return the payload of creation
      */
    @VisibleForTesting
    private[v0] def creation: Creation

    /**
      * for testing only
      * @return the payload of update
      */
    @VisibleForTesting
    private[v0] def update: Update
  }

  sealed abstract class ActionAccess extends BasicAccess(s"$STREAM_PREFIX_PATH") {

    /**
      *  start a streamApp
      *
      * @param name streamApp object name
      * @param executionContext execution context
      * @return information of streamApp (status "RUNNING" if success, "EXITED" if fail)
      */
    def start(name: String)(implicit executionContext: ExecutionContext): Future[StreamAppDescription]

    /**
      * stop a streamApp
      *
      * @param name streamApp object name
      * @param executionContext execution context
      * @return information of streamApp (status None if stop successful, or throw exception)
      */
    def stop(name: String)(implicit executionContext: ExecutionContext): Future[StreamAppDescription]
  }
  def accessOfAction: ActionAccess = new ActionAccess {
    private[this] def url(name: String, action: String): String =
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$name/$action"
    override def start(name: String)(implicit executionContext: ExecutionContext): Future[StreamAppDescription] =
      exec.put[StreamAppDescription, ErrorApi.Error](url(name, START_COMMAND))
    override def stop(name: String)(implicit executionContext: ExecutionContext): Future[StreamAppDescription] =
      exec.put[StreamAppDescription, ErrorApi.Error](url(name, STOP_COMMAND))
  }

  final class AccessOfProperty
      extends com.island.ohara.client.configurator.v0.Access[StreamAppDescription](
        s"$STREAM_PREFIX_PATH/$STREAM_PROPERTY_PREFIX_PATH") {
    def request: Request = new Request {
      private[this] var name: String = _
      private[this] var _imageName: Option[String] = None
      private[this] var jar: JarKey = _
      private[this] var _from: Option[Set[String]] = None
      private[this] var _to: Option[Set[String]] = None
      private[this] var _jmxPort: Option[Int] = None
      private[this] var _instances: Option[Int] = None
      private[this] var _nodeNames: Option[Set[String]] = None

      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }
      override def imageName(imageName: String): Request = {
        this._imageName = Some(CommonUtils.requireNonEmpty(imageName))
        this
      }
      override def jar(jar: JarKey): Request = {
        this.jar = Objects.requireNonNull(jar)
        this
      }
      override def from(from: Set[String]): Request = {
        this._from = Some(CommonUtils.requireNonEmpty(from.asJava).asScala.toSet)
        this
      }
      override def to(to: Set[String]): Request = {
        this._to = Some(CommonUtils.requireNonEmpty(to.asJava).asScala.toSet)
        this
      }
      override def jmxPort(jmxPort: Int): Request = {
        this._jmxPort = Some(CommonUtils.requireConnectionPort(jmxPort))
        this
      }
      override def instances(instances: Int): Request = {
        this._instances = Some(CommonUtils.requirePositiveInt(instances))
        this
      }
      override def nodeNames(nodeNames: Set[String]): Request = {
        this._nodeNames = Some(CommonUtils.requireNonEmpty(nodeNames.asJava).asScala.toSet)
        this
      }

      override private[v0] def creation: Creation = Creation(
        name = CommonUtils.requireNonEmpty(name),
        imageName = CommonUtils.requireNonEmpty(_imageName.getOrElse(IMAGE_NAME_DEFAULT)),
        jar = Objects.requireNonNull(jar),
        from = Objects.requireNonNull(_from.getOrElse(Set.empty)),
        to = Objects.requireNonNull(_to.getOrElse(Set.empty)),
        jmxPort = CommonUtils.requireConnectionPort(_jmxPort.getOrElse(CommonUtils.availablePort())),
        // only one of the value is needed between instances and nodes, we check the data after
        instances = _instances.getOrElse(1),
        nodeNames = _nodeNames.getOrElse(Set.empty)
      )

      override private[v0] def update: Update = Update(
        imageName = _imageName.map(CommonUtils.requireNonEmpty),
        from = _from.map(seq => CommonUtils.requireNonEmpty(seq.asJava).asScala.toSet),
        to = _to.map(seq => CommonUtils.requireNonEmpty(seq.asJava).asScala.toSet),
        jar = Option(jar),
        jmxPort = _jmxPort.map(CommonUtils.requireConnectionPort),
        instances = _instances.map(CommonUtils.requirePositiveInt),
        nodeNames = _nodeNames.map(seq => CommonUtils.requireNonEmpty(seq.asJava).asScala.toSet)
      )

      /**
        * generate POST request
        *
        * @param executionContext execution context
        * @return created data
        */
      override def create()(implicit executionContext: ExecutionContext): Future[StreamAppDescription] = {
        exec.post[Creation, StreamAppDescription, ErrorApi.Error](
          _url,
          creation
        )
      }

      override def update()(implicit executionContext: ExecutionContext): Future[StreamAppDescription] = {
        exec.put[Update, StreamAppDescription, ErrorApi.Error](
          s"${_url}/${CommonUtils.requireNonEmpty(name)}",
          update
        )
      }
    }
  }

  def accessOfProperty: AccessOfProperty = new AccessOfProperty
}
