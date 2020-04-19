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

package oharastream.ohara.it.shabondi

import com.typesafe.scalalogging.Logger
import oharastream.ohara.client.configurator.v0.ClusterState
import oharastream.ohara.client.configurator.v0.ShabondiApi.ShabondiClusterInfo
import oharastream.ohara.client.configurator.v0.TopicApi.TopicInfo
import oharastream.ohara.client.configurator.v0.{
  BrokerApi,
  ClusterInfo,
  ContainerApi,
  ShabondiApi,
  TopicApi,
  ZookeeperApi
}
import oharastream.ohara.common.setting.{ObjectKey, TopicKey}
import oharastream.ohara.common.util.CommonUtils
import oharastream.ohara.it.{ContainerPlatform, WithRemoteConfigurator}
import oharastream.ohara.metrics.BeanChannel
import oharastream.ohara.shabondi.ShabondiType
import org.junit.{Before, Test}
import org.scalatest.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global

class TestShabondi(platform: ContainerPlatform) extends WithRemoteConfigurator(platform: ContainerPlatform) {
  private[this] val log = Logger(classOf[TestShabondi])

  private[this] val zkApi        = ZookeeperApi.access.hostname(configuratorHostname).port(configuratorPort)
  private[this] val bkApi        = BrokerApi.access.hostname(configuratorHostname).port(configuratorPort)
  private[this] val containerApi = ContainerApi.access.hostname(configuratorHostname).port(configuratorPort)
  private[this] val topicApi     = TopicApi.access.hostname(configuratorHostname).port(configuratorPort)
  private[this] val shabondiApi  = ShabondiApi.access.hostname(configuratorHostname).port(configuratorPort)

  private[this] var bkKey: ObjectKey = _

  @Before
  def setup(): Unit = {
    // create zookeeper cluster
    log.info("create zkCluster...start")
    val zkCluster = result(
      zkApi.request.key(serviceKeyHolder.generateClusterKey()).nodeNames(Set(nodeNames.head)).create()
    )
    result(zkApi.start(zkCluster.key))
    assertCluster(
      () => result(zkApi.list()),
      () => result(containerApi.get(zkCluster.key).map(_.flatMap(_.containers))),
      zkCluster.key
    )
    log.info("create zkCluster...done")

    // create broker cluster
    log.info("create bkCluster...start")
    val bkCluster = result(
      bkApi.request
        .key(serviceKeyHolder.generateClusterKey())
        .zookeeperClusterKey(zkCluster.key)
        .nodeNames(Set(nodeNames.head))
        .create()
    )
    bkKey = bkCluster.key
    result(bkApi.start(bkCluster.key))
    assertCluster(
      () => result(bkApi.list()),
      () => result(containerApi.get(bkCluster.key).map(_.flatMap(_.containers))),
      bkCluster.key
    )
    log.info("create bkCluster...done")
  }

  @Test
  def testStartAndStop_ShabondiSource(): Unit = {
    val topic1 = startTopic()

    // we make sure the broker cluster exists again (for create topic)
    assertCluster(
      () => result(bkApi.list()),
      () => result(containerApi.get(bkKey).map(_.flatMap(_.containers))),
      bkKey
    )
    log.info(s"assert broker cluster [$bkKey]...done")

    // ----- create Shabondi Source
    val shabondiSource: ShabondiApi.ShabondiClusterInfo = createShabondiService(ShabondiType.Source, topic1.key)
    log.info(s"shabondi creation [$shabondiSource]...done")

    // assert Shabondi Source cluster info
    val clusterInfo = result(shabondiApi.get(shabondiSource.key))
    clusterInfo.shabondiClass shouldBe ShabondiType.Source.className
    clusterInfo.sourceToTopics shouldBe Set(topic1.key)
    clusterInfo.state shouldBe None
    clusterInfo.error shouldBe None

    assertStartAndStop(ShabondiType.Source, shabondiSource)
  }

  @Test
  def testStartAndStop_ShabondiSink(): Unit = {
    val topic1 = startTopic()

    // we make sure the broker cluster exists again (for create topic)
    assertCluster(
      () => result(bkApi.list()),
      () => result(containerApi.get(bkKey).map(_.flatMap(_.containers))),
      bkKey
    )
    log.info(s"assert broker cluster [$bkKey]...done")

    // ----- create Shabondi Sink
    val shabondiSink: ShabondiApi.ShabondiClusterInfo = createShabondiService(ShabondiType.Sink, topic1.key)
    log.info(s"shabondi creation [$shabondiSink]...done")

    // assert Shabondi Sink cluster info
    val clusterInfo = result(shabondiApi.get(shabondiSink.key))
    clusterInfo.shabondiClass shouldBe ShabondiType.Sink.className
    clusterInfo.sinkFromTopics shouldBe Set(topic1.key)
    clusterInfo.state shouldBe None
    clusterInfo.error shouldBe None

    assertStartAndStop(ShabondiType.Sink, shabondiSink)
  }

