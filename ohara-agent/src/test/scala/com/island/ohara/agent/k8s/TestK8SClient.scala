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
import scala.concurrent.ExecutionContext.Implicits.global
class TestK8SClient extends SmallTest with Matchers {

  @Test
  def testCreatorEnumator(): Unit = {
    ImagePullPolicy.ALWAYS.toString shouldBe "Always"
    ImagePullPolicy.IFNOTPRESENT.toString shouldBe "IfNotPresent"
    ImagePullPolicy.NEVER.toString shouldBe "Never"
  }

  @Test
  def testCreatePodContainerNonePolicy(): Unit = {
    val json: String =
      CreatePodContainer("podName", "image", Seq(), Seq(), ImagePullPolicy.IFNOTPRESENT, Seq("hello"), Seq("world")).toJson.toString
    json shouldBe "{\"name\":\"podName\",\"image\":\"image\",\"ports\":[],\"command\":[\"hello\"],\"args\":[\"world\"],\"imagePullPolicy\":\"IfNotPresent\",\"env\":[]}"
  }

  @Test
  def testPolicyIsAlways(): Unit = {
    val json: String =
      CreatePodContainer("podName", "image", Seq(), Seq(), ImagePullPolicy.ALWAYS, Seq(), Seq()).toJson.toString
    json shouldBe "{\"name\":\"podName\",\"image\":\"image\",\"ports\":[],\"command\":[],\"args\":[],\"imagePullPolicy\":\"Always\",\"env\":[]}"
  }

  @Test
  def testPolicyIsNever(): Unit = {
    val json: String =
      CreatePodContainer("podName", "image", Seq(), Seq(), ImagePullPolicy.NEVER, Seq("foo"), Seq("bar")).toJson.toString
    json shouldBe "{\"name\":\"podName\",\"image\":\"image\",\"ports\":[],\"command\":[\"foo\"],\"args\":[\"bar\"],\"imagePullPolicy\":\"Never\",\"env\":[]}"
  }

  @Test
  def testPolicyIsIfNotPresent(): Unit = {
    val json: String =
      CreatePodContainer("podName", "image", Seq(), Seq(), ImagePullPolicy.IFNOTPRESENT, Seq(), Seq()).toJson.toString
    json shouldBe "{\"name\":\"podName\",\"image\":\"image\",\"ports\":[],\"command\":[],\"args\":[],\"imagePullPolicy\":\"IfNotPresent\",\"env\":[]}"
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
