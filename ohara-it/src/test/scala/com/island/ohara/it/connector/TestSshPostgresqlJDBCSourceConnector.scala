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

package com.island.ohara.it.connector
import com.island.ohara.agent.{ClusterCollie, NodeCollie}
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.agent.{ClusterNameHolder, CollieTestUtils}
import com.island.ohara.it.category.SshConnectorGroup
import org.junit.experimental.categories.Category

@Category(Array(classOf[SshConnectorGroup]))
class TestSshPostgresqlJDBCSourceConnector extends BasicTestPostgresqlJDBCSourceConnector {
  private[this] var _clusterCollie: ClusterCollie = _

  override protected val nodeCache: Seq[NodeApi.Node] = CollieTestUtils.nodeCache()

  override protected val nameHolder = ClusterNameHolder(nodeCache)

  override protected def createConfigurator(nodeCache: Seq[NodeApi.Node], hostname: String, port: Int): Configurator =
    Configurator.builder.hostname(hostname).port(port).build()

  override protected def checkClusterInfo(): Unit =
    if (nodeCache.isEmpty) skipTest(s"You must assign nodes for collie tests")
    else _clusterCollie = ClusterCollie.builderOfSsh.nodeCollie(NodeCollie(nodeCache)).build()

  override protected def clusterCollie: ClusterCollie = _clusterCollie
}
