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

package com.island.ohara.agent
import java.net.URL
import java.util.Objects

import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
trait WorkerCollie extends Collie[WorkerClusterInfo] {
  def creator(): WorkerCollie.ClusterCreator

  /**
    * Create a worker client according to passed cluster name.
    * Noted: if target cluster doesn't exist, an future with exception will return
    * @param clusterName target cluster
    * @return cluster info and client
    */
  def workerClient(clusterName: String): Future[(WorkerClusterInfo, WorkerClient)] = cluster(clusterName).map {
    case (c, _) => (c, workerClient(c))
  }

  /**
    * Create a worker client according to passed cluster.
    * @param cluster target cluster
    * @return worker client
    */
  def workerClient(cluster: WorkerClusterInfo): WorkerClient = WorkerClient(cluster.connectionProps)
}

object WorkerCollie {
  trait ClusterCreator extends Collie.ClusterCreator[WorkerClusterInfo] {
    private[this] var clientPort: Int = -1
    private[this] var brokerClusterName: String = _
    private[this] var groupId: String = _
    private[this] var offsetTopicName: String = _
    private[this] var offsetTopicReplications: Short = 1
    private[this] var offsetTopicPartitions: Int = 1
    private[this] var configTopicName: String = _
    private[this] var configTopicReplications: Short = 1
    // configTopicPartitions must be 1
    private[this] var statusTopicName: String = _
    private[this] var statusTopicReplications: Short = 1
    private[this] var statusTopicPartitions: Int = 1
    private[this] var jarUrls: Seq[URL] = Seq.empty

    def brokerClusterName(name: String): ClusterCreator = {
      this.brokerClusterName = name
      this
    }

    def clientPort(clientPort: Int): ClusterCreator = {
      this.clientPort = clientPort
      this
    }

    def groupId(groupId: String): ClusterCreator = {
      this.groupId = groupId
      this
    }

    def offsetTopicName(offsetTopicName: String): ClusterCreator = {
      this.offsetTopicName = offsetTopicName
      this
    }

    @Optional("default number is 1")
    def offsetTopicReplications(replications: Option[Short]): ClusterCreator = {
      replications.foreach(offsetTopicReplications = _)
      this
    }

    @Optional("default number is 1")
    def offsetTopicReplications(replications: Short): ClusterCreator = offsetTopicReplications(Some(replications))

    @Optional("default number is 1")
    def offsetTopicPartitions(partitions: Option[Int]): ClusterCreator = {
      partitions.foreach(offsetTopicPartitions = _)
      this
    }

    @Optional("default number is 1")
    def offsetTopicPartitions(partitions: Int): ClusterCreator = offsetTopicPartitions(Some(partitions))

    def statusTopicName(statusTopicName: String): ClusterCreator = {
      this.statusTopicName = statusTopicName
      this
    }

    @Optional("default number is 1")
    def statusTopicReplications(replications: Option[Short]): ClusterCreator = {
      replications.foreach(statusTopicReplications = _)
      this
    }

    @Optional("default number is 1")
    def statusTopicReplications(replications: Short): ClusterCreator = statusTopicReplications(Some(replications))

    @Optional("default number is 1")
    def statusTopicPartitions(partitions: Option[Int]): ClusterCreator = {
      partitions.foreach(statusTopicPartitions = _)
      this
    }

    @Optional("default number is 1")
    def statusTopicPartitions(partitions: Int): ClusterCreator = statusTopicPartitions(Some(partitions))

    def configTopicName(configTopicName: String): ClusterCreator = {
      this.configTopicName = configTopicName
      this
    }

    @Optional("default number is 1")
    def configTopicReplications(replications: Option[Short]): ClusterCreator = {
      replications.foreach(configTopicReplications = _)
      this
    }

    @Optional("default number is 1")
    def configTopicReplications(replications: Short): ClusterCreator = configTopicReplications(Some(replications))

    @Optional("default is empty")
    def jarUrl(jarUrl: URL): ClusterCreator = jarUrls(Seq(jarUrl))

    @Optional("default is empty")
    def jarUrls(jarUrls: Seq[URL]): ClusterCreator = {
      this.jarUrls = jarUrls
      this
    }

    override def create(): Future[WorkerClusterInfo] = doCreate(
      clusterName = Objects.requireNonNull(clusterName),
      imageName = Objects.requireNonNull(imageName),
      brokerClusterName = Objects.requireNonNull(brokerClusterName),
      clientPort = CommonUtil.requirePositiveInt(clientPort, () => "clientPort is required"),
      groupId = Objects.requireNonNull(groupId),
      offsetTopicName = Objects.requireNonNull(offsetTopicName),
      offsetTopicReplications =
        CommonUtil.requirePositiveShort(offsetTopicReplications, () => "offsetTopicReplications is required"),
      offsetTopicPartitions =
        CommonUtil.requirePositiveInt(offsetTopicPartitions, () => "offsetTopicPartitions is required"),
      statusTopicName = Objects.requireNonNull(statusTopicName),
      statusTopicReplications =
        CommonUtil.requirePositiveShort(statusTopicReplications, () => "statusTopicReplications is required"),
      statusTopicPartitions =
        CommonUtil.requirePositiveInt(statusTopicPartitions, () => "statusTopicPartitions is required"),
      configTopicName = Objects.requireNonNull(configTopicName),
      configTopicReplications =
        CommonUtil.requirePositiveShort(configTopicReplications, () => "configTopicReplications is required"),
      Objects.requireNonNull(jarUrls),
      nodeNames =
        if (nodeNames == null || nodeNames.isEmpty) throw new NullPointerException("nodes can't be empty")
        else nodeNames
    )

    protected def doCreate(clusterName: String,
                           imageName: String,
                           brokerClusterName: String,
                           clientPort: Int,
                           groupId: String,
                           offsetTopicName: String,
                           offsetTopicReplications: Short,
                           offsetTopicPartitions: Int,
                           statusTopicName: String,
                           statusTopicReplications: Short,
                           statusTopicPartitions: Int,
                           configTopicName: String,
                           configTopicReplications: Short,
                           jarUrls: Seq[URL],
                           nodeNames: Seq[String]): Future[WorkerClusterInfo]
  }
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
  private[agent] val PLUGINS_KEY: String = "WORKER_PLUGINS"

}
