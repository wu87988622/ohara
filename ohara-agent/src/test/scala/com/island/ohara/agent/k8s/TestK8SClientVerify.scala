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

import com.island.ohara.agent.fake.FakeK8SClient
import com.island.ohara.agent.{DataCollie, ServiceCollie}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.OharaTest
import org.junit.Test
import org.scalatest.Matchers._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TestK8SClientVerify extends OharaTest {
  private[this] val nodeCache              = new ArrayBuffer[Node]()
  private[this] val dataCollie: DataCollie = DataCollie(nodeCache)

  private[this] def node: Node = Node("ohara")

  @Test
  def testMockK8sClientVerifyNode1(): Unit = {
    val fakeK8SClient = new FakeK8SClient(true, Option(K8SStatusInfo(true, "")), "")
    val serviceCollie: ServiceCollie =
      ServiceCollie.k8sModeBuilder.dataCollie(dataCollie).k8sClient(fakeK8SClient).build()
    Await.result(
      serviceCollie.verifyNode(node),
      30 seconds
    ) shouldBe "ohara node is running."
  }

  @Test
  def testMockK8sClientVerifyNode2(): Unit = {
    val fakeK8SClient = new FakeK8SClient(true, Option(K8SStatusInfo(false, "node failed.")), "")
    val serviceCollie: ServiceCollie =
      ServiceCollie.k8sModeBuilder.dataCollie(dataCollie).k8sClient(fakeK8SClient).build()
    intercept[IllegalStateException] {
      Await.result(
        serviceCollie.verifyNode(node),
        30 seconds
      )
    }.getMessage shouldBe "ohara node doesn't running container. cause: node failed."
  }

  @Test
  def testMockK8sClientVerifyNode3(): Unit = {
    val fakeK8SClient = new FakeK8SClient(false, Option(K8SStatusInfo(false, "failed")), "")
    val serviceCollie: ServiceCollie =
      ServiceCollie.k8sModeBuilder.dataCollie(dataCollie).k8sClient(fakeK8SClient).build()
    intercept[IllegalStateException] {
      Await.result(
        serviceCollie.verifyNode(node),
        30 seconds
      )
    }.getMessage shouldBe "ohara node doesn't running container. cause: failed"
  }

  @Test
  def testMockK8SClientVerifyNode4(): Unit = {
    val fakeK8SClient = new FakeK8SClient(false, None, "")
    val serviceCollie: ServiceCollie =
      ServiceCollie.k8sModeBuilder.dataCollie(dataCollie).k8sClient(fakeK8SClient).build()
    intercept[IllegalStateException] {
      Await.result(
        serviceCollie.verifyNode(node),
        30 seconds
      )
    }.getMessage shouldBe "ohara node doesn't running container. cause: ohara node doesn't exists."
  }
}
