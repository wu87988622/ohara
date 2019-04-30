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

package com.island.ohara.it.shabondi

import com.island.ohara.agent.k8s.K8SClient
import com.island.ohara.client.configurator.v0.ContainerApi._
import com.island.ohara.client.configurator.v0.ShabondiApi
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.it.IntegrationTest
import org.junit.{Before, Test}
import org.scalatest.{Inside, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class TestShabondiK8S extends IntegrationTest with Matchers with Inside {

  private val K8S_API_SERVER_URL_KEY: String = "ohara.it.k8s"
  private val K8S_API_NODE_NAME_KEY: String = "ohara.it.k8s.nodename"

  assert(sys.env.contains(K8S_API_SERVER_URL_KEY), s"\nCannot find sys.env: $K8S_API_SERVER_URL_KEY")
  assert(sys.env.contains(K8S_API_NODE_NAME_KEY), s"\nCannot find sys.env: $K8S_API_NODE_NAME_KEY")

  private val k8sApiServerUrl = sys.env(K8S_API_SERVER_URL_KEY)
  private val k8sClient = K8SClient(k8sApiServerUrl)

  private val podLabelName = "shabondi"
  private val domainName = "default"
  private val hostname = "shabondi-host"
  private val podHostname = CommonUtils.uuid()
  private val random = new scala.util.Random

  private def awaitResult[T](f: Future[T]): T = Await.result(f, 20 seconds)

  private def k8sNodeName(): Option[String] = {
    val nodeNames: Array[String] = sys.env(K8S_API_NODE_NAME_KEY).split(',')
    nodeNames.size match {
      case s if s > 0 => Some(nodeNames(random.nextInt(s)))
      case _          => None
    }
  }

  @Before
  def setup(): Unit = {
    skipTest("Skip shabondi IT before k8s environment fix.")
  }

  @Test
  def testCreatAndRemovePod(): Unit = {
    val nodeName = k8sNodeName()
    assert(nodeName.isDefined, "Cannot find any k8s node name.")

    // create pod
    val containerCreator = awaitResult(k8sClient.containerCreator())
    val containerInfoOpt = awaitResult {
      containerCreator
        .imageName(ShabondiApi.IMAGE_NAME_DEFAULT)
        .portMappings(Map(
          9090 -> 8080
        ))
        .nodename(nodeName.get)
        .hostname(podHostname)
        .labelName(podLabelName)
        .domainName(domainName)
        .name(hostname)
        .run()
    }

    val containerInfo = containerInfoOpt.get
    containerInfo.portMappings should have size 1
    inside(containerInfo.portMappings.head) {
      case PortMapping(hostIp, portPairs) =>
        hostIp should be(podHostname)
        portPairs should be(Seq(PortPair(9090, 8080)))
    }

    await(() => {
      val containers = awaitResult(k8sClient.containers)
      val container = containers.filter { c =>
        c.hostname == podHostname
      }.head
      container.state == "RUNNING"
    })

//    // remove pod
    awaitResult(k8sClient.remove(podHostname))

    await(() => {
      val containers = awaitResult(k8sClient.containers)
      !containers.exists { c =>
        c.hostname == podHostname
      }
    })

  }

}
