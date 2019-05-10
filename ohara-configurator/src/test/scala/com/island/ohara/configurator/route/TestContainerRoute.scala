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

package com.island.ohara.configurator.route

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterCreationRequest
import com.island.ohara.client.configurator.v0.NodeApi.NodeCreationRequest
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterCreationRequest
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterCreationRequest
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class TestContainerRoute extends MediumTest with Matchers {
  private[this] val configurator = Configurator.builder().fake(0, 0).build()
  private[this] val containerApi = ContainerApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] val brokerApi = BrokerApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] val workerApi = WorkerApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] val zkClusterName = CommonUtils.randomString(10)
  private[this] val bkClusterName = CommonUtils.randomString(10)
  private[this] val wkClusterName = CommonUtils.randomString(10)

  private[this] val nodeNames: Seq[String] = Seq("n0", "n1")

  @Before
  def setup(): Unit = {
    val nodeAccess = NodeApi.access().hostname(configurator.hostname).port(configurator.port)

    nodeNames.isEmpty shouldBe false
    nodeNames.foreach { n =>
      result(
        nodeAccess.add(
          NodeCreationRequest(
            name = Some(n),
            port = 22,
            user = "user",
            password = "pwd"
          )))
    }

    result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperClusterCreationRequest(
          name = zkClusterName,
          imageName = None,
          clientPort = Some(CommonUtils.availablePort()),
          electionPort = Some(CommonUtils.availablePort()),
          peerPort = Some(CommonUtils.availablePort()),
          nodeNames = nodeNames
        ))).name shouldBe zkClusterName

    result(
      brokerApi.add(
        BrokerClusterCreationRequest(
          name = bkClusterName,
          imageName = None,
          zookeeperClusterName = Some(zkClusterName),
          exporterPort = None,
          clientPort = None,
          jmxPort = None,
          nodeNames = nodeNames
        )))

    result(
      workerApi.add(WorkerClusterCreationRequest(
        name = wkClusterName,
        imageName = None,
        brokerClusterName = Some(bkClusterName),
        jmxPort = None,
        clientPort = None,
        groupId = None,
        configTopicName = None,
        configTopicReplications = None,
        offsetTopicName = None,
        offsetTopicPartitions = None,
        offsetTopicReplications = None,
        statusTopicName = None,
        statusTopicPartitions = None,
        statusTopicReplications = None,
        jarIds = Seq.empty,
        nodeNames = nodeNames
      )))
  }

  @Test
  def testGetContainersOfZookeeperCluster(): Unit = {
    val containerGroups = result(containerApi.get(zkClusterName))
    containerGroups.size should not be 0
    containerGroups.foreach(group => {
      group.clusterName shouldBe zkClusterName
      group.clusterType shouldBe "zookeeper"
      group.containers.size should not be 0
    })
  }

  @Test
  def testGetContainersOfBrokerCluster(): Unit = {
    val containerGroups = result(containerApi.get(bkClusterName))
    containerGroups.size should not be 0
    containerGroups.foreach(group => {
      group.clusterName shouldBe bkClusterName
      group.clusterType shouldBe "broker"
      group.containers.size should not be 0
    })
  }

  @Test
  def testGetContainersOfWorkerCluster(): Unit = {
    val containerGroups = result(containerApi.get(wkClusterName))
    containerGroups.size should not be 0
    containerGroups.foreach(group => {
      group.clusterName shouldBe wkClusterName
      group.clusterType shouldBe "worker"
      group.containers.size should not be 0
    })
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
