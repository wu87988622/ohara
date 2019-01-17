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

package com.island.ohara.it

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.agent.{ClusterCollie, DockerClient, NodeCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.prometheus.PrometheusJson.{Health, Targets}
import com.island.ohara.prometheus.{PrometheusClient, PrometheusConfigUtil, PrometheusDescription, PrometheusServer}
import org.junit.Assume._
import org.junit.{Before, Ignore, Test}
import org.scalatest.Matchers

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}

class TestPrometheus extends IntegrationTest with Matchers {

  private val nodes_key = "ohara.it.docker"

  private val timeout: java.time.Duration = java.time.Duration.ofSeconds(50)

  private val nodes: Option[Seq[Node]] = sys.env
    .get(nodes_key)
    .map(info => {
      info
        .split(",")
        .map(
          nodeInfo => {
            val user = nodeInfo.split(":").head
            val password = nodeInfo.split("@").head.split(":").last
            val hostname = nodeInfo.split("@").last.split(":").head
            val port = nodeInfo.split("@").last.split(":").last.toInt
            Node(hostname, port, user, password, Seq.empty, CommonUtil.current())
          }
        )
        .toList
    })

  private val node = nodes.map(_.head).orNull

  //If this check method is in BeforeClass , gradle --test will build failed that gradle can't find any tests
  //But run in junit is fine
  @Before
  def check(): Unit = {
    assumeNotNull(s"$nodes_key can't be null", node)
    val client =
      DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
    try {
      assumeTrue(client.images().contains(PrometheusServer.IMAGE_NAME_DEFAULT))
    } finally client.close()
  }

  protected val nodeCollie: NodeCollie = NodeCollie.inMemory(Seq(node))
  protected val clusterCollie: ClusterCollie = ClusterCollie(nodeCollie)

  /**
    * test kafka can export metric
    */
  @Test
  def testExporter(): Unit = {
    startZK(zkDesc => {
      startBroker(
        zkDesc.name,
        (exporterPort, _) => {
          implicit val actorSystem: ActorSystem = ActorSystem(s"${classOf[PrometheusClient].getSimpleName}--system")
          implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
          import scala.concurrent.ExecutionContext.Implicits.global
          import scala.concurrent.duration._
          try {
            val url = "http://" + node.name + ":" + exporterPort + "/metrics"
            val txt =
              Await.result(Http().singleRequest(HttpRequest(HttpMethods.GET, url)).flatMap(Unmarshal(_).to[String]),
                           10 seconds)
            txt.isEmpty shouldBe false
            txt.contains("kafka") shouldBe true
          } finally actorSystem.terminate()
        }
      )
    })
  }

//  val clientPort = CommonUtil.availablePort()
  def startZK(f: ZookeeperClusterInfo => Unit): Unit = {
    val clusterName = methodName()
    val electionPort = CommonUtil.availablePort()
    val peerPort = CommonUtil.availablePort()
    val clientPort = CommonUtil.availablePort()
    val zookeeperCollie = clusterCollie.zookeepersCollie()

    try f(
      Await.result(
        zookeeperCollie
          .creator()
          .clientPort(clientPort)
          .electionPort(electionPort)
          .peerPort(peerPort)
          .clusterName(clusterName)
          .nodeName(Await.result(nodeCollie.nodes(), 10 seconds).head.name)
          .create(),
        2 minutes
      ))
    finally Await.result(zookeeperCollie.remove(clusterName), 60 seconds)
  }

  def startBroker(zkClusterName: String, f: (Int, BrokerClusterInfo) => Unit): Unit = {
    val clusterName = methodName()
    val clientPort = CommonUtil.availablePort()
    val exporterPort = CommonUtil.availablePort()
    val brokerCollie = clusterCollie.brokerCollie()

    try f(
      exporterPort,
      Await.result(
        brokerCollie
          .creator()
          .clusterName(clusterName)
          .clientPort(clientPort)
          .exporterPort(exporterPort)
          .zookeeperClusterName(zkClusterName)
          .nodeName(Await.result(nodeCollie.nodes(), 10 seconds).head.name)
          .create(),
        2 minutes
      )
    )
    finally Await.result(brokerCollie.remove(clusterName), 60 seconds)
  }

  private val fakeUrl = "128.128.128.128"

  /**
    * test promethues add targets and remove targets
    *
    */
  @Ignore
  @Test
  def startPrometheus(): Unit = {
    simpleServer(ports => {
      prometheus(
        node,
        desc => {

          val client =
            DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
          try {
            val util = PrometheusConfigUtil(client.containerInspector(desc.name))
            val pclient = PrometheusClient(node.name + ":" + desc.clientPort)

            //check not in  target
            CommonUtil.await(() => !isContain(pclient.targets(), "123.123.123.123:" + desc.clientPort), timeout)

            //check inactive target
            CommonUtil.await(() => isActive(pclient.targets(), fakeUrl + ":" + desc.clientPort, health = false),
                             timeout)

            //check add targets
            ports
              .map(node.name + ":" + _)
              .foreach(target => {
                util.addTarget(target)
                CommonUtil.await(() => isActive(pclient.targets(), target), timeout)
              })

            //       check remove targets
            ports
              .map(node.name + ":" + _)
              .foreach(target => {
                util.removeTarget(target)
                CommonUtil.await(() => !isContain(pclient.targets(), target), timeout)
              })
          } finally client.close()
        }
      )
    })
  }

  def prometheus(node: Node, f: PrometheusDescription => Unit): Unit = {
    val clientPort = CommonUtil.availablePort()
    val server = PrometheusServer.creater().clientPort(clientPort).targets(Seq(fakeUrl + ":" + clientPort)).create(node)

    try f(server.start())
    finally server.stop()
  }

  /**
    * fake akka server
    */
  def simpleServer(f: Seq[Int] => Unit): Unit = {
    implicit val system: ActorSystem = ActorSystem("my-system-")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.server.Directives._
    try {
      val route =
        path("metrics") {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, """com_island_ohara_test{name="test"} 5.0"""))
          }
        }

      val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 0)
      val bindingFuture2 = Http().bindAndHandle(route, "0.0.0.0", 0)
      val bindingFuture3 = Http().bindAndHandle(route, "0.0.0.0", 0)
      val ports: ListBuffer[Int] = ListBuffer.empty
      bindingFuture.onComplete(_.map(_.localAddress.getPort).foreach(ports += _))
      bindingFuture2.onComplete(_.map(_.localAddress.getPort).foreach(ports += _))
      bindingFuture3.onComplete(_.map(_.localAddress.getPort).foreach(ports += _))

      f(ports.toList)
    } finally system.terminate()

  }

  /**
    * check target In targets
    */
  private def isContain(targets: Targets, target: String) = {
    targets.data.activeTargets.exists(_.discoveredLabels.__address__ == target)
  }

  /**
    * check target In targets and active
    */
  private def isActive(targets: Targets, target: String, health: Boolean = true) = {
    if (health)
      targets.data.activeTargets.exists(x => {
        x.discoveredLabels.__address__ == target && x.health == Health.UP
      })
    else
      targets.data.activeTargets.exists(x => {
        x.discoveredLabels.__address__ == target && x.health == Health.Down
      })
  }
}
