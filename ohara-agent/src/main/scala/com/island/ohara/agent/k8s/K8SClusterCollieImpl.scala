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

package com.island.ohara.agent.k8s

import java.net.URL

import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.agent._
import com.island.ohara.agent.k8s.K8SClusterCollieImpl.{
  K8SBrokerCollieImpl,
  K8SWorkerCollieImpl,
  K8SZookeeperCollieImpl
}
import com.island.ohara.agent.ssh.connectors
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0.{BrokerApi, ClusterInfo}
import com.island.ohara.common.util.{CommonUtils, Releasable, ReleaseOnce}
import com.typesafe.scalalogging.Logger
import scala.concurrent.duration._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Try}

private[agent] class K8SClusterCollieImpl(nodeCollie: NodeCollie, k8sClient: K8SClient)
    extends ReleaseOnce
    with ClusterCollie {

  override def zookeeperCollie(): ZookeeperCollie = new K8SZookeeperCollieImpl(nodeCollie, k8sClient)

  override def brokerCollie(): BrokerCollie = new K8SBrokerCollieImpl(nodeCollie, k8sClient)

  override def workerCollie(): WorkerCollie = new K8SWorkerCollieImpl(nodeCollie, k8sClient)

  override protected def doClose(): Unit = Releasable.close(k8sClient)

  override def images(nodes: Seq[Node])(implicit executionContext: ExecutionContext): Future[Map[Node, Seq[String]]] =
    Future
      .traverse(nodes) { node =>
        k8sClient.images(node.name).map(images => node -> images)
      }
      .map(_.toMap)

  override def verifyNode(node: Node)(implicit executionContext: ExecutionContext): Future[Try[String]] =
    k8sClient
      .checkNode(node.name)
      .map(report => {
        val statusInfo = report.statusInfo.getOrElse(K8SStatusInfo(false, s"${node.name} node doesn't exists."))
        if (statusInfo.isHealth)
          Try(s"${node.name} node is running.")
        else
          Failure(
            new IllegalStateException(s"${node.name} node doesn't running container. cause: ${statusInfo.message}"))
      })
}

private object K8SClusterCollieImpl {
  private[this] val LOG = Logger(classOf[K8SClusterCollieImpl])
  private[this] val TIMEOUT: FiniteDuration = 30 seconds

  private[this] trait K8SBasicCollieImpl[T <: ClusterInfo, Creator <: ClusterCreator[T]]
      extends Collie[T, Creator]
      with Releasable {
    val nodeCollie: NodeCollie
    val k8sClient: K8SClient
    val service: Service

    protected def doAddNode(previousCluster: T, previousContainers: Seq[ContainerInfo], newNodeName: String)(
      implicit executionContext: ExecutionContext): Future[T]

    override def addNode(clusterName: String, nodeName: String)(
      implicit executionContext: ExecutionContext): Future[T] = {
      nodeCollie
        .node(nodeName) // make sure there is a exist node.
        .flatMap(_ => cluster(clusterName))
        .flatMap {
          case (c, cs) => doAddNode(c, cs, nodeName)
        }
    }

    /**
      * generate unique name for the container.
      * It can be used in setting container's hostname and name
      * @param clusterName cluster name
      * @return a formatted string. form: ${clusterName}-${service}-${index}
      */
    def format(clusterName: String): String =
      s"$clusterName-${service.name}-${CommonUtils.randomString(LENGTH_OF_UUID)}"

    protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
      implicit executionContext: ExecutionContext): Future[T]

    override def remove(clusterName: String)(implicit executionContext: ExecutionContext): Future[T] = {
      cluster(clusterName)
        .flatMap {
          case (cluster, containers) =>
            Future.sequence(containers.map(c => k8sClient.remove(c.name).map(_ => cluster)))
        }
        .map(clusters => clusters.head)
    }

