package com.island.ohara.agent
import java.net.URL
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import com.island.ohara.agent.ClusterCollieImpl._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.{CommonUtil, Releasable, ReleaseOnce}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

private[agent] class ClusterCollieImpl(implicit nodeCollie: NodeCollie) extends ReleaseOnce with ClusterCollie {
  private[this] implicit val clientCache: DockerClientCache = new DockerClientCache {
    private[this] val cache: ConcurrentMap[Node, DockerClient] = new ConcurrentHashMap[Node, DockerClient]()
    override def get(node: Node): DockerClient = cache.computeIfAbsent(
      node,
      node =>
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build())

    override def close(): Unit = {
      cache.values().forEach(client => ReleaseOnce.close(client))
      cache.clear()
    }
  }
  override def zookeepersCollie(): ZookeeperCollie = new ZookeeperCollieImpl
  override def brokerCollie(): BrokerCollie = new BrokerCollieImpl
  override def workerCollie(): WorkerCollie = new WorkerCollieImpl
  override protected def doClose(): Unit = ReleaseOnce.close(clientCache)
}

private object ClusterCollieImpl {
  private[this] val LOG = Logger(classOf[ClusterCollieImpl])

  /**
    * This interface enable us to reuse the docker client object.
    */
  private trait DockerClientCache extends Releasable {
    def get(node: Node): DockerClient
    def get(nodes: Seq[Node]): Seq[DockerClient] = nodes.map(get)
  }

