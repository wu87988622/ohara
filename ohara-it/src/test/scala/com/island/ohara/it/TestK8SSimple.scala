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
import java.net.HttpRetryException
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.island.ohara.client.ConnectorJson.ErrorResponse
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import akka.stream.ActorMaterializer
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.it.TestK8SSimple.API_SERVER_URL
import com.typesafe.scalalogging.Logger
import org.junit._
import org.scalatest.Matchers
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import com.island.ohara.agent.K8SClient

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import spray.json._
import DefaultJsonProtocol._
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, ContainerState}

class TestK8SSimple extends SmallTest with Matchers {
  private val log = Logger(classOf[TestK8SSimple])

  @Before
  def testBefore(): Unit = {
    val message = s"The k8s is skip test, Please setting ${TestK8SSimple.K8S_API_SERVER_URL_KEY} properties"
    if (API_SERVER_URL.isEmpty) {
      log.info(message)
      skipTest(message)
    }

    val k8sApiServerURL: String = TestK8SSimple.API_SERVER_URL.get
    val uuid: String = TestK8SSimple.uuid

    if (!TestK8SSimple.hasPod(k8sApiServerURL, uuid)) {
      //Create a container for pod
      TestK8SSimple.createNginxPod(k8sApiServerURL, uuid)
    }

  }

  @Test
  def testCheckPodExists(): Unit = {
    val k8sApiServerURL: String = TestK8SSimple.API_SERVER_URL.get
    val uuid: String = TestK8SSimple.uuid
    TestK8SSimple.hasPod(k8sApiServerURL, uuid) shouldBe true
  }

  @Test
  def testK8SNode(): Unit = {
    TestK8SSimple.nodeInfo().size should not be 0
  }

  @Test
  def testK8SClientContainer(): Unit = {
    val k8sClient = K8SClient(API_SERVER_URL.get)

    val containers: Seq[ContainerInfo] = k8sClient.containers.filter(_.name.equals(TestK8SSimple.uuid))
    val containerSize: Int = containers.size
    containerSize shouldBe 1
    val container: ContainerInfo = containers.head
    container.portMappings.size shouldBe 1
    container.environments.size shouldBe 1
    container.environments.get("key1") shouldBe Some("value1")
    container.hostname shouldBe TestK8SSimple.uuid
    container.imageName shouldBe "nginx:1.7.9"
    container.name shouldBe TestK8SSimple.uuid
    container.size shouldBe "Unknown"
  }

  @Test
  def testK8SClientRemoveContainer(): Unit = {
    val k8sClient = K8SClient(API_SERVER_URL.get)
    val podName: String = UUID.randomUUID().toString + "-pod123"

    //Create Pod for test delete
    TestK8SSimple.createNginxPod(TestK8SSimple.API_SERVER_URL.get, podName)
    val containers: Seq[ContainerInfo] = k8sClient.containers.filter(_.name.equals(podName))
    containers.size shouldBe 1

    //Remove a container
    k8sClient.remove(podName).name shouldBe podName
  }

  @Test
  def testK8SClientlog(): Unit = {
    val k8sClient = K8SClient(API_SERVER_URL.get)
    val timestamp: Long = System.currentTimeMillis()
    val podName: String = s"zookeeper-${timestamp}"
    TestK8SSimple.createZookeeperPod(API_SERVER_URL.get, podName)

    var isContainerRunning: Boolean = false
    while (!isContainerRunning) {
      if (k8sClient.containers().filter(c => c.name.contains(podName) && c.state == ContainerState.RUNNING).size == 1) {
        isContainerRunning = true
      }
    }

    val logs: String = k8sClient.log(podName)
    logs.contains("ZooKeeper JMX enabled by default") shouldBe true
  }

  @Test
  def testK8SClientCreator(): Unit = {
    //TODO Test create container from client to K8S server
    /*val containerName: String = s"broker-container-${System.currentTimeMillis()}"
    val k8sClient = K8SClient(API_SERVER_URL.get)
    val result: Option[ContainerInfo] = k8sClient
      .containerCreator()
      .name(containerName)
      .nodename(TestK8SSimple.NODE_SERVER_HOST.get)
      .hostname(containerName)
      .imageName("islandsystems/kafka:1.0.2")
      .envs(Map())
      .portMappings(Map())
      .run()
    result.get.name shouldBe containerName*/
  }
}

