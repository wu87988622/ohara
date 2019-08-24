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

package com.island.ohara.it.agent
import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.agent.k8s.K8SClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * In agent tests we build many clusters to test. If we don't cleanup them, the resources of it nodes may be exhausted
  * and then sequential tests will be timeout.
  *
  */
trait ClusterNameHolder extends Releasable {

  /**
    * store the name used to create cluster. We can remove all created cluster in the "after" phase.
    */
  protected val usedClusterNames: mutable.HashSet[String] = new mutable.HashSet[String]()

  /**
    * our IT env is flooded with many running/exited containers. As a normal human, it is hard to observer the containers
    * invoked by our IT. Hence, this env variable enable us to add prefix to containers.
    */
  private[this] val prefix: String = sys.env.getOrElse("ohara.it.container.prefix", "cnh")

  def generateClusterName(): String = {
    val name = prefix + CommonUtils.randomString(7)
    usedClusterNames += name
    name
  }

  override def close(): Unit = release(
    clusterNames = usedClusterNames.toSet,
    excludedNodes = Set.empty
  )

  def release(clusterNames: Set[String], excludedNodes: Set[String]): Unit
}

object ClusterNameHolder {
  private[this] val LOG = Logger(classOf[ClusterNameHolder])

  /**
    * create a name holder based on ssh.
    * @param nodes nodes
    * @return name holder
    */
  def apply(nodes: Seq[Node]): ClusterNameHolder = (clusterNames: Set[String], excludedNodes: Set[String]) =>
    nodes.filterNot(node => excludedNodes.contains(node.name)).foreach { node =>
      val client =
        DockerClient.builder.hostname(node.hostname).port(node._port).user(node._user).password(node._password).build
      try client
        .containerNames()
        .filter(containerName => clusterNames.exists(clusterName => containerName.contains(clusterName)))
        .foreach { containerName =>
          try {
            println(s"[-----------------------------------$containerName-----------------------------------]")
            val containerLogs = try client.log(containerName)
            catch {
              case e: Throwable =>
                s"failed to fetch the logs for container:$containerName. caused by:${e.getMessage}"
            }
            println(containerLogs)
            println("[------------------------------------------------------------------------------------]")
            client.forceRemove(containerName)
            LOG.info(s"succeed to remove container $containerName")
          } catch {
            case e: Throwable =>
              LOG.error(s"failed to remove container $containerName", e)
          }
        } finally client.close()
  }

  /**
    * create a name holder based on k8s.
    * @param nodes nodes
    * @param client k8s client
    * @return name holder
    */
  def apply(nodes: Seq[Node], client: K8SClient): ClusterNameHolder =
    (clusterNames: Set[String], excludedNodes: Set[String]) =>
      try Await
        .result(client.containers(), 30 seconds)
        .filter(container => clusterNames.exists(clusterName => container.name.contains(clusterName)))
        .filterNot(container => excludedNodes.contains(container.nodeName))
        .foreach { container =>
          try {
            println(s"[-----------------------------------${container.name}-----------------------------------]")
            val containerLogs = try Await.result(client.log(container.name), 30 seconds)
            catch {
              case e: Throwable =>
                s"failed to fetch the logs for container:${container.name}. caused by:${e.getMessage}"
            }
            println(containerLogs)
            println("[------------------------------------------------------------------------------------]")
            Await.result(client.forceRemove(container.name), 30 seconds)
          } catch {
            case e: Throwable =>
              LOG.error(s"failed to remove container ${container.name}", e)
          }
        } finally client.close()
}
