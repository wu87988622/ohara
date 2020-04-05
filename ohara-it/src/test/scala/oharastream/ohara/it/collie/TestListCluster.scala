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

package oharastream.ohara.it.collie

import oharastream.ohara.agent._
import oharastream.ohara.agent.docker.DockerClient
import oharastream.ohara.client.configurator.v0.NodeApi.Node
import oharastream.ohara.client.configurator.v0.{BrokerApi, WorkerApi, ZookeeperApi}
import oharastream.ohara.common.util.{CommonUtils, Releasable}
import oharastream.ohara.it.category.CollieGroup
import oharastream.ohara.it.{IntegrationTest, ContainerPlatform, ServiceKeyHolder}
import org.junit.experimental.categories.Category
import org.junit.{After, Before, Test}
import org.scalatest.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
@Category(Array(classOf[CollieGroup]))
class TestListCluster extends IntegrationTest {
  private[this] val platform         = ContainerPlatform.dockerMode
  private[this] val nodes: Seq[Node] = platform.nodes
  private[this] val nameHolder       = ServiceKeyHolder(platform.containerClient)

  private[this] val dataCollie: DataCollie = DataCollie(nodes)

  private[this] val serviceCollie: ServiceCollie =
    ServiceCollie.dockerModeBuilder.dataCollie(dataCollie).build()

  @Before
  def setup(): Unit =
    if (nodes.size < 2) skipTest("please buy more servers to run this test")
    else {
      val images = {
        val dockerClient = DockerClient(DataCollie(nodes))
        try result(dockerClient.imageNames())
        finally dockerClient.close()
      }
      nodes.foreach { node =>
        withClue(s"failed to find ${ZookeeperApi.IMAGE_NAME_DEFAULT}")(
          images(node.hostname) should contain(ZookeeperApi.IMAGE_NAME_DEFAULT)
        )
        withClue(s"failed to find ${BrokerApi.IMAGE_NAME_DEFAULT}")(
          images(node.hostname) should contain(BrokerApi.IMAGE_NAME_DEFAULT)
        )
        withClue(s"failed to find ${WorkerApi.IMAGE_NAME_DEFAULT}")(
          images(node.hostname) should contain(WorkerApi.IMAGE_NAME_DEFAULT)
        )
      }
    }

  @Test
  def deadContainerAndClusterShouldDisappear(): Unit = {
    val clusterKey = nameHolder.generateClusterKey()
    try result(
      serviceCollie.zookeeperCollie.creator
        .group(oharastream.ohara.client.configurator.v0.GROUP_DEFAULT)
        .clientPort(CommonUtils.availablePort())
        .peerPort(CommonUtils.availablePort())
        .electionPort(CommonUtils.availablePort())
        .nodeNames(nodes.map(_.name).toSet)
        .key(clusterKey)
        .create()
    )
    catch {
      case e: Throwable =>
        // this is a normal case to start zookeeper, there should not have any exception...
        throw e
    }

    // we stop the running containers to simulate a "dead" cluster
    val aliveNode = nodes.head
    nameHolder.release(
      clusterKeys = Set(clusterKey),
      excludedNodes = Set(aliveNode.hostname)
    )

    await { () =>
      val containers =
        result(serviceCollie.zookeeperCollie.clusters())
          .find(_.key == clusterKey)
          .map(_.containers)
          .getOrElse(Seq.empty)
      containers.map(_.nodeName).toSet == Set(aliveNode.hostname)
    }

    // remove all containers
    nameHolder.release(
      clusterKeys = Set(clusterKey),
      excludedNodes = Set.empty
    )

    await { () =>
      !result(serviceCollie.zookeeperCollie.clusters()).map(_.key).toSet.contains(clusterKey)
    }
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(serviceCollie)
    Releasable.close(nameHolder)
  }
}
