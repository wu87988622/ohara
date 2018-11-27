package com.island.ohara.agent

import com.island.ohara.agent.DockerJson.ContainerDescription
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object KafkaJson {
  case class Node(hostname: String, port: Int, user: String, password: String)
  implicit val NODE_JSON_FORMAT: RootJsonFormat[Node] = jsonFormat4(Node)

  final case class ZookeeperCluster(name: String,
                                    clientPort: Int,
                                    followerPort: Int,
                                    electionPort: Int,
                                    nodes: Seq[String])
  implicit val ZOOKEEPER_CLUSTER_JSON_FORMAT: RootJsonFormat[ZookeeperCluster] = jsonFormat5(ZookeeperCluster)

  final case class BrokerCluster(name: String, zookeeperCluster: String, port: Int, nodes: Seq[String])
  implicit val BROKER_CLUSTER_JSON_FORMAT: RootJsonFormat[BrokerCluster] = jsonFormat4(BrokerCluster)

  final case class WorkerCluster(name: String, brokerCluster: String, port: Int, nodes: Seq[String])
  implicit val WORKER_CLUSTER_JSON_FORMAT: RootJsonFormat[WorkerCluster] = jsonFormat4(WorkerCluster)

  final case class ClusterDescription(name: String, nodes: Seq[ContainerDescription])
  implicit val CLUSTER_DESCRIPTION_JSON_FORMAT: RootJsonFormat[ClusterDescription] = jsonFormat2(ClusterDescription)

}
