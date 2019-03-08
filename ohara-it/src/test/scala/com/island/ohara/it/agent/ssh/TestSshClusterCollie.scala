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

package com.island.ohara.it.agent.ssh

import com.island.ohara.agent.{ClusterCollie, NodeCollie}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.Releasable
import com.island.ohara.it.agent.{BasicTests4ClusterCollie, ClusterNameHolder, CollieTestUtil}
import org.junit.{After, Before, Test}
class TestSshClusterCollie extends BasicTests4ClusterCollie {
  override protected val nodeCache: Seq[Node] = CollieTestUtil.nodeCache()
  private[this] val nameHolder = new ClusterNameHolder(nodeCache)
  override protected val clusterCollie: ClusterCollie =
    ClusterCollie.ssh(NodeCollie(nodeCache))

//  override protected val clusterCollie: ClusterCollie = ClusterCollie.ssh(NodeCollie(nodeCache))
  @Before
  final def setup(): Unit = if (nodeCache.isEmpty) skipTest(s"You must assign nodes for collie tests")

  override protected def generateClusterName(): String = nameHolder.generateClusterName()

  @Test
  def testListImages(): Unit = {
    val images = result(clusterCollie.images(nodeCache))
    images.isEmpty shouldBe false
    images.foreach(_._2.isEmpty shouldBe false)
  }

  @After
  def cleanupContainers(): Unit = if (cleanup) Releasable.close(nameHolder)
}
