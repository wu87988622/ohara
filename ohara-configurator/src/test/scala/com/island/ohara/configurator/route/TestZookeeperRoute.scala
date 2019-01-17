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

package com.island.ohara.configurator.route

import com.island.ohara.client.configurator.v0.NodeApi.NodeCreationRequest
import com.island.ohara.client.configurator.v0.ZookeeperApi.{ZookeeperClusterCreationRequest, ZookeeperClusterInfo}
import com.island.ohara.client.configurator.v0.{NodeApi, ZookeeperApi}
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestZookeeperRoute extends MediumTest with Matchers {
  private[this] val configurator = Configurator.fake()
  private[this] val access = ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def assert(request: ZookeeperClusterCreationRequest, cluster: ZookeeperClusterInfo): Unit = {
    cluster.name shouldBe request.name
    request.imageName.foreach(_ shouldBe cluster.imageName)
    request.clientPort.foreach(_ shouldBe cluster.clientPort)
    request.peerPort.foreach(_ shouldBe cluster.peerPort)
    request.electionPort.foreach(_ shouldBe cluster.electionPort)
    request.nodeNames shouldBe cluster.nodeNames
  }

  private[this] val nodeNames: Seq[String] = Seq("n0", "n1")

  @Before
  def setup(): Unit = {
    val nodeAccess = NodeApi.access().hostname(configurator.hostname).port(configurator.port)

    nodeNames.isEmpty shouldBe false
    nodeNames.foreach { n =>
      Await.result(nodeAccess.add(
                     NodeCreationRequest(
                       name = Some(n),
                       port = 22,
                       user = "user",
                       password = "pwd"
                     )),
                   10 seconds)
    }

    Await.result(nodeAccess.list(), 10 seconds).size shouldBe nodeNames.size
  }

  @Test
  def testEmptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(
      access.add(
        ZookeeperClusterCreationRequest(
          name = methodName(),
          imageName = Some("abcdef"),
          clientPort = Some(123),
          electionPort = Some(456),
          peerPort = Some(1345),
          nodeNames = Seq.empty
        )),
      30 seconds
    )
  }

  @Test
  def testCreateOnNonexistentNode(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(
      access.add(
        ZookeeperClusterCreationRequest(
          name = methodName(),
          imageName = Some("abcdef"),
          clientPort = Some(123),
          electionPort = Some(456),
          peerPort = Some(1345),
          nodeNames = Seq("asdasdasd")
        )),
      30 seconds
    )
  }

  @Test
  def testCreate(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    assert(request, Await.result(access.add(request), 30 seconds))
  }

  @Test
  def testList(): Unit = {
    val request0 = ZookeeperClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    assert(request0, Await.result(access.add(request0), 30 seconds))
    val request1 = ZookeeperClusterCreationRequest(
      name = methodName() + "2",
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    assert(request1, Await.result(access.add(request1), 30 seconds))

    val clusters = Await.result(access.list(), 30 seconds)
    clusters.size shouldBe 2
    assert(request0, clusters.find(_.name == request0.name).get)
    assert(request1, clusters.find(_.name == request1.name).get)
  }

  @Test
  def testRemove(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    val cluster = Await.result(access.add(request), 30 seconds)
    assert(request, cluster)

    Await.result(access.delete(request.name), 30 seconds) shouldBe cluster
  }

  @Test
  def testGetContainers(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    val cluster = Await.result(access.add(request), 30 seconds)
    assert(request, cluster)

    val containers = Await.result(access.get(request.name), 30 seconds)
    containers.size shouldBe request.nodeNames.size

    Await.result(access.delete(request.name), 30 seconds) shouldBe cluster
    Await.result(access.list(), 30 seconds).size shouldBe 0
  }

  @Test
  def testAddNode(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = Seq(nodeNames.head)
    )
    val cluster = Await.result(access.add(request), 30 seconds)
    assert(request, cluster)

    // we don't support to add zk node at runtime
    an[IllegalArgumentException] should be thrownBy Await.result(access.addNode(cluster.name, nodeNames.last),
                                                                 30 seconds)
  }
  @Test
  def testRemoveNode(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    val cluster = Await.result(access.add(request), 30 seconds)
    assert(request, cluster)

    // we don't support to remove zk node at runtime
    an[IllegalArgumentException] should be thrownBy Await.result(access.removeNode(cluster.name, nodeNames.head),
                                                                 30 seconds)
  }

  @Test
  def testInvalidClusterName(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = "abc def",
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    an[IllegalArgumentException] should be thrownBy Await.result(access.add(request), 30 seconds)
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
