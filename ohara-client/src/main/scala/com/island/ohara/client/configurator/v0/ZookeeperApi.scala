package com.island.ohara.client.configurator.v0
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object ZookeeperApi {
  val ZOOKEEPER_PREFIX_PATH: String = "zookeepers"

  /**
    * Create a basic request with default value.
    * @param name cluster name
    * @param nodeNames node names
    * @return request
    */
  def creationRequest(name: String, nodeNames: Seq[String]): ZookeeperClusterCreationRequest =
    ZookeeperClusterCreationRequest(
      name = name,
      imageName = None,
      clientPort = None,
      peerPort = None,
      electionPort = None,
      nodeNames = nodeNames
    )
  final case class ZookeeperClusterCreationRequest(name: String,
                                                   imageName: Option[String],
                                                   clientPort: Option[Int],
                                                   peerPort: Option[Int],
                                                   electionPort: Option[Int],
                                                   nodeNames: Seq[String])
      extends ClusterCreationRequest

  implicit val ZOOKEEPER_CLUSTER_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[ZookeeperClusterCreationRequest] =
    jsonFormat6(ZookeeperClusterCreationRequest)

  final case class ZookeeperClusterInfo(name: String,
                                        imageName: String,
                                        clientPort: Int,
                                        peerPort: Int,
                                        electionPort: Int,
                                        nodeNames: Seq[String])
      extends ClusterInfo

  implicit val ZOOKEEPER_CLUSTER_INFO_JSON_FORMAT: RootJsonFormat[ZookeeperClusterInfo] = jsonFormat6(
    ZookeeperClusterInfo)

  def access(): ClusterAccess[ZookeeperClusterCreationRequest, ZookeeperClusterInfo] =
    new ClusterAccess[ZookeeperClusterCreationRequest, ZookeeperClusterInfo](ZOOKEEPER_PREFIX_PATH)
}
