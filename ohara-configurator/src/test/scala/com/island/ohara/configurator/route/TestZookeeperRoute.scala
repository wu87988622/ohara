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

import com.island.ohara.client.configurator.v0.ZookeeperApi.{ZookeeperClusterCreationRequest, ZookeeperClusterInfo}
import com.island.ohara.client.configurator.v0.{BrokerApi, NodeApi, ZookeeperApi}
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.fake.FakeZookeeperCollie
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestZookeeperRoute extends MediumTest with Matchers {
  private[this] val numberOfCluster = 1
  private[this] val configurator = Configurator.builder().fake(numberOfCluster, 0).build()

  /**
    * a fake cluster has 3 fake node.
    */
  private[this] val numberOfDefaultNodes = 3 * numberOfCluster
  private[this] val zookeeperApi = ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port)

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
      result(nodeAccess.request().name(n).port(22).user("user").password("pwd").create())
    }

    result(nodeAccess.list).size shouldBe (nodeNames.size + numberOfDefaultNodes)
  }

  @Test
  def repeatedlyDelete(): Unit = {
    (0 to 10).foreach { index =>
      result(zookeeperApi.delete(index.toString))
    }
  }

  @Test
  def removeZookeeperClusterUsedByBrokerCluster(): Unit = {
    val zks = result(zookeeperApi.list)

    // we have a default zk cluster
    zks.isEmpty shouldBe false

    val zk = zks.head

    // this zookeeper cluster is used by broker cluster
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.delete(zk.name))

    // remove all broker clusters
    result(BrokerApi.access().hostname(configurator.hostname).port(configurator.port).list)
      .map(_.name)
      .foreach(name => result(BrokerApi.access().hostname(configurator.hostname).port(configurator.port).delete(name)))

    // pass
    result(zookeeperApi.delete(zk.name))
  }

  @Test
  def testEmptyNodes(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.add(
        ZookeeperClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          clientPort = Some(123),
          electionPort = Some(456),
          peerPort = Some(1345),
          nodeNames = Seq.empty
        ))
    )
  }

  @Test
  def testCreateOnNonexistentNode(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.add(
        ZookeeperClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          clientPort = Some(123),
          electionPort = Some(456),
          peerPort = Some(1345),
          nodeNames = Seq("asdasdasd")
        ))
    )
  }

  @Test
  def testImageName(): Unit = {

    def request() = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(CommonUtils.availablePort()),
      electionPort = Some(CommonUtils.availablePort()),
      peerPort = Some(CommonUtils.availablePort()),
      nodeNames = nodeNames
    )

    // pass by default image
    result(zookeeperApi.add(request()))

    // pass by latest image (since it is default image)
    result(zookeeperApi.add(request().copy(imageName = Some(ZookeeperApi.IMAGE_NAME_DEFAULT))))

    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.add(request().copy(imageName = Some(CommonUtils.randomString()))))
  }

  @Test
  def testCreate(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    assert(request, result(zookeeperApi.add(request)))
  }

  @Test
  def testList(): Unit = {
    val request0 = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(CommonUtils.availablePort()),
      electionPort = Some(CommonUtils.availablePort()),
      peerPort = Some(CommonUtils.availablePort()),
      nodeNames = nodeNames
    )
    assert(request0, result(zookeeperApi.add(request0)))
    val request1 = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10) + "2",
      imageName = None,
      clientPort = Some(CommonUtils.availablePort()),
      electionPort = Some(CommonUtils.availablePort()),
      peerPort = Some(CommonUtils.availablePort()),
      nodeNames = nodeNames
    )
    assert(request1, result(zookeeperApi.add(request1)))

    val clusters = result(zookeeperApi.list)
    clusters.size shouldBe 3
    assert(request0, clusters.find(_.name == request0.name).get)
    assert(request1, clusters.find(_.name == request1.name).get)
  }

  @Test
  def testRemove(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    val cluster = result(zookeeperApi.add(request))
    assert(request, cluster)

    result(zookeeperApi.delete(request.name))
  }

  @Test
  def testGetContainers(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    val cluster = result(zookeeperApi.add(request))
    assert(request, cluster)

    assert(request, result(zookeeperApi.get(request.name)))

    result(zookeeperApi.delete(request.name))
    result(zookeeperApi.list).size shouldBe 1
  }

  @Test
  def testAddNode(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = Seq(nodeNames.head)
    )
    val cluster = result(zookeeperApi.add(request))
    assert(request, cluster)

    // we don't support to add zk node at runtime
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.addNode(cluster.name, nodeNames.last))
  }
  @Test
  def testRemoveNode(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    val cluster = result(zookeeperApi.add(request))
    assert(request, cluster)

    // we don't support to remove zk node at runtime
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.removeNode(cluster.name, nodeNames.head))
  }

  @Test
  def testInvalidClusterName(): Unit = {
    val request = ZookeeperClusterCreationRequest(
      name = "abc def",
      imageName = None,
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = nodeNames
    )
    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.add(request))
  }

  @Test
  def createZkClusterWithSameName(): Unit = {
    val name = CommonUtils.randomString(10)
    def request() = ZookeeperClusterCreationRequest(
      name = name,
      imageName = None,
      clientPort = Some(CommonUtils.availablePort()),
      electionPort = Some(CommonUtils.availablePort()),
      peerPort = Some(CommonUtils.availablePort()),
      nodeNames = nodeNames
    )

    // pass
    result(zookeeperApi.add(request()))

    an[IllegalArgumentException] should be thrownBy result(zookeeperApi.add(request()))
  }

  @Test
  def clientPortConflict(): Unit = {
    val clientPort = CommonUtils.availablePort()
    val request = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(clientPort),
      electionPort = Some(CommonUtils.availablePort()),
      peerPort = Some(CommonUtils.availablePort()),
      nodeNames = nodeNames
    )

    // pass
    result(zookeeperApi.add(request))

    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.add(request.copy(name = CommonUtils.randomString(10))))

    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.add(
        request.copy(name = CommonUtils.randomString(10),
                     electionPort = Some(CommonUtils.availablePort()),
                     peerPort = Some(CommonUtils.availablePort()))))

    // pass
    result(
      zookeeperApi.add(request.copy(
        name = CommonUtils.randomString(10),
        clientPort = Some(CommonUtils.availablePort()),
        electionPort = Some(CommonUtils.availablePort()),
        peerPort = Some(CommonUtils.availablePort())
      )))
  }

  @Test
  def peerPortConflict(): Unit = {
    val peerPort = CommonUtils.availablePort()
    val request = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(CommonUtils.availablePort()),
      electionPort = Some(CommonUtils.availablePort()),
      peerPort = Some(peerPort),
      nodeNames = nodeNames
    )

    // pass
    result(zookeeperApi.add(request))

    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.add(request.copy(name = CommonUtils.randomString(10))))

    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.add(
        request.copy(name = CommonUtils.randomString(10),
                     electionPort = Some(CommonUtils.availablePort()),
                     clientPort = Some(CommonUtils.availablePort()))))

    // pass
    result(
      zookeeperApi.add(request.copy(
        name = CommonUtils.randomString(10),
        clientPort = Some(CommonUtils.availablePort()),
        electionPort = Some(CommonUtils.availablePort()),
        peerPort = Some(CommonUtils.availablePort())
      )))
  }

  @Test
  def electionPortConflict(): Unit = {
    val electionPort = CommonUtils.availablePort()
    val request = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(CommonUtils.availablePort()),
      electionPort = Some(electionPort),
      peerPort = Some(CommonUtils.availablePort()),
      nodeNames = nodeNames
    )

    // pass
    result(zookeeperApi.add(request))

    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.add(request.copy(name = CommonUtils.randomString(10))))

    an[IllegalArgumentException] should be thrownBy result(
      zookeeperApi.add(
        request.copy(name = CommonUtils.randomString(10),
                     peerPort = Some(CommonUtils.availablePort()),
                     clientPort = Some(CommonUtils.availablePort()))))

    // pass
    result(
      zookeeperApi.add(request.copy(
        name = CommonUtils.randomString(10),
        clientPort = Some(CommonUtils.availablePort()),
        electionPort = Some(CommonUtils.availablePort()),
        peerPort = Some(CommonUtils.availablePort())
      )))
  }

  @Test
  def testForceDelete(): Unit = {
    val initialCount = configurator.clusterCollie.zookeeperCollie().asInstanceOf[FakeZookeeperCollie].forceRemoveCount
    val request = ZookeeperClusterCreationRequest(
      name = CommonUtils.randomString(10),
      imageName = None,
      clientPort = Some(CommonUtils.availablePort()),
      electionPort = Some(CommonUtils.availablePort()),
      peerPort = Some(CommonUtils.availablePort()),
      nodeNames = nodeNames
    )
    // graceful delete
    result(zookeeperApi.delete(result(zookeeperApi.add(request)).name))
    configurator.clusterCollie
      .zookeeperCollie()
      .asInstanceOf[FakeZookeeperCollie]
      .forceRemoveCount shouldBe initialCount

    // force delete
    result(zookeeperApi.forceDelete(result(zookeeperApi.add(request)).name))
    configurator.clusterCollie
      .zookeeperCollie()
      .asInstanceOf[FakeZookeeperCollie]
      .forceRemoveCount shouldBe initialCount + 1
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
