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

import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.EnvTestingUtils
import com.island.ohara.it.agent.{BasicTests4StreamApp, ClusterNameHolder}
import com.island.ohara.it.category.SshCollieGroup
import org.junit.experimental.categories.Category

@Category(Array(classOf[SshCollieGroup]))
class TestSshStreamApp extends BasicTests4StreamApp {
  override protected val nodes: Seq[Node] = EnvTestingUtils.sshNodes()

  override protected val nameHolder: ClusterNameHolder = ClusterNameHolder(nodes)

  override protected def createConfigurator(hostname: String, port: Int): Configurator =
    Configurator.builder.hostname(hostname).port(port).build()
}
