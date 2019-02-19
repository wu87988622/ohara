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

import com.island.ohara.agent.DockerClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable

/**
  * In agent tests we build many clusters to test. If we don't cleanup them, the resources of it nodes may be exhausted
  * and then sequential tests will be timeout.
  *
  */
class ClusterNameHolder(nodes: Seq[Node]) extends Releasable {
  private[this] val log = Logger(classOf[ClusterNameHolder])

  /**
    * store the name used to create cluster. We can remove all created cluster in the "after" phase.
    */
  private[this] val usedClusterNames: mutable.HashSet[String] = new mutable.HashSet[String]()

  def generateClusterName(): String = {
    val name = CommonUtil.randomString(10)
    usedClusterNames += name
    name
  }

  override def close(): Unit = {
    nodes.foreach { node =>
      val client =
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
      try client
        .names()
        .filter(containerName => usedClusterNames.exists(clusterName => containerName.contains(clusterName)))
        .foreach { containerName =>
          try {
            client.forceRemove(containerName)
            log.info(s"succeed to remove container $containerName")
          } catch {
            case e: Throwable =>
              log.error(s"failed to remove container $containerName", e)
          }
        } finally client.close()

    }
  }
}
