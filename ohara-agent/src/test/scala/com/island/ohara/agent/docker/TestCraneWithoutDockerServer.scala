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

package com.island.ohara.agent.docker

import java.util.concurrent.{ExecutorService, Executors}

import com.island.ohara.agent.ssh.DockerClientCache
import com.island.ohara.agent.wharf.Warehouse
import com.island.ohara.agent.{Crane, NoSuchClusterException, NodeCollie}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestCraneWithoutDockerServer extends SmallTest with Matchers {

  private[this] val SIZE: Int = 10
  private[this] val WAREHOUSE_NAME = CommonUtils.randomString(Warehouse.LIMIT_OF_NAME_LENGTH)
  private[this] val nodeCache: Seq[Node] = (1 to SIZE).map { i =>
    Node(
      name = s"fake_$i",
      port = 22,
      user = "fake",
      password = "fake"
    )
  }
  private[this] val nodeCollie: NodeCollie = NodeCollie(nodeCache)
  private[this] val dockerCache: DockerClientCache = DockerClientCache.fake()
  private[this] val executor: ExecutorService = Executors.newSingleThreadExecutor()

  private[this] val crane: DockerCraneImpl = new DockerCraneImpl(nodeCollie, dockerCache, executor)

  private[this] def awaitResult[T](f: Future[T]): T = Await.result(f, 20 seconds)

  @Test
  def normalCase(): Unit = {

    awaitResult(crane.list).size shouldBe 0

    an[NoSuchClusterException] should be thrownBy awaitResult(crane.get(WAREHOUSE_NAME))

    awaitResult(
      crane
        .streamWarehouse()
        .creator()
        .clusterName(WAREHOUSE_NAME)
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .instances(SIZE)
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Seq("topic1"))
        .toTopics(Seq("topic2"))
        .create())

    awaitResult(crane.list).size shouldBe 1

    CommonUtils.await(
      () => {
        val info = awaitResult(crane.get(WAREHOUSE_NAME))
        info._1.name == WAREHOUSE_NAME && info._2.size == SIZE
      },
      java.time.Duration.ofSeconds(20)
    )

    awaitResult(crane.remove(WAREHOUSE_NAME))

    awaitResult(crane.list).size shouldBe 0
  }

  @Test
  def testDifferentInstance(): Unit = {
    // node size should be bigger than required instance
    an[IllegalArgumentException] should be thrownBy awaitResult(
      crane
        .streamWarehouse()
        .creator()
        .clusterName(CommonUtils.randomString(Warehouse.LIMIT_OF_NAME_LENGTH))
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .instances(SIZE + 1)
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Seq("topic1"))
        .toTopics(Seq("topic2"))
        .create())

    // instance should be positive
    an[IllegalArgumentException] should be thrownBy awaitResult(
      crane
        .streamWarehouse()
        .creator()
        .clusterName(CommonUtils.randomString(Warehouse.LIMIT_OF_NAME_LENGTH))
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .instances(0)
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Seq("topic1"))
        .toTopics(Seq("topic2"))
        .create())

    // nodeNames will override the effect of instances
    val res = awaitResult(
      crane
        .streamWarehouse()
        .creator()
        .clusterName(CommonUtils.randomString(Warehouse.LIMIT_OF_NAME_LENGTH))
        .imageName(CommonUtils.randomString())
        .jarUrl("jar")
        .instances(SIZE + 1)
        .nodeNames(nodeCache.take(3).map(_.name))
        .appId("app")
        .brokerProps("broker")
        .fromTopics(Seq("topic1"))
        .toTopics(Seq("topic2"))
        .create())

    res.nodeNames.size shouldBe 3
  }

  @Test
  def nullNodeCollie(): Unit = {
    an[NullPointerException] should be thrownBy Crane.builderOfDocker().nodeCollie(null)
  }

  @Test
  def nullDockerClientCache(): Unit = {
    an[NullPointerException] should be thrownBy Crane.builderOfDocker().dockerClientCache(null)
  }

  @Test
  def nullExecutor(): Unit = {
    an[NullPointerException] should be thrownBy Crane.builderOfDocker().executor(null)
  }
}
