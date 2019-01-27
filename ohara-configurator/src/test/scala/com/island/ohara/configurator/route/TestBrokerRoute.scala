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

import com.island.ohara.client.configurator.v0.BrokerApi.{BrokerClusterCreationRequest, BrokerClusterInfo}
import com.island.ohara.client.configurator.v0.NodeApi.NodeCreationRequest
import com.island.ohara.client.configurator.v0.{BrokerApi, NodeApi, ZookeeperApi}
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestBrokerRoute extends MediumTest with Matchers {
  private[this] val numberOfCluster = 1
  private[this] val configurator = Configurator.builder().fake(numberOfCluster, 0).build() /**
    * a fake cluster has 3 fake node.
    */
  private[this] val numberOfDefaultNodes = 3 * numberOfCluster
  private[this] val access = BrokerApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def assert(request: BrokerClusterCreationRequest, cluster: BrokerClusterInfo): Unit = {
    cluster.name shouldBe request.name
    request.imageName.foreach(_ shouldBe cluster.imageName)
    request.clientPort.foreach(_ shouldBe cluster.clientPort)
    request.zookeeperClusterName.foreach(_ shouldBe cluster.zookeeperClusterName)
    request.nodeNames shouldBe cluster.nodeNames
  }

  private[this] val zkClusterName = "zkCluster"

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

    Await.result(nodeAccess.list(), 10 seconds).size shouldBe (nodeNames.size + numberOfDefaultNodes)

    Await
      .result(ZookeeperApi
                .access()
                .hostname(configurator.hostname)
                .port(configurator.port)
                .add(ZookeeperApi.creationRequest(zkClusterName, nodeNames)),
              10 seconds)
      .name shouldBe zkClusterName
  }

  @Test
  def testDefaultZkInMultiZkCluster(): Unit = {
    val anotherZk = methodName()
    Await
      .result(ZookeeperApi
                .access()
                .hostname(configurator.hostname)
                .port(configurator.port)
                .add(ZookeeperApi.creationRequest(anotherZk, nodeNames)),
              10 seconds)
      .name shouldBe anotherZk
    try {
      Await
        .result(ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port).list(), 10 seconds)
        .size shouldBe 2

      // there are two zk cluster so we have to assign the zk cluster...
      an[IllegalArgumentException] should be thrownBy Await.result(
        access.add(
          BrokerApi.creationRequest(
            name = methodName(),
            nodeNames = nodeNames
          )),
        30 seconds
      )
    } finally {
      Await
        .result(ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port).delete(anotherZk),
                10 seconds)
        .name shouldBe anotherZk
    }

  }

  @Test
  def testCreateOnNonexistentNode(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(
      access.add(
        BrokerApi.creationRequest(
          name = methodName(),
          nodeNames = Seq("asdasdasd")
        )),
      30 seconds
    )
  }

  @Test
  def testEmptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(
      access.add(
        BrokerClusterCreationRequest(
          name = methodName(),
          imageName = Some("abcdef"),
          zookeeperClusterName = None,
          clientPort = Some(123),
          nodeNames = Seq.empty
        )),
      30 seconds
    )
  }

  @Test
  def testDefaultZk(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      zookeeperClusterName = Some("Asdasdasd"),
      clientPort = Some(123),
      nodeNames = nodeNames
    )
    an[IllegalArgumentException] should be thrownBy assert(request, Await.result(access.add(request), 30 seconds))
    val anotherRequest = request.copy(zookeeperClusterName = None)
    assert(anotherRequest, Await.result(access.add(anotherRequest), 30 seconds))
  }

  @Test
  def testCreate(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      zookeeperClusterName = Some(zkClusterName),
      clientPort = Some(123),
      nodeNames = nodeNames
    )
    assert(request, Await.result(access.add(request), 30 seconds))
  }

  @Test
  def testList(): Unit = {
    val request0 = BrokerClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    assert(request0, Await.result(access.add(request0), 30 seconds))
    val request1 = BrokerClusterCreationRequest(
      name = methodName() + "2",
      imageName = Some("abcdef"),
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    assert(request1, Await.result(access.add(request1), 30 seconds))

    val clusters = Await.result(access.list(), 30 seconds)
    // In fake mode, we pre-create a fake cluster
    clusters.size shouldBe 3
    assert(request0, clusters.find(_.name == request0.name).get)
    assert(request1, clusters.find(_.name == request1.name).get)
  }

  @Test
  def testRemove(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    val cluster = Await.result(access.add(request), 30 seconds)
    assert(request, cluster)

    Await.result(access.delete(request.name), 30 seconds) shouldBe cluster
  }

  @Test
  def testGetContainers(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    val cluster = Await.result(access.add(request), 30 seconds)
    assert(request, cluster)

    val containers = Await.result(access.get(request.name), 30 seconds)
    containers.size shouldBe request.nodeNames.size

    Await.result(access.delete(request.name), 30 seconds) shouldBe cluster
    // in fake mode we should have single cluster
    Await.result(access.list(), 30 seconds).size shouldBe 1
  }

  @Test
  def testAddNode(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = Seq(nodeNames.head)
    )
    val cluster = Await.result(access.add(request), 30 seconds)
    assert(request, cluster)

    Await.result(access.addNode(cluster.name, nodeNames.last), 30 seconds) shouldBe cluster.copy(
      nodeNames = cluster.nodeNames :+ nodeNames.last)
  }
  @Test
  def testRemoveNode(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    val cluster = Await.result(access.add(request), 30 seconds)
    assert(request, cluster)

    Await.result(access.removeNode(cluster.name, nodeNames.last), 30 seconds) shouldBe cluster.copy(
      nodeNames = cluster.nodeNames.filter(_ != nodeNames.last))
  }

  @Test
  def testInvalidClusterName(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = "--1",
      imageName = Some("abcdef"),
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    an[IllegalArgumentException] should be thrownBy Await.result(access.add(request), 30 seconds)
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