object TestK8SSimple {
  implicit val actorSystem: ActorSystem = ActorSystem("TestK8SServer")
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  val TIMEOUT: FiniteDuration = 30 seconds
  val K8S_API_SERVER_URL_KEY: String = "ohara.it.k8s"
  val API_SERVER_URL: Option[String] = sys.env.get(K8S_API_SERVER_URL_KEY)
  //val API_SERVER_URL: Option[String] = Option("http://10.1.3.102:8080/api/v1")
  //val NODE_SERVER_HOST: Option[String] = Option("jack-pc")

  val uuid: String = UUID.randomUUID().toString

  @AfterClass
  def afterClass(): Unit = {
    val uuid: String = TestK8SSimple.uuid
    if (TestK8SSimple.API_SERVER_URL.nonEmpty && hasPod(TestK8SSimple.API_SERVER_URL.get, uuid)) {
      Await.result(Http().singleRequest(
                     HttpRequest(HttpMethods.DELETE,
                                 uri = s"${TestK8SSimple.API_SERVER_URL.get}/namespaces/default/pods/${uuid}")),
                   TIMEOUT)
    }

    //Terminate actor system
    Await.result(actorSystem.terminate(), 60 seconds)
  }

  def createNginxPod(k8sApiServerURL: String, uuid: String): Unit = {
    val podJSON = "{\"apiVersion\": \"v1\", \"kind\": \"Pod\", \"metadata\": { \"name\": \"" + uuid + "\" },\"spec\": {\"containers\": [{\"name\": \"" + uuid + "\", \"image\": \"nginx:1.7.9\", \"env\": [{\"name\": \"key1\", \"value\": \"value1\"}],\"ports\": [{\"containerPort\": 80}]}]}}"

    Await.result(
      Http().singleRequest(
        HttpRequest(HttpMethods.POST,
                    entity = HttpEntity(ContentTypes.`application/json`, podJSON),
                    uri = s"${k8sApiServerURL}/namespaces/default/pods")),
      TestK8SSimple.TIMEOUT
    )
  }

  def createZookeeperPod(k8sApiServerURL: String, uuid: String): Unit = {
    val podJSON = "{\"apiVersion\": \"v1\", \"kind\": \"Pod\", \"metadata\": { \"name\": \"" + uuid + "\" },\"spec\": {\"containers\": [{\"name\": \"" + uuid + "\", \"image\": \"oharastream/zookeeper:0.2-SNAPSHOT\", \"env\": [{\"name\": \"key1\", \"value\": \"value1\"}],\"ports\": [{\"containerPort\": 2181}]}]}}"

    Await.result(
      Http().singleRequest(
        HttpRequest(HttpMethods.POST,
                    entity = HttpEntity(ContentTypes.`application/json`, podJSON),
                    uri = s"${k8sApiServerURL}/namespaces/default/pods")),
      TestK8SSimple.TIMEOUT
    )
  }

  def hasPod(k8sApiServerURL: String, uuid: String): Boolean = {
    case class Metadata(name: String)
    case class Item(metadata: Metadata)
    case class PodInfo(items: Seq[Item])

    implicit val METADATA_JSON_FORMAT: RootJsonFormat[Metadata] = jsonFormat1(Metadata)
    implicit val ITEM_JSON_FORMAT: RootJsonFormat[Item] = jsonFormat1(Item)
    implicit val PODINFO_JSON_FORMAT: RootJsonFormat[PodInfo] = jsonFormat1(PodInfo)

    val podInfo: PodInfo = Await.result(
      Http().singleRequest(HttpRequest(HttpMethods.GET, uri = s"${k8sApiServerURL}/pods")).flatMap(unmarshal[PodInfo]),
      TIMEOUT
    )
    podInfo.items.map(x => x.metadata.name).filter(x => x.equals(uuid)).nonEmpty
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

    Await
      .result(
        Http()
          .singleRequest(HttpRequest(HttpMethods.GET, uri = s"${API_SERVER_URL.get}/nodes"))
          .flatMap(unmarshal[Node]),
        TIMEOUT
      )
      .items
      .map(x => x.status.nodeInfo.machineID)
  }

  def unmarshal[T](response: HttpResponse)(implicit um: RootJsonFormat[T]): Future[T] =
    if (response.status.isSuccess()) Unmarshal(response).to[T]
    else
      Unmarshal(response)
        .to[ErrorResponse]
        .flatMap(error => {
          // this is a retriable exception
          if (error.error_code == StatusCodes.Conflict.intValue)
            Future.failed(new HttpRetryException(error.message, error.error_code))
          else {
            // convert the error response to runtime exception
            Future.failed(new IllegalStateException(error.toString))
          }
        })
}
