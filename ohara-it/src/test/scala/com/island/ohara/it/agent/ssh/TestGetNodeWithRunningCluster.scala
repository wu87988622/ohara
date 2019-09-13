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

import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.{ContainerApi, NodeApi, ZookeeperApi}
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.IntegrationTest
import com.island.ohara.it.agent.{ClusterNameHolder, CollieTestUtils}
import com.island.ohara.it.category.SshConfiguratorGroup
import org.junit.experimental.categories.Category
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

@Category(Array(classOf[SshConfiguratorGroup]))
class TestGetNodeWithRunningCluster extends IntegrationTest with Matchers {

  private[this] val nodeCache: Seq[Node] = CollieTestUtils.nodeCache()

  private[this] val nameHolder: ClusterNameHolder = ClusterNameHolder(nodeCache)

  private[this] val configurator: Configurator = Configurator.builder.build()

  @Before
  def setup(): Unit = if (nodeCache.isEmpty) skipTest(s"${CollieTestUtils.key} is required")
  else {
    nodeCache.foreach { node =>
      val dockerClient =
        DockerClient.builder.hostname(node.hostname).port(node._port).user(node._user).password(node._password).build
      try {
        withClue(s"failed to find ${ZookeeperApi.IMAGE_NAME_DEFAULT}")(
          dockerClient.imageNames().contains(ZookeeperApi.IMAGE_NAME_DEFAULT) shouldBe true)
      } finally dockerClient.close()
    }
    nodeCache.foreach { node =>
      result(
        NodeApi.access
          .hostname(configurator.hostname)
          .port(configurator.port)
          .request
          .hostname(node.hostname)
          .port(node._port)
          .user(node._user)
          .password(node._password)
          .create())
    }
  }

  @Test
  def test(): Unit = {
    val cluster = result(
      ZookeeperApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(nameHolder.generateClusterName())
        .nodeNames(nodeCache.map(_.name).toSet)
        .create()
        .flatMap(
          info =>
            ZookeeperApi.access
              .hostname(configurator.hostname)
              .port(configurator.port)
              .start(info.key)
              .flatMap(_ => ZookeeperApi.access.hostname(configurator.hostname).port(configurator.port).get(info.key))))
    assertCluster(
      () => result(ZookeeperApi.access.hostname(configurator.hostname).port(configurator.port).list()),
      () =>
        result(
          ContainerApi.access
            .hostname(configurator.hostname)
            .port(configurator.port)
            .get(cluster.name)
            .map(_.flatMap(_.containers))),
      cluster.name
    )
    val nodes = result(NodeApi.access.hostname(configurator.hostname).port(configurator.port).list())
    nodes.isEmpty shouldBe false
    nodes.foreach { node =>
      node.services.isEmpty shouldBe false
      withClue(s"${node.services}")(node.services.map(_.clusterNames.size).sum > 0 shouldBe true)
    }
  }

  @After
  final def tearDown(): Unit = {
    Releasable.close(configurator)
    Releasable.close(nameHolder)
  }
}
