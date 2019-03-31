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
import com.island.ohara.agent.k8s.{K8SClient, K8sContainerState}
import com.island.ohara.agent.k8s.K8SJson.K8SErrorResponse
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.it.IntegrationTest
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

  @Before
  def testBefore(): Unit = {
    val message = s"The k8s is skip test, Please setting ${TestK8SSimple.K8S_API_SERVER_URL_KEY} properties"
    if (TestK8SSimple.API_SERVER_URL.isEmpty || TestK8SSimple.NODE_SERVER_NAME.isEmpty) {
      log.info(message)
      skipTest(message)
    }
    k8sApiServerURL = TestK8SSimple.API_SERVER_URL.get
    nodeServerNames = TestK8SSimple.NODE_SERVER_NAME.get.split(",").toSeq

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
    val k8sClient = K8SClient(k8sApiServerURL)

    val containers: Seq[ContainerInfo] = k8sClient.containers.filter(_.name.equals(TestK8SSimple.uuid))
    val containerSize: Int = containers.size
    containerSize shouldBe 1
    val container: ContainerInfo = containers.head
    container.portMappings.size shouldBe 1
    container.environments.size shouldBe 1
    container.environments.get("key1") shouldBe Some("value1")
    container.hostname shouldBe TestK8SSimple.uuid
    container.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
    container.name shouldBe TestK8SSimple.uuid
    container.size shouldBe "Unknown"
  }

  @Test
  def testK8SClientRemoveContainer(): Unit = {
    val k8sClient = K8SClient(k8sApiServerURL)
    val podName: String = UUID.randomUUID().toString + "-pod123"

    try {
      //Create Pod for test delete
      TestK8SSimple.createZookeeperPod(k8sApiServerURL, podName)
      val containers: Seq[ContainerInfo] = k8sClient.containers.filter(_.hostname.equals(podName))
      containers.size shouldBe 1
    } finally {
      //Remove a container
      k8sClient.remove(podName).name shouldBe podName
    }
  }

  @Test
  def testK8SClientlog(): Unit = {
    //Must confirm to microk8s is running
    val k8sClient = K8SClient(k8sApiServerURL)
    val podName: String = s"zookeeper-${CommonUtils.randomString(10)}"
    try {
      TestK8SSimple.createZookeeperPod(k8sApiServerURL, podName)

      var isContainerRunning: Boolean = false
      while (!isContainerRunning) {
        if (k8sClient.containers.count(c => c.hostname.contains(podName) && c.state == K8sContainerState.RUNNING.name) == 1) {
          isContainerRunning = true
        }
      }

      val logs: String = k8sClient.log(podName)
      logs.contains("ZooKeeper JMX enabled by default") shouldBe true
    } finally {
      k8sClient.remove(podName).name shouldBe podName
    }
  }

  @Test
  def testErrorResponse(): Unit = {
    val k8sClient = K8SClient(s"$k8sApiServerURL/error_test")
    an[RuntimeException] should be thrownBy k8sClient.containers
  }

  @Test
  def testK8SClientCreator(): Unit = {
    val containerName: String = s"zookeeper-container-${CommonUtils.randomString(10)}"
    val k8sClient = K8SClient(k8sApiServerURL)
    try {
      val result: Option[ContainerInfo] = k8sClient
        .containerCreator()
        .name(containerName)
        .domainName("default")
        .labelName("ohara")
        .nodename(nodeServerNames.head)
        .hostname(containerName)
        .imageName(ZookeeperApi.IMAGE_NAME_DEFAULT)
        .envs(Map())
        .portMappings(Map())
        .run
      result.get.name shouldBe containerName
    } finally {
      // Remove a container
      k8sClient.remove(containerName).name shouldBe containerName
    }
  }

  @Test
  def testK8SNodeInfo(): Unit = {
    val k8sClient = K8SClient(k8sApiServerURL)
    val nodes = k8sClient.nodeNameIPInfo
    nodes.size shouldBe 3

    nodes.map(x => x.hostnames.head).mkString(",").contains(TestK8SSimple.NODE_SERVER_NAME.get) shouldBe true
  }

  @Test
  def testK8SImages(): Unit = {
    val k8sClient = K8SClient(k8sApiServerURL)
    val images: Seq[String] = Await.result(k8sClient.images(nodeServerNames.last), TIMEOUT).map(x => x.split(":").head)
    //After installed K8S, created k8s.gcr.io/kube-proxy and k8s.gcr.io/pause two docker image.
    images.size >= 2 shouldBe true
    images.contains("k8s.gcr.io/kube-proxy") shouldBe true
  }
}

object TestK8SSimple {
  implicit val actorSystem: ActorSystem = ActorSystem("TestK8SServer")
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  val K8S_API_SERVER_URL_KEY: String = "ohara.it.k8s"
  val K8S_API_NODE_NAME_KEY: String = "ohara.it.k8s.nodename"

  val API_SERVER_URL: Option[String] = sys.env.get(K8S_API_SERVER_URL_KEY)
  val NODE_SERVER_NAME: Option[String] = sys.env.get(K8S_API_NODE_NAME_KEY)

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
    val podJSON = "{\"apiVersion\": \"v1\", \"kind\": \"Pod\", \"metadata\": { \"name\": \"" + podName + "\" },\"spec\": {\"hostname\": \"" + podName + "\", \"containers\": [{\"name\": \"" + podName + "\", \"image\": \"" + ZookeeperApi.IMAGE_NAME_DEFAULT + "\", \"env\": [{\"name\": \"key1\", \"value\": \"value1\"}],\"ports\": [{\"containerPort\": 2181}]}]}}"

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
