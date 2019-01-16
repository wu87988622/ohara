package com.island.ohara.it.agent
import java.io.File

import com.island.ohara.agent._
import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.it.IntegrationTest
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestLoadCustomJarToWorkerCluster extends IntegrationTest with Matchers {

  /**
    * form: user:password@hostname:port.
    * NOTED: this key need to be matched with another key value in ohara-it/build.gradle
    */
  private[this] val key = "ohara.it.docker"

  /**
    * we need to export port to enable remote node download jar from this node
    */
  private[this] val portKey = "ohara.it.port"

  /**
    * we need to export hostname to enable remote node download jar from this node
    */
  private[this] val hostnameKey: String = "ohara.it.hostname"

  private[this] val nodeCache: Seq[Node] = sys.env
    .get(key)
    .map(_.split(",").map { nodeInfo =>
      val user = nodeInfo.split(":").head
      val password = nodeInfo.split("@").head.split(":").last
      val hostname = nodeInfo.split("@").last.split(":").head
      val port = nodeInfo.split("@").last.split(":").last.toInt
      Node(hostname, port, user, password, Seq.empty, CommonUtil.current())
    }.toSeq)
    .getOrElse(Seq.empty)

  private[this] val nodeCollie: NodeCollie = new NodeCollie {
    override def nodes(): Future[Seq[Node]] = Future.successful(nodeCache)
    override def node(name: String): Future[Node] = Future.successful(
      nodeCache.find(_.name == name).getOrElse(throw new NoSuchElementException(s"expected:$name actual:$nodeCache")))
  }

  private[this] val invalidHostname = "unknown"

  private[this] val invalidPort = 0

  private[this] val publicHostname: String = sys.env.getOrElse(hostnameKey, invalidHostname)

  private[this] val publicPort = sys.env.get(portKey).map(_.toInt).getOrElse(invalidPort)

  private[this] val configurator: Configurator =
    Configurator.builder().fake().hostname(publicHostname).port(publicPort).build()
  private[this] val jarStore: JarStore = configurator.jarStore
  private[this] val clusterCollie: ClusterCollie = ClusterCollie(nodeCollie)

  /**
    * used to debug...
    */
  private[this] val cleanup = true

  private[this] def result[T](f: Future[T]): T = Await.result(f, 60 seconds)

  @Test
  def test(): Unit = if (nodeCache.isEmpty || publicPort == invalidPort || publicHostname == invalidHostname)
    skipTest(
      s"$key, $portKey and $hostnameKey don't exist so all tests in TestLoadCustomJarToWorkerCluster are ignored")
  else {
    nodeCache.foreach { node =>
      val dockerClient =
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
      try {
        withClue(s"failed to find ${ZookeeperCollie.IMAGE_NAME_DEFAULT}")(
          dockerClient.images().contains(ZookeeperCollie.IMAGE_NAME_DEFAULT) shouldBe true)
        withClue(s"failed to find ${BrokerCollie.IMAGE_NAME_DEFAULT}")(
          dockerClient.images().contains(BrokerCollie.IMAGE_NAME_DEFAULT) shouldBe true)
        withClue(s"failed to find ${WorkerCollie.IMAGE_NAME_DEFAULT}")(
          dockerClient.images().contains(WorkerCollie.IMAGE_NAME_DEFAULT) shouldBe true)
      } finally dockerClient.close()
    }
    val currentPath = new File(".").getCanonicalPath
    // Both jars are pre-generated. see readme in test/resources
    val jars = result(
      Future.traverse(Seq(new File(currentPath, s"src/test/resources/ItConnector.jar"),
                          new File(currentPath, s"src/test/resources/ItConnector2.jar")))(jarStore.add))
    val zkCluster = result(
      clusterCollie
        .zookeepersCollie()
        .creator()
        .clusterName(CommonUtil.randomString())
        .clientPort(CommonUtil.availablePort())
        .electionPort(CommonUtil.availablePort())
        .peerPort(CommonUtil.availablePort())
        .nodeName(result(nodeCollie.nodes()).head.name)
        .create())
    try {
      val bkCluster = result(
        clusterCollie
          .brokerCollie()
          .creator()
          .clusterName(CommonUtil.randomString())
          .clientPort(CommonUtil.availablePort())
          .exporterPort(CommonUtil.availablePort())
          .zookeeperClusterName(zkCluster.name)
          .nodeName(result(nodeCollie.nodes()).head.name)
          .create())

      try {
        val wkCluster = result(
          clusterCollie
            .workerCollie()
            .creator()
            .clusterName(CommonUtil.randomString())
            .clientPort(CommonUtil.availablePort())
            .brokerClusterName(bkCluster.name)
            .nodeName(result(nodeCollie.nodes()).head.name)
            .jarUrls(result(jarStore.urls(jars.map(_.id))))
            .create())
        try {
          // add all remaining node to the running worker cluster
          result(nodeCollie.nodes()).filterNot(n => wkCluster.nodeNames.contains(n.name)).foreach { n =>
            result(clusterCollie.workerCollie().addNode(wkCluster.name, n.name))
          }
          // make sure all workers have loaded the test-purposed connector.
          result(clusterCollie.workerCollie().cluster(wkCluster.name))._1.nodeNames.foreach { name =>
            val connectorClient = ConnectorClient(s"$name:${wkCluster.clientPort}")
            try CommonUtil.await(
              () =>
                try connectorClient.plugins().exists(_.className == "com.island.ohara.it.ItConnector")
                  && connectorClient.plugins().exists(_.className == "com.island.ohara.it.ItConnector2")
                catch {
                  case _: Throwable => false
              },
              java.time.Duration.ofSeconds(30)
            )
            finally connectorClient.close()
          }
        } finally if (cleanup)
          result(clusterCollie.workerCollie().clusters()).foreach(c =>
            result(clusterCollie.workerCollie().remove(c._1.name)))
      } finally if (cleanup)
        result(clusterCollie.brokerCollie().clusters()).foreach(c =>
          result(clusterCollie.brokerCollie().remove(c._1.name)))
    } finally if (cleanup)
      result(clusterCollie.zookeepersCollie().clusters()).foreach(c =>
        result(clusterCollie.zookeepersCollie().remove(c._1.name)))

  }

  @After
  final def tearDown(): Unit = {
    ReleaseOnce.close(configurator)
    ReleaseOnce.close(clusterCollie)
  }
}
