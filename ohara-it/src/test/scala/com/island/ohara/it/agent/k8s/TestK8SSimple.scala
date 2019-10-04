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

package com.island.ohara.it.agent.k8s

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.agent.k8s.K8SJson.K8SErrorResponse
import com.island.ohara.agent.k8s.{K8SClient, K8SStatusInfo, K8sContainerState}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.it.{IntegrationTest, EnvTestingUtils}
import com.typesafe.scalalogging.Logger
import org.junit._
import org.scalatest.Matchers
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestK8SSimple extends IntegrationTest with Matchers {
  private[this] val log = Logger(classOf[TestK8SSimple])
  private[this] val TIMEOUT: FiniteDuration = 30 seconds
  private[this] var k8sApiServerURL: String = _
  private[this] var nodeServerNames: Seq[String] = _
  private[this] var k8sClient: K8SClient = _

  @Before
  def before(): Unit = {
    val message = s"The k8s is skip test, Please setting ${EnvTestingUtils.K8S_MASTER_KEY} properties"
    if (TestK8SSimple.API_SERVER_URL.isEmpty) {
      log.info(message)
      skipTest(message)
    }
    k8sApiServerURL = TestK8SSimple.API_SERVER_URL.get
    nodeServerNames = EnvTestingUtils.k8sNodes().map(_.hostname)
    k8sClient = K8SClient(k8sApiServerURL)

    val uuid: String = TestK8SSimple.uuid

    if (!TestK8SSimple.hasPod(k8sApiServerURL, uuid)) {
      //Create a container for pod
      TestK8SSimple.createZookeeperPod(k8sApiServerURL, uuid)
    }

  }

  @Test
  def testCheckPodExists(): Unit = {
    val uuid: String = TestK8SSimple.uuid
    TestK8SSimple.hasPod(k8sApiServerURL, uuid) shouldBe true
  }

  @Test
  def testK8SNode(): Unit = {
    TestK8SSimple.nodeInfo().size should not be 0
  }

  @Test
  def testK8SClientContainer(): Unit = {
    val containers: Seq[ContainerInfo] = result(
      k8sClient.containers().map(c => c.filter(info => info.name.equals(TestK8SSimple.uuid))))
    val containerSize: Int = containers.size
    containerSize shouldBe 1
    val container: ContainerInfo = containers.head
    container.portMappings.size shouldBe 1
    container.environments.size shouldBe 4
    container.environments.get("ZK_ID") shouldBe Some("0")
    container.hostname shouldBe TestK8SSimple.uuid
    container.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
    container.name shouldBe TestK8SSimple.uuid
    container.size shouldBe "Unknown"
  }

  @Test
  def testK8SClientRemoveContainer(): Unit = {
    val podName: String = UUID.randomUUID().toString + "-pod123"

    try {
      //Create Pod for test delete
      TestK8SSimple.createZookeeperPod(k8sApiServerURL, podName)
      val containers: Seq[ContainerInfo] =
        result(k8sClient.containers().map(cs => cs.filter(info => info.hostname.equals(podName))))
      containers.size shouldBe 1
    } finally {
      //Remove a container
      result(k8sClient.remove(podName)).name shouldBe podName
    }
  }

  @Test
  def testK8SClientForceRemoveContainer(): Unit = {
    val podName: String = UUID.randomUUID().toString + "-pod123"

    try {
      //Create Pod for test delete
      TestK8SSimple.createZookeeperPod(k8sApiServerURL, podName)
      result(k8sClient.containers().map(cs => cs.filter(info => info.hostname.equals(podName)))).size shouldBe 1
    } finally {
      //Remove a container
      result(k8sClient.forceRemove(podName)).name shouldBe podName
      // wait a little time
      Thread.sleep(1000L)
      result(k8sClient.containers().map(cs => cs.filter(info => info.hostname.equals(podName)))).size shouldBe 0
    }
  }

  @Test
  def testK8SClientlog(): Unit = {
    //Must confirm to microk8s is running
    val podName: String = s"zookeeper-${CommonUtils.randomString(10)}"
    try {
      TestK8SSimple.createZookeeperPod(k8sApiServerURL, podName)

      var isContainerRunning: Boolean = false
      while (!isContainerRunning) {
        if (result(k8sClient.containers()).count(c =>
              c.hostname.contains(podName) && c.state == K8sContainerState.RUNNING.name) == 1) {
          isContainerRunning = true
        }
      }

      val logs: String = result(k8sClient.log(podName))
      logs.contains("ZooKeeper JMX enabled by default") shouldBe true
    } finally {
      result(k8sClient.remove(podName)).name shouldBe podName
    }
  }

  @Test
  def testErrorResponse(): Unit = {
    val k8sClient = K8SClient(s"$k8sApiServerURL/error_test")
    an[RuntimeException] should be thrownBy result(k8sClient.containers())
  }

  @Test
  def testK8SClientCreator(): Unit = {
    val containerName: String = s"zookeeper-container-${CommonUtils.randomString(10)}"
    try {
      val result: Future[Option[ContainerInfo]] =
        k8sClient
          .containerCreator()
          .name(containerName)
          .domainName("default")
          .labelName("ohara")
          .nodeName(nodeServerNames.head)
          .hostname(containerName)
          .imageName(ZookeeperApi.IMAGE_NAME_DEFAULT)
          .envs(Map(
            "ZK_CLIENT_PORT" -> s"${CommonUtils.availablePort()}",
            "ZK_PEER_PORT" -> s"${CommonUtils.availablePort()}",
            "ZK_ELECTION_PORT" -> s"${CommonUtils.availablePort()}"
          ))
          .create()
      Await.result(result, TIMEOUT).get.name shouldBe containerName
    } finally {
      // Remove a container
      result(k8sClient.remove(containerName)).name shouldBe containerName
    }
  }

  @Test
  def testK8SImages(): Unit = {
    val images: Seq[String] = result(k8sClient.images(nodeServerNames.last)).map(x => x.split(":").head)
    //After installed K8S, created k8s.gcr.io/kube-proxy and k8s.gcr.io/pause two docker image.
    images.size >= 2 shouldBe true
  }

  @Test
  def testSlaveNode(): Unit = {
    val k8sNode: Boolean = result(k8sClient.checkNode(nodeServerNames.last)).isK8SNode
    k8sNode shouldBe true

    val unknownNode: Boolean = result(k8sClient.checkNode("ohara-it-08")).isK8SNode
    unknownNode shouldBe false
  }

  @Test
  def testNodeHealth(): Unit = {
    val oharaIt03: Boolean =
      result(k8sClient.checkNode(nodeServerNames.head)).statusInfo.getOrElse(K8SStatusInfo(false, "")).isHealth
    oharaIt03 shouldBe true

    val oharaIt04: Boolean =
      result(k8sClient.checkNode(nodeServerNames.last)).statusInfo.getOrElse(K8SStatusInfo(false, "")).isHealth
    oharaIt04 shouldBe true

    val oharaIt08: Boolean =
      result(k8sClient.checkNode("ohara-it-08")).statusInfo.getOrElse(K8SStatusInfo(false, "")).isHealth
    oharaIt08 shouldBe false
  }

  @Test
  def testAddConfig(): Unit = {
    val configs = (for (i <- 1 to 10) yield i.toString -> CommonUtils.randomString()).toMap
    val name = CommonUtils.randomString()

    // specific name for config
    result(k8sClient.addConfig(name, configs)) shouldBe name

    // random name for config
    val name2 = result(k8sClient.addConfig(configs))
    name2 should not be name

    result(k8sClient.inspectConfig(name)).keySet.forall(configs.keySet.contains) shouldBe true
    result(k8sClient.removeConfig(name)) shouldBe true

    // after remove, the configmap should not exist
    an[IllegalArgumentException] should be thrownBy result(k8sClient.inspectConfig(name))

    result(k8sClient.forceRemoveConfig(name2))
  }
}

