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
import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.{Releasable, ReleaseOnce}

import scala.collection.mutable

trait DockerClientCache extends Releasable {
  def exec[T](node: Node, f: DockerClient => T): T
}

object DockerClientCache {
  def apply(): DockerClientCache = new DockerClientCacheImpl()

  private[this] class DockerClientCacheImpl extends ReleaseOnce with DockerClientCache {
    private[this] val lock = new Object()
    private[this] val cache: mutable.HashMap[Node, DockerClient] = new mutable.HashMap[Node, DockerClient]()

    override protected def doClose(): Unit = lock.synchronized {
      cache.values.foreach(Releasable.close)
      cache.clear()
    }

    override def exec[T](node: Node, f: DockerClient => T): T = if (isClosed) throw new IllegalStateException()
    else {
      val client = lock.synchronized {
        cache.getOrElseUpdate(
          node,
          DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build())
      }
      f(client)
    }
  }
}
