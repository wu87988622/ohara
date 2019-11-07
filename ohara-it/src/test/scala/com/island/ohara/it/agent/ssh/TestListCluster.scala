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

import com.island.ohara.agent._
import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.{BrokerApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.it.agent.ClusterNameHolder
import com.island.ohara.it.category.SshCollieGroup
import com.island.ohara.it.{EnvTestingUtils, IntegrationTest}
import org.junit.experimental.categories.Category
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
@Category(Array(classOf[SshCollieGroup]))
class TestListCluster extends IntegrationTest with Matchers {
  private[this] val nodes: Seq[Node] = EnvTestingUtils.sshNodes()
  private[this] val nameHolder = ClusterNameHolder(nodes)

  private[this] val dataCollie: DataCollie = DataCollie(nodes)

  private[this] val serviceCollie: ServiceCollie =
    ServiceCollie.builderOfSsh.dataCollie(dataCollie).build()

  @Before
  def setup(): Unit = if (nodes.size < 2) skipTest("please buy more servers to run this test")
  else
    nodes.foreach { node =>
      val dockerClient =
        DockerClient(
          Agent.builder.hostname(node.hostname).port(node._port).user(node._user).password(node._password).build)
      try {
        withClue(s"failed to find ${ZookeeperApi.IMAGE_NAME_DEFAULT}")(
          dockerClient.imageNames().contains(ZookeeperApi.IMAGE_NAME_DEFAULT) shouldBe true)
        withClue(s"failed to find ${BrokerApi.IMAGE_NAME_DEFAULT}")(
          dockerClient.imageNames().contains(BrokerApi.IMAGE_NAME_DEFAULT) shouldBe true)
        withClue(s"failed to find ${WorkerApi.IMAGE_NAME_DEFAULT}")(
          dockerClient.imageNames().contains(WorkerApi.IMAGE_NAME_DEFAULT) shouldBe true)
      } finally dockerClient.close()
    }

  @Test
  def deadContainerAndClusterShouldDisappear(): Unit = {
    val clusterKey = nameHolder.generateClusterKey()
    try result(
      serviceCollie.zookeeperCollie.creator
        .imageName(ZookeeperApi.IMAGE_NAME_DEFAULT)
        .group(com.island.ohara.client.configurator.v0.GROUP_DEFAULT)
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
        result(serviceCollie.zookeeperCollie.clusters()).find(_._1.key == clusterKey).map(_._2).getOrElse(Seq.empty)
      containers.map(_.nodeName).toSet == Set(aliveNode.hostname)
    }

    // remove all containers
    nameHolder.release(
      clusterKeys = Set(clusterKey),
      excludedNodes = Set.empty
    )

    await { () =>
      !result(serviceCollie.zookeeperCollie.clusters()).map(_._1.key).toSet.contains(clusterKey)
    }
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(serviceCollie)
    Releasable.close(nameHolder)
  }
}
