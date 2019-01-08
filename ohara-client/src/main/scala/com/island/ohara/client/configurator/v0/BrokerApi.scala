package com.island.ohara.client.configurator.v0

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object BrokerApi {
  val BROKER_PREFIX_PATH: String = "brokers"

  /**
    * Create a basic request with default value.
    * @param name cluster name
    * @param nodeNames node names
    * @return request
    */
  def creationRequest(name: String, nodeNames: Seq[String]): BrokerClusterCreationRequest =
    BrokerClusterCreationRequest(
      name = name,
      imageName = None,
      zookeeperClusterName = None,
      clientPort = None,
      nodeNames = nodeNames
    )
  final case class BrokerClusterCreationRequest(name: String,
                                                imageName: Option[String],
                                                zookeeperClusterName: Option[String],
                                                clientPort: Option[Int],
                                                nodeNames: Seq[String])
      extends ClusterCreationRequest

  implicit val BROKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[BrokerClusterCreationRequest] =
    jsonFormat5(BrokerClusterCreationRequest)
  final case class BrokerClusterInfo(name: String,
                                     imageName: String,
                                     zookeeperClusterName: String,
                                     clientPort: Int,
                                     nodeNames: Seq[String])
      extends ClusterInfo
  implicit val BROKER_CLUSTER_INFO_JSON_FORMAT: RootJsonFormat[BrokerClusterInfo] = jsonFormat5(BrokerClusterInfo)

  def access(): ClusterAccess[BrokerClusterCreationRequest, BrokerClusterInfo] =
    new ClusterAccess[BrokerClusterCreationRequest, BrokerClusterInfo](BROKER_PREFIX_PATH)
}
