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
import spray.json.{DeserializationException, JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object BrokerApi {
  val BROKER_PREFIX_PATH: String = "brokers"

  /**
    * the default docker image used to run containers of broker cluster
    */
  val IMAGE_NAME_DEFAULT: String = s"oharastream/broker:${VersionUtils.VERSION}"

  final case class Creation private[BrokerApi] (name: String,
                                                imageName: String,
                                                zookeeperClusterName: Option[String],
                                                exporterPort: Int,
                                                clientPort: Int,
                                                jmxPort: Int,
                                                nodeNames: Set[String])
      extends ClusterCreationRequest {
    override def ports: Set[Int] = Set(clientPort, exporterPort, jmxPort)
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val BROKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[Creation] =
    new RootJsonFormat[Creation] {
      private[this] val nameKey: String = "name"
      private[this] val imageNameKey: String = "imageName"
      private[this] val zookeeperClusterNameKey: String = "zookeeperClusterName"
      private[this] val exporterPortKey: String = "exporterPort"
      private[this] val clientPortKey: String = "clientPort"
      private[this] val jmxPortKey: String = "jmxPort"
      private[this] val nodeNamesKey: String = "nodeNames"
      import scala.collection.JavaConverters._
      // We do the pre-check in json serialization phase so the follow-up processes don't need to check it again.
      override def read(json: JsValue): Creation =
        try Creation(
          name = CommonUtils.requireNonEmpty(noJsNull(json)(nameKey).asInstanceOf[JsString].value),
          imageName = noJsNull(json).get(imageNameKey).map(_.asInstanceOf[JsString].value).getOrElse(IMAGE_NAME_DEFAULT),
          zookeeperClusterName = noJsNull(json).get(zookeeperClusterNameKey).map(_.asInstanceOf[JsString].value),
          clientPort = CommonUtils.requireConnectionPort(
            noJsNull(json)
              .get(clientPortKey)
              .map(_.asInstanceOf[JsNumber].value.toInt)
              .getOrElse(CommonUtils.availablePort())),
          exporterPort = CommonUtils.requireConnectionPort(
            noJsNull(json)
              .get(exporterPortKey)
              .map(_.asInstanceOf[JsNumber].value.toInt)
              .getOrElse(CommonUtils.availablePort())),
          jmxPort = CommonUtils.requireConnectionPort(
            noJsNull(json)
              .get(jmxPortKey)
              .map(_.asInstanceOf[JsNumber].value.toInt)
              .getOrElse(CommonUtils.availablePort())),
          nodeNames = CommonUtils
            .requireNonEmpty(
              noJsNull(json)(nodeNamesKey).asInstanceOf[JsArray].elements.map(_.asInstanceOf[JsString].value).asJava)
            .asScala
            .toSet,
        )
        catch {
          case e: Throwable =>
            throw DeserializationException(e.getMessage, e)
        }

      override def write(obj: Creation): JsValue = JsObject(
        nameKey -> JsString(obj.name),
        imageNameKey -> JsString(obj.imageName),
        zookeeperClusterNameKey -> obj.zookeeperClusterName.map(JsString(_)).getOrElse(JsNull),
        clientPortKey -> JsNumber(obj.clientPort),
        exporterPortKey -> JsNumber(obj.exporterPort),
        jmxPortKey -> JsNumber(obj.jmxPort),
        nodeNamesKey -> JsArray(obj.nodeNames.map(JsString(_)).toVector),
      )
    }

  final case class BrokerClusterInfo private[BrokerApi] (name: String,
                                                         imageName: String,
                                                         zookeeperClusterName: String,
                                                         clientPort: Int,
                                                         exporterPort: Int,
                                                         jmxPort: Int,
                                                         nodeNames: Set[String])
      extends ClusterInfo {
    override def ports: Set[Int] = Set(clientPort, exporterPort, jmxPort)

    override def clone(newNodeNames: Set[String]): ClusterInfo = copy(nodeNames = newNodeNames)

    def connectionProps: String = nodeNames.map(n => s"$n:$clientPort").mkString(",")
  }

  /**
    * exposed to configurator
    */
  private[ohara] implicit val BROKER_CLUSTER_INFO_JSON_FORMAT: RootJsonFormat[BrokerClusterInfo] = jsonFormat7(
    BrokerClusterInfo)

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  sealed trait Request {
    def name(name: String): Request
    @Optional("the default image is IMAGE_NAME_DEFAULT")
    def imageName(imageName: String): Request
    @Optional("Ignoring zookeeper cluster name enable server to match a zk for you")
    def zookeeperClusterName(zookeeperClusterName: String): Request
    @Optional("the default port is random")
    def clientPort(clientPort: Int): Request
    @Optional("the default port is random")
    def exporterPort(exporterPort: Int): Request
    @Optional("the default port is random")
    def jmxPort(jmxPort: Int): Request
    def nodeName(nodeName: String): Request = nodeNames(Set(CommonUtils.requireNonEmpty(nodeName)))
    def nodeNames(nodeNames: Set[String]): Request

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo]

    /**
      * @return the payload of creation
      */
    private[v0] def creation(): Creation
  }

  final class Access private[BrokerApi] extends ClusterAccess[BrokerClusterInfo](BROKER_PREFIX_PATH) {
    def request(): Request = new Request {
      private[this] var name: String = _
      private[this] var imageName: String = IMAGE_NAME_DEFAULT
      private[this] var zookeeperClusterName: String = _
      private[this] var clientPort: Int = CommonUtils.availablePort()
      private[this] var exporterPort: Int = CommonUtils.availablePort()
      private[this] var jmxPort: Int = CommonUtils.availablePort()
      private[this] var nodeNames: Set[String] = Set.empty
      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }

      override def imageName(imageName: String): Request = {
        this.imageName = CommonUtils.requireNonEmpty(imageName)
        this
      }

      override def zookeeperClusterName(zookeeperClusterName: String): Request = {
        this.zookeeperClusterName = CommonUtils.requireNonEmpty(zookeeperClusterName)
        this
      }

      override def clientPort(clientPort: Int): Request = {
        this.clientPort = CommonUtils.requireConnectionPort(clientPort)
        this
      }

      override def exporterPort(exporterPort: Int): Request = {
        this.exporterPort = CommonUtils.requireConnectionPort(exporterPort)
        this
      }

      override def jmxPort(jmxPort: Int): Request = {
        this.jmxPort = CommonUtils.requireConnectionPort(jmxPort)
        this
      }

      import scala.collection.JavaConverters._
      override def nodeNames(nodeNames: Set[String]): Request = {
        this.nodeNames = CommonUtils.requireNonEmpty(nodeNames.asJava).asScala.toSet
        this
      }

      override private[v0] def creation() = Creation(
        name = CommonUtils.requireNonEmpty(name),
        imageName = CommonUtils.requireNonEmpty(imageName),
        zookeeperClusterName = Option(zookeeperClusterName),
        clientPort = CommonUtils.requireConnectionPort(clientPort),
        exporterPort = CommonUtils.requireConnectionPort(exporterPort),
        jmxPort = CommonUtils.requireConnectionPort(jmxPort),
        nodeNames = CommonUtils.requireNonEmpty(nodeNames.asJava).asScala.toSet
      )

      override def create()(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] =
        exec.post[Creation, BrokerClusterInfo, ErrorApi.Error](
          _url,
          creation()
        )
    }
  }

  def access(): Access = new Access
}
