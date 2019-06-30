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
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol.{jsonFormat6, _}
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}
object NodeApi {
  val NODES_PREFIX_PATH: String = "nodes"

  val ZOOKEEPER_SERVICE_NAME: String = "zookeeper"
  val BROKER_SERVICE_NAME: String = "broker"
  val WORKER_SERVICE_NAME: String = "connect-worker"

  case class Update(port: Option[Int], user: Option[String], password: Option[String])
  implicit val NODE_UPDATE_JSON_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update].format(jsonFormat3(Update)).requireConnectionPort("port").rejectEmptyString().refine

  case class Creation(name: String, port: Int, user: String, password: String) extends CreationRequest
  implicit val NODE_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    JsonRefiner[Creation]
      .format(jsonFormat4(Creation))
      // default implementation of node is ssh, we use "default ssh port" here
      .nullToInt("port", 22)
      .requireConnectionPort("port")
      .rejectEmptyString()
      .stringRestriction("name")
      .withNumber()
      .withCharset()
      .withDot()
      .withDash()
      .toRefiner
      .refine

  case class NodeService(name: String, clusterNames: Seq[String])
  implicit val NODE_SERVICE_JSON_FORMAT: RootJsonFormat[NodeService] = jsonFormat2(NodeService)

  /**
    * NOTED: the field "services" is filled at runtime. If you are in testing, it is ok to assign empty to it.
    */
  case class Node(name: String,
                  port: Int,
                  user: String,
                  password: String,
                  services: Seq[NodeService],
                  lastModified: Long)
      extends Data {
    override def id: String = name

    override def kind: String = "node"
  }

  implicit val NODE_JSON_FORMAT: RootJsonFormat[Node] = jsonFormat6(Node)

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {
    def name(name: String): Request
    @Optional("it is ignorable if you are going to send update request")
    def port(port: Int): Request
    @Optional("it is ignorable if you are going to send update request")
    def user(user: String): Request
    @Optional("it is ignorable if you are going to send update request")
    def password(password: String): Request

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
      private[this] var name: String = _
      private[this] var port: Option[Int] = None
      private[this] var user: String = _
      private[this] var password: String = _
      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
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

      override private[v0] def creation: Creation = Creation(
        name = CommonUtils.requireNonEmpty(name),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        port = port.map(CommonUtils.requireConnectionPort).getOrElse(throw new NullPointerException)
      )

      override private[v0] def update: Update = Update(
        port = port.map(CommonUtils.requireConnectionPort),
        user = Option(user).map(CommonUtils.requireNonEmpty),
        password = Option(password).map(CommonUtils.requireNonEmpty),
      )

      override def create()(implicit executionContext: ExecutionContext): Future[Node] =
        exec.post[Creation, Node, ErrorApi.Error](
          _url,
          creation
        )
      override def update()(implicit executionContext: ExecutionContext): Future[Node] =
        exec.put[Update, Node, ErrorApi.Error](
          s"${_url}/${CommonUtils.requireNonEmpty(name)}",
          update
        )
    }
  }

  def access: Access = new Access
}
