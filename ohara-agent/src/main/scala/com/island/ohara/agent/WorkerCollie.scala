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
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.{CommonUtil, VersionUtil}

import scala.concurrent.Future

trait WorkerCollie extends Collie[WorkerClusterInfo] {
  def creator(): WorkerCollie.ClusterCreator
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
      port.foreach(this.clientPort = _)
      this
    }

    @Optional("default is 8083")
    def clientPort(port: Int): ClusterCreator = clientPort(Some(port))

    @Optional("group id can be generated automatically")
    def groupId(groupId: String): ClusterCreator = {
      this.groupId = groupId
      this
    }
    @Optional("group id can be generated automatically")
    def offsetTopicName(offsetTopicName: String): ClusterCreator = {
      this.offsetTopicName = offsetTopicName
      this
    }
    @Optional("default number is 1")
    def offsetTopicReplications(numberOfReplications: Short): ClusterCreator = {
      this.offsetTopicReplications = numberOfReplications
      this
    }
    @Optional("default number is 1")
    def offsetTopicPartitions(numberOfPartitions: Int): ClusterCreator = {
      this.offsetTopicPartitions = numberOfPartitions
      this
    }
    @Optional("status topic can be generated automatically")
    def statusTopicName(statusTopicName: String): ClusterCreator = {
      this.statusTopicName = statusTopicName
      this
    }
    @Optional("default number is 1")
    def statusTopicReplications(numberOfReplications: Short): ClusterCreator = {
      this.statusTopicReplications = numberOfReplications
      this
    }
    @Optional("default number is 1")
    def statusTopicPartitions(numberOfPartitions: Int): ClusterCreator = {
      this.statusTopicPartitions = numberOfPartitions
      this
    }
    @Optional("config topic can be generated automatically")
    def configTopicName(configTopicName: String): ClusterCreator = {
      this.configTopicName = configTopicName
      this
    }
    @Optional("default number is 1")
    def configTopicReplications(numberOfReplications: Short): ClusterCreator = {
      this.configTopicReplications = numberOfReplications
      this
    }

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
