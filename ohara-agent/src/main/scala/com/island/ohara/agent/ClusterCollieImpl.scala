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
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import com.island.ohara.agent.ClusterCollieImpl._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0.{ClusterInfo, InfoApi}
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.client.kafka.WorkerJson.Plugin
import com.island.ohara.common.util.{CommonUtil, Releasable, ReleaseOnce}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.{ClassTag, classTag}

private[agent] class ClusterCollieImpl(implicit nodeCollie: NodeCollie) extends ReleaseOnce with ClusterCollie {
  private[this] implicit val clientCache: DockerClientCache = new DockerClientCache {
    private[this] val cache: ConcurrentMap[Node, DockerClient] = new ConcurrentHashMap[Node, DockerClient]()
    override def get(node: Node): DockerClient = cache.computeIfAbsent(
      node,
      node =>
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build())

    override def close(): Unit = {
      cache.values().forEach(client => Releasable.close(client))
      cache.clear()
    }
  }

  private[this] val zkCollie: ZookeeperCollieImpl = new ZookeeperCollieImpl {
    override def allClusters(): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = ClusterCollieImpl.this.clusters()
  }

  private[this] val bkCollie: BrokerCollieImpl = new BrokerCollieImpl {
    override def allClusters(): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = ClusterCollieImpl.this.clusters()
  }

  private[this] val wkCollie: WorkerCollieImpl = new WorkerCollieImpl {
    override def allClusters(): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = ClusterCollieImpl.this.clusters()
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

  override def clusters(): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = {
    nodeCollie
      .nodes()
      .flatMap(Future
        .traverse(_) { node =>
          // multi-thread to seek all containers from multi-nodes
          Future { clientCache.get(node).activeContainers(_.startsWith(PREFIX_KEY)) }.recover {
            case e: Throwable =>
              ClusterCollieImpl.LOG.error(s"failed to get active containers from $node", e)
              Seq.empty
          }
        }
        .map(_.flatten))
      .flatMap { allContainers =>
        def parse(
          serviceName: String,
          f: (String, Seq[ContainerInfo]) => Future[ClusterInfo]): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = Future
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
  }

  override protected def doClose(): Unit = Releasable.close(clientCache)

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

private object ClusterCollieImpl {

  /**
    * It tried to fetch connector information from starting worker cluster
    * However, it may be too slow to get latest connector information.
    * We don't throw exception since it is a common case, and Skipping retry can make quick response
    * @param connectionProps worker connection props
    * @return plugin description or nothing
    */
  private def plugins(connectionProps: String): Future[Seq[Plugin]] =
    WorkerClient(connectionProps, maxRetry = 0).plugins().recover {
      case e: Throwable =>
        LOG.error(s"Failed to fetch connectors information of cluster:$connectionProps. Use empty list instead", e)
        Seq.empty
    }

  private val LOG = Logger(classOf[ClusterCollieImpl])

  /**
    * This interface enable us to reuse the docker client object.
    */
  private trait DockerClientCache extends Releasable {
    def get(node: Node): DockerClient
    def get(nodes: Seq[Node]): Seq[DockerClient] = nodes.map(get)
  }

  private[this] abstract class BasicCollieImpl[T <: ClusterInfo: ClassTag](implicit clientCache: DockerClientCache,
                                                                           nodeCollie: NodeCollie)
      extends Collie[T] {

    def allClusters(): Future[Map[ClusterInfo, Seq[ContainerInfo]]]

    final override def clusters(): Future[Map[T, Seq[ContainerInfo]]] =
      allClusters().map {
        _.filter(entry => classTag[T].runtimeClass.isInstance(entry._1)).map {
          case (cluster, containers) => cluster.asInstanceOf[T] -> containers
        }
      }

    val serviceName: String =
      if (classTag[T].runtimeClass.isAssignableFrom(classOf[ZookeeperClusterInfo])) "zk"
      else if (classTag[T].runtimeClass.isAssignableFrom(classOf[BrokerClusterInfo])) "bk"
      else if (classTag[T].runtimeClass.isAssignableFrom(classOf[WorkerClusterInfo])) "wk"
      else throw new IllegalArgumentException(s"Who are you, ${classTag[T].runtimeClass} ???")

    def updateRoute(client: DockerClient, containerName: String, route: Map[String, String]): Unit =
      client
        .containerInspector(containerName)
        .asRoot()
        .append("/etc/hosts", route.map {
          case (hostname, ip) => s"$ip $hostname"
        }.toSeq)

    /**
      * generate unique name for the container.
      * It can be used in setting container's hostname and name
      * @param clusterName cluster name
      * @return a formatted string. form: ${clusterName}-${service}-${index}
      */
    def format(clusterName: String): String =
      Seq(
        PREFIX_KEY,
        clusterName,
        serviceName,
        CommonUtil.randomString(LENGTH_OF_CONTAINER_NAME_ID)
      ).mkString(DIVIDER)

    override def remove(clusterName: String): Future[T] = cluster(clusterName).flatMap {
      case (cluster, _) =>
        nodeCollie.nodes(cluster.nodeNames).map { nodes =>
          nodes.foreach(node => stopAndRemoveService(clientCache.get(node), clusterName, true, true))
          cluster
        }
    }

    /**
      * a helper method used to do "stop" and "remove".
      * NOTED: this method may be expensive...
      * @param client docker client
      * @param clusterName cluster name
      */
    def stopAndRemoveService(client: DockerClient, clusterName: String, swallow: Boolean, force: Boolean): Unit =
      try {
        val key = s"$PREFIX_KEY$DIVIDER$clusterName$DIVIDER$serviceName"
        val containerNames = client.containerNames().filter(_.startsWith(key))
        if (containerNames.nonEmpty) {
          var lastException: Throwable = null
          containerNames.foreach(
            name =>
              try if (force) client.forceRemove(name) else client.stop(name)
              catch {
                case e: Throwable =>
                  LOG.error(s"failed to stop $name", e)
                  lastException = e
            })
          if (lastException != null) throw lastException
        }
      } catch {
        case e: Throwable =>
          if (swallow) LOG.error(s"failed to cleanup $clusterName on $serviceName", e)
          else throw e
      }

    override def logs(clusterName: String): Future[Map[ContainerInfo, String]] = nodeCollie
      .nodes()
      .map(
        _.flatMap(
          clientCache
            .get(_)
            // we allow user to get logs from "exited" containers!
            .containers(_.startsWith(s"$PREFIX_KEY$DIVIDER$clusterName$DIVIDER$serviceName"))))
      .flatMap { containers =>
        nodeCollie
          .nodes(containers.map(_.nodeName))
          .map(n => clientCache.get(n))
          .map(_.zipWithIndex.map {
            case (client, index) =>
              val container = containers(index)
              container -> client.log(container.name)
          }.toMap)
      }

    override def removeNode(clusterName: String, nodeName: String): Future[T] = containers(clusterName)
      .flatMap { runningContainers =>
        runningContainers.size match {
          case 0 => Future.failed(new IllegalArgumentException(s"$clusterName doesn't exist"))
          case 1 if runningContainers.map(_.nodeName).contains(nodeName) =>
            Future.failed(new IllegalArgumentException(
              s"$clusterName is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead"))
          case _ => nodeCollie.node(nodeName)
        }
      }
      .flatMap { targetNode =>
        stopAndRemoveService(clientCache.get(targetNode), clusterName, false, false)
        cluster(clusterName).map(_._1)
      }

    protected def doAddNode(previousCluster: T, previousContainers: Seq[ContainerInfo], newNodeName: String): Future[T]

    override def addNode(clusterName: String, nodeName: String): Future[T] =
      nodeCollie
        .node(nodeName) // make sure there is a exist node.
        .flatMap(_ => cluster(clusterName))
        .flatMap {
          case (c, cs) => doAddNode(c, cs, nodeName)
        }
  }

  private abstract class ZookeeperCollieImpl(implicit nodeCollie: NodeCollie, clientCache: DockerClientCache)
      extends BasicCollieImpl[ZookeeperClusterInfo]
      with ZookeeperCollie {

    /**
      * This is a complicated process. We must address following issues.
      * 1) check the existence of cluster
      * 2) check the existence of nodes
      * 3) Each zookeeper container has got to export peer port, election port, and client port
      * 4) Each zookeeper container should use "docker host name" to replace "container host name".
      * 4) Add routes to all zookeeper containers
      * @return creator of broker cluster
      */
    override def creator(): ZookeeperCollie.ClusterCreator =
      (clusterName, imageName, clientPort, peerPort, electionPort, nodeNames) =>
        allClusters().flatMap { clusters =>
          if (clusters.keys.filter(_.isInstanceOf[ZookeeperClusterInfo]).exists(_.name == clusterName))
            Future.failed(new IllegalArgumentException(s"zookeeper cluster:$clusterName exists!"))
          else
            nodeCollie.nodes(nodeNames).map(_.map(node => node -> format(clusterName)).toMap).flatMap { nodes =>
              // add route in order to make zk node can connect to each other.
              val route: Map[String, String] = nodes.map {
                case (node, _) =>
                  node.name -> CommonUtil.address(node.name)
              }
              val zkServers: String = nodes.keys.map(_.name).mkString(" ")

              // ssh connection is slow so we submit request by multi-thread
              Future
                .sequence(nodes.zipWithIndex.map {
                  case ((node, containerName), index) =>
                    val client = clientCache.get(node)
                    Future {
                      try {
                        client
                          .containerCreator()
                          .imageName(imageName)
                          .portMappings(Map(
                            clientPort -> clientPort,
                            peerPort -> peerPort,
                            electionPort -> electionPort
                          ))
                          // zookeeper doesn't have advertised hostname/port so we assign the "docker host" directly
                          .hostname(node.name)
                          .envs(Map(
                            ZookeeperCollie.ID_KEY -> index.toString,
                            ZookeeperCollie.CLIENT_PORT_KEY -> clientPort.toString,
                            ZookeeperCollie.PEER_PORT_KEY -> peerPort.toString,
                            ZookeeperCollie.ELECTION_PORT_KEY -> electionPort.toString,
                            ZookeeperCollie.SERVERS_KEY -> zkServers
                          ))
                          .name(containerName)
                          .route(route)
                          .execute()
                        Some(node.name)
                      } catch {
                        case e: Throwable =>
                          stopAndRemoveService(client, clusterName, true, true)
                          LOG.error(s"failed to start $clusterName", e)
                          None
                      }
                    }
                })
                .map(_.flatten.toSeq)
                .map { successfulNodeNames =>
                  if (successfulNodeNames.isEmpty)
                    throw new IllegalArgumentException(s"failed to create $clusterName on $serviceName")
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
      }

    override def removeNode(clusterName: String, nodeName: String): Future[ZookeeperClusterInfo] =
      Future.failed(
        new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

    override protected def doAddNode(previousCluster: ZookeeperClusterInfo,
                                     previousContainers: Seq[ContainerInfo],
                                     newNodeName: String): Future[ZookeeperClusterInfo] =
      Future.failed(
        new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))
  }

  private abstract class BrokerCollieImpl(implicit nodeCollie: NodeCollie, clientCache: DockerClientCache)
      extends BasicCollieImpl[BrokerClusterInfo]
      with BrokerCollie {

    /**
      * This is a complicated process. We must address following issues.
      * 1) check the existence of cluster
      * 2) check the existence of nodes
      * 3) Each broker container has got to export exporter port and client port
      * 4) Each broker container should assign "docker host name/port" to advertised name/port
      * 5) add zookeeper routes to all broker containers (broker needs to connect to zookeeper cluster)
      * 6) Add broker routes to all broker containers
      * 7) update existed containers (if we are adding new node into a running cluster)
      * @return creator of broker cluster
      */
    override def creator(): BrokerCollie.ClusterCreator =
      (clusterName, imageName, zookeeperClusterName, clientPort, exporterPort, nodeNames) =>
        allClusters().flatMap { clusters =>
          clusters
            .filter(_._1.isInstanceOf[BrokerClusterInfo])
            .map {
              case (cluster, containers) => cluster.asInstanceOf[BrokerClusterInfo] -> containers
            }
            .find(_._1.name == clusterName)
            .map(_._2)
            .map(containers =>
              nodeCollie
                .nodes(containers.map(_.nodeName))
                .map(_.map(node => node -> containers.find(_.nodeName == node.name).get).toMap))
            .getOrElse(Future.successful(Map.empty))
            .map { existNodes =>
              // if there is a running cluster already, we should check the consistency of configuration
              existNodes.values.foreach { container =>
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
            }
            .flatMap(existNodes =>
              nodeCollie.nodes(nodeNames).map(_.map(node => node -> format(clusterName)).toMap).map((existNodes, _)))
            .map {
              case (existNodes, newNodes) =>
                existNodes.keys.foreach(node =>
                  if (newNodes.keys.exists(_.name == node.name))
                    throw new IllegalArgumentException(s"${node.name} has run the broker service for $clusterName"))
                clusters
                  .filter(_._1.isInstanceOf[ZookeeperClusterInfo])
                  .find(_._1.name == zookeeperClusterName)
                  .map(_._2)
                  .map((existNodes, newNodes, _))
                  .getOrElse(throw new NoSuchClusterException(s"zookeeper cluster:$zookeeperClusterName doesn't exist"))
            }
            .flatMap {
              case (existNodes, newNodes, zkContainers) =>
                if (zkContainers.isEmpty) throw new IllegalArgumentException(s"$clusterName doesn't exist")
                val zookeepers = zkContainers
                  .map(c => s"${c.nodeName}:${c.environments(ZookeeperCollie.CLIENT_PORT_KEY).toInt}")
                  .mkString(",")

                val existRoute: Map[String, String] = existNodes.map {
                  case (node, container) => container.nodeName -> CommonUtil.address(node.name)
                }
                // add route in order to make broker node can connect to each other (and zk node).
                val route: Map[String, String] = newNodes.map {
                  case (node, _) =>
                    node.name -> CommonUtil.address(node.name)
                } ++ zkContainers
                  .map(zkContainer => zkContainer.nodeName -> CommonUtil.address(zkContainer.nodeName))
                  .toMap

                // update the route since we are adding new node to a running broker cluster
                // we don't need to update startup broker list since kafka do the update for us.
                existNodes.foreach {
                  case (node, container) => updateRoute(clientCache.get(node), container.name, route)
                }

                // the new broker node can't take used id so we find out the max id which is used by current cluster
                val maxId: Int =
                  if (existNodes.isEmpty) 0
                  else existNodes.values.map(_.environments(BrokerCollie.ID_KEY).toInt).toSet.max + 1

                // ssh connection is slow so we submit request by multi-thread
                Future
                  .sequence(newNodes.zipWithIndex.map {
                    case ((node, hostname), index) =>
                      val client = clientCache.get(node)
                      Future {
                        try {
                          client
                            .containerCreator()
                            .imageName(imageName)
                            .portMappings(Map(
                              clientPort -> clientPort,
                              exporterPort -> exporterPort
                            ))
                            .hostname(hostname)
                            .envs(Map(
                              BrokerCollie.ID_KEY -> (maxId + index).toString,
                              BrokerCollie.CLIENT_PORT_KEY -> clientPort.toString,
                              BrokerCollie.ZOOKEEPERS_KEY -> zookeepers,
                              BrokerCollie.ADVERTISED_HOSTNAME_KEY -> node.name,
                              BrokerCollie.EXPORTER_PORT_KEY -> exporterPort.toString,
                              BrokerCollie.ADVERTISED_CLIENT_PORT_KEY -> clientPort.toString,
                              ZOOKEEPER_CLUSTER_NAME -> zookeeperClusterName
                            ))
                            .name(hostname)
                            .route(route ++ existRoute)
                            .execute()
                          Some(node.name)
                        } catch {
                          case e: Throwable =>
                            stopAndRemoveService(client, clusterName, true, true)
                            LOG.error(s"failed to start $imageName on ${node.name}", e)
                            None
                        }
                      }
                  })
                  .map(_.flatten.toSeq)
                  .map { successfulNodeNames =>
                    if (successfulNodeNames.isEmpty)
                      throw new IllegalArgumentException(s"failed to create $clusterName on $serviceName")
                    BrokerClusterInfo(
                      name = clusterName,
                      imageName = imageName,
                      zookeeperClusterName = zookeeperClusterName,
                      exporterPort = exporterPort,
                      clientPort = clientPort,
                      nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
                    )
                  }
            }
      }

    override protected def doAddNode(previousCluster: BrokerClusterInfo,
                                     previousContainers: Seq[ContainerInfo],
                                     newNodeName: String): Future[BrokerClusterInfo] = creator()
      .clusterName(previousCluster.name)
      .zookeeperClusterName(previousCluster.zookeeperClusterName)
      .exporterPort(previousCluster.exporterPort)
      .clientPort(previousCluster.clientPort)
      .imageName(previousCluster.imageName)
      .nodeName(newNodeName)
      .create()
  }

  private abstract class WorkerCollieImpl(implicit nodeCollie: NodeCollie, clientCache: DockerClientCache)
      extends BasicCollieImpl[WorkerClusterInfo]
      with WorkerCollie {

    /**
      * This is a complicated process. We must address following issues.
      * 1) check the existence of cluster
      * 2) check the existence of nodes
      * 3) Each worker container has got to export exporter port and client port
      * 4) Each worker container should assign "docker host name/port" to advertised name/port
      * 5) add broker routes to all worker containers (worker needs to connect to broker cluster)
      * 6) Add worker routes to all worker containers
      * 7) update existed containers (if we are adding new node into a running cluster)
      * @return description of worker cluster
      */
    override def creator(): WorkerCollie.ClusterCreator = (clusterName,
                                                           imageName,
                                                           brokerClusterName,
                                                           clientPort,
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
                                                           nodeNames) =>
      allClusters().flatMap { clusters =>
        clusters
          .filter(_._1.isInstanceOf[WorkerClusterInfo])
          .map {
            case (cluster, containers) => cluster.asInstanceOf[WorkerClusterInfo] -> containers
          }
          .find(_._1.name == clusterName)
          .map(_._2)
          .map(containers =>
            nodeCollie
              .nodes(containers.map(_.nodeName))
              .map(_.map(node => node -> containers.find(_.nodeName == node.name).get).toMap))
          .getOrElse(Future.successful(Map.empty))
          .flatMap(existNodes =>
            nodeCollie.nodes(nodeNames).map(_.map(node => node -> format(clusterName)).toMap).map((existNodes, _)))
          .map {
            case (existNodes, newNodes) =>
              existNodes.keys.foreach(node =>
                if (newNodes.keys.exists(_.name == node.name))
                  throw new IllegalArgumentException(s"${node.name} has run the worker service for $clusterName"))
              clusters
                .filter(_._1.isInstanceOf[BrokerClusterInfo])
                .find(_._1.name == brokerClusterName)
                .map(_._2)
                .map((existNodes, newNodes, _))
                .getOrElse(throw new NoSuchClusterException(s"broker cluster:$brokerClusterName doesn't exist"))
          }
          .flatMap {
            case (existNodes, newNodes, brokerContainers) =>
              if (brokerContainers.isEmpty)
                throw new IllegalArgumentException(s"broker cluster:$brokerClusterName doesn't exist")
              val brokers = brokerContainers
                .map(c => s"${c.nodeName}:${c.environments(BrokerCollie.CLIENT_PORT_KEY).toInt}")
                .mkString(",")

              val existRoute: Map[String, String] = existNodes.map {
                case (node, container) => container.hostname -> CommonUtil.address(node.name)
              }
              // add route in order to make broker node can connect to each other (and broker node).
              val route: Map[String, String] = newNodes.map {
                case (node, _) =>
                  node.name -> CommonUtil.address(node.name)
              } ++ brokerContainers
                .map(brokerContainer => brokerContainer.nodeName -> CommonUtil.address(brokerContainer.nodeName))
                .toMap

              // update the route since we are adding new node to a running worker cluster
              // we don't need to update startup broker list (WorkerCollie.BROKERS_KEY) since kafka do the update for us.
              existNodes.foreach {
                case (node, container) => updateRoute(clientCache.get(node), container.name, route)
              }

              // ssh connection is slow so we submit request by multi-thread
              Future
                .sequence(newNodes.map {
                  case (node, hostname) =>
                    val client = clientCache.get(node)
                    Future {
                      try {
                        client
                          .containerCreator()
                          .imageName(imageName)
                          // In --network=host mode, we don't need to export port for containers.
                          // However, this op doesn't hurt us so we don't remove it.
                          .portMappings(Map(clientPort -> clientPort))
                          .hostname(hostname)
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
                            BROKER_CLUSTER_NAME -> brokerClusterName
                          ))
                          .name(hostname)
                          .route(route ++ existRoute)
                          // we use --network=host for worker cluster since the connectors run on worker cluster may need to
                          // access external system to request data. In ssh mode, dns service "may" be not deployed.
                          // In order to simplify their effort, we directly mount host's route on the container.
                          // This is not a normal case I'd say. However, we always meet special case which must be addressed
                          // by this "special" solution...
                          .networkDriver(NETWORK_DRIVER)
                          .execute()
                        Some(node.name)
                      } catch {
                        case e: Throwable =>
                          stopAndRemoveService(client, clusterName, true, true)
                          LOG.error(s"failed to start $imageName", e)
                          None
                      }
                    }
                })
                .map(_.flatten.toSeq)
                .flatMap { successfulNodeNames =>
                  if (successfulNodeNames.isEmpty)
                    throw new IllegalArgumentException(s"failed to create $clusterName on $serviceName")
                  val nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
                  plugins(nodeNames.map(n => s"$n:$clientPort").mkString(",")).map { plugins =>
                    WorkerClusterInfo(
                      name = clusterName,
                      imageName = imageName,
                      brokerClusterName = brokerClusterName,
                      clientPort = clientPort,
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
                      sources = plugins.filter(_.typeName.toLowerCase == "source").map(InfoApi.toConnectorVersion),
                      sinks = plugins.filter(_.typeName.toLowerCase == "source").map(InfoApi.toConnectorVersion),
                      nodeNames = nodeNames
                    )
                  }
                }
          }
    }

    override protected def doAddNode(previousCluster: WorkerClusterInfo,
                                     previousContainers: Seq[ContainerInfo],
                                     newNodeName: String): Future[WorkerClusterInfo] = {
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
        .create()
    }
  }

  /**
    * We need this prefix in order to distinguish our containers from others.
    * DON'T change this constant string. Otherwise, it will break compatibility.
    * We don't use a complex string since docker limit the length of name...
    */
  private val PREFIX_KEY = "occl"

  /**
    * internal key used to save the broker cluster name.
    * All nodes of worker cluster should have this environment variable.
    */
  private val BROKER_CLUSTER_NAME = "CCI_BROKER_CLUSTER_NAME"

  /**
    * internal key used to save the zookeeper cluster name.
    * All nodes of broker cluster should have this environment variable.
    */
  private val ZOOKEEPER_CLUSTER_NAME = "CCI_ZOOKEEPER_CLUSTER_NAME"

  /**
    * used to distinguish the cluster name and service name
    */
  private val DIVIDER: String = "-"

  private[this] val LENGTH_OF_CONTAINER_NAME_ID: Int = 7

  /**
    * In ssh mode we use host driver to mount /etc/hosts from container host.
    */
  private val NETWORK_DRIVER: NetworkDriver = NetworkDriver.HOST
}
