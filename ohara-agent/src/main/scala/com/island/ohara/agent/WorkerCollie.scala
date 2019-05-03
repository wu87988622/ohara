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

import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.WorkerApi
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.basic.CounterMBean

import collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
trait WorkerCollie extends Collie[WorkerClusterInfo, WorkerCollie.ClusterCreator] {

  /**
    * Create a worker client according to passed cluster name.
    * Noted: if target cluster doesn't exist, an future with exception will return
    * @param clusterName target cluster
    * @return cluster info and client
    */
  def workerClient(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[(WorkerClusterInfo, WorkerClient)] = cluster(clusterName).map {
    case (c, _) => (c, workerClient(c))
  }

  /**
    * Create a worker client according to passed cluster.
    * @param cluster target cluster
    * @return worker client
    */
  def workerClient(cluster: WorkerClusterInfo): WorkerClient = WorkerClient(cluster.connectionProps)

  /**
    * Get all counter beans from specific worker cluster
    * @param clusterName cluster name
    * @param executionContext thread pool
    * @return counter beans
    */
  def counters(clusterName: String)(implicit executionContext: ExecutionContext): Future[Seq[CounterMBean]] =
    cluster(clusterName).map(_._1).map(counters)

  /**
    * Get all counter beans from specific worker cluster
    * @param cluster cluster
    * @return counter beans
    */
  def counters(cluster: WorkerClusterInfo): Seq[CounterMBean] = cluster.nodeNames.flatMap { node =>
    BeanChannel.builder().hostname(node).port(cluster.jmxPort).build().counterMBeans().asScala
  }

  protected def doAddNode(previousCluster: WorkerClusterInfo,
                          previousContainers: Seq[ContainerInfo],
                          newNodeName: String)(implicit executionContext: ExecutionContext): Future[WorkerClusterInfo] =
    creator()
      .clusterName(previousCluster.name)
      .brokerClusterName(previousCluster.brokerClusterName)
      .clientPort(previousCluster.clientPort)
      .groupId(previousCluster.groupId)
      .offsetTopicName(previousCluster.offsetTopicName)
      .statusTopicName(previousCluster.statusTopicName)
      .configTopicName(previousCluster.configTopicName)
      .imageName(previousCluster.imageName)
      .jarUrls(
        previousContainers.head
          .environments(WorkerCollie.PLUGINS_KEY)
          .split(",")
          .filter(_.nonEmpty)
          .map(s => new URL(s)))
      .nodeName(newNodeName)
      .jmxPort(previousCluster.jmxPort)
      .create()
}

object WorkerCollie {
  trait ClusterCreator extends Collie.ClusterCreator[WorkerClusterInfo] {
    private[this] var clientPort: Int = WorkerApi.CLIENT_PORT_DEFAULT
    private[this] var brokerClusterName: String = _
    private[this] var groupId: String = CommonUtils.randomString(10)
    private[this] var offsetTopicName: String = s"$groupId-offsetTopicName"
    private[this] var offsetTopicReplications: Short = 1
    private[this] var offsetTopicPartitions: Int = 1
    private[this] var configTopicName: String = s"$groupId-configTopicName"
    private[this] var configTopicReplications: Short = 1
    // configTopicPartitions must be 1
    private[this] var statusTopicName: String = s"$groupId-statusTopicName"
    private[this] var statusTopicReplications: Short = 1
    private[this] var statusTopicPartitions: Int = 1
    private[this] var jarUrls: Seq[URL] = Seq.empty
    private[this] var jmxPort: Int = WorkerApi.JMX_PORT_DEFAULT

    def brokerClusterName(name: String): ClusterCreator = {
      this.brokerClusterName = CommonUtils.requireNonEmpty(name)
      this
    }

    @Optional("default is WorkerApi.CLIENT_PORT_DEFAULT.WorkerApi.CLIENT_PORT_DEFAULT")
    def clientPort(clientPort: Int): ClusterCreator = {
      this.clientPort = CommonUtils.requirePositiveInt(clientPort)
      this
    }

    @Optional("default is random string")
    def groupId(groupId: String): ClusterCreator = {
      this.groupId = CommonUtils.requireNonEmpty(groupId)
      this
    }

