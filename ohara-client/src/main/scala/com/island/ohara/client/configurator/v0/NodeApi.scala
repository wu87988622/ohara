package com.island.ohara.client.configurator.v0
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object NodeApi {
  val NODES_PREFIX_PATH: String = "nodes"
  case class NodeCreationRequest(name: Option[String], port: Int, user: String, password: String)
  implicit val NODE_REQUEST_JSON_FORMAT: RootJsonFormat[NodeCreationRequest] = jsonFormat4(NodeCreationRequest)

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
