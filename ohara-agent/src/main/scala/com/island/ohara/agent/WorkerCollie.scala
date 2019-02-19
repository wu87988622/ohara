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
import com.island.ohara.common.util.{CommonUtil, VersionUtil}

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
    private[this] var clientPort: Int = WorkerCollie.CLIENT_PORT_DEFAULT
    private[this] var brokerClusterName: String = _
    private[this] var groupId: String = CommonUtil.randomString()
    private[this] var offsetTopicName = s"$groupId-offset-topic"
    private[this] var offsetTopicReplications: Short = 1
    private[this] var offsetTopicPartitions: Int = 1
    private[this] var configTopicName = s"$groupId-config-topic"
    private[this] var configTopicReplications: Short = 1
    // configTopicPartitions must be 1
    private[this] var statusTopicName = s"$groupId-status-topic"
    private[this] var statusTopicReplications: Short = 1
    private[this] var statusTopicPartitions: Int = 1
    private[this] var jarUrls: Seq[URL] = Seq.empty

    def brokerClusterName(name: String): ClusterCreator = {
      this.brokerClusterName = name
      this
    }

    @Optional("default is 8083")
    def clientPort(port: Option[Int]): ClusterCreator = {
      port.foreach(clientPort = _)
      this
    }

    @Optional("default is 8083")
    def clientPort(port: Int): ClusterCreator = clientPort(Some(port))

    @Optional("group id can be generated automatically")
    def groupId(id: Option[String]): ClusterCreator = {
      id.foreach(groupId = _)
      this
    }

    @Optional("group id can be generated automatically")
    def groupId(id: String): ClusterCreator = groupId(Some(id))

    @Optional("group id can be generated automatically")
    def offsetTopicName(name: Option[String]): ClusterCreator = {
      name.foreach(offsetTopicName = _)
      this
    }

    @Optional("group id can be generated automatically")
    def offsetTopicName(name: String): ClusterCreator = offsetTopicName(Some(name))

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

    @Optional("status topic can be generated automatically")
    def statusTopicName(name: Option[String]): ClusterCreator = {
      name.foreach(statusTopicName = _)
      this
    }

    @Optional("status topic can be generated automatically")
    def statusTopicName(name: String): ClusterCreator = statusTopicName(Some(name))

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

    @Optional("config topic can be generated automatically")
    def configTopicName(name: Option[String]): ClusterCreator = {
      name.foreach(configTopicName = _)
      this
    }

    @Optional("config topic can be generated automatically")
    def configTopicName(name: String): ClusterCreator = configTopicName(Some(name))

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
      imageName = Option(imageName).getOrElse(WorkerCollie.IMAGE_NAME_DEFAULT),
      brokerClusterName = Objects.requireNonNull(brokerClusterName),
      clientPort = CommonUtil.requirePositiveInt(clientPort, () => "clientPort should be positive number"),
      groupId = Objects.requireNonNull(groupId),
      offsetTopicName = Objects.requireNonNull(offsetTopicName),
      offsetTopicReplications = CommonUtil
        .requirePositiveShort(offsetTopicReplications, () => "offsetTopicReplications should be positive number"),
      offsetTopicPartitions =
        CommonUtil.requirePositiveInt(offsetTopicPartitions, () => "offsetTopicPartitions should be positive number"),
      statusTopicName = Objects.requireNonNull(statusTopicName),
      statusTopicReplications = CommonUtil
        .requirePositiveShort(statusTopicReplications, () => "statusTopicReplications should be positive number"),
      statusTopicPartitions =
        CommonUtil.requirePositiveInt(statusTopicPartitions, () => "statusTopicPartitions should be positive number"),
      configTopicName = Objects.requireNonNull(configTopicName),
      configTopicReplications = CommonUtil
        .requirePositiveShort(configTopicReplications, () => "configTopicReplications should be positive number"),
      Objects.requireNonNull(jarUrls),
      nodeNames =
        if (nodeNames == null || nodeNames.isEmpty) throw new IllegalArgumentException("nodes can't be empty")
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

  /**
    * ohara-it needs this property for testing.
    */
  private[ohara] val IMAGE_NAME_DEFAULT: String = s"oharastream/connect-worker:${VersionUtil.VERSION}"
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
  private[agent] val PLUGINS_KEY: String = "WORKER_PLUGINS"

}
