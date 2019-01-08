package com.island.ohara.client.configurator.v0

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object WorkerApi {
  val WORKER_PREFIX_PATH: String = "workers"

  /**
    * Create a basic request with default value.
    * @param name cluster name
    * @param nodeNames node names
    * @return request
    */
  def creationRequest(name: String, nodeNames: Seq[String]): WorkerClusterCreationRequest =
    WorkerClusterCreationRequest(
      name = name,
      imageName = None,
      brokerClusterName = None,
      clientPort = None,
      nodeNames = nodeNames
    )
  final case class WorkerClusterCreationRequest(name: String,
                                                imageName: Option[String],
                                                brokerClusterName: Option[String],
                                                clientPort: Option[Int],
                                                nodeNames: Seq[String])
      extends ClusterCreationRequest

  implicit val WORKER_CLUSTER_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[WorkerClusterCreationRequest] =
    jsonFormat5(WorkerClusterCreationRequest)

  final case class WorkerClusterInfo(name: String,
                                     imageName: String,
                                     brokerClusterName: String,
                                     clientPort: Int,
                                     groupId: String,
                                     statusTopicName: String,
                                     statusTopicPartitions: Int,
                                     statusTopicReplications: Short,
                                     configTopicName: String,
                                     configTopicPartitions: Int,
                                     configTopicReplications: Short,
                                     offsetTopicName: String,
                                     offsetTopicPartitions: Int,
                                     offsetTopicReplications: Short,
                                     nodeNames: Seq[String])
      extends ClusterInfo
  implicit val WORKER_CLUSTER_INFO_JSON_FORMAT: RootJsonFormat[WorkerClusterInfo] = jsonFormat15(WorkerClusterInfo)

  def access(): ClusterAccess[WorkerClusterCreationRequest, WorkerClusterInfo] =
    new ClusterAccess[WorkerClusterCreationRequest, WorkerClusterInfo](WORKER_PREFIX_PATH)
}
