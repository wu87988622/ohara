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
import java.util.Objects

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.{BrokerApi, ClusterInfo, Definition, WorkerApi}
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.basic.CounterMBean
import spray.json.{JsString, JsValue}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
trait WorkerCollie extends Collie[WorkerClusterInfo] {

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
  override def creator: WorkerCollie.ClusterCreator = (executionContext, creation) => {
    implicit val exec: ExecutionContext = executionContext
    clusters().flatMap(clusters => {
      clusters
        .filter(_._1.isInstanceOf[WorkerClusterInfo])
        .map {
          case (cluster, containers) => cluster.asInstanceOf[WorkerClusterInfo] -> containers
        }
        .find(_._1.name == creation.name)
        .map(_._2)
        .map(containers =>
          nodeCollie
            .nodes(containers.map(_.nodeName).toSet)
            .map(_.map(node => node -> containers.find(_.nodeName == node.name).get).toMap))
        .getOrElse(Future.successful(Map.empty))
        .flatMap(existNodes =>
          nodeCollie
            .nodes(creation.nodeNames)
            .map(_.map(node => node -> Collie.format(prefixKey, creation.name, serviceName)).toMap)
            .map((existNodes, _)))
        .map {
          case (existNodes, nodes) =>
            // the broker cluster should be defined in data creating phase already
            // here we just throw an exception for absent value to ensure everything works as expect
            (existNodes,
             // find the nodes which have not run the services
             nodes.filterNot(n => existNodes.exists(_._1.hostname == n._1.hostname)),
             brokerContainers(
               creation.brokerClusterName.getOrElse(
                 throw new RuntimeException("The broker cluser name should be define"))))
        }
        .flatMap {
          case (existNodes, newNodes, brokerContainers) =>
            brokerContainers.flatMap(brokerContainers => {

              if (brokerContainers.isEmpty)
                throw new IllegalArgumentException(s"broker cluster:${creation.brokerClusterName} doesn't exist")
              val brokers = brokerContainers
                .map(c => s"${c.nodeName}:${c.environments(BrokerApi.CLIENT_PORT_KEY).toInt}")
                .mkString(",")

              val route = resolveHostNames(
                (existNodes.keys.map(_.hostname) ++ newNodes.keys.map(_.hostname) ++ brokerContainers
                  .map(_.nodeName)).toSet)
              existNodes.foreach {
                case (node, container) => hookOfNewRoute(node, container, route)
              }

              // ssh connection is slow so we submit request by multi-thread
              Future
                .sequence(newNodes.map {
                  case (node, containerName) =>
                    val containerInfo = ContainerInfo(
                      nodeName = node.name,
                      id = Collie.UNKNOWN,
                      imageName = creation.imageName,
                      created = Collie.UNKNOWN,
                      state = Collie.UNKNOWN,
                      kind = Collie.UNKNOWN,
                      name = containerName,
                      size = Collie.UNKNOWN,
                      portMappings = Seq(
                        PortMapping(
                          hostIp = Collie.UNKNOWN,
                          portPairs = creation.ports
                            .map(port =>
                              PortPair(
                                hostPort = port,
                                containerPort = port
                            ))
                            .toSeq
                        )),
                      environments = creation.settings.map {
                        case (k, v) =>
                          k -> (v match {
                            // the string in json representation has quote in the beginning and end.
                            // we don't like the quotes since it obstruct us to cast value to pure string.
                            case JsString(s) => s
                            // save the json string for all settings
                            // TODO: the setting required by worker scripts is either string or number. Hence, the other types
                            // should be skipped... by chia
                            case _ => CommonUtils.toEnvString(v.toString)
                          })
                        // TODO: put this setting into definition in #2191...by Sam
                      } + (WorkerCollie.BROKERS_KEY -> brokers)
                      // the default hostname is container name and it is not exposed publicly.
                      // Hence, we have to set the jmx hostname to node name
                        + (WorkerCollie.JMX_HOSTNAME_KEY -> node.hostname)
                      // the sync mechanism in kafka needs to know each other location.
                      // the key controls the hostname exposed to other nodes.
                        + (WorkerCollie.ADVERTISED_HOSTNAME_KEY -> node.name)
                      // we convert all settings to specific string in order to fetch all settings from
                      // container env quickly. Also, the specific string enable us to pick up the "true" settings
                      // from envs since there are many system-defined settings in container envs.
                        + toEnvString(creation.settings),
                      hostname = containerName
                    )
                    doCreator(executionContext, creation.name, containerName, containerInfo, node, route).map(_ =>
                      Some(containerInfo))
                })
                .map(_.flatten.toSeq)
                .map {
                  successfulContainers =>
                    val clusterInfo = WorkerClusterInfo(
                      settings = WorkerApi.access.request
                        .settings(creation.settings)
                        .nodeNames(
                          creation.nodeNames ++ existNodes.keySet.map(_.hostname) ++ newNodes.keySet.map(_.hostname))
                        .creation
                        .settings,
                      connectors = Seq.empty,
                      deadNodes = newNodes.keySet.map(_.hostname) -- successfulContainers.map(_.nodeName),
                      state = None,
                      error = None,
                      lastModified = CommonUtils.current()
                    )
                    postCreateWorkerCluster(clusterInfo, successfulContainers)
                    clusterInfo
                }
            })
        }
    })
  }