    override def removeNode(clusterName: String, nodeName: String)(
      implicit executionContext: ExecutionContext): Future[T] = {
      containers(clusterName)
        .flatMap { runningContainers =>
          runningContainers.size match {
            case 0 => Future.failed(new IllegalArgumentException(s"$clusterName doesn't exist"))
            case 1 if runningContainers.map(_.nodeName).contains(nodeName) =>
              Future.failed(new IllegalArgumentException(
                s"$clusterName is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead"))
            case _ =>
              k8sClient.removeNode(clusterName, nodeName, service.name)
          }
        }
        .flatMap { _ =>
          cluster(clusterName).map(_._1)
        }
    }
    override def logs(clusterName: String)(
      implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] =
      k8sClient.containers
        .flatMap(
          cs =>
            Future.sequence(
              cs.filter(_.name.startsWith(clusterName))
                .map(container => k8sClient.log(container.hostname).map(container -> _))
          ))
        .map(_.toMap)

    def query(clusterName: String, service: Service)(
      implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = {
      nodeCollie.nodes().flatMap { nodes =>
        Future
          .sequence(nodes.map(n => {
            k8sClient.containers.map(cs => cs.filter(x => x.name.startsWith(s"$clusterName$DIVIDER${service.name}")))
          }))
          .map(_.flatten)
      }
    }

    override def clusters(implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] = nodeCollie
      .nodes()
      .flatMap(
        nodes =>
          Future.sequence(
            nodes.map(node => {
              k8sClient.containers.map(cs =>
                cs.filter(x => x.nodeName.equals(node.name) && x.name.contains(s"$DIVIDER${service.name}$DIVIDER")))
            })
        )
      )
      .map(_.flatten)
      .map(f => {
        f.map(container => container.name.split(DIVIDER).head -> container)
          .groupBy(_._1)
          .map {
            case (clusterName, value) => clusterName -> value.map(_._2)
          }
          .map {
            case (clusterName, containers) =>
              toClusterDescription(clusterName, containers).map(_ -> containers)
          }
          .toSeq
      })
      .flatMap(Future.sequence(_))
      .map(_.toMap)

    override def close(): Unit = {
      Releasable.close(k8sClient)
    }
  }

