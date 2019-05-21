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

package com.island.ohara.it.agent

import com.island.ohara.agent.StreamCollie
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.BrokerApi.{BrokerClusterCreationRequest, BrokerClusterInfo}
import com.island.ohara.client.configurator.v0.NodeApi.{Node, NodeCreationRequest}
import com.island.ohara.client.configurator.v0.StreamApi.{
  ActionAccess,
  ListAccess,
  StreamAppDescription,
  StreamPropertyRequest
}
import com.island.ohara.client.configurator.v0.TopicApi.{TopicCreationRequest, TopicInfo}
import com.island.ohara.client.configurator.v0.WorkerApi.{WorkerClusterCreationRequest, WorkerClusterInfo}
import com.island.ohara.client.configurator.v0.ZookeeperApi.{ZookeeperClusterCreationRequest, ZookeeperClusterInfo}
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.IntegrationTest
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

abstract class BasicTests4StreamApp extends IntegrationTest with Matchers {

  private[this] var nodeCache: Seq[Node] = _
  private[this] var nameHolder: ClusterNameHolder = _

  protected def createNodes(): Seq[Node]

  protected def createNameHolder(nodeCache: Seq[Node]): ClusterNameHolder

  protected def createConfigurator(nodeCache: Seq[Node]): Configurator

  private[this] var configurator: Configurator = _

  private[this] var zkApi: ClusterAccess[ZookeeperClusterCreationRequest, ZookeeperClusterInfo] = _
  private[this] var bkApi: ClusterAccess[BrokerClusterCreationRequest, BrokerClusterInfo] = _
  private[this] var wkApi: ClusterAccess[WorkerClusterCreationRequest, WorkerClusterInfo] = _
  private[this] var topicApi: Access[TopicCreationRequest, TopicInfo] = _

  private[this] var streamAppActionAccess: ActionAccess = _
  private[this] var streamAppListAccess: ListAccess = _
  private[this] var streamAppPropertyAccess: Access[StreamPropertyRequest, StreamAppDescription] = _
  private[this] var bkName: String = _
  private[this] var wkName: String = _
  private[this] val instances = 1

  @Before
  def setup(): Unit = {
    nodeCache = createNodes()
    if (nodeCache.isEmpty) skipTest(s"You must assign nodes for stream tests")
    else {
      nameHolder = createNameHolder(nodeCache)
      bkName = nameHolder.generateClusterName()
      wkName = nameHolder.generateClusterName()
      configurator = createConfigurator(nodeCache)
      zkApi = ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port)
      bkApi = BrokerApi.access().hostname(configurator.hostname).port(configurator.port)
      wkApi = WorkerApi.access().hostname(configurator.hostname).port(configurator.port)
      topicApi = TopicApi.access().hostname(configurator.hostname).port(configurator.port)
      streamAppActionAccess = StreamApi.accessOfAction().hostname(configurator.hostname).port(configurator.port)
      streamAppListAccess = StreamApi.accessOfList().hostname(configurator.hostname).port(configurator.port)
      streamAppPropertyAccess = StreamApi.accessOfProperty().hostname(configurator.hostname).port(configurator.port)
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
            name = nameHolder.generateClusterName(),
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
    val jarPath = CommonUtils.path(System.getProperty("user.dir"), "build", "libs", "ohara-streamapp.jar")

    // We make sure the broker cluster exists again (for create topic)
    assertCluster(() => result(bkApi.list), bkName)

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
    jarInfo.head.name shouldBe "ohara-streamapp.jar"

    // Create streamApp properties
    val stream = result(streamAppPropertyAccess.add(StreamPropertyRequest(jarInfo.head.id, None, None, None, None)))

    // Update streamApp properties
    val req = StreamPropertyRequest(
      jarInfo.head.id,
      Some(CommonUtils.randomString(10)),
      Some(Seq(topic1.id)),
      Some(Seq(topic2.id)),
      Some(instances)
    )
    val properties = result(
      streamAppPropertyAccess.update(stream.id, req)
    )
    properties.from.size shouldBe 1
    properties.to.size shouldBe 1
    properties.instances shouldBe instances
    properties.state shouldBe None
    properties.error shouldBe None
    properties.workerClusterName shouldBe wkName

    //Start streamApp
    val res1 =
      result(streamAppActionAccess.start(stream.id))
    res1.id shouldBe stream.id
    res1.state.get shouldBe ContainerState.RUNNING.name
    res1.error shouldBe None

    // save the cluster name to cache
    nameHolder.addClusterName(StreamCollie.formatUniqueName(res1.id))

    //Stop streamApp
    val res2 =
      result(streamAppActionAccess.stop(stream.id))
    res2.state.isEmpty shouldBe true
    res2.error shouldBe None
  }

  @After
  def cleanUp(): Unit = {
    Releasable.close(configurator)
    Releasable.close(nameHolder)
  }
}
