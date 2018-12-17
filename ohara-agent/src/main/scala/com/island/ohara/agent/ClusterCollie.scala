package com.island.ohara.agent
import java.util.Objects

import com.island.ohara.agent.AgentJson.{BrokerCluster, Node, WorkerCluster, ZookeeperCluster}
import com.island.ohara.agent.DockerJson.ContainerDescription
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.typesafe.scalalogging.Logger

/**
  * This is the top-of-the-range "collie". It maintains and organizes all collies.
  * Each getter should return new instance of collie since each collie has close() method.
  * However, it is ok to keep global instance of collie if they have dump close().
  * Currently, default implementation is based on ssh and docker command. It is simple but slow.
  * TODO: We are looking for k8s implementation...by chia
  */
trait ClusterCollie extends ReleaseOnce {

  /**
    * create a collie for zookeeper cluster
    * @return zookeeper collie
    */
  def zookeepersCollie(): ZookeeperCollie

  /**
    * create a collie for broker cluster
    * @return broker collie
    */
  def brokerCollie(): BrokerCollie

  /**
    * create a collie for worker cluster
    * @return worker collie
    */
  def workerCollie(): WorkerCollie
}

/**
  * the default implementation uses ssh and docker command to manage all clusters.
  * Each node running the service has name "${clusterName}-${service}-${index}".
  * For example, there is a worker cluster called "workercluster" and it is run on 3 nodes.
  * node-0 => workercluster-worker-0
  * node-1 => workercluster-worker-1
  * node-2 => workercluster-worker-2
  */
object ClusterCollie {
  private[this] val LOG = Logger(classOf[ClusterCollie])

  def apply(implicit nodeCollie: NodeCollie): ClusterCollie = new ClusterCollieImpl

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

  //----------------------------------------------------[HELPER]----------------------------------------------------//
  /**
    * generate unique name for the container.
    * It can be used in setting container's hostname and name
    * @param clusterName cluster name
    * @param service service. zookeeper, broker or worker
    * @param index the order of node in service
    * @return a formatted string. form: ${clusterName}-${service}-${index}
    */
  private[this] def format(clusterName: String, service: Service, index: Int): String =
    s"$clusterName-${service.name}-$index"

