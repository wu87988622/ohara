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

import java.io.File
import java.util.concurrent.ExecutionException

import com.island.ohara.agent.docker.{ContainerState, DockerClient}
import com.island.ohara.client.configurator.v0.JarApi.JarKey
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi.{AccessOfProperty, ActionAccess}
import com.island.ohara.client.configurator.v0.{ZookeeperApi, _}
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

  private[this] var zkApi: ZookeeperApi.Access = _
  private[this] var bkApi: BrokerApi.Access = _
  private[this] var wkApi: WorkerApi.Access = _
  private[this] var containerApi: ContainerApi.Access = _
  private[this] var topicApi: TopicApi.Access = _
  private[this] var jarApi: JarApi.Access = _

  private[this] var streamAppActionAccess: ActionAccess = _
  private[this] var streamAppPropertyAccess: AccessOfProperty = _
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
      zkApi = ZookeeperApi.access.hostname(configurator.hostname).port(configurator.port)
      bkApi = BrokerApi.access.hostname(configurator.hostname).port(configurator.port)
      wkApi = WorkerApi.access.hostname(configurator.hostname).port(configurator.port)
      containerApi = ContainerApi.access.hostname(configurator.hostname).port(configurator.port)
      topicApi = TopicApi.access.hostname(configurator.hostname).port(configurator.port)
      jarApi = JarApi.access().hostname(configurator.hostname).port(configurator.port)
      streamAppActionAccess = StreamApi.accessOfAction.hostname(configurator.hostname).port(configurator.port)
      streamAppPropertyAccess = StreamApi.accessOfProperty.hostname(configurator.hostname).port(configurator.port)
      val nodeApi = NodeApi.access.hostname(configurator.hostname).port(configurator.port)
      // add all available nodes
      nodeCache.foreach { node =>
        result(
          nodeApi.request.name(node.name).port(node.port).user(node.user).password(node.password).create()
        )
      }
      val nodes = result(nodeApi.list())
      nodes.size shouldBe nodeCache.size
      nodeCache.forall(node => nodes.map(_.name).contains(node.name)) shouldBe true

      // create zookeeper cluster
      log.info("create zkCluster...start")
      val zkCluster = result(
        zkApi.request.name(nameHolder.generateClusterName()).nodeNames(nodeCache.take(1).map(_.name).toSet).create()
      )
      assertCluster(() => result(zkApi.list()), zkCluster.name)
      await(() => {
        val containers = result(containerApi.get(zkCluster.name).map(_.flatMap(_.containers)))
        containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
      })

      log.info("create zkCluster...done")

      // create broker cluster
      log.info("create bkCluster...start")
      val bkCluster = result(
        bkApi.request
          .name(bkName)
          .zookeeperClusterName(zkCluster.name)
          .nodeNames(nodeCache.take(1).map(_.name).toSet)
          .create())
      assertCluster(() => result(bkApi.list()), bkCluster.name)
      await(() => {
        val containers = result(containerApi.get(bkCluster.name).map(_.flatMap(_.containers)))
        containers.nonEmpty && containers.map(_.state).forall(_.equals(ContainerState.RUNNING.name))
      })
      log.info("create bkCluster...done")
      brokerConnProps = bkCluster.connectionProps

      // create worker cluster
      log.info("create wkCluster...start")
      val wkCluster = result(
        wkApi.request
          .name(wkName)
          .brokerClusterName(bkCluster.name)
          .nodeNames(nodeCache.take(instances).map(_.name).toSet)
          .create())
      assertCluster(() => result(wkApi.list()), wkCluster.name)
      log.info("create wkCluster...done")
    }
  }

  @Test
  def testFailedClusterRemoveGracefully(): Unit = {

    // create fake jar
    val jar = CommonUtils.createTempJar("fake")

    // upload streamApp jar
    val jarInfo = result(jarApi.request().group(wkName).upload(jar))

    // create streamApp properties
    val stream = result(
      streamAppPropertyAccess.request
        .name(CommonUtils.randomString(10))
        .jar(JarKey(jarInfo.group, jarInfo.name))
        .create())

    // update streamApp properties (use non-existed topics to make sure cluster failed)
    val properties = result(
      streamAppPropertyAccess.request.name(stream.name).from(Set("bar-fake")).to(Set("foo-fake")).update()
    )
    properties.from.size shouldBe 1
    properties.to.size shouldBe 1
    properties.instances shouldBe instances
    properties.state shouldBe None
    properties.error shouldBe None

    // start streamApp
    result(streamAppActionAccess.start(stream.name))

    // get the actually container names
    val map = nodeCache.map { node =>
      if (configurator.k8sClient.isDefined) {
        val client = configurator.k8sClient.get
        node -> result(client.containers).map(_.name).filter(name => name.contains(properties.name))
      } else {
        val client =
          DockerClient.builder.hostname(node.name).port(node.port).user(node.user).password(node.password).build
        try node -> client.containerNames().filter(name => name.contains(properties.name))
        finally client.close()
      }
    }

    // we only have one instance, container exited means cluster dead (the state here uses container state is ok
    // since we use the same name for cluster state
    await(() => {
      val res = result(streamAppPropertyAccess.get(stream.name))
      res.state.isDefined && res.state.get == ContainerState.DEAD.name &&
      // only 1 instance, dead nodes are equal to all nodes
      res.deadNodes == res.nodeNames &&
      res.nodeNames.size == 1
    })

    // stop and remove failed cluster gracefully
    result(streamAppActionAccess.stop(stream.name))

    // wait streamApp until removed actually
    await(() => {
      val res = result(streamAppPropertyAccess.get(stream.name))
      res.state.isEmpty
    })

    // check the containers are all removed
    map.foreach {
      case (node, containers) =>
        if (configurator.k8sClient.isDefined) {
          val client = configurator.k8sClient.get
          containers.foreach(container => !result(client.containers).map(_.name).contains(container) shouldBe true)
        } else {
          val client =
            DockerClient.builder.hostname(node.name).port(node.port).user(node.user).password(node.password).build
          try containers.foreach(container => client.nonExist(container) shouldBe true)
          finally client.close()
        }
    }
  }

  @Test
  def testRunSimpleStreamApp(): Unit = {
    val from = "fromTopic"
    val to = "toTopic"
    val jar = new File(CommonUtils.path(System.getProperty("user.dir"), "build", "libs", "ohara-streamapp.jar"))

    // we make sure the broker cluster exists again (for create topic)
    assertCluster(() => result(bkApi.list()), bkName)

    // create topic
    val topic1 = result(topicApi.request.name(from).brokerClusterName(bkName).create())
    val topic2 = result(topicApi.request.name(to).brokerClusterName(bkName).create())

    // upload streamApp jar
    val jarInfo = result(jarApi.request().group(wkName).upload(jar))
    jarInfo.name shouldBe "ohara-streamapp"
    jarInfo.group shouldBe wkName

    // create streamApp properties
    val stream = result(
      streamAppPropertyAccess.request
        .name(CommonUtils.randomString(10))
        .jar(JarKey(jarInfo.group, jarInfo.name))
        .create())

    // update streamApp properties
    val properties = result(
      streamAppPropertyAccess.request
        .name(stream.name)
        .from(Set(topic1.name))
        .to(Set(topic2.name))
        .instances(instances)
        .update()
    )
    properties.from.size shouldBe 1
    properties.to.size shouldBe 1
    properties.instances shouldBe instances
    properties.state shouldBe None
    properties.error shouldBe None

    // get streamApp property (cluster not create yet, hence no state)
    val getProperties = result(streamAppPropertyAccess.get(stream.name))
    getProperties.from.size shouldBe 1
    getProperties.to.size shouldBe 1
    getProperties.instances shouldBe instances
    getProperties.state shouldBe None
    getProperties.error shouldBe None

    // start streamApp
    val res1 =
      result(streamAppActionAccess.start(stream.name))
    res1.name shouldBe stream.name
    res1.state.get shouldBe ContainerState.RUNNING.name
    res1.error shouldBe None
    // save the cluster name to cache
    nameHolder.addClusterName(res1.name)

    // check the cluster has the metrics data (each stream cluster has two metrics : IN_TOPIC and OUT_TOPIC)
    await(() => result(streamAppPropertyAccess.get(stream.name)).metrics.meters.nonEmpty)
    result(streamAppPropertyAccess.get(stream.name)).metrics.meters.size shouldBe 2

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
    await(() => result(streamAppPropertyAccess.get(stream.name)).metrics.meters.forall(_.value > 0))

    // check the metrics data again
    val metrics = result(streamAppPropertyAccess.get(stream.name)).metrics.meters
    metrics.foreach { metric =>
      metric.document should include("the number of rows")
      metric.value shouldBe 1D
    }

    //stop streamApp
    val res2 =
      result(streamAppActionAccess.stop(stream.name))
    res2.state.isEmpty shouldBe true
    res2.error shouldBe None

    // after stop streamApp, property should still exist
    result(streamAppPropertyAccess.get(stream.name)).name shouldBe stream.name
  }

  @After
  def cleanUp(): Unit = {
    Releasable.close(nameHolder)
    Releasable.close(configurator)
  }
}
