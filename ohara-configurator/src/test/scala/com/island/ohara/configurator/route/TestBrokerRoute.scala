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
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterCreationRequest
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterCreationRequest
import com.island.ohara.client.configurator.v0.{BrokerApi, NodeApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class TestBrokerRoute extends MediumTest with Matchers {
  private[this] val configurator = Configurator.builder().fake(0, 0).build()
  private[this] val brokerApi = BrokerApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def assert(request: BrokerClusterCreationRequest, cluster: BrokerClusterInfo): Unit = {
    cluster.name shouldBe request.name
    request.imageName.foreach(_ shouldBe cluster.imageName)
    request.clientPort.foreach(_ shouldBe cluster.clientPort)
    request.zookeeperClusterName.foreach(_ shouldBe cluster.zookeeperClusterName)
    request.nodeNames shouldBe cluster.nodeNames
  }

  private[this] val zkClusterName = CommonUtils.randomString(10)

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

    result(nodeAccess.list()).size shouldBe nodeNames.size

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
  }

  @Test
  def removeBrokerClusterUsedByWorkerCluster(): Unit = {
    val bk = result(
      brokerApi.add(
        BrokerClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          zookeeperClusterName = Some(zkClusterName),
          exporterPort = None,
          clientPort = Some(123),
          nodeNames = nodeNames
        )))

    val wk = result(
      WorkerApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(WorkerClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          brokerClusterName = Some(bk.name),
          clientPort = Some(CommonUtils.availablePort()),
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

    val bks = result(brokerApi.list())

    bks.isEmpty shouldBe false

    // this zookeeper cluster is used by worker cluster
    an[IllegalArgumentException] should be thrownBy result(brokerApi.delete(bk.name))

    // remove wk cluster
    result(WorkerApi.access().hostname(configurator.hostname).port(configurator.port).delete(wk.name))

    // pass
    result(brokerApi.delete(bk.name))
  }

  @Test
  def testDefaultZkInMultiZkCluster(): Unit = {
    val anotherZk = CommonUtils.randomString(10)
    result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          ZookeeperClusterCreationRequest(name = anotherZk,
                                          imageName = None,
                                          clientPort = None,
                                          electionPort = None,
                                          peerPort = None,
                                          nodeNames = nodeNames))).name shouldBe anotherZk
    try {
      result(ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port).list()).size shouldBe 2

      // there are two zk cluster so we have to assign the zk cluster...
      an[IllegalArgumentException] should be thrownBy result(
        brokerApi.add(
          BrokerClusterCreationRequest(
            name = CommonUtils.randomString(10),
            imageName = None,
            zookeeperClusterName = None,
            exporterPort = Some(CommonUtils.availablePort()),
            clientPort = Some(CommonUtils.availablePort()),
            nodeNames = nodeNames
          ))
      )
    } finally result(ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port).delete(anotherZk)).name shouldBe anotherZk

  }

  @Test
  def testCreateOnNonexistentNode(): Unit = {

    an[IllegalArgumentException] should be thrownBy result(
      brokerApi.add(
        BrokerClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          zookeeperClusterName = None,
          exporterPort = Some(CommonUtils.availablePort()),
          clientPort = Some(CommonUtils.availablePort()),
          nodeNames = Seq("asdasdasd")
        ))
    )
  }

  @Test
  def testEmptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      brokerApi.add(
        BrokerClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
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
      name = CommonUtils.randomString(10),
      imageName = None,
      zookeeperClusterName = Some("Asdasdasd"),
      exporterPort = None,
      clientPort = Some(123),
      nodeNames = nodeNames
    )
    an[IllegalArgumentException] should be thrownBy assert(request, result(brokerApi.add(request)))
    val anotherRequest = request.copy(zookeeperClusterName = None)
    assert(anotherRequest, result(brokerApi.add(anotherRequest)))
  }

  @Test
  def testImageName(): Unit = {

    def request() = BrokerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      zookeeperClusterName = Some(zkClusterName),
      exporterPort = None,
      clientPort = Some(CommonUtils.availablePort()),
      nodeNames = nodeNames
    )

    // pass by default image
    var bk = result(brokerApi.add(request()))
    result(brokerApi.delete(bk.name))

    // pass by latest image (since it is default image)
    bk = result(brokerApi.add(request().copy(imageName = Some(BrokerApi.IMAGE_NAME_DEFAULT))))
    result(brokerApi.delete(bk.name))

    an[IllegalArgumentException] should be thrownBy result(
      brokerApi.add(request().copy(imageName = Some(CommonUtils.randomString()))))
  }

  @Test
  def testCreate(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      zookeeperClusterName = Some(zkClusterName),
      exporterPort = None,
      clientPort = Some(123),
      nodeNames = nodeNames
    )
    assert(request, result(brokerApi.add(request)))
  }

  @Test
  def testList(): Unit = {
    val request0 = BrokerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      exporterPort = Some(CommonUtils.availablePort()),
      clientPort = Some(CommonUtils.availablePort()),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    assert(request0, result(brokerApi.add(request0)))

    val zk2 = result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          clientPort = Some(CommonUtils.availablePort()),
          electionPort = Some(CommonUtils.availablePort()),
          peerPort = Some(CommonUtils.availablePort()),
          nodeNames = nodeNames
        )))

    val request1 = BrokerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      exporterPort = Some(CommonUtils.availablePort()),
      clientPort = Some(CommonUtils.availablePort()),
      zookeeperClusterName = Some(zk2.name),
      nodeNames = nodeNames
    )
    assert(request1, result(brokerApi.add(request1)))

    val clusters = result(brokerApi.list())
    clusters.size shouldBe 2
    assert(request0, clusters.find(_.name == request0.name).get)
    assert(request1, clusters.find(_.name == request1.name).get)
  }

  @Test
  def testRemove(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(123),
      exporterPort = None,
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    val cluster = result(brokerApi.add(request))
    assert(request, cluster)

    result(brokerApi.delete(request.name)) shouldBe cluster
  }

  @Test
  def testGetContainers(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(123),
      exporterPort = None,
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    val cluster = result(brokerApi.add(request))
    assert(request, cluster)

    val containers = result(brokerApi.get(request.name))
    containers.size shouldBe request.nodeNames.size

    result(brokerApi.delete(request.name)) shouldBe cluster
    result(brokerApi.list()).size shouldBe 0
  }

  @Test
  def testAddNode(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(123),
      exporterPort = None,
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = Seq(nodeNames.head)
    )
    val cluster = result(brokerApi.add(request))
    assert(request, cluster)

    result(brokerApi.addNode(cluster.name, nodeNames.last)) shouldBe BrokerClusterInfo(
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
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(123),
      exporterPort = None,
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    val cluster = result(brokerApi.add(request))
    assert(request, cluster)

    result(brokerApi.removeNode(cluster.name, nodeNames.last)) shouldBe BrokerClusterInfo(
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
      imageName = None,
      clientPort = Some(123),
      exporterPort = None,
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )
    an[IllegalArgumentException] should be thrownBy result(brokerApi.add(request))
  }

  @Test
  def runMultiBkClustersOnSameZkCluster(): Unit = {
    val request = BrokerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      exporterPort = None,
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )

    // pass
    result(brokerApi.add(request))

    // we can't create multi broker clusters on same zk cluster
    an[IllegalArgumentException] should be thrownBy result(
      brokerApi.add(request.copy(name = CommonUtils.randomString(10))))
  }

  @Test
  def createBkClusterWithSameName(): Unit = {
    val name = CommonUtils.randomString(10)
    def request() = BrokerClusterCreationRequest(
      name = name,
      imageName = None,
      exporterPort = None,
      clientPort = Some(123),
      zookeeperClusterName = Some(zkClusterName),
      nodeNames = nodeNames
    )

    // pass
    result(brokerApi.add(request()))

    val zk2 = result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          clientPort = Some(CommonUtils.availablePort()),
          electionPort = Some(CommonUtils.availablePort()),
          peerPort = Some(CommonUtils.availablePort()),
          nodeNames = nodeNames
        )))

    an[IllegalArgumentException] should be thrownBy result(
      brokerApi.add(
        request().copy(
          zookeeperClusterName = Some(zk2.name)
        )))
  }

  @Test
  def clientPortConflict(): Unit = {
    val clientPort = CommonUtils.availablePort()
    val request = BrokerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      zookeeperClusterName = None,
      clientPort = Some(clientPort),
      exporterPort = Some(CommonUtils.availablePort()),
      nodeNames = nodeNames
    )

    // pass
    result(brokerApi.add(request))

    val zk2 = result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          clientPort = Some(CommonUtils.availablePort()),
          electionPort = Some(CommonUtils.availablePort()),
          peerPort = Some(CommonUtils.availablePort()),
          nodeNames = nodeNames
        )))

    an[IllegalArgumentException] should be thrownBy result(
      brokerApi.add(request.copy(name = CommonUtils.randomString(10), zookeeperClusterName = Some(zk2.name))))

    an[IllegalArgumentException] should be thrownBy result(
      brokerApi.add(
        request.copy(name = CommonUtils.randomString(10),
                     zookeeperClusterName = Some(zk2.name),
                     exporterPort = Some(CommonUtils.availablePort()))))

    // pass
    result(
      brokerApi.add(request.copy(
        name = CommonUtils.randomString(10),
        zookeeperClusterName = Some(zk2.name),
        clientPort = Some(CommonUtils.availablePort()),
        exporterPort = Some(CommonUtils.availablePort())
      )))
  }

  @Test
  def exporterPortConflict(): Unit = {
    val exporterPort = CommonUtils.availablePort()
    val request = BrokerClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      zookeeperClusterName = None,
      clientPort = Some(CommonUtils.availablePort()),
      exporterPort = Some(exporterPort),
      nodeNames = nodeNames
    )

    // pass
    result(brokerApi.add(request))

    val zk2 = result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          clientPort = Some(CommonUtils.availablePort()),
          electionPort = Some(CommonUtils.availablePort()),
          peerPort = Some(CommonUtils.availablePort()),
          nodeNames = nodeNames
        )))

    an[IllegalArgumentException] should be thrownBy result(
      brokerApi.add(request.copy(name = CommonUtils.randomString(10), zookeeperClusterName = Some(zk2.name))))

    an[IllegalArgumentException] should be thrownBy result(
      brokerApi.add(
        request.copy(name = CommonUtils.randomString(10),
                     zookeeperClusterName = Some(zk2.name),
                     clientPort = Some(CommonUtils.availablePort()))))

    // pass
    result(
      brokerApi.add(request.copy(
        name = CommonUtils.randomString(10),
        zookeeperClusterName = Some(zk2.name),
        clientPort = Some(CommonUtils.availablePort()),
        exporterPort = Some(CommonUtils.availablePort())
      )))
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
