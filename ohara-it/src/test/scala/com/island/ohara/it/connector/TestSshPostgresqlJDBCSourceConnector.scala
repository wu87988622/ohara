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
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.EnvTestingUtils
import com.island.ohara.it.agent.ClusterNameHolder
import com.island.ohara.it.category.SshConnectorGroup
import org.junit.experimental.categories.Category
import scala.concurrent.ExecutionContext.Implicits.global

@Category(Array(classOf[SshConnectorGroup]))
class TestSshPostgresqlJDBCSourceConnector extends BasicTestPostgresqlJDBCSourceConnector {
  override val configurator: Configurator = Configurator.builder
    .hostname(EnvTestingUtils.configuratorHostName())
    .port(EnvTestingUtils.configuratorHostPort())
    .build()

  override protected val nodes: Seq[NodeApi.Node] = EnvTestingUtils.sshNodes()

  override protected val nameHolder = ClusterNameHolder(nodes)

  override protected def createor(): Unit = {
    val nodeApi = NodeApi.access.hostname(configurator.hostname).port(configurator.port)
    nodes.foreach { node =>
      result(
        nodeApi.request.hostname(node.hostname).port(node._port).user(node._user).password(node._password).create())
    }
  }
}
