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

package com.island.ohara.agent
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class TestContainerCollie extends SmallTest with Matchers {
  private[this] val fakeClusterName: String = FakeContainerCollie.clusterName
  private[this] val TIMEOUT = 10 seconds

  @Test
  def testAddNodeNameUpperCase(): Unit = {
    validErrorNodeName("Node1", "Your node name can't uppercase")
  }

  @Test
  def testAddNodeNameEmpty(): Unit = {
    validErrorNodeName("", "cluster and node name can't empty")
  }

  @Test
  def testAddNodeNormal(): Unit = {
    val nodeName1 = "node1"
    val nodeName2 = "node2"

    val node1 = Node(name = nodeName1,
                     port = 22,
                     user = "user1",
                     password = "123456",
                     services = Seq.empty,
                     lastModified = CommonUtils.current())
    val node2 = node1.copy(name = nodeName2)

    val container1 =
      ContainerInfo(nodeName1, "0", "fakeimage", "", "RUNNING", "", "container1", "0", Seq(), Map(), s"xxx")

    val fakeContainerCollie = new FakeContainerCollie(NodeCollie(Seq(node1, node2)), Seq(container1))
    val cluster = Await.result(fakeContainerCollie.cluster(FakeContainerCollie.clusterName), TIMEOUT)
    cluster._1.nodeNames.size shouldBe 1

    val result: FakeContainerCollieClusterInfo =
      Await.result(fakeContainerCollie.addNode(FakeContainerCollie.clusterName, nodeName2), TIMEOUT)
    result.nodeNames.size shouldBe 2
    result.nodeNames(0) shouldBe "node1"
    result.nodeNames(1) shouldBe "node2"
  }

  @Test
  def testRemoveEmptyNode(): Unit = {
    val fakeContainerCollie = new FakeContainerCollie(NodeCollie(Seq.empty), Seq.empty)
    val removeNode: Future[Boolean] = fakeContainerCollie.removeNode(fakeClusterName, "node1")
    Await.result(removeNode, TIMEOUT) shouldBe false
  }

  @Test
  def testRemoveContainerNodeNameNotExists(): Unit = {
    val node1Name = "node1"
    val node1 = Node(name = node1Name,
                     port = 22,
                     user = "user1",
                     password = "123456",
                     services = Seq.empty,
                     lastModified = CommonUtils.current())
    val containerInfo =
      ContainerInfo("node2", "0", "fakeimage", "", "RUNNING", "", "container1", "0", Seq.empty, Map.empty, s"xxx")
    val fakeContainerCollie = new FakeContainerCollie(NodeCollie(Seq(node1)), Seq(containerInfo))
    val removeNode: Future[Boolean] = fakeContainerCollie.removeNode(fakeClusterName, node1Name)
    Await.result(removeNode, TIMEOUT) shouldBe false
  }

  @Test
  def testRemoveSingleNode(): Unit = {
    val node1Name = "node1"
    val node1 = Node(name = node1Name,
                     port = 22,
                     user = "user1",
                     password = "123456",
                     services = Seq.empty,
                     lastModified = CommonUtils.current())
    val containerInfo =
      ContainerInfo(node1Name,
                    "0",
                    "fakeimage",
                    "",
                    "RUNNING",
                    "",
                    "container1",
                    "0",
                    Seq(),
                    Map(),
                    s"xxx-${node1Name}")
    val fakeContainerCollie = new FakeContainerCollie(NodeCollie(Seq(node1)), Seq(containerInfo))
    val removeNode: Future[Boolean] = fakeContainerCollie.removeNode(fakeClusterName, node1Name)
    intercept[IllegalArgumentException] {
      Await.result(removeNode, TIMEOUT)
    }.getMessage shouldBe s"${FakeContainerCollie.clusterName} is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead"
  }

  @Test
  def testRemoveNotExistsNode(): Unit = {
    val node1Name = "node1"
    val node1 = Node(name = node1Name,
                     port = 22,
                     user = "user1",
                     password = "123456",
                     services = Seq.empty,
                     lastModified = CommonUtils.current())
    val containerInfo =
      ContainerInfo(node1Name,
                    "0",
                    "fakeimage",
                    "",
                    "RUNNING",
                    "",
                    "container1",
                    "0",
                    Seq(),
                    Map(),
                    s"xxx-${node1Name}")
    val fakeContainerCollie = new FakeContainerCollie(NodeCollie(Seq(node1)), Seq(containerInfo))
    val removeNode: Future[Boolean] = fakeContainerCollie.removeNode(fakeClusterName, "node3")
    Await.result(removeNode, TIMEOUT) shouldBe false
  }

  @Test
  def testRemoveNodeNormal(): Unit = {
    val node1Name = "node1"
    val node2Name = "node2"

    val node1 = Node(name = node1Name,
                     port = 22,
                     user = "user1",
                     password = "123456",
                     services = Seq.empty,
                     lastModified = CommonUtils.current())
    val node2 = node1.copy(name = node2Name)
    val container1 =
      ContainerInfo(node1Name,
                    "0",
                    "fakeimage",
                    "",
                    "RUNNING",
                    "",
                    "container1",
                    "0",
                    Seq(),
                    Map(),
                    s"xxx-${node1Name}")

    val container2 =
      ContainerInfo(node2Name,
                    "0",
                    "fakeimage",
                    "",
                    "RUNNING",
                    "",
                    "container1",
                    "0",
                    Seq(),
                    Map(),
                    s"xxx-${node1Name}")

    val fakeContainerCollie = new FakeContainerCollie(NodeCollie(Seq(node1, node2)), Seq(container1, container2))
    val removeNode: Future[Boolean] = fakeContainerCollie.removeNode(fakeClusterName, "node1")
    Await.result(removeNode, TIMEOUT) shouldBe true
  }

  private[this] def validErrorNodeName(nodeName: String, expectErrMsg: String): Unit = {
    val node1 = Node(name = nodeName,
                     port = 22,
                     user = "user1",
                     password = "123456",
                     services = Seq.empty,
                     lastModified = CommonUtils.current())
    val containerInfo =
      ContainerInfo(node1.name,
                    "0",
                    "fakeimage",
                    "",
                    "RUNNING",
                    "",
                    "container1",
                    "0",
                    Seq(),
                    Map(),
                    s"xxx-${node1.name}")
    val fakeContainerCollie = new FakeContainerCollie(NodeCollie(Seq(node1)), Seq(containerInfo))
    val addNode: Future[FakeContainerCollieClusterInfo] = fakeContainerCollie.addNode(fakeClusterName, nodeName)
    intercept[IllegalArgumentException] {
      Await.result(addNode, TIMEOUT)
    }.getMessage shouldBe expectErrMsg
  }
}
