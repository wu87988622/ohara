package com.island.ohara.agent
import java.util.Objects

import com.island.ohara.agent.AgentJson.{
  BrokerClusterDescription,
  Node,
  WorkerClusterDescription,
  ZookeeperClusterDescription,
  _
}
import com.island.ohara.agent.ClusterCollieImpl._
import com.island.ohara.common.util.{CommonUtil, Releasable, ReleaseOnce}
import com.typesafe.scalalogging.Logger
private[agent] class ClusterCollieImpl(implicit nodeCollie: NodeCollie) extends ReleaseOnce with ClusterCollie {

  override def zookeepersCollie(): ZookeeperCollie = new ZookeeperCollieImpl
  override def brokerCollie(): BrokerCollie = new BrokerCollieImpl
  override def workerCollie(): WorkerCollie = new WorkerCollieImpl
  override protected def doClose(): Unit = nodeCollie.close()
}

private object ClusterCollieImpl {
  private[this] val LOG = Logger(classOf[ClusterCollieImpl])

  private[this] trait BasicCollieImpl[T <: ClusterDescription] extends Collie[T] with Releasable {

    val nodeCollie: NodeCollie

    val service: Service

    override def containers(clusterName: String): Seq[ContainerDescription] = nodeCollie.flatMap { node =>
      val client =
        DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
      try client.containers().filter(_.name.startsWith(s"$clusterName$DIVIDER${service.name}"))
      finally client.close()
    }.toSeq

    protected def toClusterDescription(clusterName: String, containers: Seq[ContainerDescription]): T
    override def iterator: Iterator[T] = nodeCollie
      .flatMap { node =>
        val client =
          DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
        try client.containers().filter(_.name.contains(s"$DIVIDER${service.name}$DIVIDER"))
        finally client.close()
      }
      .map(container => container.name.split(DIVIDER).head -> container)
      .groupBy(_._1)
      .map {
        case (clusterName, value) => clusterName -> value.map(_._2)
      }
      .map {
        case (clusterName, containers) => toClusterDescription(clusterName, containers.toSeq)
      }
      .iterator

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
    def format(clusterName: String): String = s"$clusterName-${service.name}-${CommonUtil.uuid(LENGTH_OF_UUID)}"

    override def remove(clusterName: String): Unit = nodeCollie.foreach { node =>
      val client =
        DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
      try stopAndRemoveService(client, clusterName, false)
      finally client.close()
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
    def query(clusterName: String, service: Service): Seq[ContainerDescription] = {
      nodeCollie.flatMap { node =>
        val client =
          DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
        try client.containers().filter(_.name.startsWith(s"$clusterName$DIVIDER${service.name}"))
        finally client.close()
      }.toSeq
    }

    override def logs(clusterName: String): Map[ContainerDescription, String] = query(clusterName, service).map {
      container =>
        val node = nodeCollie.get(container.nodeName)
        val client =
          DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
        try container -> client.log(container.name)
        finally client.close()
    }.toMap

    override def removeNode(clusterName: String, nodeName: String): T = {
      val targetNode =
        nodeCollie.find(_.name == nodeName).getOrElse(throw new IllegalArgumentException(s"$nodeName doesn't exist"))
      val runningContainers = containers(clusterName)
      runningContainers.size match {
        case 0 => throw new IllegalArgumentException(s"$clusterName doesn't exist")
        case 1 if runningContainers.map(_.nodeName).contains(nodeName) =>
          throw new IllegalArgumentException(
            s"$clusterName is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead")
        case _ =>
          val client =
            DockerClient
              .builder()
              .user(targetNode.user)
              .password(targetNode.password)
              .hostname(targetNode.name)
              .port(targetNode.port)
              .build()
          try stopAndRemoveService(client, clusterName, false)
          finally client.close()
          cluster(clusterName)
      }
    }

    protected def doAddNode(previousCluster: T, newNodeName: String): T
    override def addNode(clusterName: String, nodeName: String): T = {
      if (!nodeCollie.exists(_.name == nodeName)) throw new IllegalArgumentException(s"$nodeName doesn't exist")
      doAddNode(cluster(clusterName), nodeName)
    }

    override def close(): Unit = {
      // do nothing
    }
  }

  private class ZookeeperCollieImpl(implicit val nodeCollie: NodeCollie)
      extends ZookeeperCollie
      with BasicCollieImpl[ZookeeperClusterDescription] {

    override val service: Service = ZOOKEEPER

    override def creator(): ZookeeperCollie.ClusterCreator = new ZookeeperCollie.ClusterCreator {
      private[this] var clientPort: Int = ZookeeperCollie.CLIENT_PORT_DEFAULT
      private[this] var peerPort: Int = ZookeeperCollie.PEER_PORT_DEFAULT
      private[this] var electionPort: Int = ZookeeperCollie.ELECTION_PORT_DEFAULT
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
      override def create(nodeNames: Seq[String]): ZookeeperClusterDescription = {
        if (imageName == null) imageName = ZookeeperCollie.IMAGE_NAME_DEFAULT
        Objects.requireNonNull(clusterName)
        if (exists(clusterName)) throw new IllegalArgumentException(s"zookeeper cluster:$clusterName exists!")
        val nodes: Map[Node, String] =
          nodeNames.map(nodeCollie.get).map(node => node -> format(clusterName)).toMap
        // add route in order to make zk node can connect to each other.
        val route: Map[String, String] = nodes.map {
          case (node, _) =>
            node.name -> CommonUtil.address(node.name)
        }

        val zkServers: String = nodes.values.mkString(" ")
        val successfulNodeNames: Seq[String] = nodes.zipWithIndex
          .flatMap {
            case ((node, hostname), index) =>
              val client =
                DockerClient
                  .builder()
                  .user(node.user)
                  .password(node.password)
                  .hostname(node.name)
                  .port(node.port)
                  .build()
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
                  LOG.error(s"failed to start $imageName", e)
                  None
              } finally client.close()
          }
          .map(_.nodeName)
          .toSeq
        if (successfulNodeNames.isEmpty)
          throw new IllegalArgumentException(s"failed to create $clusterName on $ZOOKEEPER")
        ZookeeperClusterDescription(
          name = clusterName,
          imageName = imageName,
          clientPort = clientPort,
          peerPort = peerPort,
          electionPort = electionPort,
          nodeNames = successfulNodeNames
        )
      }
    }

    override def toClusterDescription(clusterName: String,
                                      containers: Seq[ContainerDescription]): ZookeeperClusterDescription = {
      val first = containers.head
      ZookeeperClusterDescription(
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
    override def removeNode(clusterName: String, nodeName: String): ZookeeperClusterDescription =
      throw new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster")

    override protected def doAddNode(previousCluster: ZookeeperClusterDescription,
                                     newNodeName: String): ZookeeperClusterDescription =
      throw new UnsupportedOperationException("zookeeper collie doesn't support to add node to a running cluster")
  }

  private class BrokerCollieImpl(implicit val nodeCollie: NodeCollie)
      extends BrokerCollie
      with BasicCollieImpl[BrokerClusterDescription] {

    override val service: Service = BROKER

    override def creator(): BrokerCollie.ClusterCreator = new BrokerCollie.ClusterCreator {
      private[this] var zkClusterName: String = _
      private[this] var clientPort: Int = BrokerCollie.CLIENT_PORT_DEFAULT
      override def zookeeperClusterName(name: String): BrokerCollie.ClusterCreator = {
        this.zkClusterName = Objects.requireNonNull(name)
        this
      }
      override def clientPort(clientPort: Int): BrokerCollie.ClusterCreator = {
        this.clientPort = clientPort
        this
      }
      override def create(nodeNames: Seq[String]): BrokerClusterDescription = {
        if (imageName == null) imageName = BrokerCollie.IMAGE_NAME_DEFAULT
        Objects.requireNonNull(clusterName)
        Objects.requireNonNull(zkClusterName)
        val existNodes: Map[Node, ContainerDescription] =
          containers(clusterName).map(container => nodeCollie.get(container.nodeName) -> container).toMap

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
          check(ZOOKEEPER_CLUSTER_NAME, zkClusterName)
        }
        val newNodes: Map[Node, String] =
          nodeNames.map(nodeCollie.get).map(node => node -> format(clusterName)).toMap
        existNodes.keys.foreach(
          node =>
            if (newNodes.keys.exists(_.name == node.name))
              throw new IllegalArgumentException(s"${node.name} has run the worker service for $clusterName"))
        val zkContainers = query(zkClusterName, ZOOKEEPER)
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
        } ++ zkContainers.map(zkContainer => zkContainer.nodeName -> CommonUtil.address(zkContainer.nodeName)).toMap

        // update the route since we are adding new node to a running broker cluster
        // we don't need to update startup broker list since kafka do the update for us.
        existNodes.foreach {
          case (node, container) =>
            val client =
              DockerClient.builder().hostname(node.name).password(node.password).user(node.user).port(node.port).build()
            try updateRoute(client, container.name, route)
            finally client.close()
        }

        val maxId: Int =
          if (existNodes.isEmpty) 0 else existNodes.values.map(_.environments(BrokerCollie.ID_KEY).toInt).toSet.max + 1

        val successfulNodeNames = newNodes.zipWithIndex
          .flatMap {
            case ((node, hostname), index) =>
              val client = DockerClient
                .builder()
                .hostname(node.name)
                .password(node.password)
                .user(node.user)
                .port(node.port)
                .build()
              try client
                .containerCreator()
                .imageName(imageName)
                .portMappings(Map(clientPort -> clientPort))
                .hostname(hostname)
                .envs(Map(
                  BrokerCollie.ID_KEY -> (maxId + index).toString,
                  BrokerCollie.CLIENT_PORT_KEY -> clientPort.toString,
                  BrokerCollie.ZOOKEEPERS_KEY -> zookeepers,
                  BrokerCollie.ADVERTISED_HOSTNAME_KEY -> node.name,
                  BrokerCollie.ADVERTISED_CLIENT_PORT_KEY -> clientPort.toString,
                  ZOOKEEPER_CLUSTER_NAME -> zkClusterName
                ))
                .name(hostname)
                .route(route ++ existRoute)
                .run()
              catch {
                case e: Throwable =>
                  stopAndRemoveService(client, clusterName, true)
                  LOG.error(s"failed to start $imageName on ${node.name}", e)
                  None
              } finally client.close()
          }
          .map(_.nodeName)
          .toSeq
        if (successfulNodeNames.isEmpty) throw new IllegalArgumentException(s"failed to create $clusterName on $BROKER")
        BrokerClusterDescription(
          name = clusterName,
          imageName = imageName,
          zookeeperClusterName = zkClusterName,
          clientPort = clientPort,
          nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
        )
      }
    }
    override def toClusterDescription(clusterName: String,
                                      containers: Seq[ContainerDescription]): BrokerClusterDescription = {
      val first = containers.head
      BrokerClusterDescription(
        name = clusterName,
        imageName = first.imageName,
        zookeeperClusterName = first.environments(ZOOKEEPER_CLUSTER_NAME),
        clientPort =
          first.environments.get(BrokerCollie.CLIENT_PORT_KEY).map(_.toInt).getOrElse(BrokerCollie.CLIENT_PORT_DEFAULT),
        nodeNames = containers.map(_.nodeName)
      )
    }
    override protected def doAddNode(previousCluster: BrokerClusterDescription,
                                     newNodeName: String): BrokerClusterDescription = creator()
      .clusterName(previousCluster.name)
      .zookeeperClusterName(previousCluster.zookeeperClusterName)
      .clientPort(previousCluster.clientPort)
      .imageName(previousCluster.imageName)
      .create(newNodeName)
  }

