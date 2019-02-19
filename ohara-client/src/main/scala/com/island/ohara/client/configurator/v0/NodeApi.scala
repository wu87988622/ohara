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
import com.island.ohara.common.util.CommonUtil
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonFormat}

object NodeApi {
  val NODES_PREFIX_PATH: String = "nodes"

  val ZOOKEEPER_SERVICE_NAME: String = "zookeeper"
  val BROKER_SERVICE_NAME: String = "broker"
  val WORKER_SERVICE_NAME: String = "connect-worker"

  case class NodeCreationRequest(name: Option[String], port: Int, user: String, password: String)
  implicit val NODE_REQUEST_JSON_FORMAT: RootJsonFormat[NodeCreationRequest] = jsonFormat4(NodeCreationRequest)

  case class NodeService(name: String, clusterNames: Seq[String])
  implicit val NODE_SERVICE_JSON_FORMAT: RootJsonFormat[NodeService] = jsonFormat2(NodeService)

  trait Node extends Data {
    def port: Int
    def user: String
    def password: String
    def services: Seq[NodeService]
  }

  object Node {
    def apply(name: String, port: Int, user: String, password: String): Node = NodeImpl(
      name = name,
      port = port,
      user = user,
      password = password,
      services = Seq.empty,
      lastModified = CommonUtil.current()
    )
  }

  implicit val NODE_JSON_FORMAT: RootJsonFormat[Node] = new RootJsonFormat[Node] {
    override def read(json: JsValue): Node = NODE_IMPL_JSON_FORMAT.read(json)

    override def write(obj: Node): JsValue = NODE_IMPL_JSON_FORMAT.write(toCaseClass(obj))
  }

  private[this] def toCaseClass(obj: Node): NodeImpl = obj match {
    case _: NodeImpl => obj.asInstanceOf[NodeImpl]
    case _ =>
      NodeImpl(
        name = obj.name,
        port = obj.port,
        user = obj.user,
        password = obj.password,
        services = obj.services,
        lastModified = obj.lastModified
      )
  }

  def copy(node: Node, services: Seq[NodeService]): Node = node match {
    case n: NodeImpl => n.copy(services = services)
    case _           => copy(toCaseClass(node), services)
  }

  /**
    * NOTED: the field "services" is filled at runtime. If you are in testing, it is ok to assign empty to it.
    */
  private[this] case class NodeImpl(name: String,
                                    port: Int,
                                    user: String,
                                    password: String,
                                    services: Seq[NodeService],
                                    lastModified: Long)
      extends Node {

    /**
      *  node's name should be unique in ohara so we make id same to name.
      * @return name
      */
    override def id: String = name
    override def kind: String = "node"
  }

  private[this] implicit val NODE_IMPL_JSON_FORMAT: RootJsonFormat[NodeImpl] = jsonFormat6(NodeImpl)

  def access(): Access[NodeCreationRequest, Node] =
    new Access[NodeCreationRequest, Node](NODES_PREFIX_PATH)
}
