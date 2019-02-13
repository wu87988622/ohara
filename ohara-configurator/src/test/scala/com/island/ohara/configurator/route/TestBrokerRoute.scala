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
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
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

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Before
  def setup(): Unit = {
    val nodeAccess = NodeApi.access().hostname(configurator.hostname).port(configurator.port)

    nodeNames.isEmpty shouldBe false

    nodeNames.foreach { n =>
      result(
        nodeAccess.add(
          NodeCreationRequest(
            name = Some(n),
            port = 22,
            user = "user",
            password = "pwd"
          )))
    }

    result(nodeAccess.list()).size shouldBe (nodeNames.size + numberOfDefaultNodes)

    result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperApi.creationRequest(zkClusterName, nodeNames))).name shouldBe zkClusterName
  }

  @Test
  def testDefaultZkInMultiZkCluster(): Unit = {
    val anotherZk = CommonUtil.randomString(10)
    result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperApi.creationRequest(anotherZk, nodeNames))).name shouldBe anotherZk
    try {
      result(ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port).list()).size shouldBe 2

      // there are two zk cluster so we have to assign the zk cluster...
      an[IllegalArgumentException] should be thrownBy result(
        access.add(
          BrokerApi.creationRequest(
            name = CommonUtil.randomString(10),
            nodeNames = nodeNames
          ))
      )
    } finally {
      result(ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port).delete(anotherZk)).name shouldBe anotherZk
    }

  }

  @Test
  def testCreateOnNonexistentNode(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      access.add(
        BrokerApi.creationRequest(
          name = CommonUtil.randomString(10),
          nodeNames = Seq("asdasdasd")
        ))
    )
  }

  @Test
  def testEmptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      access.add(
        BrokerClusterCreationRequest(
          name = CommonUtil.randomString(10),
          imageName = Some("abcdef"),
          zookeeperClusterName = None,
          exporterPort = None,
          clientPort = Some(123),
          nodeNames = Seq.empty
        ))
    )
  }

  @Test
  def testDefaultZk(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some("abcdef"),
      zookeeperClusterName = Some("Asdasdasd"),
      exporterPort = None,
      clientPort = Some(123),
      nodeNames = nodeNames
    )
    an[IllegalArgumentException] should be thrownBy assert(request, result(access.add(request)))
    val anotherRequest = request.copy(zookeeperClusterName = None)
    assert(anotherRequest, result(access.add(anotherRequest)))
  }

  @Test
  def testCreate(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some("abcdef"),
      zookeeperClusterName = Some(zkClusterName),
      exporterPort = None,
      clientPort = Some(123),
      nodeNames = nodeNames
    )
    assert(request, result(access.add(request)))
  }

  @Test
  def testList(): Unit = {
    val request0 = BrokerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some("abcdef"),
      exporterPort = None,
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    assert(request0, result(access.add(request0)))

    val zk2 = result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperApi.creationRequest(CommonUtil.randomString(10), nodeNames)))

    val request1 = BrokerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some("abcdef"),
      exporterPort = None,
      clientPort = Some(123),
      zookeeperClusterName = Some(zk2.name),
      nodeNames = nodeNames
    )
    assert(request1, result(access.add(request1)))

    val clusters = result(access.list())
    // In fake mode, we pre-create a fake cluster
    clusters.size shouldBe 3
    assert(request0, clusters.find(_.name == request0.name).get)
    assert(request1, clusters.find(_.name == request1.name).get)
  }

  @Test
  def testRemove(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      exporterPort = None,
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    val cluster = result(access.add(request))
    assert(request, cluster)

    result(access.delete(request.name)) shouldBe cluster
  }

  @Test
  def testGetContainers(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      exporterPort = None,
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    val cluster = result(access.add(request))
    assert(request, cluster)

    val containers = result(access.get(request.name))
    containers.size shouldBe request.nodeNames.size

    result(access.delete(request.name)) shouldBe cluster
    // in fake mode we should have single cluster
    result(access.list()).size shouldBe 1
  }

  @Test
  def testAddNode(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      exporterPort = None,
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = Seq(nodeNames.head)
    )
    val cluster = result(access.add(request))
    assert(request, cluster)

    result(access.addNode(cluster.name, nodeNames.last)) shouldBe BrokerClusterInfo(
      name = cluster.name,
      imageName = cluster.imageName,
      clientPort = cluster.clientPort,
      exporterPort = cluster.exporterPort,
      zookeeperClusterName = cluster.zookeeperClusterName,
      nodeNames = cluster.nodeNames :+ nodeNames.last
    )
  }
  @Test
  def testRemoveNode(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      exporterPort = None,
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    val cluster = result(access.add(request))
    assert(request, cluster)

    result(access.removeNode(cluster.name, nodeNames.last)) shouldBe BrokerClusterInfo(
      name = cluster.name,
      imageName = cluster.imageName,
      clientPort = cluster.clientPort,
      exporterPort = cluster.exporterPort,
      zookeeperClusterName = cluster.zookeeperClusterName,
      nodeNames = cluster.nodeNames.filter(_ != nodeNames.last)
    )
  }

  @Test
  def testInvalidClusterName(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = "--1",
      imageName = Some("abcdef"),
      clientPort = Some(123),
      exporterPort = None,
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    an[IllegalArgumentException] should be thrownBy result(access.add(request))
  }

  @Test
  def runMultiBkClustersOnSameZkCluster(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some("abcdef"),
      exporterPort = None,
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )

    // pass
    result(access.add(request))

    // we can't create multi broker clusters on same zk cluster
    an[IllegalArgumentException] should be thrownBy result(access.add(request.copy(name = CommonUtil.randomString(10))))
  }

  @Test
  def createBkClusterWithSameName(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some("abcdef"),
      exporterPort = None,
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )

    // pass
    result(access.add(request))

    val zk2 = result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperApi.creationRequest(CommonUtil.randomString(10), nodeNames)))

    an[IllegalArgumentException] should be thrownBy result(
      access.add(
        request.copy(
          zookeeperClusterName = Some(zk2.name)
        )))
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
