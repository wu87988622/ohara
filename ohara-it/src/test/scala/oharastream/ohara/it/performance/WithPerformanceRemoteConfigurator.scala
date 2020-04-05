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

import java.util.concurrent.TimeUnit

import oharastream.ohara.agent.container.ContainerClient
import oharastream.ohara.client.configurator.v0.NodeApi.Node
import oharastream.ohara.common.util.{CommonUtils, Releasable, VersionUtils}
import oharastream.ohara.it.{ContainerPlatform, IntegrationTest, ServiceKeyHolder}
import org.junit.{After, Before}

/**
  * a basic setup offering a configurator running on remote node.
  * this stuff is also in charge of releasing the configurator after testing.
  */
abstract class WithPerformanceRemoteConfigurator extends IntegrationTest {
  private[this] val platform                           = ContainerPlatform.default
  protected val containerClient: ContainerClient       = platform.containerClient
  private[this] val serviceKeyHolder: ServiceKeyHolder = ServiceKeyHolder(containerClient, false)
  private[this] val configuratorContainerKey           = serviceKeyHolder.generateClusterKey()
  protected val configuratorHostname: String           = platform.configuratorHostname
  protected val configuratorPort: Int                  = CommonUtils.availablePort()

  /**
    * we have to combine the group and name in order to make name holder to delete related container.
    */
  private[this] val configuratorContainerName: String =
    s"${configuratorContainerKey.group()}-${configuratorContainerKey.name()}"

  private[this] val imageName = s"oharastream/configurator:${VersionUtils.VERSION}"

  protected var nodes: Seq[Node] = _

  @Before
  def setupConfigurator(): Unit = {
    result(
      containerClient.containerCreator
        .nodeName(configuratorHostname)
        .imageName(imageName)
        .portMappings(Map(configuratorPort -> configuratorPort))
        .arguments(
          Seq(
            "--hostname",
            configuratorHostname,
            "--port",
            configuratorPort.toString
          ) ++ platform.arguments
        )
        .routes(nodes.map(node => node.hostname -> CommonUtils.address(node.hostname)).toMap)
        .name(configuratorContainerName)
        .create()
    )

    // Wait configurator start completed
    TimeUnit.SECONDS.sleep(10)
  }

  @After
  def releaseConfigurator(): Unit = {
    Releasable.close(serviceKeyHolder)
    // the client is used by name holder so we have to close it later
    Releasable.close(containerClient)
  }
}
