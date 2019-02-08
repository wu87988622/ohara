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

import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.NodeApi.Node
import org.junit.Before

import scala.concurrent.Future

/**
  * a basic test for ssh collie. It instantiate a ssh collie based on passed arguments.
  */
abstract class BasicTestsOfSshCollie extends BasicTestsOfCollie {
  override protected val nodeCache: Seq[Node] = CollieTestUtil.nodeCache()
  override protected val clusterCollie: ClusterCollie = ClusterCollie(new NodeCollie {
    override def nodes(): Future[Seq[Node]] = Future.successful(nodeCache)
    override def node(name: String): Future[Node] = Future.successful(
      nodeCache.find(_.name == name).getOrElse(throw new NoSuchElementException(s"expected:$name actual:$nodeCache")))
  })

  @Before
  final def setup(): Unit = if (nodeCache.isEmpty) skipTest(s"${CollieTestUtil.key} is required")
}
