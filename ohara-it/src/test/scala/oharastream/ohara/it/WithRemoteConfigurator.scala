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

import oharastream.ohara.agent.DataCollie
import oharastream.ohara.agent.container.ContainerClient
import oharastream.ohara.agent.docker.DockerClient
import oharastream.ohara.agent.k8s.K8SClient
import oharastream.ohara.client.configurator.v0.NodeApi
import oharastream.ohara.client.configurator.v0.NodeApi.Node
import oharastream.ohara.common.util.{CommonUtils, Releasable, VersionUtils}
import oharastream.ohara.it.EnvTestingUtils.K8S_NAMESPACE_KEY
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.{After, Before}
import org.scalatest.Matchers._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * a basic setup offering a configurator running on remote node.
  * this stuff is also in charge of releasing the configurator after testing.
  */
@RunWith(value = classOf[Parameterized])
abstract class WithRemoteConfigurator(platform: PlatformModeInfo) extends IntegrationTest {
  protected val containerClient: ContainerClient    = platform.containerClient
  protected[this] val nodes: Seq[Node]              = platform.nodes
  protected val nodeNames: Seq[String]              = nodes.map(_.hostname)
  protected val serviceNameHolder: ServiceKeyHolder = ServiceKeyHolder(containerClient, false)

  private[this] val configuratorNode            = EnvTestingUtils.configuratorNode()
  private[this] val configuratorContainerClient = DockerClient(DataCollie(Seq(configuratorNode)))
  private[this] val configuratorServiceKeyHolder: ServiceKeyHolder =
    ServiceKeyHolder(configuratorContainerClient, false)
  private[this] val configuratorContainerKey = configuratorServiceKeyHolder.generateClusterKey()
  protected val configuratorHostname: String = configuratorNode.hostname
  protected val configuratorPort: Int        = CommonUtils.availablePort()

  /**
    * we have to combine the group and name in order to make name holder to delete related container.
    */
  protected val configuratorContainerName: String =
    s"${configuratorContainerKey.group()}-${configuratorContainerKey.name()}"

  private[this] val imageName = s"oharastream/configurator:${VersionUtils.VERSION}"

  @Before
  def setupConfigurator(): Unit = {
    result(configuratorContainerClient.imageNames(configuratorHostname)) should contain(imageName)
    result(
      configuratorContainerClient.containerCreator
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
        .routes(EnvTestingUtils.routes(nodes))
        .name(configuratorContainerName)
        .create()
    )

    // wait for configurator
    TimeUnit.SECONDS.sleep(20)

    val nodeApi = NodeApi.access.hostname(configuratorHostname).port(configuratorPort)
    (nodes ++ Seq(configuratorNode)).foreach { node =>
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

    Releasable.close(configuratorServiceKeyHolder)
    Releasable.close(configuratorContainerClient)
  }
}

object WithRemoteConfigurator {
  @Parameters(name = "{index} mode = {0}")
  def parameters: java.util.Collection[PlatformModeInfo] = {
    def dockerMode =
      sys.env
        .get(EnvTestingUtils.DOCKER_NODES_KEY)
        .map(EnvTestingUtils.dockerNodes)
        .map(
          nodes =>
            PlatformModeInfo.builder
              .modeName("DOCKER")
              .nodes(nodes)
              .containerClient(DockerClient(DataCollie(nodes)))
              .build
        )

    def ks8Mode =
      Seq(
        sys.env.get(EnvTestingUtils.K8S_MASTER_KEY),
        sys.env.get(EnvTestingUtils.K8S_METRICS_SERVER_URL),
        sys.env.get(EnvTestingUtils.K8S_NODES_KEY)
      ).flatten match {
        case Seq(masterUrl, metricsUrl, k8sNodeString) =>
          Some(
            PlatformModeInfo.builder
              .modeName("K8S")
              .nodes(EnvTestingUtils.k8sNode(k8sNodeString))
              .containerClient(
                K8SClient.builder
                  .apiServerURL(masterUrl)
                  .namespace(sys.env.getOrElse(K8S_NAMESPACE_KEY, "default"))
                  .metricsApiServerURL(metricsUrl)
                  .build()
              )
              .arguments(
                Seq(
                  "--k8s",
                  masterUrl,
                  "--k8s-metrics-server",
                  metricsUrl
                )
              )
              .build
          )
        case _ => None
      }

    val modes = (dockerMode ++ ks8Mode).toSeq
    if (modes.isEmpty) java.util.Collections.singletonList(PlatformModeInfo.empty)
    else modes.asJava
  }
}
