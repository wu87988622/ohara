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

package com.island.ohara.agent.ssh

import java.net.URL
import java.util.concurrent.ExecutorService

import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.{Releasable, ReleaseOnce}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

private[agent] class ClusterCollieImpl(expiredTime: Duration, nodeCollie: NodeCollie, executor: ExecutorService)
    extends ReleaseOnce
    with ClusterCollie {

  private[this] val dockerCache = DockerClientCache()
  private[this] val clusterCache: Cache[Map[ClusterInfo, Seq[ContainerInfo]]] = Cache
    .builder[Map[ClusterInfo, Seq[ContainerInfo]]]()
    .expiredTime(expiredTime)
    .default(Map.empty)
    .updater((executionContext: ExecutionContext) => doClusters(executionContext))
    .executor(executor)
    .build()

  private[this] val zkCollie: ZookeeperCollieImpl = new ZookeeperCollieImpl(nodeCollie, dockerCache, clusterCache)

  private[this] val bkCollie: BrokerCollieImpl = new BrokerCollieImpl(nodeCollie, dockerCache, clusterCache)

  private[this] val wkCollie: WorkerCollieImpl = new WorkerCollieImpl(nodeCollie, dockerCache, clusterCache)

  override def zookeeperCollie(): ZookeeperCollie = zkCollie
  override def brokerCollie(): BrokerCollie = bkCollie
  override def workerCollie(): WorkerCollie = wkCollie

  private[this] def toZkCluster(clusterName: String, containers: Seq[ContainerInfo]): Future[ClusterInfo] = {
    val first = containers.head
    Future.successful(
      ZookeeperClusterInfo(
        name = clusterName,
        imageName = first.imageName,
        clientPort = first.environments(ZookeeperCollie.CLIENT_PORT_KEY).toInt,
        peerPort = first.environments(ZookeeperCollie.PEER_PORT_KEY).toInt,
        electionPort = first.environments(ZookeeperCollie.ELECTION_PORT_KEY).toInt,
        nodeNames = containers.map(_.nodeName)
      ))
  }

  private[this] def toWkCluster(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[ClusterInfo] = {
    val port = containers.head.environments(WorkerCollie.CLIENT_PORT_KEY).toInt
    connectors(containers.map(c => s"${c.nodeName}:$port").mkString(",")).map { connectors =>
      WorkerClusterInfo(
        name = clusterName,
        imageName = containers.head.imageName,
        brokerClusterName = containers.head.environments(BROKER_CLUSTER_NAME),
        clientPort = port,
        groupId = containers.head.environments(WorkerCollie.GROUP_ID_KEY),
        offsetTopicName = containers.head.environments(WorkerCollie.OFFSET_TOPIC_KEY),
        offsetTopicPartitions = containers.head.environments(WorkerCollie.OFFSET_TOPIC_PARTITIONS_KEY).toInt,
        offsetTopicReplications = containers.head.environments(WorkerCollie.OFFSET_TOPIC_REPLICATIONS_KEY).toShort,
        configTopicName = containers.head.environments(WorkerCollie.CONFIG_TOPIC_KEY),
        configTopicPartitions = 1,
        configTopicReplications = containers.head.environments(WorkerCollie.CONFIG_TOPIC_REPLICATIONS_KEY).toShort,
        statusTopicName = containers.head.environments(WorkerCollie.STATUS_TOPIC_KEY),
        statusTopicPartitions = containers.head.environments(WorkerCollie.STATUS_TOPIC_PARTITIONS_KEY).toInt,
        statusTopicReplications = containers.head.environments(WorkerCollie.STATUS_TOPIC_REPLICATIONS_KEY).toShort,
        jarNames = containers.head
          .environments(WorkerCollie.PLUGINS_KEY)
          .split(",")
          .filter(_.nonEmpty)
          .map(u => new URL(u).getFile),
        connectors = connectors,
        nodeNames = containers.map(_.nodeName)
      )
    }
  }

  private[this] def toBkCluster(clusterName: String, containers: Seq[ContainerInfo]): Future[ClusterInfo] = {
    val first = containers.head
    Future.successful(
      BrokerClusterInfo(
        name = clusterName,
        imageName = first.imageName,
        zookeeperClusterName = first.environments(ZOOKEEPER_CLUSTER_NAME),
        exporterPort = first.environments(BrokerCollie.EXPORTER_PORT_KEY).toInt,
        clientPort = first.environments(BrokerCollie.CLIENT_PORT_KEY).toInt,
        nodeNames = containers.map(_.nodeName)
      ))
  }

  override def clusters(implicit executionContext: ExecutionContext): Future[Map[ClusterInfo, Seq[ContainerInfo]]] =
    clusterCache.get
  private[this] def doClusters(
    implicit executionContext: ExecutionContext): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = nodeCollie
    .nodes()
    .flatMap(Future
      .traverse(_) { node =>
        // multi-thread to seek all containers from multi-nodes
        dockerCache.exec(node, _.activeContainers(containerName => containerName.startsWith(PREFIX_KEY))).recover {
          case e: Throwable =>
            LOG.error(s"failed to get active containers from $node", e)
            Seq.empty
        }
      }
      .map(_.flatten))
    .flatMap { allContainers =>
      def parse(serviceName: String,
                f: (String, Seq[ContainerInfo]) => Future[ClusterInfo]): Future[Map[ClusterInfo, Seq[ContainerInfo]]] =
        Future
          .sequence(
            allContainers
              .filter(_.name.contains(s"$DIVIDER$serviceName$DIVIDER"))
              // form: PREFIX_KEY-CLUSTER_NAME-SERVICE-HASH
              .map(container => container.name.split(DIVIDER)(1) -> container)
              .groupBy(_._1)
              .map {
                case (clusterName, value) => clusterName -> value.map(_._2)
              }
              .map {
                case (clusterName, containers) => f(clusterName, containers).map(_ -> containers)
              })
          .map(_.toMap)

      parse(zkCollie.serviceName, toZkCluster).flatMap { zkMap =>
        parse(bkCollie.serviceName, toBkCluster).flatMap { bkMap =>
          parse(wkCollie.serviceName, toWkCluster).map { wkMap =>
            zkMap ++ bkMap ++ wkMap
          }
        }
      }
    }

  override protected def doClose(): Unit = {
    Releasable.close(dockerCache)
    Releasable.close(clusterCache)
  }

  override def images(nodes: Seq[Node])(implicit executionContext: ExecutionContext): Future[Map[Node, Seq[String]]] =
    Future.traverse(nodes)(node => Future(dockerCache.exec(node, node -> _.imageNames()))).map(_.toMap)
}
