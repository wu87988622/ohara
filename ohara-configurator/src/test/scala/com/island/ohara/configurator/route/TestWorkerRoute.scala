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

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterCreationRequest
import com.island.ohara.client.configurator.v0.NodeApi.NodeCreationRequest
import com.island.ohara.client.configurator.v0.WorkerApi.{WorkerClusterCreationRequest, WorkerClusterInfo}
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterCreationRequest
import com.island.ohara.client.configurator.v0.{BrokerApi, NodeApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestWorkerRoute extends MediumTest with Matchers {
  private[this] val numberOfCluster = 1
  private[this] val configurator = Configurator.builder().fake(numberOfCluster, 0).build() /**
    * a fake cluster has 3 fake node.
    */
  private[this] val numberOfDefaultNodes = 3 * numberOfCluster
  private[this] val access = WorkerApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def assert(request: WorkerClusterCreationRequest, cluster: WorkerClusterInfo): Unit = {
    cluster.name shouldBe request.name
    request.imageName.foreach(_ shouldBe cluster.imageName)
    request.clientPort.foreach(_ shouldBe cluster.clientPort)
    request.brokerClusterName.foreach(_ shouldBe cluster.brokerClusterName)
    request.nodeNames shouldBe cluster.nodeNames
  }

  private[this] val bkClusterName = Await
    .result(BrokerApi.access().hostname(configurator.hostname).port(configurator.port).list(), 10 seconds)
    .head
    .name

  private[this] val nodeNames: Seq[String] = Seq("n0", "n1")

  private[this] def creationRequest(name: String,
                                    brokerClusterName: String,
                                    nodeNames: Seq[String]): WorkerClusterCreationRequest =
    WorkerClusterCreationRequest(
      name = name,
      imageName = None,
      brokerClusterName = Some(brokerClusterName),
      clientPort = None,
      groupId = None,
      statusTopicName = None,
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = None,
      configTopicReplications = None,
      offsetTopicName = None,
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )
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
  }

  @Test
  def testDefaultBk(): Unit = {
    val request = creationRequest(
      name = CommonUtil.randomString(10),
      brokerClusterName = "Asdasdasd",
      nodeNames = nodeNames
    )
    an[IllegalArgumentException] should be thrownBy assert(request, result(access.add(request)))
    val anotherRequest = request.copy(brokerClusterName = None)
    assert(anotherRequest, result(access.add(anotherRequest)))
  }

  @Test
  def testAllSetting(): Unit = {
    val request = WorkerClusterCreationRequest(
      name = CommonUtil.randomString(10),
      imageName = Some(CommonUtil.randomString(10)),
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(123),
      groupId = Some(CommonUtil.randomString(10)),
      configTopicName = Some(CommonUtil.randomString(10)),
      configTopicReplications = Some(2.asInstanceOf[Short]),
      offsetTopicName = Some(CommonUtil.randomString(10)),
      offsetTopicPartitions = Some(123),
      offsetTopicReplications = Some(2.asInstanceOf[Short]),
      statusTopicName = Some(CommonUtil.randomString(10)),
      statusTopicPartitions = Some(123),
      statusTopicReplications = Some(2.asInstanceOf[Short]),
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    val wkCluster = result(access.add(request))
    wkCluster.clientPort shouldBe request.clientPort.get
    wkCluster.groupId shouldBe request.groupId.get
    wkCluster.configTopicName shouldBe request.configTopicName.get
    wkCluster.configTopicReplications shouldBe request.configTopicReplications.get
    wkCluster.offsetTopicName shouldBe request.offsetTopicName.get
    wkCluster.offsetTopicPartitions shouldBe request.offsetTopicPartitions.get
    wkCluster.offsetTopicReplications shouldBe request.offsetTopicReplications.get
    wkCluster.statusTopicName shouldBe request.statusTopicName.get
    wkCluster.statusTopicPartitions shouldBe request.statusTopicPartitions.get
    wkCluster.statusTopicReplications shouldBe request.statusTopicReplications.get
  }

  @Test
  def testDefaultBrokerInMultiBrokerCluster(): Unit = {
    val zkClusterName = CommonUtil.randomString(10)
    result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperClusterCreationRequest(
          name = zkClusterName,
          imageName = None,
          clientPort = Some(CommonUtil.availablePort()),
          electionPort = Some(CommonUtil.availablePort()),
          peerPort = Some(CommonUtil.availablePort()),
          nodeNames = nodeNames
        ))).name shouldBe zkClusterName
    val anotherBk = CommonUtil.randomString(10)
    result(
      BrokerApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(BrokerClusterCreationRequest(
          name = anotherBk,
          imageName = None,
          zookeeperClusterName = Some(zkClusterName),
          clientPort = Some(CommonUtil.availablePort()),
          exporterPort = Some(CommonUtil.availablePort()),
          nodeNames = nodeNames
        ))).name shouldBe anotherBk
    try {
      result(BrokerApi.access().hostname(configurator.hostname).port(configurator.port).list()).size shouldBe 2

      // there are two bk cluster so we have to assign the bk cluster...
      an[IllegalArgumentException] should be thrownBy result(
        access.add(
          WorkerApi.creationRequest(
            name = CommonUtil.randomString(10),
            nodeNames = nodeNames
          ))
      )
    } finally result(BrokerApi.access().hostname(configurator.hostname).port(configurator.port).delete(anotherBk)).name shouldBe anotherBk
  }

  @Test
  def testCreateOnNonexistentNode(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      access.add(
        WorkerApi.creationRequest(
          name = CommonUtil.randomString(10),
          nodeNames = Seq("asdasdasd")
        ))
    )
  }

  @Test
  def testEmptyNodes(): Unit = {
    creationRequest(
      name = CommonUtil.randomString(10),
      brokerClusterName = bkClusterName,
      nodeNames = Seq.empty
    )
    an[IllegalArgumentException] should be thrownBy result(
      access.add(
        creationRequest(
          name = CommonUtil.randomString(10),
          brokerClusterName = bkClusterName,
          nodeNames = Seq.empty
        ))
    )
  }
  @Test
  def testCreate(): Unit = {
    val request = creationRequest(
      name = CommonUtil.randomString(10),
      brokerClusterName = bkClusterName,
      nodeNames = nodeNames
    )
    assert(request, result(access.add(request)))
  }

  @Test
  def testList(): Unit = {
    val request0 = creationRequest(
      name = CommonUtil.randomString(10),
      brokerClusterName = bkClusterName,
      nodeNames = nodeNames
    ).copy(clientPort = Some(CommonUtil.availablePort()))

    assert(request0, result(access.add(request0)))
    val request1 = creationRequest(
      name = CommonUtil.randomString(10),
      brokerClusterName = bkClusterName,
      nodeNames = nodeNames
    ).copy(clientPort = Some(CommonUtil.availablePort()))
    assert(request1, result(access.add(request1)))

    val clusters = result(access.list())
    clusters.size shouldBe 2
    assert(request0, clusters.find(_.name == request0.name).get)
    assert(request1, clusters.find(_.name == request1.name).get)
  }

  @Test
  def testRemove(): Unit = {
    val request = creationRequest(
      name = CommonUtil.randomString(10),
      brokerClusterName = bkClusterName,
      nodeNames = nodeNames
    )
    val cluster = result(access.add(request))
    assert(request, cluster)

    result(access.delete(request.name)) shouldBe cluster
  }

  @Test
  def testGetContainers(): Unit = {
    val request = creationRequest(
      name = CommonUtil.randomString(10),
      brokerClusterName = bkClusterName,
      nodeNames = nodeNames
    )
    val cluster = result(access.add(request))
    assert(request, cluster)

    val containers = result(access.get(request.name))
    containers.size shouldBe request.nodeNames.size

    result(access.delete(request.name)) shouldBe cluster
    result(access.list()).size shouldBe 0
  }

  @Test
  def testAddNode(): Unit = {
    val request = creationRequest(
      name = CommonUtil.randomString(10),
      brokerClusterName = bkClusterName,
      nodeNames = Seq(nodeNames.head)
    )
    val cluster = result(access.add(request))
    assert(request, cluster)

    result(access.addNode(cluster.name, nodeNames.last)) shouldBe
      WorkerClusterInfo(
        name = cluster.name,
        imageName = cluster.imageName,
        brokerClusterName = cluster.brokerClusterName,
        clientPort = cluster.clientPort,
        groupId = cluster.groupId,
        statusTopicName = cluster.statusTopicName,
        statusTopicPartitions = cluster.statusTopicPartitions,
        statusTopicReplications = cluster.statusTopicReplications,
        configTopicName = cluster.configTopicName,
        configTopicPartitions = cluster.configTopicPartitions,
        configTopicReplications = cluster.configTopicReplications,
        offsetTopicName = cluster.offsetTopicName,
        offsetTopicPartitions = cluster.offsetTopicPartitions,
        offsetTopicReplications = cluster.offsetTopicReplications,
        jarNames = cluster.jarNames,
        sources = Seq.empty,
        sinks = Seq.empty,
        nodeNames = cluster.nodeNames :+ nodeNames.last
      )
  }
  @Test
  def testRemoveNode(): Unit = {
    val request = creationRequest(
      name = CommonUtil.randomString(10),
      brokerClusterName = bkClusterName,
      nodeNames = nodeNames
    )
    val cluster = result(access.add(request))
    assert(request, cluster)

    result(access.removeNode(cluster.name, nodeNames.last)) shouldBe WorkerClusterInfo(
      name = cluster.name,
      imageName = cluster.imageName,
      brokerClusterName = cluster.brokerClusterName,
      clientPort = cluster.clientPort,
      groupId = cluster.groupId,
      statusTopicName = cluster.statusTopicName,
      statusTopicPartitions = cluster.statusTopicPartitions,
      statusTopicReplications = cluster.statusTopicReplications,
      configTopicName = cluster.configTopicName,
      configTopicPartitions = cluster.configTopicPartitions,
      configTopicReplications = cluster.configTopicReplications,
      offsetTopicName = cluster.offsetTopicName,
      offsetTopicPartitions = cluster.offsetTopicPartitions,
      offsetTopicReplications = cluster.offsetTopicReplications,
      jarNames = cluster.jarNames,
      sources = Seq.empty,
      sinks = Seq.empty,
      nodeNames = cluster.nodeNames.filter(_ != nodeNames.last)
    )
  }

  @Test
  def testInvalidClusterName(): Unit = {
    val request = WorkerClusterCreationRequest(
      name = "123123.",
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = None,
      groupId = None,
      configTopicName = None,
      configTopicReplications = None,
      offsetTopicName = None,
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      statusTopicName = None,
      statusTopicPartitions = None,
      statusTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )
    an[IllegalArgumentException] should be thrownBy result(access.add(request))
  }

  @Test
  def createWkClusterWithSameName(): Unit = {
    val request = WorkerApi.creationRequest(
      name = CommonUtil.randomString(10),
      nodeNames = nodeNames
    )

    // pass
    result(access.add(request))

    // we don't need to create another bk cluster since it is feasible to create multi wk cluster on same broker cluster
    an[IllegalArgumentException] should be thrownBy result(access.add(request))
  }

  @Test
  def clientPortConflict(): Unit = {
    val clientPort = CommonUtil.availablePort()
    val request = creationRequest(
      name = CommonUtil.randomString(10),
      brokerClusterName = bkClusterName,
      nodeNames = nodeNames
    ).copy(clientPort = Some(clientPort))

    // pass
    result(access.add(request))

    an[IllegalArgumentException] should be thrownBy result(
      access.add(request.copy(name = CommonUtil.randomString(10), clientPort = Some(clientPort))))

    // pass
    result(access.add(request.copy(name = CommonUtil.randomString(10), clientPort = Some(CommonUtil.availablePort()))))
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