  private[this] trait BasicCollieImpl[T <: ClusterInfo] extends Collie[T] {

    val clientCache: DockerClientCache
    val nodeCollie: NodeCollie

    val service: Service

    protected def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo]): T

    override def clusters(): Future[Map[T, Seq[ContainerInfo]]] = nodeCollie
      .nodes()
      .map(
        _.flatMap(clientCache.get(_).containers().filter(_.name.contains(s"$DIVIDER${service.name}$DIVIDER")))
          .map(container => container.name.split(DIVIDER).head -> container)
          .groupBy(_._1)
          .map {
            case (clusterName, value) => clusterName -> value.map(_._2)
          }
          .map {
            case (clusterName, containers) => toClusterDescription(clusterName, containers) -> containers
          }
          .toMap)

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
    def format(clusterName: String): String = s"$clusterName-${service.name}-${CommonUtil.randomString(LENGTH_OF_UUID)}"

    override def remove(clusterName: String): Future[T] = cluster(clusterName).flatMap {
      case (cluster, _) =>
        nodeCollie.nodes(cluster.nodeNames).map { nodes =>
          nodes.foreach(node => stopAndRemoveService(clientCache.get(node), clusterName, true))
          cluster
        }
    }

    /**
      * a helper method used to do "stop" and "remove".
      * NOTED: this method may be expensive...
      * @param client docker client
      * @param clusterName cluster name
      */
    def stopAndRemoveService(client: DockerClient, clusterName: String, swallow: Boolean): Unit =
      try {
        val key = s"$clusterName$DIVIDER${service.name}"
        val containers = client.containers().filter(_.name.startsWith(key))
        if (containers.nonEmpty) {
          var lastException: Throwable = null
          containers.foreach(
            container =>
              try client.stop(container.name)
              catch {
                case e: Throwable =>
                  LOG.error(s"failed to stop $container", e)
                  lastException = e
            })
          // we need to list the containers again since the "cleanup" containers will be removed automatically
          client
            .containers()
            .filter(_.name.startsWith(key))
            .foreach(container =>
              try client.remove(container.name)
              catch {
                case e: Throwable =>
                  LOG.error(s"failed to remove $container", e)
                  lastException = e
            })
          if (lastException != null) throw lastException
        }
      } catch {
        case e: Throwable =>
          if (swallow) LOG.error(s"failed to cleanup $clusterName on $service", e)
          else throw e
      }

    /**
      * get all containers belonging to specified cluster.
      * @param clusterName cluster name
      * @return containers information
      */
    def query(clusterName: String, service: Service): Future[Seq[ContainerInfo]] = nodeCollie
      .nodes()
      .map(_.flatMap(clientCache.get(_).containers().filter(_.name.startsWith(s"$clusterName$DIVIDER${service.name}"))))

    override def logs(clusterName: String): Future[Map[ContainerInfo, String]] = query(clusterName, service).flatMap {
      containers =>
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
        stopAndRemoveService(clientCache.get(targetNode), clusterName, false)
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

  private class ZookeeperCollieImpl(implicit val nodeCollie: NodeCollie, val clientCache: DockerClientCache)
      extends ZookeeperCollie
      with BasicCollieImpl[ZookeeperClusterInfo] {

    override val service: Service = ZOOKEEPER

    override def creator(): ZookeeperCollie.ClusterCreator =
      (clusterName, imageName, clientPort, peerPort, electionPort, nodeNames) =>
        exists(clusterName)
          .flatMap(if (_) Future.failed(new IllegalArgumentException(s"zookeeper cluster:$clusterName exists!"))
          else nodeCollie.nodes(nodeNames))
          .map(_.map(node => node -> format(clusterName)).toMap)
          .map { nodes =>
            // add route in order to make zk node can connect to each other.
            val route: Map[String, String] = nodes.map {
              case (node, _) =>
                node.name -> CommonUtil.address(node.name)
            }
            val zkServers: String = nodes.values.mkString(" ")
            val successfulNodeNames: Seq[String] = nodes.zipWithIndex
              .flatMap {
                case ((node, hostname), index) =>
                  val client = clientCache.get(node)
                  try client
                    .containerCreator()
                    .imageName(imageName)
                    .portMappings(
                      Map(
                        clientPort -> clientPort,
                        peerPort -> peerPort,
                        electionPort -> electionPort
                      ))
                    .hostname(hostname)
                    .envs(Map(
                      ZookeeperCollie.ID_KEY -> index.toString,
                      ZookeeperCollie.CLIENT_PORT_KEY -> clientPort.toString,
                      ZookeeperCollie.PEER_PORT_KEY -> peerPort.toString,
                      ZookeeperCollie.ELECTION_PORT_KEY -> electionPort.toString,
                      ZookeeperCollie.SERVERS_KEY -> zkServers
                    ))
                    .name(hostname)
                    .route(route)
                    .run()
                  catch {
                    case e: Throwable =>
                      stopAndRemoveService(client, clusterName, true)
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

    override def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo]): ZookeeperClusterInfo = {
      val first = containers.head
      ZookeeperClusterInfo(
        name = clusterName,
        imageName = first.imageName,
        clientPort = first.environments
          .get(ZookeeperCollie.CLIENT_PORT_KEY)
          .map(_.toInt)
          .getOrElse(ZookeeperCollie.CLIENT_PORT_DEFAULT),
        peerPort = first.environments
          .get(ZookeeperCollie.PEER_PORT_KEY)
          .map(_.toInt)
          .getOrElse(ZookeeperCollie.PEER_PORT_DEFAULT),
        electionPort = first.environments
          .get(ZookeeperCollie.ELECTION_PORT_KEY)
          .map(_.toInt)
          .getOrElse(ZookeeperCollie.ELECTION_PORT_DEFAULT),
        nodeNames = containers.map(_.nodeName)
      )
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

  private class BrokerCollieImpl(implicit val nodeCollie: NodeCollie, val clientCache: DockerClientCache)
      extends BrokerCollie
      with BasicCollieImpl[BrokerClusterInfo] {

    override val service: Service = BROKER

    override def creator(): BrokerCollie.ClusterCreator =
      (clusterName, imageName, zookeeperClusterName, clientPort, exporterPort, nodeNames) =>
        exists(clusterName)
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
                  throw new IllegalArgumentException(s"${node.name} has run the worker service for $clusterName"))
              query(zookeeperClusterName, ZOOKEEPER).map((existNodes, newNodes, _))
          }
          .map {
            case (existNodes, newNodes, zkContainers) =>
              if (zkContainers.isEmpty) throw new IllegalArgumentException(s"$clusterName doesn't exist")
              val zookeepers = zkContainers
                .map(c =>
                  s"${c.nodeName}:${c.environments.getOrElse(ZookeeperCollie.CLIENT_PORT_KEY, ZookeeperCollie.CLIENT_PORT_DEFAULT)}")
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

              val maxId: Int =
                if (existNodes.isEmpty) 0
                else existNodes.values.map(_.environments(BrokerCollie.ID_KEY).toInt).toSet.max + 1

              val successfulNodeNames = newNodes.zipWithIndex
                .flatMap {
                  case ((node, hostname), index) =>
                    val client = clientCache.get(node)
                    try client
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
                      .run()
                    catch {
                      case e: Throwable =>
                        stopAndRemoveService(client, clusterName, true)
                        LOG.error(s"failed to start $imageName on ${node.name}", e)
                        None
                    }
                }
                .map(_.nodeName)
                .toSeq
              if (successfulNodeNames.isEmpty)
                throw new IllegalArgumentException(s"failed to create $clusterName on $BROKER")
              BrokerClusterInfo(
                name = clusterName,
                imageName = imageName,
                zookeeperClusterName = zookeeperClusterName,
                clientPort = clientPort,
                nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
              )
        }
    override def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo]): BrokerClusterInfo = {
      val first = containers.head
      BrokerClusterInfo(
        name = clusterName,
        imageName = first.imageName,
        zookeeperClusterName = first.environments(ZOOKEEPER_CLUSTER_NAME),
        clientPort =
          first.environments.get(BrokerCollie.CLIENT_PORT_KEY).map(_.toInt).getOrElse(BrokerCollie.CLIENT_PORT_DEFAULT),
        nodeNames = containers.map(_.nodeName)
      )
    }
    override protected def doAddNode(previousCluster: BrokerClusterInfo,
                                     previousContainers: Seq[ContainerInfo],
                                     newNodeName: String): Future[BrokerClusterInfo] = creator()
      .clusterName(previousCluster.name)
      .zookeeperClusterName(previousCluster.zookeeperClusterName)
      .clientPort(previousCluster.clientPort)
      .imageName(previousCluster.imageName)
      .nodeName(newNodeName)
      .create()
  }

  private class WorkerCollieImpl(implicit val nodeCollie: NodeCollie, val clientCache: DockerClientCache)
      extends WorkerCollie
      with BasicCollieImpl[WorkerClusterInfo] {

    override val service: Service = WORKER

    /**
      * create a new worker cluster if there is no existent worker cluster. Otherwise, this method do the following
      * jobs. 1) update the route of running cluster 2) run related worker containers
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
      exists(clusterName)
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
                s"${c.nodeName}:${c.environments.getOrElse(BrokerCollie.CLIENT_PORT_KEY, BrokerCollie.CLIENT_PORT_DEFAULT)}")
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
            val successfulNodeNames = newNodes
              .flatMap {
                case (node, hostname) =>
                  val client = clientCache.get(node)
                  try client
                    .containerCreator()
                    .imageName(imageName)
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
                    .run()
                  catch {
                    case e: Throwable =>
                      stopAndRemoveService(client, clusterName, true)
                      LOG.error(s"failed to start $imageName", e)
                      None
                  }
              }
              .map(_.nodeName)
              .toSeq
            if (successfulNodeNames.isEmpty)
              throw new IllegalArgumentException(s"failed to create $clusterName on $WORKER")
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
              nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
            )
      }

    override def toClusterDescription(clusterName: String, containers: Seq[ContainerInfo]): WorkerClusterInfo =
      WorkerClusterInfo(
        name = clusterName,
        imageName = containers.head.imageName,
        brokerClusterName = containers.head.environments(BROKER_CLUSTER_NAME),
        clientPort = containers.head.environments(WorkerCollie.CLIENT_PORT_KEY).toInt,
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
        nodeNames = containers.map(_.nodeName)
      )

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
    * internal key used to save the broker cluster name.
    * All nodes of worker cluster should have this environment variable.
    */
  private[this] val BROKER_CLUSTER_NAME = "CCI_BROKER_CLUSTER_NAME"

  /**
    * internal key used to save the zookeeper cluster name.
    * All nodes of broker cluster should have this environment variable.
    */
  private[this] val ZOOKEEPER_CLUSTER_NAME = "CCI_ZOOKEEPER_CLUSTER_NAME"

  /**
    * used to distinguish the cluster name and service name
    */
  private[this] val DIVIDER: String = "-"

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
