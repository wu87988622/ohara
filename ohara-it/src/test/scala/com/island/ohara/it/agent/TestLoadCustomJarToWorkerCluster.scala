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

package com.island.ohara.it.agent
import java.io.File

import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.jar.JarStore
import com.island.ohara.it.IntegrationTest
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestLoadCustomJarToWorkerCluster extends IntegrationTest with Matchers {

  /**
    * we need to export port to enable remote node download jar from this node
    */
  private[this] val portKey = "ohara.it.port"

  /**
    * we need to export hostname to enable remote node download jar from this node
    */
  private[this] val hostnameKey: String = "ohara.it.hostname"

  private[this] val nodeCache: Seq[Node] = CollieTestUtil.nodeCache()

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

  @Before
  def setup(): Unit = if (nodeCache.isEmpty || publicPort == invalidPort || publicHostname == invalidHostname)
    skipTest(
      s"${CollieTestUtil.key}, $portKey and $hostnameKey don't exist so all tests in TestLoadCustomJarToWorkerCluster are ignored")

  @Test
  def test(): Unit = {
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
        .clusterName(CommonUtil.randomString(10))
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
          .clusterName(CommonUtil.randomString(10))
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
            .clusterName(CommonUtil.randomString(10))
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
            val workerClient = WorkerClient(s"$name:${wkCluster.clientPort}")
            CommonUtil.await(
              () =>
                try result(workerClient.plugins()).exists(_.className == "com.island.ohara.it.ItConnector")
                  && result(workerClient.plugins()).exists(_.className == "com.island.ohara.it.ItConnector2")
                catch {
                  case _: Throwable => false
              },
              java.time.Duration.ofSeconds(30)
            )
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
    Releasable.close(configurator)
    Releasable.close(clusterCollie)
  }
}
