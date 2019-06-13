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

import java.util.concurrent.ExecutionException

import com.island.ohara.agent.StreamCollie
import com.island.ohara.agent.docker.{ContainerState, DockerClient}
import com.island.ohara.client.configurator.v0.BrokerApi.{BrokerClusterCreationRequest, BrokerClusterInfo}
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi.{
  ActionAccess,
  ListAccess,
  StreamAppDescription,
  StreamPropertyRequest
}
import com.island.ohara.client.configurator.v0.WorkerApi.{WorkerClusterCreationRequest, WorkerClusterInfo}
import com.island.ohara.client.configurator.v0.ZookeeperApi.{ZookeeperClusterCreationRequest, ZookeeperClusterInfo}
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.IntegrationTest
import com.island.ohara.kafka.Producer
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

abstract class BasicTests4StreamApp extends IntegrationTest with Matchers {

  private[this] val log = Logger(classOf[BasicTests4StreamApp])

  /**
    * we need to export port to enable remote node download jar from this node
    */
  private[this] val portKey = "ohara.it.port"

  /**
    * we need to export hostname to enable remote node download jar from this node
    */
  private[this] val hostnameKey: String = "ohara.it.hostname"

  private[this] val invalidHostname = "unknown"
  private[this] val invalidPort = 0
  private[this] val publicHostname: String = sys.env.getOrElse(hostnameKey, invalidHostname)
  private[this] val publicPort = sys.env.get(portKey).map(_.toInt).getOrElse(invalidPort)

  private[this] var nodeCache: Seq[Node] = _
  private[this] var nameHolder: ClusterNameHolder = _

  protected def createNodes(): Seq[Node]

  protected def createNameHolder(nodeCache: Seq[Node]): ClusterNameHolder

  protected def createConfigurator(nodeCache: Seq[Node], hostname: String, port: Int): Configurator

  private[this] var configurator: Configurator = _

  private[this] var zkApi: ClusterAccess[ZookeeperClusterCreationRequest, ZookeeperClusterInfo] = _
  private[this] var bkApi: ClusterAccess[BrokerClusterCreationRequest, BrokerClusterInfo] = _
  private[this] var wkApi: ClusterAccess[WorkerClusterCreationRequest, WorkerClusterInfo] = _
  private[this] var topicApi: com.island.ohara.client.configurator.v0.TopicApi.Access = _

  private[this] var streamAppActionAccess: ActionAccess = _
  private[this] var streamAppListAccess: ListAccess = _
  private[this] var streamAppPropertyAccess: Access[StreamPropertyRequest, StreamAppDescription] = _
  private[this] var bkName: String = _
  private[this] var wkName: String = _
  private[this] var brokerConnProps: String = _
  private[this] val instances = 1

