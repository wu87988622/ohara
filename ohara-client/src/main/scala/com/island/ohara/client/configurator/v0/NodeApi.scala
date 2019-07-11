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

import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}
object NodeApi {
  val NODES_PREFIX_PATH: String = "nodes"

  val ZOOKEEPER_SERVICE_NAME: String = "zookeeper"
  val BROKER_SERVICE_NAME: String = "broker"
  val WORKER_SERVICE_NAME: String = "connect-worker"

  case class Update(port: Option[Int], user: Option[String], password: Option[String], tags: Option[Set[String]])
  implicit val NODE_UPDATE_JSON_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update].format(jsonFormat4(Update)).requireConnectionPort("port").rejectEmptyString().refine

  case class Creation(hostname: String, port: Int, user: String, password: String, tags: Set[String])
      extends CreationRequest {
    override def name: String = hostname
  }
  implicit val NODE_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    JsonRefiner[Creation]
      .format(jsonFormat5(Creation))
      // default implementation of node is ssh, we use "default ssh port" here
      .nullToInt("port", 22)
      .requireConnectionPort("port")
      .rejectEmptyString()
      .stringRestriction("hostname")
      .withNumber()
      .withCharset()
      .withDot()
      .withDash()
      .toRefiner
      .nullToEmptyArray(Data.TAGS_KEY)
      .nullToAnotherValueOfKey("hostname", "name")
      .refine

  case class NodeService(name: String, clusterNames: Seq[String])
  implicit val NODE_SERVICE_JSON_FORMAT: RootJsonFormat[NodeService] = jsonFormat2(NodeService)

  /**
    * NOTED: the field "services" is filled at runtime. If you are in testing, it is ok to assign empty to it.
    */
  case class Node(hostname: String,
                  port: Int,
                  user: String,
                  password: String,
                  services: Seq[NodeService],
                  lastModified: Long,
                  tags: Set[String])
      extends Data {
    override def name: String = hostname
    override def kind: String = "node"
  }

  implicit val NODE_JSON_FORMAT: RootJsonFormat[Node] = new RootJsonFormat[Node] {
    private[this] val format = jsonFormat7(Node)
    override def read(json: JsValue): Node = format.read(json)

    // TODO: remove name from this object ... by chia
    override def write(obj: Node): JsValue = {
      val json = format.write(obj).asJsObject
      json.copy(fields = json.fields ++ Map("name" -> JsString(obj.hostname)))
    }
  }

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {
    def hostname(hostname: String): Request

    @Optional("it is ignorable if you are going to send update request")
    def port(port: Int): Request

    @Optional("it is ignorable if you are going to send update request")
    def user(user: String): Request

    @Optional("it is ignorable if you are going to send update request")
    def password(password: String): Request

    @Optional("default value is empty array")
    def tags(tags: Set[String]): Request

    /**
      * Retrieve the inner object of request payload. Noted, it throw unchecked exception if you haven't filled all required fields
      * @return the payload of creation
      */
    @VisibleForTesting
    private[v0] def creation: Creation

    /**
      * Retrieve the inner object of request payload. Noted, it throw unchecked exception if you haven't filled all required fields
      * @return the payload of creation
      */
    @VisibleForTesting
    private[v0] def update: Update

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[Node]

    /**
      * generate the PUT request
      * @param executionContext thread pool
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[Node]
  }

  class Access private[v0] extends com.island.ohara.client.configurator.v0.Access[Node](NODES_PREFIX_PATH) {
    def request: Request = new Request {
      private[this] var hostname: String = _
      private[this] var port: Option[Int] = None
      private[this] var user: String = _
      private[this] var password: String = _
      private[this] var tags: Set[String] = _
      override def hostname(hostname: String): Request = {
        this.hostname = CommonUtils.requireNonEmpty(hostname)
        this
      }
      override def port(port: Int): Request = {
        this.port = Some(CommonUtils.requireConnectionPort(port))
        this
      }
      override def user(user: String): Request = {
        this.user = CommonUtils.requireNonEmpty(user)
        this
      }
      override def password(password: String): Request = {
        this.password = CommonUtils.requireNonEmpty(password)
        this
      }

      override def tags(tags: Set[String]): Request = {
        this.tags = Objects.requireNonNull(tags)
        this
      }

      override private[v0] def creation: Creation = Creation(
        hostname = CommonUtils.requireNonEmpty(hostname),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        port = port.map(CommonUtils.requireConnectionPort).getOrElse(throw new NullPointerException),
        tags = if (tags == null) Set.empty else tags
      )

      override private[v0] def update: Update = Update(
        port = port.map(CommonUtils.requireConnectionPort),
        user = Option(user).map(CommonUtils.requireNonEmpty),
        password = Option(password).map(CommonUtils.requireNonEmpty),
        tags = Option(tags)
      )

      override def create()(implicit executionContext: ExecutionContext): Future[Node] =
        exec.post[Creation, Node, ErrorApi.Error](
          _url,
          creation
        )
      override def update()(implicit executionContext: ExecutionContext): Future[Node] =
        exec.put[Update, Node, ErrorApi.Error](
          s"${_url}/${CommonUtils.requireNonEmpty(hostname)}",
          update
        )
    }
  }

  def access: Access = new Access
}
