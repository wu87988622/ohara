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
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.fake.FakeWorkerCollie
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestWorkerRoute extends MediumTest with Matchers {
  private[this] val numberOfCluster = 1
  private[this] val configurator = Configurator.builder().fake(numberOfCluster, 0).build()

  /**
    * a fake cluster has 3 fake node.
    */
  private[this] val numberOfDefaultNodes = 3 * numberOfCluster
  private[this] val workerApi = WorkerApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def assert(request: WorkerClusterCreationRequest, cluster: WorkerClusterInfo): Unit = {
    cluster.name shouldBe request.name
    request.imageName.foreach(_ shouldBe cluster.imageName)
    request.clientPort.foreach(_ shouldBe cluster.clientPort)
    request.brokerClusterName.foreach(_ shouldBe cluster.brokerClusterName)
    request.nodeNames shouldBe cluster.nodeNames
  }

  private[this] val bkClusterName =
    Await.result(BrokerApi.access().hostname(configurator.hostname).port(configurator.port).list, 10 seconds).head.name

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

    result(nodeAccess.list).size shouldBe (nodeNames.size + numberOfDefaultNodes)
  }

  @Test
  def testDefaultBk(): Unit = {
    val request0 = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = None,
      clientPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      jmxPort = Some(CommonUtils.availablePort()),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )
    assert(request0, result(workerApi.add(request0)))

    val request1 = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )
    assert(request1, result(workerApi.add(request1)))
  }

  @Test
  def runOnIncorrectBk(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      workerApi.add(WorkerClusterCreationRequest(
        name = CommonUtils.randomString(10),
        imageName = None,
        brokerClusterName = Some(CommonUtils.randomString(10)),
        clientPort = Some(CommonUtils.availablePort()),
        jmxPort = Some(CommonUtils.availablePort()),
        groupId = Some(CommonUtils.randomString(10)),
        statusTopicName = Some(CommonUtils.randomString(10)),
        statusTopicPartitions = None,
        statusTopicReplications = None,
        configTopicName = Some(CommonUtils.randomString(10)),
        configTopicReplications = None,
        offsetTopicName = Some(CommonUtils.randomString(10)),
        offsetTopicPartitions = None,
        offsetTopicReplications = None,
        jars = Seq.empty,
        nodeNames = nodeNames
      )))
  }

  @Test
  def testAllSetting(): Unit = {
    val request = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = Some(2.asInstanceOf[Short]),
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = Some(123),
      offsetTopicReplications = Some(2.asInstanceOf[Short]),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = Some(123),
      statusTopicReplications = Some(2.asInstanceOf[Short]),
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    val wkCluster = result(workerApi.add(request))
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
    val zkClusterName = CommonUtils.randomString(10)
    result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperClusterCreationRequest(
          name = zkClusterName,
          imageName = None,
          clientPort = Some(CommonUtils.availablePort()),
          electionPort = Some(CommonUtils.availablePort()),
          peerPort = Some(CommonUtils.availablePort()),
          nodeNames = nodeNames
        ))).name shouldBe zkClusterName
    val anotherBk = CommonUtils.randomString(10)
    result(
      BrokerApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(BrokerClusterCreationRequest(
          name = anotherBk,
          imageName = None,
          zookeeperClusterName = Some(zkClusterName),
          clientPort = Some(CommonUtils.availablePort()),
          exporterPort = Some(CommonUtils.availablePort()),
          jmxPort = Some(CommonUtils.availablePort()),
          nodeNames = nodeNames
        ))).name shouldBe anotherBk
    try {
      result(BrokerApi.access().hostname(configurator.hostname).port(configurator.port).list).size shouldBe 2

      // there are two bk cluster so we have to assign the bk cluster...
      an[IllegalArgumentException] should be thrownBy result(
        workerApi.add(
          WorkerApi.creationRequest(
            name = CommonUtils.randomString(10),
            nodeNames = nodeNames
          ))
      )
    } finally result(BrokerApi.access().hostname(configurator.hostname).port(configurator.port).delete(anotherBk)).name shouldBe anotherBk
  }

  @Test
  def testCreateOnNonexistentNode(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      workerApi.add(
        WorkerClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          brokerClusterName = Some(bkClusterName),
          clientPort = Some(CommonUtils.availablePort()),
          jmxPort = Some(CommonUtils.availablePort()),
          groupId = Some(CommonUtils.randomString(10)),
          statusTopicName = Some(CommonUtils.randomString(10)),
          statusTopicPartitions = None,
          statusTopicReplications = None,
          configTopicName = Some(CommonUtils.randomString(10)),
          configTopicReplications = None,
          offsetTopicName = Some(CommonUtils.randomString(10)),
          offsetTopicPartitions = None,
          offsetTopicReplications = None,
          jars = Seq.empty,
          nodeNames = Seq(CommonUtils.randomString(10))
        ))
    )
  }

  @Test
  def testEmptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      workerApi.add(WorkerClusterCreationRequest(
        name = CommonUtils.randomString(10),
        imageName = None,
        brokerClusterName = Some(bkClusterName),
        clientPort = Some(CommonUtils.availablePort()),
        jmxPort = Some(CommonUtils.availablePort()),
        groupId = Some(CommonUtils.randomString(10)),
        statusTopicName = Some(CommonUtils.randomString(10)),
        statusTopicPartitions = None,
        statusTopicReplications = None,
        configTopicName = Some(CommonUtils.randomString(10)),
        configTopicReplications = None,
        offsetTopicName = Some(CommonUtils.randomString(10)),
        offsetTopicPartitions = None,
        offsetTopicReplications = None,
        jars = Seq.empty,
        nodeNames = Seq.empty
      )))
  }

  @Test
  def testImageName(): Unit = {
    def request() = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    // pass by default image
    result(workerApi.add(request()))

    // pass by latest image (since it is default image)
    result(workerApi.add(request().copy(imageName = Some(WorkerApi.IMAGE_NAME_DEFAULT))))

    an[IllegalArgumentException] should be thrownBy result(
      workerApi.add(request().copy(imageName = Some(CommonUtils.randomString()))))
  }

  @Test
  def testCreate(): Unit = {
    val request = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )
    assert(request, result(workerApi.add(request)))
  }

  @Test
  def testList(): Unit = {
    val request0 = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    assert(request0, result(workerApi.add(request0)))

    val request1 = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )
    assert(request1, result(workerApi.add(request1)))

    val clusters = result(workerApi.list)
    clusters.size shouldBe 2
    assert(request0, clusters.find(_.name == request0.name).get)
    assert(request1, clusters.find(_.name == request1.name).get)
  }

  @Test
  def testRemove(): Unit = {
    val request = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )
    val cluster = result(workerApi.add(request))
    assert(request, cluster)

    result(workerApi.delete(request.name)) shouldBe cluster
  }

  @Test
  def testGetContainers(): Unit = {
    val request = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )
    val cluster = result(workerApi.add(request))
    assert(request, cluster)

    assert(request, result(workerApi.get(request.name)))

    result(workerApi.delete(request.name)) shouldBe cluster
    result(workerApi.list).size shouldBe 0
  }

  @Test
  def testAddNode(): Unit = {
    val request = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = Seq(nodeNames.head)
    )
    val cluster = result(workerApi.add(request))
    assert(request, cluster)

    result(workerApi.addNode(cluster.name, nodeNames.last)) shouldBe
      WorkerClusterInfo(
        name = cluster.name,
        imageName = cluster.imageName,
        brokerClusterName = cluster.brokerClusterName,
        clientPort = cluster.clientPort,
        jmxPort = cluster.jmxPort,
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
        connectors = Seq.empty,
        nodeNames = cluster.nodeNames :+ nodeNames.last
      )
  }
  @Test
  def testRemoveNode(): Unit = {
    val request = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )
    val cluster = result(workerApi.add(request))
    assert(request, cluster)

    result(workerApi.removeNode(cluster.name, nodeNames.last)) shouldBe WorkerClusterInfo(
      name = cluster.name,
      imageName = cluster.imageName,
      brokerClusterName = cluster.brokerClusterName,
      clientPort = cluster.clientPort,
      jmxPort = cluster.jmxPort,
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
      connectors = Seq.empty,
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
      jmxPort = None,
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
    an[IllegalArgumentException] should be thrownBy result(workerApi.add(request))
  }

  @Test
  def createWkClusterWithSameName(): Unit = {
    val name = CommonUtils.randomString(10)
    def request() = WorkerClusterCreationRequest(
      name = name,
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    // pass
    result(workerApi.add(request()))

    // we don't need to create another bk cluster since it is feasible to create multi wk cluster on same broker cluster
    an[IllegalArgumentException] should be thrownBy result(workerApi.add(request()))
  }

  @Test
  def clientPortConflict(): Unit = {
    val clientPort = CommonUtils.availablePort()

    def createReq() = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(clientPort),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    // pass
    result(workerApi.add(createReq()))

    an[IllegalArgumentException] should be thrownBy result(workerApi.add(createReq()))

    // pass by different port
    result(workerApi.add(createReq().copy(clientPort = Some(CommonUtils.availablePort()))))
  }

  @Test
  def jmxPortConflict(): Unit = {
    val jmxPort = CommonUtils.availablePort()

    def createReq() = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(jmxPort),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    // pass
    result(workerApi.add(createReq()))

    an[IllegalArgumentException] should be thrownBy result(workerApi.add(createReq()))

    // pass by different port
    result(workerApi.add(createReq().copy(jmxPort = Some(CommonUtils.availablePort()))))
  }

  @Test
  def duplicateGroupId(): Unit = {
    val groupId = CommonUtils.randomString(10)

    def createReq() = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(groupId),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    // pass
    result(workerApi.add(createReq()))

    an[IllegalArgumentException] should be thrownBy result(workerApi.add(createReq()))
  }

  @Test
  def duplicateConfigTopic(): Unit = {
    val configTopic = CommonUtils.randomString(10)
    def createReq() = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(configTopic),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    // pass
    result(workerApi.add(createReq()))

    an[IllegalArgumentException] should be thrownBy result(workerApi.add(createReq()))
  }

  @Test
  def duplicateOffsetTopic(): Unit = {
    val offsetTopic = CommonUtils.randomString(10)
    def createReq() = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(offsetTopic),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    // pass
    result(workerApi.add(createReq()))

    an[IllegalArgumentException] should be thrownBy result(workerApi.add(createReq()))
  }

  @Test
  def duplicateStatusTopic(): Unit = {
    val statusTopic = CommonUtils.randomString(10)
    def createReq() = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(statusTopic),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    // pass
    result(workerApi.add(createReq()))

    an[IllegalArgumentException] should be thrownBy result(workerApi.add(createReq()))
  }

  @Test
  def testForceDelete(): Unit = {
    val initialCount = configurator.clusterCollie.workerCollie().asInstanceOf[FakeWorkerCollie].forceRemoveCount
    val request = WorkerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      brokerClusterName = Some(bkClusterName),
      clientPort = Some(CommonUtils.availablePort()),
      jmxPort = Some(CommonUtils.availablePort()),
      groupId = Some(CommonUtils.randomString(10)),
      statusTopicName = Some(CommonUtils.randomString(10)),
      statusTopicPartitions = None,
      statusTopicReplications = None,
      configTopicName = Some(CommonUtils.randomString(10)),
      configTopicReplications = None,
      offsetTopicName = Some(CommonUtils.randomString(10)),
      offsetTopicPartitions = None,
      offsetTopicReplications = None,
      jars = Seq.empty,
      nodeNames = nodeNames
    )

    // graceful delete
    result(workerApi.delete(result(workerApi.add(request)).name))
    configurator.clusterCollie.workerCollie().asInstanceOf[FakeWorkerCollie].forceRemoveCount shouldBe initialCount

    // force delete
    result(workerApi.forceDelete(result(workerApi.add(request)).name))
    configurator.clusterCollie.workerCollie().asInstanceOf[FakeWorkerCollie].forceRemoveCount shouldBe initialCount + 1

  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
