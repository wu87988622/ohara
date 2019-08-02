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

import com.island.ohara.client.configurator.Data
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import com.island.ohara.kafka.connector.json.ObjectKey
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsString, JsValue, RootJsonFormat}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object StreamApi {

  /**
    * container name is controlled by streamRoute, the service name here use five words was ok.
    */
  val STREAM_SERVICE_NAME: String = "stream"
  val STREAM_PREFIX_PATH: String = STREAM_SERVICE_NAME

  /**
    * The default value of group for this API.
    */
  val GROUP_DEFAULT: String = com.island.ohara.client.configurator.v0.GROUP_DEFAULT

  val LIMIT_OF_NAME_LENGTH: Int = ZookeeperApi.LIMIT_OF_NAME_LENGTH

  /**
    * StreamApp Docker Image name
    */
  final val IMAGE_NAME_DEFAULT: String = s"oharastream/streamapp:${VersionUtils.VERSION}"

  final case class Creation(name: String,
                            imageName: String,
                            jar: ObjectKey,
                            from: Set[String],
                            to: Set[String],
                            jmxPort: Int,
                            instances: Int,
                            nodeNames: Set[String],
                            tags: Map[String, JsValue])
      extends ClusterCreationRequest {
    override def group: String = GROUP_DEFAULT
    override def ports: Set[Int] = Set(jmxPort)
  }
  implicit val STREAM_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    JsonRefiner[Creation]
      .format(jsonFormat9(Creation))
      // the default value
      .nullToString("imageName", IMAGE_NAME_DEFAULT)
      .nullToEmptyArray("from")
      .nullToEmptyArray("to")
      .nullToRandomPort("jmxPort")
      .nullToInt("instances", 1)
      .nullToEmptyArray("nodeNames")
      .requireBindPort("jmxPort")
      .requirePositiveNumber("instances")
      .rejectEmptyString()
      .stringRestriction(NAME_KEY)
      .withNumber()
      .withLowerCase()
      .withLengthLimit(LIMIT_OF_NAME_LENGTH)
      .toRefiner
      .nullToString("name", () => CommonUtils.randomString(LIMIT_OF_NAME_LENGTH))
      .nullToEmptyObject(TAGS_KEY)
      .refine

  final case class Update(imageName: Option[String],
                          from: Option[Set[String]],
                          to: Option[Set[String]],
                          jar: Option[ObjectKey],
                          jmxPort: Option[Int],
                          instances: Option[Int],
                          nodeNames: Option[Set[String]],
                          tags: Option[Map[String, JsValue]])
  implicit val STREAM_UPDATE_JSON_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update]
      .format(jsonFormat8(Update))
      .requireBindPort("jmxPort")
      .requirePositiveNumber("instances")
      .rejectEmptyString()
      .refine

  /**
    * The Stream Cluster Information stored in configurator
    *
    * @param name cluster name
    * @param imageName image name
    * @param instances numbers of streamApp running container
    * @param jar uploaded jar key
    * @param from the candidate topics for streamApp consume from
    * @param to the candidate topics for streamApp produce to
    * @param metrics the metrics bean
    * @param exactlyOnce enable exactly once (currently it's always false)
    * @param nodeNames actual running nodes
    * @param deadNodes dead nodes of dead containers from this cluster
    * @param jmxPort  jmx port
    * @param state the state of this cluster (see '''ContainerState''')
    * @param error the error message if the state was failed to fetch
    * @param lastModified this data change time
    * @param tags user defined data
    */
  final case class StreamClusterInfo(name: String,
                                     imageName: String,
                                     instances: Int,
                                     jar: ObjectKey,
                                     from: Set[String],
                                     to: Set[String],
                                     metrics: Metrics,
                                     // TODO remove this default value after we could handle from UI
                                     exactlyOnce: Boolean = false,
                                     nodeNames: Set[String],
                                     deadNodes: Set[String],
                                     jmxPort: Int,
                                     state: Option[String] = None,
                                     error: Option[String],
                                     lastModified: Long,
                                     tags: Map[String, JsValue])
      extends ClusterInfo
      //TODO : move Data class to ClusterInfo after finished #1544
      with Data {
    // streamapp does not support to define group
    override def group: String = GROUP_DEFAULT
    override def kind: String = STREAM_SERVICE_NAME
    override def ports: Set[Int] = Set(jmxPort)

    override def clone(newNodeNames: Set[String]): StreamClusterInfo = copy(nodeNames = newNodeNames)
    override def clone2(state: Option[String], error: Option[String]): StreamClusterInfo =
      this.copy(state = state, error = error)
  }
  private[ohara] implicit val STREAM_CLUSTER_INFO_JSON_FORMAT: OharaJsonFormat[StreamClusterInfo] =
    JsonRefiner[StreamClusterInfo]
      .format(new RootJsonFormat[StreamClusterInfo] {
        private[this] val format = jsonFormat15(StreamClusterInfo)
        override def read(json: JsValue): StreamClusterInfo = format.read(json)
        override def write(obj: StreamClusterInfo): JsValue =
          JsObject(
            noJsNull(
              format.write(obj).asJsObject.fields ++ Map(GROUP_KEY -> JsString(GROUP_DEFAULT))
            ))
      })
      .refine

  sealed trait Request {
    @Optional("default name is a random string. But it is required in updating")
    def name(name: String): Request
    @Optional("the default image is IMAGE_NAME_DEFAULT")
    def imageName(imageName: String): Request
    def jar(jar: ObjectKey): Request
    def from(from: Set[String]): Request
    def to(to: Set[String]): Request
    @Optional("the default port is random")
    def jmxPort(jmxPort: Int): Request
    @Optional("this parameter has lower priority than nodeNames")
    def instances(instances: Int): Request
    @Optional("this parameter has higher priority than instances")
    def nodeNames(nodeNames: Set[String]): Request
    @Optional("default value is empty array in creation and None in update")
    def tags(tags: Map[String, JsValue]): Request

    /**
      * generate POST request
      *
      * @param executionContext execution context
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo]

    /**
      * generate the PUT request
      *
      * @param executionContext execution context
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo]

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

  final class Access extends ClusterAccess[StreamClusterInfo](s"$STREAM_PREFIX_PATH") {

    def request: Request = new Request {
      private[this] var name: String = _
      private[this] var _imageName: Option[String] = None
      private[this] var jar: ObjectKey = _
      private[this] var _from: Option[Set[String]] = None
      private[this] var _to: Option[Set[String]] = None
      private[this] var _jmxPort: Option[Int] = None
      private[this] var _instances: Option[Int] = None
      private[this] var _nodeNames: Option[Set[String]] = None
      private[this] var tags: Map[String, JsValue] = _

      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }
      override def imageName(imageName: String): Request = {
        this._imageName = Some(CommonUtils.requireNonEmpty(imageName))
        this
      }
      override def jar(jar: ObjectKey): Request = {
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

      override def tags(tags: Map[String, JsValue]): Request = {
        this.tags = Objects.requireNonNull(tags)
        this
      }

      override private[v0] def creation: Creation = Creation(
        name = if (CommonUtils.isEmpty(name)) CommonUtils.randomString(10) else name,
        imageName = CommonUtils.requireNonEmpty(_imageName.getOrElse(IMAGE_NAME_DEFAULT)),
        jar = Objects.requireNonNull(jar),
        from = _from.getOrElse(Set.empty),
        to = _to.getOrElse(Set.empty),
        jmxPort = CommonUtils.requireConnectionPort(_jmxPort.getOrElse(CommonUtils.availablePort())),
        // only one of the value is needed between instances and nodes, we check the data after
        instances = _instances.getOrElse(1),
        nodeNames = _nodeNames.getOrElse(Set.empty),
        tags = if (tags == null) Map.empty else tags
      )

      override private[v0] def update: Update = Update(
        imageName = _imageName.map(CommonUtils.requireNonEmpty),
        from = _from.map(seq => CommonUtils.requireNonEmpty(seq.asJava).asScala.toSet),
        to = _to.map(seq => CommonUtils.requireNonEmpty(seq.asJava).asScala.toSet),
        jar = Option(jar),
        jmxPort = _jmxPort.map(CommonUtils.requireConnectionPort),
        instances = _instances.map(CommonUtils.requirePositiveInt),
        nodeNames = _nodeNames.map(seq => CommonUtils.requireNonEmpty(seq.asJava).asScala.toSet),
        tags = Option(tags)
      )

      override def create()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] = {
        exec.post[Creation, StreamClusterInfo, ErrorApi.Error](
          url,
          creation
        )
      }

      override def update()(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] = {
        exec.put[Update, StreamClusterInfo, ErrorApi.Error](
          s"${url}/${CommonUtils.requireNonEmpty(name)}",
          update
        )
      }
    }
  }

  def access: Access = new Access
}