object TestK8SSimple {
  implicit val actorSystem: ActorSystem = ActorSystem("TestK8SServer")
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  val API_SERVER_URL: Option[String] = sys.env.get(EnvTestingUtils.K8S_MASTER_KEY)

  val uuid: String = UUID.randomUUID().toString

  @AfterClass
  def afterClass(): Unit = {
    if (TestK8SSimple.API_SERVER_URL.nonEmpty && hasPod(TestK8SSimple.API_SERVER_URL.get, uuid)) {
      IntegrationTest.result(
        Http().singleRequest(
          HttpRequest(HttpMethods.DELETE, uri = s"${TestK8SSimple.API_SERVER_URL.get}/namespaces/default/pods/$uuid")))
    }

    //Terminate actor system
    IntegrationTest.result(actorSystem.terminate())
  }

  def createZookeeperPod(k8sApiServerURL: String, podName: String): Unit = {
    val ZK_CLIENT_PORT = CommonUtils.availablePort()
    val ZK_ELECTION_PORT = CommonUtils.availablePort()
    val ZK_PEER_PORT = CommonUtils.availablePort()
    val podJSON = s"""
                     |{
                     |  "apiVersion": "v1",
                     |  "kind": "Pod",
                     |  "metadata": {
                     |    "name": "$podName"
                     |  },
                     |  "spec": {
                     |    "hostname": "$podName",
                     |    "containers": [
                     |      {
                     |        "name": "$podName",
                     |        "image": "${ZookeeperApi.IMAGE_NAME_DEFAULT}",
                     |        "ports": [
                     |          {
                     |            "containerPort": $ZK_CLIENT_PORT,
                     |            "hostPort": $ZK_CLIENT_PORT
                     |          },
                     |          {
                     |            "containerPort": $ZK_ELECTION_PORT,
                     |            "hostPort": $ZK_ELECTION_PORT
                     |          },
                     |          {
                     |            "containerPort": $ZK_PEER_PORT,
                     |            "hostPort": $ZK_PEER_PORT
                     |          }
                     |        ],
                     |        "env": [
                     |          {
                     |            "name": "ZK_ID",
                     |            "value": "0"
                     |          },
                     |          {
                     |            "name": "ZK_ELECTION_PORT",
                     |            "value": "$ZK_ELECTION_PORT"
                     |          },
                     |          {
                     |            "name": "ZK_CLIENT_PORT",
                     |            "value": "$ZK_CLIENT_PORT"
                     |          },
                     |          {
                     |            "name": "ZK_PEER_PORT",
                     |            "value": "$ZK_PEER_PORT"
                     |          }
                     |        ]
                     |      }
                     |    ]
                     |  }
                     |}
                """.stripMargin
    IntegrationTest.result(
      Http().singleRequest(
        HttpRequest(HttpMethods.POST,
                    entity = HttpEntity(ContentTypes.`application/json`, podJSON),
                    uri = s"$k8sApiServerURL/namespaces/default/pods"))
    )
  }

