package com.island.ohara.configurator
import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson.{ZookeeperClusterDescription, ZookeeperClusterRequest}
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestZookeeperRoute extends MediumTest with Matchers {
  private[this] val configurator = Configurator.local()
  private[this] val client = ConfiguratorClient(configurator.connectionProps)

  private[this] def assert(request: ZookeeperClusterRequest, cluster: ZookeeperClusterDescription): Unit = {
    cluster.name shouldBe request.name
    request.imageName.foreach(_ shouldBe cluster.imageName)
    request.clientPort.foreach(_ shouldBe cluster.clientPort)
    request.peerPort.foreach(_ shouldBe cluster.peerPort)
    request.electionPort.foreach(_ shouldBe cluster.electionPort)
    request.nodeNames shouldBe cluster.nodeNames
  }

  @Test
  def testCreate(): Unit = {
    val request = ZookeeperClusterRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = Seq("abc")
    )
    assert(request, client.createCluster[ZookeeperClusterRequest, ZookeeperClusterDescription](request))
  }

  @Test
  def testList(): Unit = {
    val request0 = ZookeeperClusterRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = Seq("abc")
    )
    assert(request0, client.createCluster[ZookeeperClusterRequest, ZookeeperClusterDescription](request0))
    val request1 = ZookeeperClusterRequest(
      name = methodName() + "-2",
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = Seq("abc")
    )
    assert(request1, client.createCluster[ZookeeperClusterRequest, ZookeeperClusterDescription](request1))

    val clusters = client.listCluster[ZookeeperClusterDescription]
    clusters.size shouldBe 2
    assert(request0, clusters.find(_.name == request0.name).get)
    assert(request1, clusters.find(_.name == request1.name).get)
  }

  @Test
  def testRemove(): Unit = {
    val request = ZookeeperClusterRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = Seq("abc")
    )
    val cluster = client.createCluster[ZookeeperClusterRequest, ZookeeperClusterDescription](request)
    assert(request, cluster)

    client.removeCluster[ZookeeperClusterDescription](request.name) shouldBe cluster
  }

  @Test
  def testGetContainers(): Unit = {
    val request = ZookeeperClusterRequest(
      name = methodName(),
      imageName = Some("abcdef"),
      clientPort = Some(123),
      electionPort = Some(456),
      peerPort = Some(1345),
      nodeNames = Seq("abc")
    )
    val cluster = client.createCluster[ZookeeperClusterRequest, ZookeeperClusterDescription](request)
    assert(request, cluster)

    val containers = client.containersOfZookeeperCluster(request.name)
    containers.size shouldBe request.nodeNames.size

    client.removeCluster[ZookeeperClusterDescription](request.name) shouldBe cluster
    client.containersOfZookeeperCluster(request.name).size shouldBe 0
  }
  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(configurator)
  }
}