  private class WorkerCollieImpl(implicit val nodeCollie: NodeCollie)
      extends WorkerCollie
      with BasicCollieImpl[WorkerClusterDescription] {

    override val service: Service = WORKER

    override def creator(): WorkerCollie.ClusterCreator = new WorkerCollie.ClusterCreator {
      private[this] var brokerClusterName: String = _
      private[this] var clientPort: Int = WorkerCollie.CLIENT_PORT_DEFAULT
      private[this] var groupId: String = CommonUtil.uuid()
      private[this] var offsetTopicName = s"$groupId-offset-topic"
      private[this] var offsetTopicReplications: Short = 1
      private[this] var offsetTopicPartitions: Int = 1
      private[this] var configTopicName = s"$groupId-config-topic"
      private[this] var configTopicReplications: Short = 1
      private[this] var statusTopicName = s"$groupId-status-topic"
      private[this] var statusTopicReplications: Short = 1
      private[this] var statusTopicPartitions: Int = 1
      override def brokerClusterName(name: String): WorkerCollie.ClusterCreator = {
        this.brokerClusterName = Objects.requireNonNull(name)
        this
      }
      override def clientPort(clientPort: Int): WorkerCollie.ClusterCreator = {
        this.clientPort = clientPort
        this
      }

      override def groupId(groupId: String): WorkerCollie.ClusterCreator = {
        this.groupId = Objects.requireNonNull(groupId)
        this
      }
      override def offsetTopicName(offsetTopicName: String): WorkerCollie.ClusterCreator = {
        this.offsetTopicName = Objects.requireNonNull(offsetTopicName)
        this
      }
      override def offsetTopicReplications(numberOfReplications: Short): WorkerCollie.ClusterCreator = {
        this.offsetTopicReplications = numberOfReplications
        this
      }
      override def offsetTopicPartitions(numberOfPartitions: Int): WorkerCollie.ClusterCreator = {
        this.offsetTopicPartitions = numberOfPartitions
        this
      }
      override def statusTopicReplications(numberOfReplications: Short): WorkerCollie.ClusterCreator = {
        this.statusTopicReplications = numberOfReplications
        this
      }
      override def statusTopicPartitions(numberOfPartitions: Int): WorkerCollie.ClusterCreator = {
        this.statusTopicPartitions = numberOfPartitions
        this
      }
      override def configTopicReplications(numberOfReplications: Short): WorkerCollie.ClusterCreator = {
        this.configTopicReplications = numberOfReplications
        this
      }
      override def statusTopicName(statusTopicName: String): WorkerCollie.ClusterCreator = {
        this.statusTopicName = Objects.requireNonNull(statusTopicName)
        this
      }
      override def configTopicName(configTopicName: String): WorkerCollie.ClusterCreator = {
        this.configTopicName = Objects.requireNonNull(configTopicName)
        this
      }

      /**
        * create a new worker cluster if there is no existent worker cluster. Otherwise, this method do the following
        * jobs. 1) update the route of running cluster 2) run related worker containers
        * @param nodeNames node names
        * @return description of worker cluster
        */
      override def create(nodeNames: Seq[String]): WorkerClusterDescription = {
        if (imageName == null) imageName = WorkerCollie.IMAGE_NAME_DEFAULT
        Objects.requireNonNull(clusterName)
        Objects.requireNonNull(brokerClusterName)
        Objects.requireNonNull(groupId)
        Objects.requireNonNull(offsetTopicName)
        Objects.requireNonNull(statusTopicName)
        Objects.requireNonNull(configTopicName)
        val existNodes: Map[Node, ContainerDescription] =
          containers(clusterName).map(container => nodeCollie.get(container.nodeName) -> container).toMap

        // if there is a running cluster already, we should check the consistency of configuration
        existNodes.values.foreach { container =>
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
        val newNodes: Map[Node, String] =
          nodeNames.map(nodeCollie.get).map(node => node -> format(clusterName)).toMap
        existNodes.keys.foreach(
          node =>
            if (newNodes.keys.exists(_.name == node.name))
              throw new IllegalArgumentException(s"${node.name} has run the worker service for $clusterName"))
        val brokerContainers = query(brokerClusterName, BROKER)
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
          case (node, container) =>
            val client =
              DockerClient.builder().hostname(node.name).password(node.password).user(node.user).port(node.port).build()
            try updateRoute(client, container.name, route)
            finally client.close()
        }

        val successfulNodeNames = newNodes
          .flatMap {
            case (node, hostname) =>
              val client = DockerClient
                .builder()
                .hostname(node.name)
                .password(node.password)
                .user(node.user)
                .port(node.port)
                .build()
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
              } finally client.close()
          }
          .map(_.nodeName)
          .toSeq
        if (successfulNodeNames.isEmpty) throw new IllegalArgumentException(s"failed to create $clusterName on $WORKER")
        WorkerClusterDescription(
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
          nodeNames = successfulNodeNames ++ existNodes.map(_._1.name)
        )
      }
    }

