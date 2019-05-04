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

package com.island.ohara.it.agent.ssh

import java.io.File

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterCreationRequest
import com.island.ohara.client.configurator.v0.NodeApi.{Node, NodeCreationRequest}
import com.island.ohara.client.configurator.v0.StreamApi.StreamPropertyRequest
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterCreationRequest
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterCreationRequest
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.IntegrationTest
import com.island.ohara.it.agent.{ClusterNameHolder, CollieTestUtils}
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestSshStreamApp extends IntegrationTest with Matchers {

  private[this] var bkName: String = _
  private[this] var wkName: String = _
  private[this] val instances = 1

  private[this] val nodeCache: Seq[Node] = CollieTestUtils.nodeCache()
  private[this] val nameHolder = new ClusterNameHolder(nodeCache)
  private[this] var configurator: Configurator = _

  private[this] def zkApi = ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] def bkApi = BrokerApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] def wkApi = WorkerApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] def topicApi = TopicApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def streamAppActionAccess =
    StreamApi.accessOfAction().hostname(configurator.hostname).port(configurator.port)
  private[this] def streamAppListAccess =
    StreamApi.accessOfList().hostname(configurator.hostname).port(configurator.port)
  private[this] def streamAppPropertyAccess =
    StreamApi.accessOfProperty().hostname(configurator.hostname).port(configurator.port)

  private[this] def generateClusterName(): String = nameHolder.generateClusterName()

  @Before
  def setup(): Unit = {
    if (nodeCache.isEmpty) skipTest(s"You must assign nodes for stream tests")
    else {
      bkName = generateClusterName()
      wkName = generateClusterName()
      configurator = Configurator.builder().build()
      val nodeApi = NodeApi.access().hostname(configurator.hostname).port(configurator.port)

      // add all available nodes
      nodeCache.foreach { node =>
        result(
          nodeApi.add(
            NodeCreationRequest(
              name = Some(node.name),
              port = node.port,
              user = node.user,
              password = node.password
            )
          )
        )
      }
      val nodes = result(nodeApi.list)
      nodes.size shouldBe nodeCache.size
      nodeCache.forall(node => nodes.map(_.name).contains(node.name)) shouldBe true

      // create zookeeper cluster
      val zkCluster = result(
        zkApi.add(
          ZookeeperClusterCreationRequest(
            name = generateClusterName(),
            clientPort = Some(CommonUtils.availablePort()),
            electionPort = Some(CommonUtils.availablePort()),
            peerPort = Some(CommonUtils.availablePort()),
            imageName = None,
            nodeNames = nodeCache.take(1).map(_.name)
          )
        ))
      assertCluster(() => result(zkApi.list), zkCluster.name)

      // create broker cluster
      val bkCluster = result(
        bkApi.add(
          BrokerClusterCreationRequest(
            name = bkName,
            clientPort = Some(CommonUtils.availablePort()),
            exporterPort = Some(CommonUtils.availablePort()),
            jmxPort = Some(CommonUtils.availablePort()),
            imageName = None,
            zookeeperClusterName = Some(zkCluster.name),
            nodeNames = nodeCache.take(1).map(_.name)
          )
        ))
      assertCluster(() => result(bkApi.list), bkCluster.name)

      // create worker cluster
      val wkCluster = result(
        wkApi.add(
          WorkerClusterCreationRequest(
            name = wkName,
            imageName = None,
            brokerClusterName = Some(bkCluster.name),
            clientPort = Some(CommonUtils.availablePort()),
            jmxPort = Some(CommonUtils.availablePort()),
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
            nodeNames = nodeCache.take(instances).map(_.name)
          )
        ))
      assertCluster(() => result(wkApi.list), wkCluster.name)
    }
  }

  @Test
  def testRunSimpleStreamApp(): Unit = {
    val from = "fromTopic"
    val to = "toTopic"
    val jarPath =
      s"${System.getProperty("user.dir")}${File.separator}build${File.separator}libs${File.separator}ohara-streamapp.jar"

    // create topic
    val topic1 = result(
      topicApi.add(
        TopicCreationRequest(name = Some(from),
                             brokerClusterName = Some(bkName),
                             numberOfPartitions = None,
                             numberOfReplications = None)
      ))
    val topic2 = result(
      topicApi.add(
        TopicCreationRequest(name = Some(to),
                             brokerClusterName = Some(bkName),
                             numberOfPartitions = None,
                             numberOfReplications = None)
      ))

    // Upload streamApp jar
    val jarInfo = result(
      streamAppListAccess.upload(Seq(jarPath), Some(wkName))
    )
    jarInfo.size shouldBe 1
    jarInfo.head.jarName shouldBe "ohara-streamapp.jar"

    // Update streamApp properties
    val req = StreamPropertyRequest(
      CommonUtils.randomString(10),
      Seq(topic1.id),
      Seq(topic2.id),
      instances
    )
    val properties = result(
      streamAppPropertyAccess.update(jarInfo.head.id, req)
    )
    properties.from.size shouldBe 1
    properties.to.size shouldBe 1
    properties.instances shouldBe instances
    properties.state shouldBe None
    properties.error shouldBe None
    properties.workerClusterName shouldBe wkName

    //Start streamApp
    val res1 =
      result(streamAppActionAccess.start(jarInfo.head.id))
    res1.id shouldBe jarInfo.head.id
    res1.state.get shouldBe ContainerState.RUNNING.name
    res1.error shouldBe None

    //Stop streamApp
    val res2 =
      result(streamAppActionAccess.stop(jarInfo.head.id))
    res2.state.isEmpty shouldBe true
    res2.error shouldBe None
  }

  @After
  def cleanUp(): Unit = {
    Releasable.close(nameHolder)
    Releasable.close(configurator)
  }
}
