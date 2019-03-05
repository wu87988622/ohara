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

import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0.{ClusterInfo, InfoApi}
import com.island.ohara.common.util.{Releasable, ReleaseOnce}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

private[agent] class ClusterCollieImpl(implicit nodeCollie: NodeCollie) extends ReleaseOnce with ClusterCollie {
  private[this] implicit val clientCache: DockerClientCache = new DockerClientCache

  private[this] val zkCollie: ZookeeperCollieImpl = new ZookeeperCollieImpl {
    override def allClusters(containerNameFilter: String => Boolean): Future[Map[ClusterInfo, Seq[ContainerInfo]]] =
      ClusterCollieImpl.this.allClusters(containerNameFilter)
  }

  private[this] val bkCollie: BrokerCollieImpl = new BrokerCollieImpl {
    override def allClusters(containerNameFilter: String => Boolean): Future[Map[ClusterInfo, Seq[ContainerInfo]]] =
      ClusterCollieImpl.this.allClusters(containerNameFilter)
  }

  private[this] val wkCollie: WorkerCollieImpl = new WorkerCollieImpl {
    override def allClusters(containerNameFilter: String => Boolean): Future[Map[ClusterInfo, Seq[ContainerInfo]]] =
      ClusterCollieImpl.this.allClusters(containerNameFilter)
  }

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

  private[this] def toWkCluster(clusterName: String, containers: Seq[ContainerInfo]): Future[ClusterInfo] = {
    val port = containers.head.environments(WorkerCollie.CLIENT_PORT_KEY).toInt
    plugins(containers.map(c => s"${c.nodeName}:$port").mkString(",")).map { plugins =>
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
        sources = plugins.filter(_.typeName.toLowerCase == "source").map(InfoApi.toConnectorVersion),
        sinks = plugins.filter(_.typeName.toLowerCase == "source").map(InfoApi.toConnectorVersion),
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

  /**
    * list all clusters passed by name filter.
    * NOTED: The filter introduced here is used to reduce ssh communication overhead of remote node. Each container detail
    * spends a ssh connection, hence the cost is proportional to number of containers.
    * @param containerNameFilter name filter
    * @return matched clusters
    */
  def allClusters(containerNameFilter: String => Boolean): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = nodeCollie
    .nodes()
    .flatMap(Future
      .traverse(_) { node =>
        // multi-thread to seek all containers from multi-nodes
        Future {
          clientCache
            .get(node)
            .activeContainers(containerName =>
              containerName.startsWith(PREFIX_KEY) && containerNameFilter(containerName))
        }.recover {
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

  override def clusters(): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = allClusters(_ => true)

  override protected def doClose(): Unit = {
    Releasable.close(clientCache)
//    Releasable.close(clustersCache)
  }

  override def images(nodes: Seq[Node]): Future[Map[Node, Seq[String]]] =
    Future
      .traverse(nodes) { node =>
        Future {
          val dockerClient =
            DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
          try node -> dockerClient.imageNames()
          finally dockerClient.close()
        }
      }
      .map(_.toMap)

}
