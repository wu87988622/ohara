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
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestCollie extends OharaTest {
  private[this] val fakeClusterKey: ObjectKey = FakeCollie.key
  private[this] val TIMEOUT                   = 10 seconds

  private[this] def node(hostname: String): Node = Node(
    hostname = hostname,
    port = Some(22),
    user = Some("user1"),
    password = Some("123456"),
    services = Seq.empty,
    lastModified = CommonUtils.current(),
    resources = Seq.empty,
    tags = Map.empty
  )

  @Test
  def testRemoveEmptyNode(): Unit = {
    val fakeCollie                  = new FakeCollie(DataCollie(Seq.empty), Seq.empty)
    val removeNode: Future[Boolean] = fakeCollie.removeNode(fakeClusterKey, "node1")
    Await.result(removeNode, TIMEOUT) shouldBe false
  }

  @Test
  def testRemoveContainerNodeNameNotExists(): Unit = {
    val node1Name = "node1"
    val node1     = node(node1Name)
    val containerInfo =
      ContainerInfo("node2", "0", "fakeimage", "RUNNING", "", "container1", -1, Seq.empty, Map.empty, s"xxx")
    val fakeCollie                  = new FakeCollie(DataCollie(Seq(node1)), Seq(containerInfo))
    val removeNode: Future[Boolean] = fakeCollie.removeNode(fakeClusterKey, node1Name)
    Await.result(removeNode, TIMEOUT) shouldBe false
  }

  @Test
  def testRemoveSingleNode(): Unit = {
    val node1Name = "node1"
    val node1     = node(node1Name)
    val containerInfo =
      ContainerInfo(node1Name, "0", "fakeimage", "RUNNING", "", "container1", -1, Seq(), Map(), s"xxx-$node1Name")
    val fakeCollie                  = new FakeCollie(DataCollie(Seq(node1)), Seq(containerInfo))
    val removeNode: Future[Boolean] = fakeCollie.removeNode(fakeClusterKey, node1Name)
    intercept[IllegalArgumentException] {
      Await.result(removeNode, TIMEOUT)
    }.getMessage shouldBe s"cluster [${FakeCollie.key}] is a single-node cluster. You can't remove the last node by removeNode(). Please use remove(clusterName) instead"
  }

  @Test
  def testRemoveNotExistsNode(): Unit = {
    val node1Name = "node1"
    val node1     = node(node1Name)
    val containerInfo =
      ContainerInfo(node1Name, "0", "fakeimage", "RUNNING", "", "container1", -1, Seq(), Map(), s"xxx-$node1Name")
    val fakeCollie                  = new FakeCollie(DataCollie(Seq(node1)), Seq(containerInfo))
    val removeNode: Future[Boolean] = fakeCollie.removeNode(fakeClusterKey, "node3")
    Await.result(removeNode, TIMEOUT) shouldBe false
  }

  @Test
  def testRemoveNodeNormal(): Unit = {
    val node1Name = "node1"
    val node2Name = "node2"

    val node1 = node(node1Name)
    val node2 = node(node2Name)
    val container1 =
      ContainerInfo(node1Name, "0", "fakeimage", "RUNNING", "", "container1", -1, Seq(), Map(), s"xxx-$node1Name")

    val container2 =
      ContainerInfo(node2Name, "0", "fakeimage", "RUNNING", "", "container1", -1, Seq(), Map(), s"xxx-$node1Name")

    val fakeCollie                  = new FakeCollie(DataCollie(Seq(node1, node2)), Seq(container1, container2))
    val removeNode: Future[Boolean] = fakeCollie.removeNode(fakeClusterKey, "node1")
    Await.result(removeNode, TIMEOUT) shouldBe true
  }

  @Test
  def testClusterState(): Unit = {
    val node1Name = "node1"
    val node1     = node(node1Name)

    // case : all containers are running => cluster running
    val containers = (0 until 5).map { index =>
      ContainerInfo(
        nodeName = node1Name,
        id = s"$index",
        imageName = "fakeimage",
        state = ContainerState.RUNNING.name,
        kind = "",
        name = s"container-$index",
        size = -1,
        portMappings = Seq(),
        environments = Map(),
        hostname = s"xxx-$node1Name"
      )
    }
    val fakeRunning = new FakeCollie(DataCollie(Seq(node1)), containers)
    Await.result(fakeRunning.clusterWithAllContainers(), TIMEOUT).keys.head.state shouldBe Some(
      ServiceState.RUNNING.name
    )

    // case : some containers are failed but one running => cluster running
    val fakeRunning1 =
      new FakeCollie(DataCollie(Seq(node1)), containers :+ containers.head.copy(state = ContainerState.DEAD.name))
    Await.result(fakeRunning1.clusterWithAllContainers(), TIMEOUT).keys.head.state shouldBe Some(
      ServiceState.RUNNING.name
    )

    // case : some containers are failed but one running => cluster running
    val fakeRunning2 =
      new FakeCollie(DataCollie(Seq(node1)), containers :+ containers.head.copy(state = ContainerState.EXITED.name))
    Await.result(fakeRunning2.clusterWithAllContainers(), TIMEOUT).keys.head.state shouldBe Some(
      ServiceState.RUNNING.name
    )

    // case : one container is pending => cluster pending
    val fakePending =
      new FakeCollie(DataCollie(Seq(node1)), containers :+ containers.head.copy(state = ContainerState.CREATED.name))
    Await.result(fakePending.clusterWithAllContainers(), TIMEOUT).keys.head.state shouldBe Some(
      ServiceState.PENDING.name
    )

    // case : all containers are dead => cluster failed
    val containers2 = (0 until 5).map { index =>
      ContainerInfo(
        nodeName = node1Name,
        id = s"$index",
        imageName = "fakeimage",
        state = ContainerState.DEAD.name,
        kind = "",
        name = s"container-$index",
        size = -1,
        portMappings = Seq(),
        environments = Map(),
        hostname = s"xxx-$node1Name"
      )
    }
    val fakeFailed = new FakeCollie(DataCollie(Seq(node1)), containers2)
    Await.result(fakeFailed.clusterWithAllContainers(), TIMEOUT).keys.head.state shouldBe Some(ServiceState.FAILED.name)

    // case : some containers are exit but others are dead => cluster failed
    val fakeFailed1 =
      new FakeCollie(DataCollie(Seq(node1)), containers2 :+ containers2.head.copy(state = ContainerState.EXITED.name))
    Await.result(fakeFailed1.clusterWithAllContainers(), TIMEOUT).keys.head.state shouldBe Some(
      ServiceState.FAILED.name
    )

    // case : some containers is running but others are dead => cluster running
    val fakeFailed2 =
      new FakeCollie(DataCollie(Seq(node1)), containers2 :+ containers2.head.copy(state = ContainerState.RUNNING.name))
    Await.result(fakeFailed2.clusterWithAllContainers(), TIMEOUT).keys.head.state shouldBe Some(
      ServiceState.RUNNING.name
    )
  }

  @Test
  def dividerCannotBeUsedInCreatingContainerName(): Unit = {
    an[IllegalArgumentException] should be thrownBy Collie
      .containerName(Collie.DIVIDER, CommonUtils.randomString(), CommonUtils.randomString(), CommonUtils.randomString())

    an[IllegalArgumentException] should be thrownBy Collie
      .containerName(CommonUtils.randomString(), Collie.DIVIDER, CommonUtils.randomString(), CommonUtils.randomString())

    an[IllegalArgumentException] should be thrownBy Collie
      .containerName(CommonUtils.randomString(), CommonUtils.randomString(), Collie.DIVIDER, CommonUtils.randomString())

    an[IllegalArgumentException] should be thrownBy Collie
      .containerName(CommonUtils.randomString(), CommonUtils.randomString(), CommonUtils.randomString(), Collie.DIVIDER)
  }

  @Test
  def dividerCannotBeUsedInCreatingHostname(): Unit = {
    an[IllegalArgumentException] should be thrownBy Collie.containerHostName(
      Collie.DIVIDER,
      CommonUtils.randomString(),
      CommonUtils.randomString(),
      CommonUtils.randomString()
    )

    an[IllegalArgumentException] should be thrownBy Collie.containerHostName(
      CommonUtils.randomString(),
      Collie.DIVIDER,
      CommonUtils.randomString(),
      CommonUtils.randomString()
    )

    an[IllegalArgumentException] should be thrownBy Collie.containerHostName(
      CommonUtils.randomString(),
      CommonUtils.randomString(),
      Collie.DIVIDER,
      CommonUtils.randomString()
    )

    an[IllegalArgumentException] should be thrownBy Collie.containerHostName(
      CommonUtils.randomString(),
      CommonUtils.randomString(),
      CommonUtils.randomString(),
      Collie.DIVIDER
    )
  }

  @Test
  def testLengthOfHostname(): Unit = {
    val s = Collie.containerHostName(
      CommonUtils.randomString(Collie.LENGTH_OF_CONTAINER_HOSTNAME),
      CommonUtils.randomString(),
      CommonUtils.randomString(),
      CommonUtils.randomString()
    )
    s.length shouldBe Collie.LENGTH_OF_CONTAINER_HOSTNAME
  }

  @Test
  def hostnameShouldNotStartWithDivider(): Unit = {
    val s = Collie.containerHostName(
      "a",
      "b",
      "c",
      CommonUtils.randomString(
        Collie.LENGTH_OF_CONTAINER_HOSTNAME - Collie.LENGTH_OF_CONTAINER_NAME_ID
        // the length of other names
          - 3
        // the length of all DIVIDER
          - 4
        // this +1 makes length of hostname be 51 and the first "a" will be removed
          + 1
      )
    )
    s.length shouldBe Collie.LENGTH_OF_CONTAINER_HOSTNAME - 1
    s.startsWith(Collie.DIVIDER) shouldBe false
  }
}