  @Before
  def setup(): Unit = {
    nodeCache = createNodes()
    if (nodeCache.isEmpty || publicPort == invalidPort || publicHostname == invalidHostname) {
      skipTest(
        s"${CollieTestUtils.key}, $portKey and $hostnameKey don't exist so all tests in BasicTests4StreamApp are ignored"
      )
    } else {
      nameHolder = createNameHolder(nodeCache)
      bkName = nameHolder.generateClusterName()
      wkName = nameHolder.generateClusterName()
      configurator = createConfigurator(nodeCache, publicHostname, publicPort)
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
          nodeApi.request().name(node.name).port(node.port).user(node.user).password(node.password).create()
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
      brokerConnProps = bkCluster.connectionProps

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
  def testFailedClusterRemoveGracefully(): Unit = {

    // create fake jar
    val jarPath = CommonUtils.createTempFile("fake").getAbsolutePath

    // upload streamApp jar
    val jarInfo = result(
      streamAppListAccess.upload(Seq(jarPath), Some(wkName))
    )

    // create streamApp properties
    val stream = result(streamAppPropertyAccess.add(StreamPropertyRequest(jarInfo.head.id, None, None, None, None)))

    // update streamApp properties (use non-existed topics to make sure cluster failed)
    val req = StreamPropertyRequest(
      jarInfo.head.id,
      Some(CommonUtils.randomString(10)),
      Some(Seq("bar-fake")),
      Some(Seq("foo-fake")),
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

    // start streamApp
    result(streamAppActionAccess.start(stream.id))

    // get the actually container names
    val map = nodeCache.map { node =>
      val client =
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
      try {
        node -> client.containerNames().filter(name => name.contains(StreamCollie.formatUniqueName(properties.id)))
      } finally client.close()
    }

    // we only have one instance, container exited means cluster dead (the state here uses container state is ok
    // since we use the same name for cluster state
    await(() => {
      val res = result(streamAppPropertyAccess.get(stream.id))
      res.state.isDefined && res.state.get == ContainerState.DEAD.name
    })

    // stop and remove failed cluster gracefully
    result(streamAppActionAccess.stop(stream.id))

    // check the containers are all removed
    map.foreach {
      case (node, containers) =>
        val client =
          DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
        try containers.foreach(container => client.nonExist(container) shouldBe true)
        finally client.close()
    }
  }

  @Test
  def testRunSimpleStreamApp(): Unit = {
    val from = "fromTopic"
    val to = "toTopic"
    val jarPath = CommonUtils.path(System.getProperty("user.dir"), "build", "libs", "ohara-streamapp.jar")

    // we make sure the broker cluster exists again (for create topic)
    assertCluster(() => result(bkApi.list), bkName)

    // create topic
    val topic1 = result(topicApi.request().name(from).brokerClusterName(bkName).create())
    val topic2 = result(topicApi.request().name(to).brokerClusterName(bkName).create())

    // upload streamApp jar
    val jarInfo = result(
      streamAppListAccess.upload(Seq(jarPath), Some(wkName))
    )
    jarInfo.size shouldBe 1
    jarInfo.head.name shouldBe "ohara-streamapp.jar"

    // create streamApp properties
    val stream = result(streamAppPropertyAccess.add(StreamPropertyRequest(jarInfo.head.id, None, None, None, None)))

    // update streamApp properties
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

    // get streamApp property (cluster not create yet, hence no state)
    val getProperties = result(streamAppPropertyAccess.get(stream.id))
    getProperties.from.size shouldBe 1
    getProperties.to.size shouldBe 1
    getProperties.instances shouldBe instances
    getProperties.state shouldBe None
    getProperties.error shouldBe None
    getProperties.workerClusterName shouldBe wkName

    // start streamApp
    val res1 =
      result(streamAppActionAccess.start(stream.id))
    res1.id shouldBe stream.id
    res1.state.get shouldBe ContainerState.RUNNING.name
    res1.error shouldBe None
    // save the cluster name to cache
    nameHolder.addClusterName(StreamCollie.formatUniqueName(res1.id))

    // check the cluster has the metrics data (each stream cluster has two metrics : IN_TOPIC and OUT_TOPIC)
    await(() => {
      // TODO: Since failed container could not be get from clusterCache, we directly use dockerClient to get logs
      // TODO: remove this after #1350 fixed...by Sam
      nodeCache.foreach {
        node =>
          if (node.user.isEmpty && node.password.isEmpty) {
            // k8s env
            val client = configurator.k8sClient.get
            result(client.containers)
              .filter(info => info.name.contains(StreamCollie.formatUniqueName(res1.id)))
              .foreach { container =>
                log.debug(s"container [${container.name}] log: ${result(client.log(container.name))}")
              }
          } else {
            // docker env
            val client =
              DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
            try client.containerNames().filter(name => name.contains(StreamCollie.formatUniqueName(res1.id))).foreach {
              containerName =>
                try {
                  log.debug(s"container [$containerName] log: ${client.log(containerName)}")
                } catch {
                  case e: Throwable =>
                    log.error(s"failed to retrieve container log $containerName", e)
                }
            } finally client.close()
          }
      }
      result(streamAppPropertyAccess.get(stream.id)).metrics.meters.nonEmpty
    })
    result(streamAppPropertyAccess.get(stream.id)).metrics.meters.size shouldBe 2

    // write some data into topic
    val producer = Producer
      .builder[Row, Array[Byte]]()
      .connectionProps(brokerConnProps)
      .allAcks()
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      await(
        () => {
          try producer
            .sender()
            .key(Row.EMPTY)
            .value(Array.emptyByteArray)
            .topicName(topic1.name)
            .send()
            .get()
            .topicName() == topic1.name
          catch {
            case e: ExecutionException =>
              e.getCause match {
                case _: UnknownTopicOrPartitionException => false
              }
          }
        }
      )
    } finally producer.close()

    // wait until the metrics cache data update
    await(() => result(streamAppPropertyAccess.get(stream.id)).metrics.meters.forall(_.value > 0))

    // check the metrics data again
    val metrics = result(streamAppPropertyAccess.get(stream.id)).metrics.meters
    metrics.foreach { metric =>
      metric.document should include("the number of rows")
      metric.value shouldBe 1D
    }

    //stop streamApp
    val res2 =
      result(streamAppActionAccess.stop(stream.id))
    res2.state.isEmpty shouldBe true
    res2.error shouldBe None

    // after stop streamApp, property should still exist
    result(streamAppPropertyAccess.get(stream.id)).id shouldBe stream.id
  }

  @After
  def cleanUp(): Unit = {
    Releasable.close(configurator)
    Releasable.close(nameHolder)
  }
}
