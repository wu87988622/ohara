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
import com.island.ohara.agent.{ServiceCollie, DataCollie}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class TestK8SClientVerify extends OharaTest with Matchers {
  private[this] val nodeCache = new ArrayBuffer[Node]()
  private[this] val dataCollie: DataCollie = DataCollie(nodeCache)

  private[this] def node: Node = Node(
    hostname = "ohara",
    port = Some(22),
    user = Some("fake"),
    password = Some("fake"),
    services = Seq.empty,
    lastModified = CommonUtils.current(),
    validationReport = None,
    tags = Map.empty
  )

  @Test
  def testMockK8sClientVerifyNode1(): Unit = {
    val fakeK8SClient = new FakeK8SClient(true, Option(K8SStatusInfo(true, "")), "")
    val serviceCollie: ServiceCollie =
      ServiceCollie.builderOfK8s().dataCollie(dataCollie).k8sClient(fakeK8SClient).build()
    val runningNode =
      Await.result(
        serviceCollie.verifyNode(node),
        30 seconds
      )
    runningNode match {
      case Success(value) => value shouldBe "ohara node is running."
      case Failure(e)     => throw new AssertionError()
    }
  }

  @Test
  def testMockK8sClientVerifyNode2(): Unit = {
    val fakeK8SClient = new FakeK8SClient(true, Option(K8SStatusInfo(false, "node failed.")), "")
    val serviceCollie: ServiceCollie =
      ServiceCollie.builderOfK8s().dataCollie(dataCollie).k8sClient(fakeK8SClient).build()
    val runningNode =
      Await.result(
        serviceCollie.verifyNode(node),
        30 seconds
      )
    runningNode match {
      case Success(value) => throw new AssertionError()
      case Failure(e)     => e.getMessage shouldBe "ohara node doesn't running container. cause: node failed."
    }
  }

  @Test
  def testMockK8sClientVerifyNode3(): Unit = {
    val fakeK8SClient = new FakeK8SClient(false, Option(K8SStatusInfo(false, "failed")), "")
    val serviceCollie: ServiceCollie =
      ServiceCollie.builderOfK8s().dataCollie(dataCollie).k8sClient(fakeK8SClient).build()
    val runningNode =
      Await.result(
        serviceCollie.verifyNode(node),
        30 seconds
      )
    runningNode match {
      case Success(value) => throw new AssertionError()
      case Failure(e)     => e.getMessage() shouldBe "ohara node doesn't running container. cause: failed"
    }
  }

  @Test
  def testMockK8SClientVerifyNode4(): Unit = {
    val fakeK8SClient = new FakeK8SClient(false, None, "")
    val serviceCollie: ServiceCollie =
      ServiceCollie.builderOfK8s().dataCollie(dataCollie).k8sClient(fakeK8SClient).build()
    val runningNode =
      Await.result(
        serviceCollie.verifyNode(
          Node(
            hostname = "ohara",
            port = Some(22),
            user = Some("fake"),
            password = Some("fake"),
            services = Seq.empty,
            lastModified = CommonUtils.current(),
            validationReport = None,
            tags = Map.empty
          )),
        30 seconds
      )
    runningNode match {
      case Success(value) => throw new AssertionError()
      case Failure(e) =>
        e.getMessage() shouldBe "ohara node doesn't running container. cause: ohara node doesn't exists."
    }
  }
}
