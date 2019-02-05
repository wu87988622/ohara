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
import spray.json.RootJsonFormat

object NodeApi {
  val NODES_PREFIX_PATH: String = "nodes"
  case class NodeCreationRequest(name: Option[String], port: Int, user: String, password: String)
  implicit val NODE_REQUEST_JSON_FORMAT: RootJsonFormat[NodeCreationRequest] = jsonFormat4(NodeCreationRequest)

  case class NodeService(name: String, clusterNames: Seq[String])
  implicit val NODE_SERVICE_JSON_FORMAT: RootJsonFormat[NodeService] = jsonFormat2(NodeService)

  def node(name: String, port: Int, user: String, password: String): Node = Node(
    name = name,
    port = port,
    user = user,
    password = password,
    services = Seq.empty,
    lastModified = CommonUtil.current()
  )

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

    /**
      *  node's name should be unique in ohara so we make id same to name.
      * @return name
      */
    override def id: String = name
    override def kind: String = "node"
  }

  implicit val NODE_JSON_FORMAT: RootJsonFormat[Node] = jsonFormat6(Node)

  def access(): Access[NodeCreationRequest, Node] =
    new Access[NodeCreationRequest, Node](NODES_PREFIX_PATH)
}
