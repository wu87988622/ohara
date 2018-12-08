package com.island.ohara.agent
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object AgentJson {

  case class Node(name: String, port: Int, user: String, password: String)

  implicit val NODE_JSON_FORMAT: RootJsonFormat[Node] = jsonFormat4(Node)

  final case class ZookeeperCluster(name: String,
                                    clientPort: Int,
                                    peerPort: Int,
                                    electionPort: Int,
                                    nodeNames: Seq[String])
  implicit val ZOOKEEPER_CLUSTER_JSON_FORMAT: RootJsonFormat[ZookeeperCluster] = jsonFormat5(ZookeeperCluster)

  final case class BrokerCluster(name: String, zookeeperClusterName: String, clientPort: Int, nodeNames: Seq[String])
  implicit val BROKER_CLUSTER_JSON_FORMAT: RootJsonFormat[BrokerCluster] = jsonFormat4(BrokerCluster)

  final case class WorkerCluster(name: String, brokerClusterName: String, clientPort: Int, nodeNames: Seq[String])
  implicit val WORKER_CLUSTER_JSON_FORMAT: RootJsonFormat[WorkerCluster] = jsonFormat4(WorkerCluster)
}
