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
import com.island.ohara.agent.docker.{ContainerState, DockerClient}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.{BrokerApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.it.IntegrationTest
import com.island.ohara.it.agent.{ClusterNameHolder, CollieTestUtils}
import com.typesafe.scalalogging.Logger
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestListCluster extends IntegrationTest with Matchers {
  private[this] val log = Logger(classOf[TestListCluster])
  private[this] val nodeCache: Seq[Node] = CollieTestUtils.nodeCache()
  private[this] val nameHolder = new ClusterNameHolder(nodeCache)

  private[this] val nodeCollie: NodeCollie = NodeCollie(nodeCache)

  private[this] val clusterCollie: ClusterCollie =
    ClusterCollie.builderOfSsh().nodeCollie(nodeCollie).build()

  private[this] val cleanup: Boolean = true

  @Before
  def setup(): Unit = if (nodeCache.isEmpty)
    skipTest(
      s"${CollieTestUtils.key} don't exist so all tests in ${classOf[TestListCluster].getSimpleName} are ignored")
  else
    nodeCache.foreach { node =>
      val dockerClient =
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
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
  def deadZookeeperClusterShouldNotDisappear(): Unit = {

    val name = nameHolder.generateClusterName()
    log.info(s"[TestListCluster] before create zk cluster:$name")
    try result(
      clusterCollie
        .zookeeperCollie()
        .creator()
        .imageName(ZookeeperApi.IMAGE_NAME_DEFAULT)
        // the port:1000 is not illegal so we can't create zookeeper cluster
        .clientPort(1000)
        .peerPort(CommonUtils.availablePort())
        .electionPort(CommonUtils.availablePort())
        .nodeNames(nodeCache.map(_.name).toSet)
        .clusterName(name)
        .create()
    )
    catch {
      case _: Throwable =>
      // creation is "async" so we can't assume the result...
    }

    log.info("[TestListCluster] before check zk containers")
    nodeCache.foreach { node =>
      val dockerClient =
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
      try await(() => result(dockerClient.activeContainers(_.contains(name))).isEmpty)
      finally dockerClient.close()
    }

    log.info("[TestListCluster] before check zk clusters still can be fetch")
    await(() => result(clusterCollie.zookeeperCollie().clusters).exists(_._1.name == name))
  }

  @Test
  def deadBrokerClusterShouldNotDisappear(): Unit = {
    log.info("[TestListCluster] before create zk cluster")
    val zkCluster = result(
      clusterCollie
        .zookeeperCollie()
        .creator()
        .imageName(ZookeeperApi.IMAGE_NAME_DEFAULT)
        .clientPort(CommonUtils.availablePort())
        .peerPort(CommonUtils.availablePort())
        .electionPort(CommonUtils.availablePort())
        .nodeNames(nodeCache.map(_.name).toSet)
        .clusterName(nameHolder.generateClusterName())
        .create()
    )

    log.info("[TestListCluster] before create bk cluster")
    try {
      assertCluster(() => result(clusterCollie.zookeeperCollie().clusters).keys.toSeq, zkCluster.name)
      // since we only get "active" containers, all containers belong to the cluster should be running.
      // Currently, both k8s and pure docker have the same context of "RUNNING".
      // It is ok to filter container via RUNNING state.
      await(() => {
        val containers = result(clusterCollie.zookeeperCollie().containers(zkCluster.name))
        containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
      })
      val name = nameHolder.generateClusterName()
      try result(
        clusterCollie
          .brokerCollie()
          .creator()
          .imageName(BrokerApi.IMAGE_NAME_DEFAULT)
          // the port:1000 is not illegal so we can't create broker cluster
          .clientPort(1000)
          .exporterPort(CommonUtils.availablePort())
          .nodeNames(nodeCache.map(_.name).toSet)
          .clusterName(name)
          .zookeeperClusterName(zkCluster.name)
          .create()
      )
      catch {
        case _: Throwable =>
        // creation is "async" so we can't assume the result...
      }

      log.info("[TestListCluster] before check bk containers")
      nodeCache.foreach { node =>
        val dockerClient =
          DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
        try await(() => result(dockerClient.activeContainers(_.contains(name))).isEmpty)
        finally dockerClient.close()
      }

      log.info("[TestListCluster] before check bk clusters still can be fetch")
      await(() => result(clusterCollie.brokerCollie().clusters).exists(_._1.name == name))
    } finally if (cleanup) result(clusterCollie.zookeeperCollie().remove(zkCluster.name))
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(clusterCollie)
    if (cleanup) Releasable.close(nameHolder)
  }
}