  /**
    * a helper method used to do "stop" and "remove".
    * NOTED: this method may be expensive...
    * @param client docker client
    * @param clusterName cluster name
    * @param service service
    */
  private[this] def stopAndRemoveService(client: DockerClient,
                                         clusterName: String,
                                         service: Service,
                                         swallow: Boolean) = try {
    val key = s"$clusterName$DIVIDER${service.name}"
    val containers = client.containers().filter(_.name.startsWith(key))
    if (containers.isEmpty) throw new IllegalArgumentException(s"$clusterName doesn't exist in $service")
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
  private[this] def query(clusterName: String, service: Service)(
    implicit nodeCollie: NodeCollie): Seq[ContainerDescription] = {
    nodeCollie.flatMap { node =>
      val client =
        DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
      try client.containers().filter(_.name.startsWith(s"$clusterName$DIVIDER${service.name}"))
      finally client.close()
    }.toSeq
  }

  /**
    * get all containers belonging to specified service.
    * @param service service
    * @return containers information
    */
  private[this] def query(service: Service)(implicit nodeCollie: NodeCollie): Seq[ContainerDescription] = {
    nodeCollie.flatMap { node =>
      val client =
        DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
      try client.containers().filter(_.name.contains(s"$DIVIDER${service.name}$DIVIDER"))
      finally client.close()
    }.toSeq
  }

  private[this] def getLogs(clusterName: String, service: Service)(
    implicit nodeCollie: NodeCollie): Map[ContainerDescription, String] =
    query(clusterName, service).map { container =>
      val node = nodeCollie.get(container.nodeName)
      val client =
        DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
      try container -> client.log(container.name)
      finally client.close()
    }.toMap

  private[this] class ClusterCollieImpl(implicit nodeCollie: NodeCollie) extends ClusterCollie {
    override def zookeepersCollie(): ZookeeperCollie = new ZookeeperCollie {
      override def remove(clusterName: String): Unit = nodeCollie.foreach { node =>
        val client =
          DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
        try stopAndRemoveService(client, clusterName, ZOOKEEPER, false)
        finally client.close()
      }
      override def creator(): ZookeeperCollie.ClusterCreator = new ZookeeperCollie.ClusterCreator {
        private[this] var imageName: String = ZookeeperCollie.IMAGE_NAME_DEFAULT
        private[this] var clusterName: String = _
        private[this] var clientPort: Int = ZookeeperCollie.CLIENT_PORT_DEFAULT
        private[this] var peerPort: Int = ZookeeperCollie.PEER_PORT_DEFAULT
        private[this] var electionPort: Int = ZookeeperCollie.ELECTION_PORT_DEFAULT
        override def imageName(imageName: String): ZookeeperCollie.ClusterCreator = {
          this.imageName = Objects.requireNonNull(imageName)
          this
        }
        override def clusterName(clusterName: String): ZookeeperCollie.ClusterCreator = {
          this.clusterName = Objects.requireNonNull(clusterName)
          this
        }
        override def clientPort(clientPort: Int): ZookeeperCollie.ClusterCreator = {
          this.clientPort = clientPort
          this
        }
        override def peerPort(peerPort: Int): ZookeeperCollie.ClusterCreator = {
          this.peerPort = peerPort
          this
        }
        override def electionPort(electionPort: Int): ZookeeperCollie.ClusterCreator = {
          this.electionPort = electionPort
          this
        }
        override def create(nodeNames: Seq[String]): ZookeeperCluster = {
          Objects.requireNonNull(imageName)
          Objects.requireNonNull(clusterName)
          val nodes: Seq[Node] = nodeNames.map(nodeCollie.get)
          // add route in order to make zk node can connect to each other.
          val route = nodes.zipWithIndex.map {
            case (node, index) =>
              s"${format(clusterName, ZOOKEEPER, index)}" -> CommonUtil.address(node.name)
          }.toMap

          val zkServers: String = nodes.zipWithIndex
            .map {
              case (_, index) => format(clusterName, ZOOKEEPER, index)
            }
            .mkString(" ")

          val runningNodes = nodes.zipWithIndex
            .flatMap {
              case (node, index) =>
                val client =
                  DockerClient
                    .builder()
                    .user(node.user)
                    .password(node.password)
                    .hostname(node.name)
                    .port(node.port)
                    .build()
                try client
                  .executor()
                  .imageName(imageName)
                  .portMappings(
                    Map(
                      clientPort -> clientPort,
                      peerPort -> peerPort,
                      electionPort -> electionPort
                    ))
                  .hostname(format(clusterName, ZOOKEEPER, index))
                  .envs(Map(
                    ZookeeperCollie.ID_KEY -> index.toString,
                    ZookeeperCollie.CLIENT_PORT_KEY -> clientPort.toString,
                    ZookeeperCollie.PEER_PORT_KEY -> peerPort.toString,
                    ZookeeperCollie.ELECTION_PORT_KEY -> electionPort.toString,
                    ZookeeperCollie.SERVERS_KEY -> zkServers
                  ))
                  .name(format(clusterName, ZOOKEEPER, index))
                  .route(route)
                  .run()
                catch {
                  case e: Throwable =>
                    stopAndRemoveService(client, clusterName, ZOOKEEPER, true)
                    LOG.error(s"failed to start $imageName", e)
                    None
                } finally client.close()
            }
            .map(_.nodeName)
          if (runningNodes.isEmpty) throw new IllegalArgumentException(s"failed to create $clusterName on $ZOOKEEPER")
          ZookeeperCluster(
            name = clusterName,
            clientPort = clientPort,
            peerPort = peerPort,
            electionPort = electionPort,
            nodeNames = runningNodes
          )
        }
      }
      override def iterator: Iterator[AgentJson.ZookeeperCluster] = query(ZOOKEEPER)
        .map(container => container.name.split(DIVIDER).head -> container)
        .groupBy(_._1)
        .map {
          case (clusterName, value) => clusterName -> value.map(_._2)
        }
        .map {
          case (clusterName, containers) =>
            val first = containers.head
            ZookeeperCluster(
              name = clusterName,
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
        .iterator
      override protected def doClose(): Unit = {
        // do nothing
      }
      override def containers(clusterName: String): Seq[ContainerDescription] = query(clusterName, ZOOKEEPER)
      override def logs(clusterName: String): Map[ContainerDescription, String] = getLogs(clusterName, ZOOKEEPER)
    }
    override def brokerCollie(): BrokerCollie = new BrokerCollie {
      override def remove(clusterName: String): Unit = nodeCollie.foreach { node =>
        val client =
          DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
        try stopAndRemoveService(client, clusterName, BROKER, false)
        finally client.close()
      }
      override def creator(): BrokerCollie.ClusterCreator = new BrokerCollie.ClusterCreator {
        private[this] var imageName: String = BrokerCollie.IMAGE_NAME_DEFAULT
        private[this] var clusterName: String = _
        private[this] var zkClusterName: String = _
        private[this] var clientPort: Int = BrokerCollie.CLIENT_PORT_DEFAULT
        override def imageName(imageName: String): BrokerCollie.ClusterCreator = {
          this.imageName = Objects.requireNonNull(imageName)
          this
        }
        override def clusterName(clusterName: String): BrokerCollie.ClusterCreator = {
          this.clusterName = Objects.requireNonNull(clusterName)
          this
        }
        override def zookeeperClusterName(name: String): BrokerCollie.ClusterCreator = {
          this.zkClusterName = Objects.requireNonNull(name)
          this
        }
        override def clientPort(clientPort: Int): BrokerCollie.ClusterCreator = {
          this.clientPort = clientPort
          this
        }
        override def create(nodeNames: Seq[String]): BrokerCluster = {
          Objects.requireNonNull(imageName)
          Objects.requireNonNull(clusterName)
          Objects.requireNonNull(zkClusterName)
          val nodes = nodeNames.map(nodeCollie.get)
          val zkContainers = query(zkClusterName, ZOOKEEPER)
          if (zkContainers.isEmpty) throw new IllegalArgumentException(s"$clusterName doesn't exist")
          val zookeepers = zkContainers
            .map(c =>
              s"${c.hostname}:${c.environments.getOrElse(ZookeeperCollie.CLIENT_PORT_KEY, ZookeeperCollie.CLIENT_PORT_DEFAULT)}")
            .mkString(",")
          // add route in order to make broker node can connect to each other (and zk node).
          val route: Map[String, String] = nodes.zipWithIndex.map {
            case (node, index) =>
              s"${format(clusterName, BROKER, index)}" -> CommonUtil.address(node.name)
          }.toMap ++ zkContainers
            .map(zkContainer => zkContainer.hostname -> CommonUtil.address(zkContainer.nodeName))
            .toMap
          val runningNodes = nodes.zipWithIndex
            .flatMap {
              case (node, index) =>
                val client = DockerClient
                  .builder()
                  .hostname(node.name)
                  .password(node.password)
                  .user(node.user)
                  .port(node.port)
                  .build()
                try client
                  .executor()
                  .imageName(imageName)
                  .portMappings(Map(clientPort -> clientPort))
                  .hostname(format(clusterName, BROKER, index))
                  .envs(
                    Map(
                      BrokerCollie.ID_KEY -> index.toString,
                      BrokerCollie.CLIENT_PORT_KEY -> clientPort.toString,
                      BrokerCollie.ZOOKEEPERS_KEY -> zookeepers
                    ))
                  .name(format(clusterName, BROKER, index))
                  .route(route)
                  // see docker/kafka.sh for more details
                  .command(BrokerCollie.SCRIPT_NAME)
                  .run()
                catch {
                  case e: Throwable =>
                    stopAndRemoveService(client, clusterName, BROKER, true)
                    LOG.error(s"failed to start $imageName", e)
                    None
                } finally client.close()
            }
            .map(_.nodeName)
          if (runningNodes.isEmpty) throw new IllegalArgumentException(s"failed to create $clusterName on $BROKER")
          BrokerCluster(
            name = clusterName,
            zookeeperClusterName = zkClusterName,
            clientPort = clientPort,
            nodeNames = runningNodes
          )
        }
      }

      override def iterator: Iterator[AgentJson.BrokerCluster] = query(BROKER)
        .map(container => container.name.split(DIVIDER).head -> container)
        .groupBy(_._1)
        .map {
          case (clusterName, value) => clusterName -> value.map(_._2)
        }
        .map {
          case (clusterName, containers) =>
            val first = containers.head
            BrokerCluster(
              name = clusterName,
              zookeeperClusterName = first.environments(BrokerCollie.ZOOKEEPERS_KEY),
              clientPort = first.environments
                .get(BrokerCollie.CLIENT_PORT_KEY)
                .map(_.toInt)
                .getOrElse(BrokerCollie.CLIENT_PORT_DEFAULT),
              nodeNames = containers.map(_.nodeName)
            )
        }
        .iterator
      override protected def doClose(): Unit = {
        // do nothing
      }

      override def containers(clusterName: String): Seq[ContainerDescription] = query(clusterName, BROKER)
      override def logs(clusterName: String): Map[ContainerDescription, String] = getLogs(clusterName, BROKER)
    }
    override def workerCollie(): WorkerCollie = new WorkerCollie {
      override def remove(clusterName: String): Unit = nodeCollie.foreach { node =>
        val client =
          DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
        try stopAndRemoveService(client, clusterName, WORKER, false)
        finally client.close()
      }
      override def creator(): WorkerCollie.ClusterCreator = new WorkerCollie.ClusterCreator {
        private[this] var imageName: String = WorkerCollie.IMAGE_NAME_DEFAULT
        private[this] var clusterName: String = _
        private[this] var brokerClusterName: String = _
        private[this] var clientPort: Int = WorkerCollie.CLIENT_PORT_DEFAULT
        override def imageName(imageName: String): WorkerCollie.ClusterCreator = {
          this.imageName = Objects.requireNonNull(imageName)
          this
        }
        override def clusterName(clusterName: String): WorkerCollie.ClusterCreator = {
          this.clusterName = Objects.requireNonNull(clusterName)
          this
        }
        override def brokerClusterName(name: String): WorkerCollie.ClusterCreator = {
          this.brokerClusterName = Objects.requireNonNull(name)
          this
        }
        override def clientPort(clientPort: Int): WorkerCollie.ClusterCreator = {
          this.clientPort = clientPort
          this
        }
        override def create(nodeNames: Seq[String]): WorkerCluster = {
          Objects.requireNonNull(imageName)
          Objects.requireNonNull(clusterName)
          Objects.requireNonNull(brokerClusterName)
          val nodes = nodeNames.map(nodeCollie.get)
          val brokerContainers = query(brokerClusterName, BROKER)
          if (brokerContainers.isEmpty) throw new IllegalArgumentException(s"$clusterName doesn't exist")
          val brokers = brokerContainers
            .map(c =>
              s"${c.hostname}:${c.environments.getOrElse(BrokerCollie.CLIENT_PORT_KEY, BrokerCollie.CLIENT_PORT_DEFAULT)}")
            .mkString(",")
          // add route in order to make broker node can connect to each other (and broker node).
          val route: Map[String, String] = nodes.zipWithIndex.map {
            case (node, index) =>
              s"${format(clusterName, WORKER, index)}" -> CommonUtil.address(node.name)
          }.toMap ++ brokerContainers
            .map(brokerContainer => brokerContainer.hostname -> CommonUtil.address(brokerContainer.nodeName))
            .toMap

          val group = CommonUtil.uuid()
          val runningNodes = nodes.zipWithIndex
            .flatMap {
              case (node, index) =>
                val client = DockerClient
                  .builder()
                  .hostname(node.name)
                  .password(node.password)
                  .user(node.user)
                  .port(node.port)
                  .build()
                try client
                  .executor()
                  .imageName(imageName)
                  .portMappings(Map(clientPort -> clientPort))
                  .hostname(format(clusterName, WORKER, index))
                  .envs(Map(
                    WorkerCollie.CLIENT_PORT_KEY -> clientPort.toString,
                    WorkerCollie.BROKERS_KEY -> brokers,
                    WorkerCollie.GROUP_ID_KEY -> group,
                    WorkerCollie.OFFSET_TOPIC_KEY -> s"$group-offset-topic",
                    WorkerCollie.CONFIG_TOPIC_KEY -> s"$group-config-topic",
                    WorkerCollie.STATUS_TOPIC_KEY -> s"$group-status-topic"
                  ))
                  .name(format(clusterName, WORKER, index))
                  .route(route)
                  // see docker/kafka.sh for more details
                  .command(WorkerCollie.SCRIPT_NAME)
                  .run()
                catch {
                  case e: Throwable =>
                    stopAndRemoveService(client, clusterName, WORKER, true)
                    LOG.error(s"failed to start $imageName", e)
                    None
                } finally client.close()
            }
            .map(_.nodeName)
          if (runningNodes.isEmpty) throw new IllegalArgumentException(s"failed to create $clusterName on $WORKER")
          WorkerCluster(
            name = clusterName,
            brokerClusterName = brokerClusterName,
            clientPort = clientPort,
            nodeNames = runningNodes
          )
        }
      }

      override def containers(clusterName: String): Seq[ContainerDescription] = query(clusterName, WORKER)

      override protected def doClose(): Unit = {
        // do nothing
      }
      override def iterator: Iterator[AgentJson.WorkerCluster] = query(WORKER)
        .map(container => container.name.split(DIVIDER).head -> container)
        .groupBy(_._1)
        .map {
          case (clusterName, value) => clusterName -> value.map(_._2)
        }
        .map {
          case (clusterName, containers) =>
            val first = containers.head
            WorkerCluster(
              name = clusterName,
              brokerClusterName = first.environments(WorkerCollie.BROKERS_KEY),
              clientPort = first.environments
                .get(WorkerCollie.CLIENT_PORT_KEY)
                .map(_.toInt)
                .getOrElse(WorkerCollie.CLIENT_PORT_DEFAULT),
              nodeNames = containers.map(_.nodeName)
            )
        }
        .iterator
      override def logs(clusterName: String): Map[ContainerDescription, String] = getLogs(clusterName, WORKER)
    }
    override protected def doClose(): Unit = nodeCollie.close()
  }
}