  /**
    * Please implement nodeCollie
    */
  protected def nodeCollie: NodeCollie

  /**
    * Implement prefix name for paltform
    */
  protected def prefixKey: String

  override val serviceName: String = WorkerApi.WORKER_SERVICE_NAME

  /**
    * there is new route to the node. the sub class can update the running container to apply new route.
    */
  protected def hookOfNewRoute(node: Node, container: ContainerInfo, route: Map[String, String]): Unit = {
    //Nothing
  }

  /**
    * Please implement this function to create the container to a different platform
    */
  protected def doCreator(executionContext: ExecutionContext,
                          clusterName: String,
                          containerName: String,
                          containerInfo: ContainerInfo,
                          node: Node,
                          route: Map[String, String]): Future[Unit]

  /**
    * After the worker container creates complete, you maybe need to do other things.
    */
  protected def postCreateWorkerCluster(clusterInfo: ClusterInfo, successfulContainers: Seq[ContainerInfo]): Unit = {
    //Default Nothing
  }

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
  def counters(cluster: WorkerClusterInfo): Seq[CounterMBean] = cluster.aliveNodes.flatMap { node =>
    BeanChannel.builder().hostname(node).port(cluster.jmxPort).build().counterMBeans().asScala
  }.toSeq

  private[agent] def toWorkerCluster(clusterName: String, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[WorkerClusterInfo] = {
    val creation = WorkerApi.access.request
      .settings(seekSettings(containers.head.environments))
      .nodeNames(containers.map(_.nodeName).toSet)
      .creation
    connectors(containers.map(c => s"${c.nodeName}:${creation.clientPort}").mkString(",")).map { connectors =>
      WorkerClusterInfo(
        settings = creation.settings,
        connectors = connectors,
        // Currently, docker and k8s has same naming rule for "Running",
        // it is ok that we use the containerState.RUNNING here.
        deadNodes = creation.nodeNames -- containers
          .filter(_.state == ContainerState.RUNNING.name)
          .map(_.nodeName)
          .toSet,
        state = toClusterState(containers).map(_.name),
        // TODO how could we fetch the error?...by Sam
        error = None,
        lastModified = CommonUtils.current()
      )
    }
  }

  /**
    * It tried to fetch connector information from starting worker cluster
    * However, it may be too slow to get latest connector information.
    * We don't throw exception since it is a common case, and Skipping retry can make quick response
    * @param connectionProps worker connection props
    * @return plugin description or nothing
    */
  private[this] def connectors(connectionProps: String)(
    implicit executionContext: ExecutionContext): Future[Seq[Definition]] =
    WorkerClient.builder.connectionProps(connectionProps).disableRetry().build.connectorDefinitions().recover {
      case e: Throwable =>
        ClusterCollie.LOG
          .error(s"Failed to fetch connectors information of cluster:$connectionProps. Use empty list instead", e)
        Seq.empty
    }

  /**
    * get the containers for specific broker cluster. This method is used to update the route.
    * @param clusterName name of broker cluster
    * @param executionContext thread pool
    * @return containers
    */
  protected def brokerContainers(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]]
}

