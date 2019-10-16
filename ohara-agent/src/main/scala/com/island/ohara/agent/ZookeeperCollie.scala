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
import com.island.ohara.client.configurator.v0.ZookeeperApi
import com.island.ohara.client.configurator.v0.ZookeeperApi.{Creation, ZookeeperClusterStatus}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import spray.json.JsString

import scala.concurrent.{ExecutionContext, Future}

/**
  * An interface of controlling zookeeper cluster.
  * It isolates the implementation of container manager from Configurator.
  */
trait ZookeeperCollie extends Collie[ZookeeperClusterStatus] {

  // the required files for zookeeper
  private[agent] val zooCfgPath: String = "/home/ohara/default/conf/zoo.cfg"
  private[agent] val myIdPath: String = "/home/ohara/default/data/myid"

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
    clusters().flatMap(clusters => {
      if (clusters.keys.exists(_.key == creation.key))
        Future.failed(
          new UnsupportedOperationException(s"zookeeper collie doesn't support to add node to a running cluster"))
      else
        dataCollie.valuesByNames[Node](creation.nodeNames).flatMap {
          newNodes =>
            // add route in order to make zk node can connect to each other.
            val route: Map[String, String] = newNodes.map(node => node.name -> CommonUtils.address(node.name)).toMap
            // ssh connection is slow so we submit request by multi-thread
            Future
              .sequence(newNodes.zipWithIndex.map {
                case (node, nodeIndex) =>
                  val hostname = Collie.containerHostName(prefixKey, creation.group, creation.name, serviceName)
                  val zkServers = newNodes.map(_.name).zipWithIndex.map {
                    case (nodeName, serverIndex) =>
                      /**
                        * this is a long story.
                        * zookeeper quorum has to bind three ports: client port, peer port and election port
                        * 1) the client port, by default, is bound on all network interface (0.0.0.0)
                        * 2) the peer port and election port are bound on the "server name". this config has form:
                        *    server.$i=$serverName:$peerPort:$electionPort
                        *    Hence, the $serverName must be equal to hostname of container. Otherwise, the BindException
                        *    will be thrown. By contrast, the other $serverNames are used to connect (if the quorum is not lead)
                        *    Hence, the other $serverNames MUST be equal to "node names"
                        */
                      val serverName = if (serverIndex == nodeIndex) hostname else nodeName
                      s"server.$serverIndex=$serverName:${creation.peerPort}:${creation.electionPort}"
                  }
                  val containerInfo = ContainerInfo(
                    nodeName = node.name,
                    id = Collie.UNKNOWN,
                    imageName = creation.imageName,
                    created = Collie.UNKNOWN,
                    // this fake container will be cached before refreshing cache so we make it running.
                    // other, it will be filtered later ...
                    state = ContainerState.RUNNING.name,
                    kind = Collie.UNKNOWN,
                    name = Collie.containerName(prefixKey, creation.group, creation.name, serviceName),
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
                    },
                    hostname = hostname
                  )

                  /**
                    * Construct the required configs for current container
                    * we will loop all the files in FILE_DATA of arguments : --file A --file B --file C
                    * the format of A, B, C should be file_name=k1=v1,k2=v2,k3,k4=v4...
                    */
                  val configFiles = Map(
                    zooCfgPath -> {
                      Seq(
                        s"${ZookeeperApi.TICK_TIME_KEY}=2000",
                        s"${ZookeeperApi.INIT_LIMIT_KEY}=10",
                        s"${ZookeeperApi.SYNC_LIMIT_KEY}=5",
                        s"${ZookeeperApi.MAX_CLIENT_CNXNS_KEY}=60",
                        s"${ZookeeperApi.CLIENT_PORT_KEY}=${creation.clientPort}",
                        s"${ZookeeperApi.DATA_DIR_KEY}=${myIdPath.substring(0, myIdPath.lastIndexOf("/"))}"
                      ) ++ zkServers
                    }.mkString(","),
                    myIdPath -> Seq(nodeIndex.toString).mkString(",")
                  )
                  val arguments = configFiles.flatMap { case (k, v) => Seq("--file", s"$k=$v") }.toSeq
                  doCreator(executionContext, containerInfo.name, containerInfo, node, route, arguments)
                    .map(_ => Some(containerInfo))
                    .recover {
                      case _: Throwable =>
                        None
                    }
              })
              .map(_.flatten.toSeq)
              .map { successfulContainers =>
                val state = toClusterState(successfulContainers).map(_.name)
                postCreate(
                  new ZookeeperClusterStatus(
                    group = creation.group,
                    name = creation.name,
                    // no state means cluster is NOT running so we cleanup the dead nodes
                    aliveNodes = state.map(_ => successfulContainers.map(_.nodeName).toSet).getOrElse(Set.empty),
                    state = state,
                    error = None
                  ),
                  successfulContainers
                )
              }
        }
    })
  }

  protected def dataCollie: DataCollie

  /**
    * The prefix name for platform
    * @return
    */
  protected def prefixKey: String

  protected def doCreator(executionContext: ExecutionContext,
                          containerName: String,
                          containerInfo: ContainerInfo,
                          node: Node,
                          route: Map[String, String],
                          arguments: Seq[String]): Future[Unit]

  protected def postCreate(clusterStatus: ZookeeperClusterStatus, successfulContainers: Seq[ContainerInfo]): Unit = {
    //Default Nothing
  }

  override protected[agent] def toStatus(key: ObjectKey, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[ZookeeperClusterStatus] =
    Future.successful(
      new ZookeeperClusterStatus(
        group = key.group(),
        name = key.name(),
        // Currently, docker and k8s has same naming rule for "Running",
        // it is ok that we use the containerState.RUNNING here.
        aliveNodes = containers.filter(_.state == ContainerState.RUNNING.name).map(_.nodeName).toSet,
        state = toClusterState(containers).map(_.name),
        // TODO how could we fetch the error?...by Sam
        error = None
      ))
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
