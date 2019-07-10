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

package com.island.ohara.agent.k8s

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import com.island.ohara.agent.k8s.K8SClient.ImagePullPolicy
import com.island.ohara.agent.k8s.K8SJson._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo

import scala.concurrent.ExecutionContext.Implicits.global
class TestK8SClient extends SmallTest with Matchers {

  @Test
  def testCreatorEnumator(): Unit = {
    ImagePullPolicy.ALWAYS.toString shouldBe "Always"
    ImagePullPolicy.IFNOTPRESENT.toString shouldBe "IfNotPresent"
    ImagePullPolicy.NEVER.toString shouldBe "Never"
  }

  @Test
  def testPullPolicyIFNOTPRESENT(): Unit = {
    val nodeName = "ohara-it-02"
    val podName = "container1"
    val s = imagePolicyURL(nodeName, podName, ImagePullPolicy.IFNOTPRESENT)
    try {
      val client = K8SClient(s.url)
      try {
        val result: Option[ContainerInfo] = Await.result(
          client
            .containerCreator()
            .name(podName)
            .imageName("hello world")
            .labelName("ohara")
            .hostname("test1")
            .domainName("ohara")
            .pullImagePolicy(ImagePullPolicy.IFNOTPRESENT)
            .nodeName(nodeName)
            .threadPool(scala.concurrent.ExecutionContext.Implicits.global)
            .create(),
          30 seconds
        )
        result.get.name shouldBe podName
        result.get.environments shouldBe Map.empty
        result.get.nodeName shouldBe nodeName
      } finally client.close()
    } finally s.close()
  }

  @Test
  def testPullPolicyIsAlways(): Unit = {
    val nodeName = "ohara-it-02"
    val podName = "container1"
    val s = imagePolicyURL(nodeName, podName, ImagePullPolicy.ALWAYS)
    try {
      val client = K8SClient(s.url)
      try {
        val result: Option[ContainerInfo] = Await.result(
          client
            .containerCreator()
            .name(podName)
            .imageName("hello world")
            .labelName("ohara")
            .hostname("test1")
            .domainName("ohara")
            .pullImagePolicy(ImagePullPolicy.ALWAYS)
            .nodeName(nodeName)
            .create(),
          30 seconds
        )
        result.get.name shouldBe podName
        result.get.environments shouldBe Map.empty
        result.get.nodeName shouldBe nodeName
      } finally client.close()
    } finally s.close()
  }

  @Test
  def testPullPolicyIsNever(): Unit = {
    val nodeName = "ohara-it-02"
    val podName = "container1"
    val s = imagePolicyURL(nodeName, podName, ImagePullPolicy.NEVER)
    try {
      val client = K8SClient(s.url)
      try {
        val result: Option[ContainerInfo] = Await.result(
          client
            .containerCreator()
            .name(podName)
            .imageName("hello world")
            .labelName("ohara")
            .hostname("test1")
            .domainName("ohara")
            .pullImagePolicy(ImagePullPolicy.NEVER)
            .nodeName(nodeName)
            .create(),
          30 seconds
        )
        result.get.name shouldBe podName
        result.get.environments shouldBe Map.empty
        result.get.nodeName shouldBe nodeName
      } finally client.close()
    } finally s.close()
  }

  @Test
  def testPullPolicyNotSetting(): Unit = {
    val nodeName = "ohara-it-02"
    val podName = "container1"
    val s = imagePolicyURL(nodeName, podName, ImagePullPolicy.IFNOTPRESENT)
    try {
      val client = K8SClient(s.url)
      try {
        val result: Option[ContainerInfo] = Await.result(
          client
            .containerCreator()
            .name(podName)
            .imageName("hello world")
            .labelName("ohara")
            .hostname("test1")
            .domainName("ohara")
            .nodeName(nodeName)
            .create(),
          30 seconds
        )
        result.get.name shouldBe podName
        result.get.environments shouldBe Map.empty
        result.get.nodeName shouldBe nodeName
      } finally client.close()
    } finally s.close()
  }

