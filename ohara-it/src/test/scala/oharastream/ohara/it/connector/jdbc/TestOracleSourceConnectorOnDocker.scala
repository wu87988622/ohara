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

package oharastream.ohara.it.connector.jdbc

import oharastream.ohara.agent.DataCollie
import oharastream.ohara.agent.docker.DockerClient
import oharastream.ohara.client.configurator.v0.NodeApi
import oharastream.ohara.configurator.Configurator
import oharastream.ohara.it.category.ConnectorGroup
import oharastream.ohara.it.{EnvTestingUtils, ServiceKeyHolder}
import org.junit.experimental.categories.Category

@Category(Array(classOf[ConnectorGroup]))
class TestOracleSourceConnectorOnDocker extends BasicTestOracleJDBCSourceConnector {
  override val configurator: Configurator = Configurator.builder
    .hostname(EnvTestingUtils.configuratorHostName())
    .port(EnvTestingUtils.configuratorHostPort())
    .build()

  override protected val nodes: Seq[NodeApi.Node] = EnvTestingUtils.dockerNodes()

  override protected val nameHolder: ServiceKeyHolder = ServiceKeyHolder(DockerClient(DataCollie(nodes)))
}
