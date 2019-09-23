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
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.ZookeeperApi.{Creation, ZookeeperClusterInfo}
import com.island.ohara.client.configurator.v0.{ClusterInfo, ZookeeperApi}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import spray.json.JsString

import scala.concurrent.{ExecutionContext, Future}

/**
  * An interface of controlling zookeeper cluster.
  * It isolates the implementation of container manager from Configurator.
  */
trait ZookeeperCollie extends Collie[ZookeeperClusterInfo] {

  override val serviceName: String = ZookeeperApi.ZOOKEEPER_SERVICE_NAME

  /**
    * This is a complicated process. We must address following issues.
    * 1) check the existence of cluster
    * 2) check the existence of nodes
    * 3) Each zookeeper container has got to export peer port, election port, and client port
    * 4) Each zookeeper container should use "docker host name" to replace "container host name".
    * 4) Add routes to all zookeeper containers
    * @return creator of broker cluster
    */
  override def creator: ZookeeperCollie.ClusterCreator = (executionContext, creation) => {
    implicit val exec: ExecutionContext = executionContext
    clusters().flatMap(
      clusters => {
        if (clusters.keys.filter(_.isInstanceOf[ZookeeperClusterInfo]).exists(_.key == creation.key))
          Future.failed(
            new UnsupportedOperationException(s"zookeeper collie doesn't support to add node to a running cluster"))
        else
          nodeCollie
            .nodes(creation.nodeNames)
            .map(_.map(node => node -> Collie.format(prefixKey, creation.group, creation.name, serviceName)).toMap)
            .flatMap {
              nodes =>
                // add route in order to make zk node can connect to each other.
                val route: Map[String, String] = routeInfo(nodes)

                val zkServers: String = nodes.keys.map(_.name).mkString(" ")
                // ssh connection is slow so we submit request by multi-thread
                Future
                  .sequence(nodes.zipWithIndex.map {
                    case ((node, containerName), index) =>
                      val containerInfo = ContainerInfo(
                        nodeName = node.name,
                        id = Collie.UNKNOWN,
                        imageName = creation.imageName,
                        created = Collie.UNKNOWN,
                        // this fake container will be cached before refreshing cache so we make it running.
                        // other, it will be filtered later ...
                        state = ContainerState.RUNNING.name,
                        kind = Collie.UNKNOWN,
                        name = containerName,
                        size = Collie.UNKNOWN,
                        portMappings = Seq(PortMapping(
                          hostIp = Collie.UNKNOWN,
                          portPairs = Seq(
                            PortPair(
                              hostPort = creation.clientPort,
                              containerPort = creation.clientPort
                            ),
                            PortPair(
                              hostPort = creation.peerPort,
                              containerPort = creation.peerPort
                            ),
                            PortPair(
                              hostPort = creation.electionPort,
                              containerPort = creation.electionPort
                            )
                          )
                        )),
                        environments = creation.settings.map {
                          case (k, v) =>
                            k -> (v match {
                              // the string in json representation has quote in the beginning and end.
                              // we don't like the quotes since it obstruct us to cast value to pure string.
                              case JsString(s) => s
                              // save the json string for all settings
                              case _ => CommonUtils.toEnvString(v.toString)
                            })
                          // each zookeeper instance needs an unique id to identify
                        } + (ZookeeperApi.ZK_ID_KEY -> index.toString)
                        // zookeeper cluster will use this setting to communicate to each other zookeeper instance
                          + (ZookeeperApi.SERVERS_KEY -> zkServers)
                        // we convert all settings to specific string in order to fetch all settings from
                        // container env quickly. Also, the specific string enable us to pick up the "true" settings
                        // from envs since there are many system-defined settings in container envs.
                          + toEnvString(creation.settings),
                        // zookeeper doesn't have advertised hostname/port so we assign the "docker host" directly
                        // Note: We should assign "node" name to the container hostname directly here to avoid some
                        // dns problem. For example, we may want to connect to zk to dig something issue and assign
                        // node name here can save our life to solve the connection problem...
                        hostname = node.name
                      )
                      doCreator(executionContext, containerName, containerInfo, node, route)
                        .map(_ => Some(containerInfo))
                        .recover {
                          case _: Throwable =>
                            None
                        }
                  })
                  .map(_.flatten.toSeq)
                  .map {
                    successfulContainers =>
                      val nodeNames = creation.nodeNames ++ nodes.keySet.map(_.hostname)
                      val state = toClusterState(successfulContainers).map(_.name)
                      val clusterInfo = ZookeeperClusterInfo(
                        settings = ZookeeperApi.access.request
                          .settings(creation.settings)
                          .nodeNames(nodeNames)
                          .creation
                          .settings,
                        // no state means cluster is NOT running so we cleanup the dead nodes
                        deadNodes =
                          state.map(_ => nodeNames -- successfulContainers.map(_.nodeName)).getOrElse(Set.empty),
                        // We do not care the user parameters since it's stored in configurator already
                        state = state,
                        error = None,
                        lastModified = CommonUtils.current()
                      )
                      postCreateZookeeperCluster(clusterInfo, successfulContainers)
                  }
            }
      })
  }

  /**
    * Please implement nodeCollie
    * @return
    */
  protected def nodeCollie: NodeCollie

  /**
    * The prefix name for platform
    * @return
    */
  protected def prefixKey: String

  protected def doCreator(executionContext: ExecutionContext,
                          containerName: String,
                          containerInfo: ContainerInfo,
                          node: Node,
                          route: Map[String, String]): Future[Unit]

  protected def postCreateZookeeperCluster(clusterInfo: ClusterInfo, successfulContainers: Seq[ContainerInfo]): Unit = {
    //Default Nothing
  }

  protected def routeInfo(nodes: Map[Node, String]): Map[String, String] =
    nodes.map {
      case (node, _) =>
        node.name -> CommonUtils.address(node.name)
    }

  private[agent] def toZookeeperCluster(key: ObjectKey,
                                        containers: Seq[ContainerInfo]): Future[ZookeeperClusterInfo] = {
    val creation = ZookeeperApi.access.request
      .settings(seekSettings(containers.head.environments))
      // The nodeNames is NOT able to updated at runtime but we prefer consistent code for all cluster services
      .nodeNames(
        containers
          .map(_.environments)
          .map(envs => ZookeeperApi.access.request.settings(seekSettings(envs)).creation)
          .flatMap(_.nodeNames)
          .toSet)
      .creation
    Future.successful(
      ZookeeperClusterInfo(
        settings = creation.settings,
        // Currently, docker and k8s has same naming rule for "Running",
        // it is ok that we use the containerState.RUNNING here.
        deadNodes = creation.nodeNames -- containers
          .filter(_.state == ContainerState.RUNNING.name)
          .map(_.nodeName)
          .toSet,
        // We do not care the user parameters since it's stored in configurator already
        state = toClusterState(containers).map(_.name),
        // TODO how could we fetch the error?...by Sam
        error = None,
        lastModified = CommonUtils.current()
      ))
  }
}

object ZookeeperCollie {
  trait ClusterCreator extends Collie.ClusterCreator with ZookeeperApi.Request {
    override def create(): Future[Unit] =
      doCreate(
        executionContext = Objects.requireNonNull(executionContext),
        creation = creation
      )

    protected def doCreate(executionContext: ExecutionContext, creation: Creation): Future[Unit]
  }
}
