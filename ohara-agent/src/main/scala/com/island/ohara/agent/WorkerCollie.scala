package com.island.ohara.agent
import com.island.ohara.client.ConfiguratorJson.WorkerClusterDescription
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.{Releasable, VersionUtil}

import scala.concurrent.Future

trait WorkerCollie extends Releasable with Collie[WorkerClusterDescription] {
  def creator(): WorkerCollie.ClusterCreator
}

object WorkerCollie {
  trait ClusterCreator extends Collie.ClusterCreator[WorkerClusterDescription] {
    def brokerClusterName(name: String): ClusterCreator
    def clientPort(clientPort: Int): ClusterCreator
    @Optional("group id cant be generated automatically")
    def groupId(groupId: String): ClusterCreator
    @Optional("group id cant be generated automatically")
    def offsetTopicName(offsetTopicName: String): ClusterCreator
    @Optional("default number is 1")
    def offsetTopicReplications(numberOfReplications: Short): ClusterCreator
    @Optional("default number is 1")
    def offsetTopicPartitions(numberOfPartitions: Int): ClusterCreator
    @Optional("status topic cant be generated automatically")
    def statusTopicName(statusTopicName: String): ClusterCreator
    @Optional("default number is 1")
    def statusTopicReplications(numberOfReplications: Short): ClusterCreator
    @Optional("default number is 1")
    def statusTopicPartitions(numberOfPartitions: Int): ClusterCreator
    @Optional("config topic cant be generated automatically")
    def configTopicName(configTopicName: String): ClusterCreator
    @Optional("default number is 1")
    def configTopicReplications(numberOfReplications: Short): ClusterCreator
    def create(nodeNames: Seq[String]): Future[WorkerClusterDescription]
  }

  /**
    * ohara-it needs this property for testing.
    */
  private[ohara] val IMAGE_NAME_DEFAULT: String = s"islandsystems/connect-worker:${VersionUtil.VERSION}"
  private[agent] val GROUP_ID_KEY: String = "WORKER_GROUP"
  private[agent] val OFFSET_TOPIC_KEY: String = "WORKER_OFFSET_TOPIC"
  private[agent] val OFFSET_TOPIC_REPLICATIONS_KEY: String = "WORKER_OFFSET_TOPIC_REPLICATIONS"
  private[agent] val OFFSET_TOPIC_PARTITIONS_KEY: String = "WORKER_OFFSET_TOPIC_PARTITIONS"
  private[agent] val CONFIG_TOPIC_KEY: String = "WORKER_CONFIG_TOPIC"
  private[agent] val CONFIG_TOPIC_REPLICATIONS_KEY: String = "WORKER_CONFIG_TOPIC_REPLICATIONS"
  private[agent] val STATUS_TOPIC_KEY: String = "WORKER_STATUS_TOPIC"
  private[agent] val STATUS_TOPIC_REPLICATIONS_KEY: String = "WORKER_STATUS_TOPIC_REPLICATIONS"
  private[agent] val STATUS_TOPIC_PARTITIONS_KEY: String = "WORKER_STATUS_TOPIC_PARTITIONS"
  private[agent] val BROKERS_KEY: String = "WORKER_BROKERS"
  private[agent] val ADVERTISED_HOSTNAME_KEY: String = "WORKER_ADVERTISED_HOSTNAME"
  private[agent] val ADVERTISED_CLIENT_PORT_KEY: String = "WORKER_ADVERTISED_CLIENT_PORT"
  private[agent] val CLIENT_PORT_KEY: String = "WORKER_CLIENT_PORT"
  private[agent] val CLIENT_PORT_DEFAULT: Int = 8083

}
