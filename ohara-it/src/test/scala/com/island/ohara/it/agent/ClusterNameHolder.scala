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
import com.island.ohara.agent.Agent
import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.agent.k8s.K8SClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.setting.ObjectKey
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
  private[this] val log = Logger(classOf[ClusterNameHolder])

  /**
    * store the name used to create cluster. We can remove all created cluster in the "after" phase.
    */
  private[this] val usedClusterKeys: mutable.HashSet[ObjectKey] = new mutable.HashSet[ObjectKey]()

  /**
    * our IT env is flooded with many running/exited containers. As a normal human, it is hard to observer the containers
    * invoked by our IT. Hence, this env variable enable us to add prefix to containers.
    */
  private[this] val prefix: String = sys.env.getOrElse("ohara.it.container.prefix", "cnh")

  /**
    * generate a random cluster key. The key is logged and this holder will iterate all nodes to remove all related
    * containers
    *
    * Noted: the group of key is always "default".
    * @return a random key
    */
  def generateClusterKey(): ObjectKey = {
    val key = ObjectKey.of(com.island.ohara.client.configurator.v0.GROUP_DEFAULT, prefix + CommonUtils.randomString(7))
    log.info(s"cluster key is $key")
    usedClusterKeys += key
    key
  }

  /**
    * Add a cluster name to this ClusterNameHolder for managing.
    *
    * @param clusterKey cluster key
    */
  def addClusterKey(clusterKey: ObjectKey): Unit = {
    usedClusterKeys += clusterKey
  }

  override def close(): Unit = release(
    clusterKeys = usedClusterKeys.toSet,
    excludedNodes = Set.empty,
    finalClose = true
  )

  /**
    * remove all containers belonging to input clusters. The argument "excludedNodes" enable you to remove a part of
    * containers from input clusters.
    * @param clusterKeys clusters to remove
    * @param excludedNodes nodes to keep the containers
    */
  def release(clusterKeys: Set[ObjectKey], excludedNodes: Set[String]): Unit =
    release(clusterKeys = clusterKeys, excludedNodes = excludedNodes, finalClose = false)

  /**
    * remove all containers belonging to input clusters. The argument "excludedNodes" enable you to remove a part of
    * containers from input clusters.
    * @param clusterKeys clusters to remove
    * @param excludedNodes nodes to keep the containers
    * @param finalClose true if this name holder should be closed as well
    */
  protected def release(clusterKeys: Set[ObjectKey], excludedNodes: Set[String], finalClose: Boolean): Unit
}

object ClusterNameHolder {
  /**
    * used to debug :)
    */
  private[this] val KEEP_CONTAINERS = sys.env.get("ohara.it.keep.containers").exists(_.toLowerCase == "true")
  private[this] val LOG             = Logger(classOf[ClusterNameHolder])

  /**
    * create a name holder based on ssh.
    * @param nodes nodes
    * @return name holder
    */
  def apply(nodes: Seq[Node]): ClusterNameHolder =
    (clusterKey: Set[ObjectKey], excludedNodes: Set[String], finalClose: Boolean) =>
      /**
        * Some IT need to close containers so we don't obstruct them form "releasing".
        * However, the final close is still controlled by the global flag.
        */
      if (!finalClose || !KEEP_CONTAINERS)
        nodes.filterNot(node => excludedNodes.contains(node.name)).foreach { node =>
          val client =
            DockerClient(
              Agent.builder.hostname(node.hostname).port(node._port).user(node._user).password(node._password).build
            )
          try client
            .containerNames()
            .filter(
              containerName =>
                clusterKey
                  .exists(key => containerName.name.contains(key.group()) && containerName.name.contains(key.name()))
            )
            .foreach { containerName =>
              try {
                println(
                  s"[-----------------------------------[image:${containerName.imageName}][name:${containerName.name}-----------------------------------]"
                )
                val containerLogs = try client.log(containerName.name, None)
                catch {
                  case e: Throwable =>
                    s"failed to fetch the logs for container:${containerName.name}. caused by:${e.getMessage}"
                }
                println(containerLogs)
                println("[------------------------------------------------------------------------------------]")
                client.forceRemove(containerName.name)
                LOG.info(s"succeed to remove container ${containerName.name}")
              } catch {
                case e: Throwable =>
                  LOG.error(s"failed to remove container ${containerName.name}", e)
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
    (clusterKey: Set[ObjectKey], excludedNodes: Set[String], finalClose: Boolean) =>
      /**
        * Some IT need to close containers so we don't obstruct them form "releasing".
        * However, the final close is still controlled by the global flag.
        */
      if (!finalClose || !KEEP_CONTAINERS)
        Await
          .result(client.containers(), 30 seconds)
          .filter(
            container =>
              clusterKey.exists(key => container.name.contains(key.group()) && container.name.contains(key.name()))
          )
          .filterNot(container => excludedNodes.contains(container.nodeName))
          .foreach { container =>
            try {
              println(s"[-----------------------------------${container.name}-----------------------------------]")
              val containerLogs = try Await.result(client.log(container.name, None), 30 seconds)
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
          }
}
