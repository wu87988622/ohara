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

package com.island.ohara.it.collie

import com.island.ohara.agent.Agent
import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.{ContainerApi, NodeApi, ZookeeperApi}
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.category.CollieGroup
import com.island.ohara.it.{EnvTestingUtils, IntegrationTest}
import org.junit.experimental.categories.Category
import org.junit.{After, Before, Test}
import org.scalatest.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global

@Category(Array(classOf[CollieGroup]))
class TestGetNodeWithRunningCluster extends IntegrationTest {
  private[this] val nodes: Seq[Node] = EnvTestingUtils.dockerNodes()

  private[this] val nameHolder: ClusterNameHolder = ClusterNameHolder(nodes)

  private[this] val configurator: Configurator = Configurator.builder.build()

  @Before
  def setup(): Unit = {
    nodes.foreach { node =>
      val dockerClient =
        DockerClient(
          Agent.builder.hostname(node.hostname).port(node._port).user(node._user).password(node._password).build
        )
      try {
        withClue(s"failed to find ${ZookeeperApi.IMAGE_NAME_DEFAULT}")(
          dockerClient.imageNames().contains(ZookeeperApi.IMAGE_NAME_DEFAULT) shouldBe true
        )
      } finally dockerClient.close()
    }
    nodes.foreach { node =>
      result(
        NodeApi.access
          .hostname(configurator.hostname)
          .port(configurator.port)
          .request
          .hostname(node.hostname)
          .port(node._port)
          .user(node._user)
          .password(node._password)
          .create()
      )
    }
  }

  @Test
  def test(): Unit = {
    val cluster = result(
      ZookeeperApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .key(nameHolder.generateClusterKey())
        .nodeNames(nodes.map(_.name).toSet)
        .create()
        .flatMap(
          info =>
            ZookeeperApi.access
              .hostname(configurator.hostname)
              .port(configurator.port)
              .start(info.key)
              .flatMap(_ => ZookeeperApi.access.hostname(configurator.hostname).port(configurator.port).get(info.key))
        )
    )
    assertCluster(
      () => result(ZookeeperApi.access.hostname(configurator.hostname).port(configurator.port).list()),
      () =>
        result(
          ContainerApi.access
            .hostname(configurator.hostname)
            .port(configurator.port)
            .get(cluster.key)
            .map(_.flatMap(_.containers))
        ),
      cluster.key
    )
    result(NodeApi.access.hostname(configurator.hostname).port(configurator.port).list()).isEmpty shouldBe false
    result(NodeApi.access.hostname(configurator.hostname).port(configurator.port).list()).foreach { node =>
      node.services.isEmpty shouldBe false
      withClue(s"${node.services}")(node.services.map(_.clusterKeys.size).sum > 0 shouldBe true)
    }
  }

  @After
  final def tearDown(): Unit = {
    Releasable.close(configurator)
    Releasable.close(nameHolder)
  }
}