    @Optional("default is random string")
    def offsetTopicName(offsetTopicName: String): ClusterCreator = {
      this.offsetTopicName = CommonUtils.requireNonEmpty(offsetTopicName)
      this
    }

    @Optional("default number is 1")
    def offsetTopicReplications(offsetTopicReplications: Short): ClusterCreator = {
      this.offsetTopicReplications = CommonUtils.requirePositiveShort(offsetTopicReplications)
      this
    }
    @Optional("default number is 1")
    def offsetTopicPartitions(offsetTopicPartitions: Int): ClusterCreator = {
      this.offsetTopicPartitions = CommonUtils.requirePositiveInt(offsetTopicPartitions)
      this
    }

    @Optional("default is random string")
    def statusTopicName(statusTopicName: String): ClusterCreator = {
      this.statusTopicName = CommonUtils.requireNonEmpty(statusTopicName)
      this
    }

    @Optional("default number is 1")
    def statusTopicReplications(statusTopicReplications: Short): ClusterCreator = {
      this.statusTopicReplications = CommonUtils.requirePositiveShort(statusTopicReplications)
      this
    }
    @Optional("default number is 1")
    def statusTopicPartitions(statusTopicPartitions: Int): ClusterCreator = {
      this.statusTopicPartitions = CommonUtils.requirePositiveInt(statusTopicPartitions)
      this
    }

    def configTopicName(configTopicName: String): ClusterCreator = {
      this.configTopicName = CommonUtils.requireNonEmpty(configTopicName)
      this
    }

    @Optional("default number is 1")
    def configTopicReplications(configTopicReplications: Short): ClusterCreator = {
      this.configTopicReplications = CommonUtils.requirePositiveShort(configTopicReplications)
      this
    }

    @Optional("default is empty")
    def jarUrl(jarUrl: URL): ClusterCreator = jarUrls(Seq(jarUrl))

    @Optional("default is empty")
    def jarUrls(jarUrls: Seq[URL]): ClusterCreator = {
      this.jarUrls = Objects.requireNonNull(jarUrls)
      this
    }

    @Optional("default is WorkerApi.CLIENT_PORT_DEFAULT")
    def jmxPort(jmxPort: Int): ClusterCreator = {
      this.jmxPort = CommonUtils.requirePositiveInt(jmxPort)
      this
    }

    override def create()(implicit executionContext: ExecutionContext): Future[WorkerClusterInfo] = doCreate(
      executionContext = Objects.requireNonNull(executionContext),
      clusterName = CommonUtils.requireNonEmpty(clusterName),
      imageName = CommonUtils.requireNonEmpty(imageName),
      brokerClusterName = CommonUtils.requireNonEmpty(brokerClusterName),
      clientPort = CommonUtils.requirePositiveInt(clientPort),
      jmxPort = CommonUtils.requirePositiveInt(jmxPort),
      groupId = CommonUtils.requireNonEmpty(groupId),
      offsetTopicName = CommonUtils.requireNonEmpty(offsetTopicName),
      offsetTopicReplications = CommonUtils.requirePositiveShort(offsetTopicReplications),
      offsetTopicPartitions = CommonUtils.requirePositiveInt(offsetTopicPartitions),
      statusTopicName = CommonUtils.requireNonEmpty(statusTopicName),
      statusTopicReplications = CommonUtils.requirePositiveShort(statusTopicReplications),
      statusTopicPartitions = CommonUtils.requirePositiveInt(statusTopicPartitions),
      configTopicName = CommonUtils.requireNonEmpty(configTopicName),
      configTopicReplications = CommonUtils.requirePositiveShort(configTopicReplications),
      jarUrls = Objects.requireNonNull(jarUrls),
      nodeNames = CommonUtils.requireNonEmpty(nodeNames.asJava).asScala
    )

    protected def doCreate(executionContext: ExecutionContext,
                           clusterName: String,
                           imageName: String,
                           brokerClusterName: String,
                           clientPort: Int,
                           jmxPort: Int,
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
  private[agent] val JMX_HOSTNAME_KEY: String = "JMX_HOSTNAME"
  private[agent] val JMX_PORT_KEY: String = "JMX_PORT"
}