  def hasPod(k8sApiServerURL: String, podName: String): Boolean = {
    case class Metadata(name: String)
    case class Item(metadata: Metadata)
    case class PodInfo(items: Seq[Item])

    implicit val METADATA_JSON_FORMAT: RootJsonFormat[Metadata] = jsonFormat1(Metadata)
    implicit val ITEM_JSON_FORMAT: RootJsonFormat[Item] = jsonFormat1(Item)
    implicit val PODINFO_JSON_FORMAT: RootJsonFormat[PodInfo] = jsonFormat1(PodInfo)

    val podInfo: PodInfo = IntegrationTest.result(
      Http().singleRequest(HttpRequest(HttpMethods.GET, uri = s"$k8sApiServerURL/pods")).flatMap(unmarshal[PodInfo])
    )
    podInfo.items.map(x => x.metadata.name).exists(x => x.equals(podName))
  }

  def nodeInfo(): Seq[String] = {
    case class Node(items: Seq[Item])
    case class Item(status: Status)
    case class Status(nodeInfo: NodeInfo)
    case class NodeInfo(machineID: String)

    implicit val NODEINFO_JSON_FORMAT: RootJsonFormat[NodeInfo] = jsonFormat1(NodeInfo)
    implicit val STATUS_JSON_FORMAT: RootJsonFormat[Status] = jsonFormat1(Status)
    implicit val ITEM_JSON_FORMAT: RootJsonFormat[Item] = jsonFormat1(Item)
    implicit val NODE_JSON_FORMAT: RootJsonFormat[Node] = jsonFormat1(Node)

    IntegrationTest
      .result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.GET, uri = s"${API_SERVER_URL.get}/nodes"))
          .flatMap(unmarshal[Node])
      )
      .items
      .map(x => x.status.nodeInfo.machineID)
  }

  def unmarshal[T](response: HttpResponse)(implicit um: RootJsonFormat[T]): Future[T] =
    if (response.status.isSuccess()) Unmarshal(response).to[T]
    else
      Unmarshal(response)
        .to[K8SErrorResponse]
        .flatMap(error => {
          Future.failed(new RuntimeException(error.message))
        })
}
