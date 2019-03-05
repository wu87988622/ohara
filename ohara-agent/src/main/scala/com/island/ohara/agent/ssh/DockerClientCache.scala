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

package com.island.ohara.agent.ssh

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import com.island.ohara.agent.DockerClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.Releasable

/**
  * This interface enable us to reuse the docker client object.
  */
private class DockerClientCache extends Releasable {
  private[this] val cache: ConcurrentMap[Node, DockerClient] = new ConcurrentHashMap[Node, DockerClient]()

  /**
    * get cached client instance related to input node
    * @param node remote node
    * @return cached client
    */
  def get(node: Node): DockerClient = cache.computeIfAbsent(
    node,
    node => DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build())

  /**
    * get cached client instances related to input nodes
    * @param nodes remote nodes
    * @return cached client
    */
  def get(nodes: Seq[Node]): Seq[DockerClient] = nodes.map(get)

  override def close(): Unit = {
    // if we are in closing service, closing the cached client can produce error when other threads keep using the client.
    cache.values().forEach(client => Releasable.close(client))
    cache.clear()
  }
}