  @Test
  def testCheckBrokerDependencies(): Unit = {
    val topic1 = startTopic()

    // we make sure the broker cluster exists again (for create topic)
    assertCluster(
      () => result(bkApi.list()),
      () => result(containerApi.get(bkKey).map(_.flatMap(_.containers))),
      bkKey
    )
    log.info(s"assert broker cluster [$bkKey]...done")

    // ----- create Shabondi Source & Sink
    val shabondiSource: ShabondiApi.ShabondiClusterInfo = createShabondiService(ShabondiType.Source, topic1.key)
    val shabondiSink: ShabondiApi.ShabondiClusterInfo   = createShabondiService(ShabondiType.Sink, topic1.key)

    // ---- Start Shabondi Source & Sink
    result(shabondiApi.start(shabondiSource.key))
    result(shabondiApi.start(shabondiSink.key))

    // wait for source and sink ready
    await(() => {
      val clusterInfo1 = result(shabondiApi.get(shabondiSource.key))
      clusterInfo1.state.contains(ClusterState.RUNNING)
      val clusterInfo2 = result(shabondiApi.get(shabondiSink.key))
      clusterInfo1.state.contains(ClusterState.RUNNING) && clusterInfo2.state.contains(ClusterState.RUNNING)
    })

    val thrown = the[IllegalArgumentException] thrownBy result(bkApi.stop(bkKey))
    log.info("thrown message: " + thrown.getMessage)
    thrown.getMessage should startWith(s"you can't remove broker cluster:" + bkKey.toString)
    thrown.getMessage should include(shabondiSource.key.toString)
    thrown.getMessage should include(shabondiSink.key.toString)
  }

  @Test
  def testCheckTopicDependencies(): Unit = {
    val topic1 = startTopic()

    // we make sure the broker cluster exists again (for create topic)
    assertCluster(
      () => result(bkApi.list()),
      () => result(containerApi.get(bkKey).map(_.flatMap(_.containers))),
      bkKey
    )
    log.info(s"assert broker cluster [$bkKey]...done")

    // ----- create Shabondi Source & Sink
    val shabondiSource: ShabondiApi.ShabondiClusterInfo = createShabondiService(ShabondiType.Source, topic1.key)
    val shabondiSink: ShabondiApi.ShabondiClusterInfo   = createShabondiService(ShabondiType.Sink, topic1.key)

    // ---- Start Shabondi Source & Sink
    result(shabondiApi.start(shabondiSource.key))
    result(shabondiApi.start(shabondiSink.key))

    await(() => {
      val clusterInfo1 = result(shabondiApi.get(shabondiSource.key))
      val clusterInfo2 = result(shabondiApi.get(shabondiSink.key))
      clusterInfo1.state.contains(ClusterState.RUNNING) && clusterInfo2.state.contains(ClusterState.RUNNING)
    })

    val thrown = the[IllegalArgumentException] thrownBy result(topicApi.stop(topic1.key))
    log.info("thrown message: " + thrown.getMessage)
    thrown.getMessage should startWith(s"topic:" + topic1.key.toString)
    thrown.getMessage should include(shabondiSource.key.toString)
    thrown.getMessage should include(shabondiSink.key.toString)
  }

  private def startTopic(): TopicInfo = {
    val topic = TopicKey.of("default", CommonUtils.randomString(5))
    val topicInfo = result(
      topicApi.request.key(topic).brokerClusterKey(bkKey).create()
    )
    result(topicApi.start(topicInfo.key))
    log.info(s"start topic [$topic]...done")
    topicInfo
  }

  private def createShabondiService(shabondiType: ShabondiType, topicKey: TopicKey): ShabondiClusterInfo = {
    val request = shabondiApi.request
      .key(serviceKeyHolder.generateClusterKey())
      .brokerClusterKey(bkKey)
      .nodeName(nodeNames.head)
      .shabondiClass(shabondiType.className)
      .clientPort(CommonUtils.availablePort())
    shabondiType match {
      case ShabondiType.Source => request.sourceToTopics(Set(topicKey))
      case ShabondiType.Sink   => request.sinkFromTopics(Set(topicKey))
    }
    result(request.create())
  }

  private def assertStartAndStop(shabondiType: ShabondiType, clusterInfo: ShabondiClusterInfo): Unit = {
    // ---- Start Shabondi service
    result(shabondiApi.start(clusterInfo.key))
    await(() => {
      val resultInfo = result(shabondiApi.get(clusterInfo.key))
      resultInfo.state.contains(ClusterState.RUNNING)
    })

    { // assert Shabondi Source cluster info
      val resultInfo = result(shabondiApi.get(clusterInfo.key))
      resultInfo.shabondiClass shouldBe shabondiType.className
      resultInfo.nodeNames shouldBe Set(nodeNames.head)
      resultInfo.aliveNodes shouldBe Set(nodeNames.head)
      resultInfo.deadNodes shouldBe Set.empty
    }

    testJmx(clusterInfo)

    // ---- Stop Shabondi service
    result(shabondiApi.stop(clusterInfo.key))
    await(() => {
      val clusters = result(shabondiApi.list())
      !clusters.map(_.key).contains(clusterInfo.key) ||
      clusters.find(_.key == clusterInfo.key).get.state.isEmpty
    })

    { // assert Shabondi Source cluster info
      val resultInfo = result(shabondiApi.get(clusterInfo.key))
      resultInfo.state shouldBe None
      resultInfo.nodeNames shouldBe Set(nodeNames.head)
      resultInfo.aliveNodes shouldBe Set.empty
      resultInfo.deadNodes shouldBe Set.empty
    }
  }

  private[this] def testJmx(cluster: ClusterInfo): Unit =
    cluster.nodeNames.foreach(
      node =>
        await(
          () =>
            try BeanChannel.builder().hostname(node).port(cluster.jmxPort).build().nonEmpty()
            catch {
              // the jmx service may be not ready.
              case _: Throwable =>
                false
            }
        )
    )
}