    override def toClusterDescription(clusterName: String,
                                      containers: Seq[ContainerDescription]): WorkerClusterDescription = {
      val first = containers.head
      WorkerClusterDescription(
        name = clusterName,
        imageName = first.imageName,
        brokerClusterName = first.environments(BROKER_CLUSTER_NAME),
        clientPort =
          first.environments.get(WorkerCollie.CLIENT_PORT_KEY).map(_.toInt).getOrElse(WorkerCollie.CLIENT_PORT_DEFAULT),
        groupId = first.environments(WorkerCollie.GROUP_ID_KEY),
        offsetTopicName = first.environments(WorkerCollie.OFFSET_TOPIC_KEY),
        offsetTopicPartitions = first.environments(WorkerCollie.OFFSET_TOPIC_PARTITIONS_KEY).toInt,
        offsetTopicReplications = first.environments(WorkerCollie.OFFSET_TOPIC_REPLICATIONS_KEY).toShort,
        configTopicName = first.environments(WorkerCollie.CONFIG_TOPIC_KEY),
        configTopicPartitions = 1,
        configTopicReplications = first.environments(WorkerCollie.CONFIG_TOPIC_REPLICATIONS_KEY).toShort,
        statusTopicName = first.environments(WorkerCollie.STATUS_TOPIC_KEY),
        statusTopicPartitions = first.environments(WorkerCollie.STATUS_TOPIC_PARTITIONS_KEY).toInt,
        statusTopicReplications = first.environments(WorkerCollie.STATUS_TOPIC_REPLICATIONS_KEY).toShort,
        nodeNames = containers.map(_.nodeName)
      )
    }
    override protected def doAddNode(previousCluster: WorkerClusterDescription,
                                     newNodeName: String): WorkerClusterDescription = creator()
      .clusterName(previousCluster.name)
      .brokerClusterName(previousCluster.brokerClusterName)
      .clientPort(previousCluster.clientPort)
      .groupId(previousCluster.groupId)
      .offsetTopicName(previousCluster.offsetTopicName)
      .statusTopicName(previousCluster.statusTopicName)
      .configTopicName(previousCluster.configTopicName)
      .imageName(previousCluster.imageName)
      .create(newNodeName)
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