object WorkerCollie {
  trait ClusterCreator extends Collie.ClusterCreator[WorkerClusterInfo] {
    private[this] val request = WorkerApi.access.request
    override protected def doCopy(clusterInfo: WorkerClusterInfo): Unit = request.settings(clusterInfo.settings)

    def brokerClusterName(name: String): ClusterCreator = {
      request.brokerClusterName(name)
      this
    }

    def clientPort(clientPort: Int): ClusterCreator = {
      request.clientPort(clientPort)
      this
    }

    def groupId(groupId: String): ClusterCreator = {
      request.groupId(groupId)
      this
    }

    def offsetTopicName(offsetTopicName: String): ClusterCreator = {
      request.offsetTopicName(offsetTopicName)
      this
    }

    def offsetTopicReplications(offsetTopicReplications: Short): ClusterCreator = {
      request.offsetTopicReplications(offsetTopicReplications)
      this
    }

    def offsetTopicPartitions(offsetTopicPartitions: Int): ClusterCreator = {
      request.offsetTopicPartitions(offsetTopicPartitions)
      this
    }

    def statusTopicName(statusTopicName: String): ClusterCreator = {
      request.statusTopicName(statusTopicName)
      this
    }

    def statusTopicReplications(statusTopicReplications: Short): ClusterCreator = {
      request.statusTopicReplications(statusTopicReplications)
      this
    }

    def statusTopicPartitions(statusTopicPartitions: Int): ClusterCreator = {
      request.statusTopicPartitions(statusTopicPartitions)
      this
    }

    def configTopicName(configTopicName: String): ClusterCreator = {
      request.configTopicName(configTopicName)
      this
    }

    def configTopicReplications(configTopicReplications: Short): ClusterCreator = {
      request.configTopicReplications(configTopicReplications)
      this
    }

    @Optional("default is empty")
    def jarInfos(jarInfos: Seq[FileInfo]): ClusterCreator = {
      // skip the empty array to avoid generating weird string to env
      if (jarInfos.nonEmpty) {
        request
          .jarInfos(jarInfos)
          // The URLs don't follow the json representation since we use this string in bash and we don't want
          // to make a complicated parse process in bash.
          .setting(JAR_URLS_KEY, JsString(jarInfos.map(_.url.toURI.toASCIIString).mkString(",")))
      }
      this
    }

    @Optional("default is random port")
    def jmxPort(jmxPort: Int): ClusterCreator = {
      request.jmxPort(jmxPort)
      this
    }

    /**
      * add a key-value data map for container
      *
      * @param key data key
      * @param value data value
      * @return this creator
      */
    @Optional("default is empty map")
    def setting(key: String, value: JsValue): ClusterCreator = {
      request.setting(key, value)
      this
    }

    override def create(): Future[WorkerClusterInfo] = {
      // initial the basic creation required parameters (defined in ClusterInfo) for worker
      val creation = request.name(clusterName).imageName(imageName).nodeNames(nodeNames).creation
      doCreate(
        executionContext = Objects.requireNonNull(executionContext),
        creation = creation
      )
    }

    override protected def checkClusterName(clusterName: String): String = {
      WorkerApi.WORKER_CREATION_JSON_FORMAT.check("name", JsString(clusterName))
      clusterName
    }

    protected def doCreate(executionContext: ExecutionContext, creation: WorkerApi.Creation): Future[WorkerClusterInfo]
  }
  private[agent] val BROKERS_KEY: String = "WORKER_BROKERS"
  private[agent] val ADVERTISED_HOSTNAME_KEY: String = "WORKER_ADVERTISED_HOSTNAME"
  private[agent] val ADVERTISED_CLIENT_PORT_KEY: String = "WORKER_ADVERTISED_CLIENT_PORT"
  private[agent] val JAR_URLS_KEY: String = "WORKER_JAR_URLS"
  private[agent] val JMX_HOSTNAME_KEY: String = "JMX_HOSTNAME"
}
