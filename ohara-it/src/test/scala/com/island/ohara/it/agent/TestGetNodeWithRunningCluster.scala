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

package com.island.ohara.it.agent

import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.NodeApi.{Node, NodeCreationRequest}
import com.island.ohara.client.configurator.v0.{NodeApi, ZookeeperApi}
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.IntegrationTest
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestGetNodeWithRunningCluster extends IntegrationTest with Matchers {

  private[this] val nodeCache: Seq[Node] = CollieTestUtil.nodeCache()

  private[this] val nodeCollie: NodeCollie = new NodeCollie {
    override def nodes(): Future[Seq[Node]] = Future.successful(nodeCache)
    override def node(name: String): Future[Node] = Future.successful(
      nodeCache.find(_.name == name).getOrElse(throw new NoSuchElementException(s"expected:$name actual:$nodeCache")))
  }

  private[this] val configurator: Configurator =
    Configurator.builder().fake().hostname(CommonUtil.anyLocalAddress()).port(0).build()
  private[this] val clusterCollie: ClusterCollie = ClusterCollie(nodeCollie)

  /**
    * used to debug...
    */
  private[this] val cleanup = false

  private[this] def result[T](f: Future[T]): T = Await.result(f, 60 seconds)

  @Before
  def setup(): Unit = if (nodeCache.isEmpty) skipTest(s"${CollieTestUtil.key} is required")

  @Test
  def test(): Unit = {
    nodeCache.foreach { node =>
      val dockerClient =
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
      try {
        withClue(s"failed to find ${ZookeeperCollie.IMAGE_NAME_DEFAULT}")(
          dockerClient.images().contains(ZookeeperCollie.IMAGE_NAME_DEFAULT) shouldBe true)
      } finally dockerClient.close()
    }
    nodeCache.foreach { node =>
      result(
        NodeApi
          .access()
          .hostname(configurator.hostname)
          .port(configurator.port)
          .add(NodeCreationRequest(Some(node.name), node.port, node.user, node.password)))
    }

    val cluster = result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperApi.creationRequest(methodName(), nodeCache.map(_.name))))

    try {
      val nodes = result(NodeApi.access().hostname(configurator.hostname).port(configurator.port).list())
      nodes.isEmpty shouldBe false
      nodes.foreach { node =>
        node.services.isEmpty shouldBe false
        withClue(s"${node.services}")(node.services.map(_.clusterNames.size).sum > 0 shouldBe true)
      }
    } finally if (cleanup)
      result(ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port).delete(cluster.name))
  }

  @After
  final def tearDown(): Unit = {
    Releasable.close(configurator)
    Releasable.close(clusterCollie)
  }
}
