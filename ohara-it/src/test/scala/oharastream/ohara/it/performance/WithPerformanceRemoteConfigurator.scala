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

package oharastream.ohara.it.performance

import oharastream.ohara.agent.container.ContainerClient
import oharastream.ohara.client.configurator.NodeApi.Node
import oharastream.ohara.common.util.Releasable
import oharastream.ohara.it.{ContainerPlatform, IntegrationTest, ServiceKeyHolder}
import org.junit.After

/**
  * a basic setup offering a configurator running on remote node.
  * this stuff is also in charge of releasing the configurator after testing.
  */
abstract class WithPerformanceRemoteConfigurator extends IntegrationTest {
  private[this] val platform                           = ContainerPlatform.default
  private[this] val resourceRef                        = platform.setup()
  protected val containerClient: ContainerClient       = resourceRef.containerClient
  private[this] val serviceKeyHolder: ServiceKeyHolder = ServiceKeyHolder(containerClient)
  protected val configuratorHostname: String           = resourceRef.configuratorHostname
  protected val configuratorPort: Int                  = resourceRef.configuratorPort

  protected var nodes: Seq[Node] = _

  @After
  def releaseConfigurator(): Unit = {
    Releasable.close(serviceKeyHolder)
    // the client is used by name holder so we have to close it later
    Releasable.close(resourceRef)
  }
}
