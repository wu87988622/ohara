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

package oharastream.ohara.it

import java.util.concurrent.TimeUnit

import oharastream.ohara.agent.container.ContainerClient
import oharastream.ohara.client.configurator.v0.NodeApi
import oharastream.ohara.client.configurator.v0.NodeApi.Node
import oharastream.ohara.common.util.{CommonUtils, Releasable, VersionUtils}
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.{After, Before}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * a basic setup offering a configurator running on remote node.
  * this stuff is also in charge of releasing the configurator after testing.
  */
@RunWith(value = classOf[Parameterized])
abstract class WithRemoteConfigurator(platform: ContainerPlatform) extends IntegrationTest {
  protected final val containerClient: ContainerClient    = platform.containerClient
  protected[this] val nodes: Seq[Node]                    = platform.nodes
  protected final val serviceNameHolder: ServiceKeyHolder = ServiceKeyHolder(containerClient, false)
  protected final val configuratorHostname: String        = platform.configuratorHostname
  private[this] val configuratorContainerKey              = serviceNameHolder.generateClusterKey()
  protected final val configuratorPort: Int               = CommonUtils.availablePort()

  /**
    * we have to combine the group and name in order to make name holder to delete related container.
    */
  private[this] val configuratorContainerName: String =
    s"${configuratorContainerKey.group()}-${configuratorContainerKey.name()}"

  private[this] val imageName = s"oharastream/configurator:${VersionUtils.VERSION}"

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
        // add the routes manually since not all envs have deployed the DNS.
        .routes(nodes.map(node => node.hostname -> CommonUtils.address(node.hostname)).toMap)
        .name(configuratorContainerName)
        .create()
    )

    // wait for configurator
    TimeUnit.SECONDS.sleep(20)

    val nodeApi = NodeApi.access.hostname(configuratorHostname).port(configuratorPort)
    nodes.foreach { node =>
      if (!result(nodeApi.list()).map(_.hostname).contains(node.hostname)) {
        result(
          nodeApi.request
            .hostname(node.hostname)
            .port(node.port.get)
            .user(node.user.get)
            .password(node.password.get)
            .create()
        )
      }
    }
  }

  @After
  def releaseConfigurator(): Unit = {
    Releasable.close(serviceNameHolder)
    // the client is used by name holder so we have to close it later
    Releasable.close(containerClient)
  }
}

object WithRemoteConfigurator {
  @Parameters(name = "{index} mode = {0}")
  def parameters: java.util.Collection[ContainerPlatform] = {
    val modes = ContainerPlatform.all
    if (modes.isEmpty) java.util.Collections.singletonList(ContainerPlatform.empty)
    else modes.asJava
  }
}
