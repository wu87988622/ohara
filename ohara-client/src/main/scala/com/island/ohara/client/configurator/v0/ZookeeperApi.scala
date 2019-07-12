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
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object ZookeeperApi {

  /**
    * docker does limit the length of name (< 64). Since we format container name with some part of prefix,
    * limit the name length to one-third of 64 chars should be suitable for most cases.
    */
  val LIMIT_OF_NAME_LENGTH: Int = 20

  val ZOOKEEPER_PREFIX_PATH: String = "zookeepers"

  /**
    * the default docker image used to run containers of worker cluster
    */
  val IMAGE_NAME_DEFAULT: String = s"oharastream/zookeeper:${VersionUtils.VERSION}"

  final case class Creation private[ZookeeperApi] (name: String,
                                                   imageName: String,
                                                   clientPort: Int,
                                                   peerPort: Int,
                                                   electionPort: Int,
                                                   nodeNames: Set[String])
      extends ClusterCreationRequest {
    override def ports: Set[Int] = Set(clientPort, peerPort, electionPort)
    // the properties is not stored in configurator so we can't maintain the tags now
    // TODO: see https://github.com/oharastream/ohara/issues/1544
    override def tags: Map[String, JsValue] = Map.empty
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val ZOOKEEPER_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    JsonRefiner[Creation]
      .format(jsonFormat6(Creation))
      .rejectEmptyString()
      // the node names can't be empty
      .rejectEmptyArray()
      .nullToRandomPort("clientPort")
      .requireBindPort("clientPort")
      .nullToRandomPort("peerPort")
      .requireBindPort("peerPort")
      .nullToRandomPort("electionPort")
      .requireBindPort("electionPort")
      .nullToString("imageName", IMAGE_NAME_DEFAULT)
      .stringRestriction(Data.NAME_KEY)
      .withNumber()
      .withLowerCase()
      .withLengthLimit(LIMIT_OF_NAME_LENGTH)
      .toRefiner
      .nullToString("name", () => CommonUtils.randomString(10))
      .refine

  final case class ZookeeperClusterInfo private[ZookeeperApi] (name: String,
                                                               imageName: String,
                                                               clientPort: Int,
                                                               peerPort: Int,
                                                               electionPort: Int,
                                                               nodeNames: Set[String],
                                                               deadNodes: Set[String])
      extends ClusterInfo {
    override def ports: Set[Int] = Set(clientPort, peerPort, electionPort)
    override def clone(newNodeNames: Set[String]): ClusterInfo = copy(nodeNames = newNodeNames)
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val ZOOKEEPER_CLUSTER_INFO_JSON_FORMAT: RootJsonFormat[ZookeeperClusterInfo] = jsonFormat7(
    ZookeeperClusterInfo)

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  sealed trait Request {
    @Optional("default name is a random string")
    def name(name: String): Request
    @Optional("the default image is IMAGE_NAME_DEFAULT")
    def imageName(imageName: String): Request
    @Optional("the default port is random")
    def clientPort(clientPort: Int): Request
    @Optional("the default port is random")
    def peerPort(clientPort: Int): Request
    @Optional("the default port is random")
    def electionPort(clientPort: Int): Request
    def nodeName(nodeName: String): Request = nodeNames(Set(CommonUtils.requireNonEmpty(nodeName)))
    def nodeNames(nodeNames: Set[String]): Request

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo]

    /**
      * @return the payload of creation
      */
    private[v0] def creation: Creation
  }

  final class Access private[ZookeeperApi] extends ClusterAccess[ZookeeperClusterInfo](ZOOKEEPER_PREFIX_PATH) {
    def request: Request = new Request {
      private[this] var name: String = CommonUtils.randomString(LIMIT_OF_NAME_LENGTH)
      private[this] var imageName: String = IMAGE_NAME_DEFAULT
      private[this] var clientPort: Int = CommonUtils.availablePort()
      private[this] var peerPort: Int = CommonUtils.availablePort()
      private[this] var electionPort: Int = CommonUtils.availablePort()
      private[this] var nodeNames: Set[String] = Set.empty
      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }

      override def imageName(imageName: String): Request = {
        this.imageName = CommonUtils.requireNonEmpty(imageName)
        this
      }

      override def clientPort(clientPort: Int): Request = {
        this.clientPort = CommonUtils.requireConnectionPort(clientPort)
        this
      }

      override def peerPort(peerPort: Int): Request = {
        this.peerPort = CommonUtils.requireConnectionPort(peerPort)
        this
      }

      override def electionPort(electionPort: Int): Request = {
        this.electionPort = CommonUtils.requireConnectionPort(electionPort)
        this
      }

      import scala.collection.JavaConverters._
      override def nodeNames(nodeNames: Set[String]): Request = {
        this.nodeNames = CommonUtils.requireNonEmpty(nodeNames.asJava).asScala.toSet
        this
      }

      override private[v0] def creation: Creation = Creation(
        name = CommonUtils.requireNonEmpty(name),
        imageName = CommonUtils.requireNonEmpty(imageName),
        clientPort = CommonUtils.requireConnectionPort(clientPort),
        peerPort = CommonUtils.requireConnectionPort(peerPort),
        electionPort = CommonUtils.requireConnectionPort(electionPort),
        nodeNames = CommonUtils.requireNonEmpty(nodeNames.asJava).asScala.toSet
      )

      override def create()(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
        exec.post[Creation, ZookeeperClusterInfo, ErrorApi.Error](
          _url,
          creation
        )
    }
  }

  def access: Access = new Access
}