  private class K8SZookeeperCollieImpl(val nodeCollie: NodeCollie, val k8sClient: K8SClient)
      extends ZookeeperCollie
      with K8SBasicCollieImpl[ZookeeperClusterInfo, ZookeeperCollie.ClusterCreator] {

    override def creator(): ZookeeperCollie.ClusterCreator =
      (executionContext, clusterName, imageName, clientPort, peerPort, electionPort, nodeNames) => {
        implicit val exec: ExecutionContext = executionContext
        exist(clusterName)
          .flatMap(if (_) Future.failed(new IllegalArgumentException(s"zookeeper cluster:$clusterName exists!"))
          else nodeCollie.nodes(nodeNames))
          .map(_.map(node => node -> format(clusterName)).toMap)
          .map { nodes =>
            val zkServers: String = nodes.values.mkString(" ")
            val successfulNodeNames: Seq[String] = nodes.zipWithIndex
              .flatMap {
                case ((node, hostname), index) =>
                  try {
                    val creatorContainerInfo: Future[Option[ContainerInfo]] = k8sClient
                      .containerCreator()
                      .flatMap(
                        creator =>
                          creator
                            .imageName(imageName)
                            .portMappings(Map(
                              clientPort -> clientPort,
                              peerPort -> peerPort,
                              electionPort -> electionPort
                            ))
                            .nodename(node.name)
                            .hostname(s"${hostname}-${node.name}")
                            .labelName(OHARA_LABEL)
                            .domainName(K8S_DOMAIN_NAME)
                            .envs(Map(
                              ZookeeperCollie.ID_KEY -> index.toString,
                              ZookeeperCollie.CLIENT_PORT_KEY -> clientPort.toString,
                              ZookeeperCollie.PEER_PORT_KEY -> peerPort.toString,
                              ZookeeperCollie.ELECTION_PORT_KEY -> electionPort.toString,
                              ZookeeperCollie.SERVERS_KEY -> s"$zkServers-${node.name}"
                            ))
                            .name(hostname)
                            .run())
                    Await.result(creatorContainerInfo, TIMEOUT)
                  } catch {
                    case e: Throwable =>
                      LOG.error(s"failed to start $clusterName", e)
                      None
                  }
              }
              .map(_.nodeName)
              .toSeq
            if (successfulNodeNames.isEmpty)
              throw new IllegalArgumentException(s"failed to create $clusterName on $ZOOKEEPER")
            ZookeeperClusterInfo(
              name = clusterName,
              imageName = imageName,
              clientPort = clientPort,
              peerPort = peerPort,
              electionPort = electionPort,
              nodeNames = successfulNodeNames
            )
          }
      }

    override protected def doAddNode(
      previousCluster: ZookeeperClusterInfo,
      previousContainers: Seq[ContainerInfo],
      newNodeName: String)(implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
      Future.failed(
        new UnsupportedOperationException("zookeeper collie doesn't support to add node from a running cluster"))

    override val service: Service = ZOOKEEPER

    override def removeNode(clusterName: String, nodeName: String)(
      implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
      Future.failed(
        new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

    override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
      implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = Future.successful {
      val first = containers.head
      ZookeeperClusterInfo(
        name = clusterName,
        imageName = first.imageName,
        clientPort = first.environments(ZookeeperCollie.CLIENT_PORT_KEY).toInt,
        peerPort = first.environments(ZookeeperCollie.PEER_PORT_KEY).toInt,
        electionPort = first.environments(ZookeeperCollie.ELECTION_PORT_KEY).toInt,
        nodeNames = containers.map(_.nodeName)
      )
    }
  }

  private class K8SBrokerCollieImpl(val nodeCollie: NodeCollie, val k8sClient: K8SClient)
      extends BrokerCollie
      with K8SBasicCollieImpl[BrokerClusterInfo, BrokerCollie.ClusterCreator] {

    override def creator(): BrokerCollie.ClusterCreator =
      (executionContext,
       clusterName,
       imageName,
       zookeeperClusterName,
       clientPort,
       exporterPort,
       jmxPort,
       nodeNames) => {
        implicit val exec: ExecutionContext = executionContext
        exist(clusterName)
          .flatMap(if (_) containers(clusterName) else Future.successful(Seq.empty))
          .flatMap(existContainers =>
            nodeCollie
              .nodes(existContainers.map(_.nodeName))
              .map(_.zipWithIndex.map {
                case (node, index) => node -> existContainers(index)
              }.toMap)
              .map { existNodes =>
                // if there is a running cluster already, we should check the consistency of configuration
                existNodes.values.foreach {
                  container =>
                    def checkValue(previous: String, newValue: String): Unit =
                      if (previous != newValue) throw new IllegalArgumentException(s"previous:$previous new:$newValue")
                    def check(key: String, newValue: String): Unit = {
                      val previous = container.environments(key)
                      if (previous != newValue) throw new IllegalArgumentException(s"previous:$previous new:$newValue")
                    }
                    checkValue(container.imageName, imageName)
                    check(BrokerCollie.CLIENT_PORT_KEY, clientPort.toString)
                    check(ZOOKEEPER_CLUSTER_NAME, zookeeperClusterName)
                }
                existNodes
            })
          .flatMap(existNodes =>
            nodeCollie.nodes(nodeNames).map(_.map(node => node -> format(clusterName)).toMap).map((existNodes, _)))
          .flatMap {
            case (existNodes, newNodes) =>
              existNodes.keys.foreach(node =>
                if (newNodes.keys.exists(_.name == node.name))
                  throw new IllegalArgumentException(s"${node.name} has run the broker service for $clusterName"))

              query(zookeeperClusterName, ZOOKEEPER).map((existNodes, newNodes, _))
          }
          .map {
            case (existNodes, newNodes, zkContainers) =>
              if (zkContainers.isEmpty) throw new IllegalArgumentException(s"$clusterName doesn't exist")
              val zookeepers = zkContainers
                .map(c => s"${c.hostname}.$K8S_DOMAIN_NAME:${c.environments(ZookeeperCollie.CLIENT_PORT_KEY).toInt}")
                .mkString(",")

              val maxId: Int =
                if (existNodes.isEmpty) 0
                else existNodes.values.map(_.environments(BrokerCollie.ID_KEY).toInt).toSet.max + 1

              val successfulNodeNames = newNodes.zipWithIndex
                .map {
                  case ((node, hostname), index) =>
                    val client = k8sClient
                    try {
                      val creator: Future[Option[ContainerInfo]] = client
                        .containerCreator()
                        .flatMap(
                          creator =>
                            creator
                              .imageName(imageName)
                              .nodename(node.name)
                              .labelName(OHARA_LABEL)
                              .domainName(K8S_DOMAIN_NAME)
                              .portMappings(Map(
                                clientPort -> clientPort,
                                exporterPort -> exporterPort,
                                jmxPort -> jmxPort
                              ))
                              .hostname(hostname)
                              .envs(Map(
                                BrokerCollie.ID_KEY -> (maxId + index).toString,
                                BrokerCollie.CLIENT_PORT_KEY -> clientPort.toString,
                                BrokerCollie.ZOOKEEPERS_KEY -> zookeepers,
                                BrokerCollie.ADVERTISED_HOSTNAME_KEY -> node.name,
                                BrokerCollie.EXPORTER_PORT_KEY -> exporterPort.toString,
                                BrokerCollie.ADVERTISED_CLIENT_PORT_KEY -> clientPort.toString,
                                ZOOKEEPER_CLUSTER_NAME -> zookeeperClusterName,
                                BrokerCollie.JMX_HOSTNAME_KEY -> node.name,
                                BrokerCollie.JMX_PORT_KEY -> jmxPort.toString
                              ))
                              .name(hostname)
                              .run())
                      Await.result(creator, TIMEOUT)
                    } catch {
                      case e: Throwable =>
                        LOG.error(s"failed to start $imageName on ${node.name}", e)
                        None
                    }
                }
                .map(_.get.nodeName)
                .toSeq
              if (successfulNodeNames.isEmpty)
                throw new IllegalArgumentException(s"failed to create $clusterName on $BROKER")
              BrokerClusterInfo(
                name = clusterName,
                imageName = imageName,
                zookeeperClusterName = zookeeperClusterName,
                exporterPort = exporterPort,
                clientPort = clientPort,
                jmxPort = jmxPort,
                nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
              )
          }
      }

    override protected def doAddNode(
      previousCluster: BrokerClusterInfo,
      previousContainers: Seq[ContainerInfo],
      newNodeName: String)(implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = creator()
      .clusterName(previousCluster.name)
      .zookeeperClusterName(previousCluster.zookeeperClusterName)
      .clientPort(previousCluster.clientPort)
      .exporterPort(previousCluster.exporterPort)
      .jmxPort(previousCluster.jmxPort)
      .imageName(previousCluster.imageName)
      .nodeName(newNodeName)
      .create()

    override val service: Service = BROKER

    override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
      implicit executionContext: ExecutionContext): Future[BrokerClusterInfo] = Future.successful {
      val first = containers.head
      BrokerClusterInfo(
        name = clusterName,
        imageName = first.imageName,
        zookeeperClusterName = first.environments(ZOOKEEPER_CLUSTER_NAME),
        exporterPort = first.environments(BrokerCollie.EXPORTER_PORT_KEY).toInt,
        clientPort = first.environments(BrokerCollie.CLIENT_PORT_KEY).toInt,
        jmxPort = first.environments(BrokerCollie.JMX_PORT_KEY).toInt,
        nodeNames = containers.map(_.nodeName)
      )
    }
  }

  private class K8SWorkerCollieImpl(val nodeCollie: NodeCollie, val k8sClient: K8SClient)
      extends WorkerCollie
      with K8SBasicCollieImpl[WorkerClusterInfo, WorkerCollie.ClusterCreator] {

    override def creator(): WorkerCollie.ClusterCreator = (executionContext,
                                                           clusterName,
                                                           imageName,
                                                           brokerClusterName,
                                                           clientPort,
                                                           jmxPort,
                                                           groupId,
                                                           offsetTopicName,
                                                           offsetTopicReplications,
                                                           offsetTopicPartitions,
                                                           statusTopicName,
                                                           statusTopicReplications,
                                                           statusTopicPartitions,
                                                           configTopicName,
                                                           configTopicReplications,
                                                           jarUrls,
                                                           nodeNames) => {
      implicit val exec: ExecutionContext = executionContext
      exist(clusterName)
        .flatMap(if (_) containers(clusterName) else Future.successful(Seq.empty))
        .flatMap(
          existContainers =>
            nodeCollie
              .nodes(existContainers.map(_.nodeName))
              .map(_.zipWithIndex.map {
                case (node, index) => node -> existContainers(index)
              }.toMap)
              .map { existNodes =>
                // if there is a running cluster already, we should check the consistency of configuration
                existNodes.values.foreach {
                  container =>
                    def checkValue(previous: String, newValue: String): Unit =
                      if (previous != newValue) throw new IllegalArgumentException(s"previous:$previous new:$newValue")
                    def check(key: String, newValue: String): Unit = {
                      val previous = container.environments(key)
                      if (previous != newValue) throw new IllegalArgumentException(s"previous:$previous new:$newValue")
                    }
                    checkValue(container.imageName, imageName)
                    check(WorkerCollie.GROUP_ID_KEY, groupId)
                    check(WorkerCollie.OFFSET_TOPIC_KEY, offsetTopicName)
                    check(WorkerCollie.OFFSET_TOPIC_PARTITIONS_KEY, offsetTopicPartitions.toString)
                    check(WorkerCollie.OFFSET_TOPIC_REPLICATIONS_KEY, offsetTopicReplications.toString)
                    check(WorkerCollie.STATUS_TOPIC_KEY, statusTopicName)
                    check(WorkerCollie.STATUS_TOPIC_PARTITIONS_KEY, statusTopicPartitions.toString)
                    check(WorkerCollie.STATUS_TOPIC_REPLICATIONS_KEY, statusTopicReplications.toString)
                    check(WorkerCollie.CONFIG_TOPIC_KEY, configTopicName)
                    check(WorkerCollie.CONFIG_TOPIC_REPLICATIONS_KEY, configTopicReplications.toString)
                    check(WorkerCollie.CLIENT_PORT_KEY, clientPort.toString)
                    check(BROKER_CLUSTER_NAME, brokerClusterName)
                }
                existNodes
            })
        .flatMap(existNodes =>
          nodeCollie.nodes(nodeNames).map(_.map(node => node -> format(clusterName)).toMap).map((existNodes, _)))
        .flatMap {
          case (existNodes, newNodes) =>
            existNodes.keys.foreach(node =>
              if (newNodes.keys.exists(_.name == node.name))
                throw new IllegalArgumentException(s"${node.name} has run the worker service for $clusterName"))
            query(brokerClusterName, BROKER).map((existNodes, newNodes, _))
        }
        .map {
          case (existNodes, newNodes, brokerContainers) =>
            if (brokerContainers.isEmpty)
              throw new IllegalArgumentException(s"broker cluster:$brokerClusterName doesn't exist")
            val brokers = brokerContainers
              .map(c =>
                s"${c.hostname}.${K8S_DOMAIN_NAME}:${c.environments.getOrElse(BrokerCollie.CLIENT_PORT_KEY, BrokerApi.CLIENT_PORT_DEFAULT)}")
              .mkString(",")

            val successfulNodeNames = newNodes
              .map {
                case (node, hostname) =>
                  val client = k8sClient
                  try {
                    val creator: Future[Option[ContainerInfo]] = client
                      .containerCreator()
                      .flatMap(
                        creator =>
                          creator
                            .imageName(imageName)
                            .portMappings(Map(clientPort -> clientPort))
                            .hostname(hostname)
                            .nodename(node.name)
                            .envs(Map(
                              WorkerCollie.CLIENT_PORT_KEY -> clientPort.toString,
                              WorkerCollie.BROKERS_KEY -> brokers,
                              WorkerCollie.GROUP_ID_KEY -> groupId,
                              WorkerCollie.OFFSET_TOPIC_KEY -> offsetTopicName,
                              WorkerCollie.OFFSET_TOPIC_PARTITIONS_KEY -> offsetTopicPartitions.toString,
                              WorkerCollie.OFFSET_TOPIC_REPLICATIONS_KEY -> offsetTopicReplications.toString,
                              WorkerCollie.CONFIG_TOPIC_KEY -> configTopicName,
                              WorkerCollie.CONFIG_TOPIC_REPLICATIONS_KEY -> configTopicReplications.toString,
                              WorkerCollie.STATUS_TOPIC_KEY -> statusTopicName,
                              WorkerCollie.STATUS_TOPIC_PARTITIONS_KEY -> statusTopicPartitions.toString,
                              WorkerCollie.STATUS_TOPIC_REPLICATIONS_KEY -> statusTopicReplications.toString,
                              WorkerCollie.ADVERTISED_HOSTNAME_KEY -> node.name,
                              WorkerCollie.ADVERTISED_CLIENT_PORT_KEY -> clientPort.toString,
                              WorkerCollie.PLUGINS_KEY -> jarUrls.mkString(","),
                              BROKER_CLUSTER_NAME -> brokerClusterName,
                              WorkerCollie.JMX_HOSTNAME_KEY -> node.name,
                              WorkerCollie.JMX_PORT_KEY -> jmxPort.toString
                            ))
                            .labelName(OHARA_LABEL)
                            .domainName(K8S_DOMAIN_NAME)
                            .name(hostname)
                            .run())
                    Await.result(creator, TIMEOUT)
                  } catch {
                    case e: Throwable =>
                      LOG.error(s"failed to start $imageName", e)
                      None
                  }
              }
              .map(_.get.nodeName)
              .toSeq
            if (successfulNodeNames.isEmpty)
              throw new IllegalArgumentException(s"failed to create $clusterName on $WORKER")
            WorkerClusterInfo(
              name = clusterName,
              imageName = imageName,
              brokerClusterName = brokerClusterName,
              clientPort = clientPort,
              jmxPort = jmxPort,
              groupId = groupId,
              offsetTopicName = offsetTopicName,
              offsetTopicPartitions = offsetTopicPartitions,
              offsetTopicReplications = offsetTopicReplications,
              configTopicName = configTopicName,
              configTopicPartitions = 1,
              configTopicReplications = configTopicReplications,
              statusTopicName = statusTopicName,
              statusTopicPartitions = statusTopicPartitions,
              statusTopicReplications = statusTopicReplications,
              jarNames = jarUrls.map(_.getFile),
              connectors = Seq.empty,
              nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
            )
        }
    }

    override protected def doAddNode(
      previousCluster: WorkerClusterInfo,
      previousContainers: Seq[ContainerInfo],
      newNodeName: String)(implicit executionContext: ExecutionContext): Future[WorkerClusterInfo] = creator()
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

    override val service: Service = WORKER

    override protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo])(
      implicit executionContext: ExecutionContext): Future[WorkerClusterInfo] = {
      // TODO: the following code is duplicate to ssh collie. We should refactor them ... by chia
      val port = containers.head.environments(WorkerCollie.CLIENT_PORT_KEY).toInt
      connectors(containers.map(c => s"${c.nodeName}:$port").mkString(",")).map { connectors =>
        WorkerClusterInfo(
          name = clusterName,
          imageName = containers.head.imageName,
          brokerClusterName = containers.head.environments(BROKER_CLUSTER_NAME),
          clientPort = containers.head.environments(WorkerCollie.CLIENT_PORT_KEY).toInt,
          jmxPort = containers.head.environments(WorkerCollie.JMX_PORT_KEY).toInt,
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
  }

  val K8S_DOMAIN_NAME: String = "default"

  val OHARA_LABEL: String = "ohara"

  private[this] val BROKER_CLUSTER_NAME = "K8S_BROKER_CLUSTER_NAME"

  private[this] val ZOOKEEPER_CLUSTER_NAME = "K8S_ZOOKEEPER_CLUSTER_NAME"

  private[agent] val DIVIDER: String = "-"

  private[this] sealed abstract class Service {
    def name: String
  }

  private[this] case object ZOOKEEPER extends Service {
    override def name: String = "zookeeper"
  }
  private[this] case object BROKER extends Service {
    override def name: String = "broker"
  }
  private[this] case object WORKER extends Service {
    override def name: String = "worker"
  }

  private[this] val LENGTH_OF_UUID: Int = 10

}