  @Test
  def testImages(): Unit = {
    val node = CommonUtils.randomString()
    val images = Seq(CommonUtils.randomString(), CommonUtils.randomString())
    val plain = s"""
                 |{
                 |  "status": {
                 |    "addresses": [],
                 |    "images": [
                 |      {
                 |        "names": [${images.map(s => "\"" + s + "\"").mkString(",")}]
                 |      }
                 |    ],
                 |    "conditions": []
                 |  },
                 |  "metadata": {
                 |    "name": "${CommonUtils.randomString()}"
                 |  }
                 |}
               """.stripMargin

    // test json serialization
    val nodeItems: NodeItems = NODEITEMS_JSON_FORMAT.read(plain.parseJson)
    nodeItems.status.images.flatMap(_.names) shouldBe images
    nodeItems.status.addresses shouldBe Seq.empty
    nodeItems.status.conditions shouldBe Seq.empty

    // test communication
    val s = toServer {
      path("nodes" / Segment) { passedNode =>
        get {
          if (passedNode != node) complete(new IllegalArgumentException)
          else complete(nodeItems)
        }
      }
    }
    try {
      val client = K8SClient(s.url)
      try {
        val imagesFromServer = Await.result(client.images(node), 30 seconds)
        imagesFromServer shouldBe images
      } finally client.close()
    } finally s.close()
  }

  private[this] def imagePolicyURL(nodeName: String,
                                   podName: String,
                                   expectImagePullPolicy: ImagePullPolicy): SimpleServer = {
    val nodesResponse = s"""
                           |{"items": [
                           |    {
                           |      "metadata": {
                           |        "name": "${nodeName}"
                           |      },
                           |      "status": {
                           |        "conditions": [
                           |          {
                           |            "type": "Ready",
                           |            "status": "True",
                           |            "lastHeartbeatTime": "2019-05-14T06:14:46Z",
                           |            "lastTransitionTime": "2019-04-15T08:21:11Z",
                           |            "reason": "KubeletReady",
                           |            "message": "kubelet is posting ready status"
                           |          }
                           |        ],
                           |        "addresses": [
                           |          {
                           |            "type": "InternalIP",
                           |            "address": "10.2.0.4"
                           |          },
                           |          {
                           |            "type": "Hostname",
                           |            "address": "ohara-it-02"
                           |          }
                           |        ],
                           |        "images": [
                           |          {
                           |            "names": [
                           |              "quay.io/coreos/etcd@sha256:ea49a3d44a50a50770bff84eab87bac2542c7171254c4d84c609b8c66aefc211",
                           |              "quay.io/coreos/etcd:v3.3.9"
                           |            ],
                           |            "sizeBytes": 39156721
                           |          }
                           |        ]
                           |      }
                           |    }
                           |  ]
                           |}
                """.stripMargin

    val createPodResult = s"""
                             |{
                             |  "metadata": {
                             |    "name": "${podName}",
                             |    "uid": "aaaaaaaaaaaa",
                             |    "creationTimestamp": "2019-05-13 00:00:00"
                             |  },
                             |  "status": {
                             |    "phase": "true"
                             |  }
                             |}
                """.stripMargin

    // test json serialization
    val k8sNodeInfo: K8SNodeInfo = K8SNODEINFO_JSON_FORMAT.read(nodesResponse.parseJson)
    k8sNodeInfo.items(0).metadata.name shouldBe "ohara-it-02"
    k8sNodeInfo.items(0).status.images.size shouldBe 1

    // test communication
    toServer {
      path("nodes") {
        get {
          complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, nodesResponse)))
        }
      } ~
        path("namespaces" / "default" / "pods") {
          post {
            entity(as[CreatePod]) { createPod =>
              createPod.spec.containers(0).imagePullPolicy shouldBe expectImagePullPolicy
              complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, createPodResult)))
            }
          }
        }
    }
  }

  private[this] def toServer(route: server.Route): SimpleServer = {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    val server = Await.result(Http().bindAndHandle(route, "localhost", 0), 30 seconds)
    new SimpleServer {
      override def hostname: String = server.localAddress.getHostString
      override def port: Int = server.localAddress.getPort
      override def close(): Unit = {
        Await.result(server.unbind(), 30 seconds)
        Await.result(system.terminate(), 30 seconds)
      }
    }
  }
}
