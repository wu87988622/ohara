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
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.it.IntegrationTest
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestListCluster extends IntegrationTest with Matchers {

  private[this] val nodeCache: Seq[Node] = CollieTestUtil.nodeCache()

  private[this] val nodeCollie: NodeCollie = new NodeCollie {
    override def nodes(): Future[Seq[Node]] = Future.successful(nodeCache)
    override def node(name: String): Future[Node] = Future.successful(
      nodeCache.find(_.name == name).getOrElse(throw new NoSuchElementException(s"expected:$name actual:$nodeCache")))
  }

  private[this] val clusterCollie: ClusterCollie = ClusterCollie(nodeCollie)

  @Before
  def setup(): Unit = if (nodeCache.isEmpty)
    skipTest(s"${CollieTestUtil.key} don't exist so all tests in ${classOf[TestListCluster].getSimpleName} are ignored")
  else
    nodeCache.foreach { node =>
      val dockerClient =
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
      try {
        withClue(s"failed to find ${ZookeeperCollie.IMAGE_NAME_DEFAULT}")(
          dockerClient.images().contains(ZookeeperCollie.IMAGE_NAME_DEFAULT) shouldBe true)
        withClue(s"failed to find ${BrokerCollie.IMAGE_NAME_DEFAULT}")(
          dockerClient.images().contains(BrokerCollie.IMAGE_NAME_DEFAULT) shouldBe true)
        withClue(s"failed to find ${WorkerCollie.IMAGE_NAME_DEFAULT}")(
          dockerClient.images().contains(WorkerCollie.IMAGE_NAME_DEFAULT) shouldBe true)
      } finally dockerClient.close()
    }

  @Test
  def deadZookeeperClusterShouldDisappear(): Unit = {

    val name = CommonUtil.randomString(10)

    // the port:22 is not illegal so we can't create zookeeper cluster
    try Await.result(clusterCollie
                       .zookeepersCollie()
                       .creator()
                       .clientPort(1000)
                       .nodeNames(nodeCache.map(_.name))
                       .clusterName(name)
                       .create(),
                     30 seconds)
    catch {
      case _: Throwable =>
      // creation is "async" so we can't assume the result...
    }

    nodeCache.foreach { node =>
      val dockerClient =
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
      try dockerClient.containers().count(_.name.contains(name)) shouldBe 1
      finally dockerClient.close()
    }

    CommonUtil.await(() => Await.result(clusterCollie.zookeepersCollie().clusters(), 30 seconds).isEmpty,
                     java.time.Duration.ofSeconds(10))
  }

  @Test
  def deadBrokerClusterShouldDisappear(): Unit = {
    val zkCluster = Await.result(
      clusterCollie
        .zookeepersCollie()
        .creator()
        .clientPort(CommonUtil.availablePort())
        .nodeNames(nodeCache.map(_.name))
        .clusterName(CommonUtil.randomString(10))
        .create(),
      30 seconds
    )

    try {
      val name = CommonUtil.randomString(10)
      try Await.result(
        clusterCollie
          .brokerCollie()
          .creator()
          .clientPort(1000)
          .nodeNames(nodeCache.map(_.name))
          .clusterName(name)
          .zookeeperClusterName(zkCluster.name)
          .create(),
        30 seconds
      )
      catch {
        case _: Throwable =>
        // creation is "async" so we can't assume the result...
      }

      nodeCache.foreach { node =>
        val dockerClient =
          DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
        try dockerClient.containers().count(_.name.contains(name)) shouldBe 1
        finally dockerClient.close()
      }

      CommonUtil.await(() => Await.result(clusterCollie.brokerCollie().clusters(), 30 seconds).isEmpty,
                       java.time.Duration.ofSeconds(10))
    } finally Await.result(clusterCollie.zookeepersCollie().remove(zkCluster.name), 30 seconds)
  }

  @After
  def tearDown(): Unit = Releasable.close(clusterCollie)
}
