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

package oharastream.ohara.it.stream

import java.io.File
import java.util.concurrent.ExecutionException

import com.typesafe.scalalogging.Logger
import oharastream.ohara.client.configurator.v0.ClusterState
import oharastream.ohara.client.configurator.v0.{ZookeeperApi, _}
import oharastream.ohara.common.data.{Row, Serializer}
import oharastream.ohara.common.setting.{ObjectKey, TopicKey}
import oharastream.ohara.common.util.CommonUtils
import oharastream.ohara.it.{ContainerPlatform, WithRemoteConfigurator}
import oharastream.ohara.kafka.Producer
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import org.junit.{Before, Test}
import org.scalatest.matchers.should.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global

class TestStream(platform: ContainerPlatform) extends WithRemoteConfigurator(platform: ContainerPlatform) {
  private[this] val log = Logger(classOf[TestStream])

  private[this] val zkApi: ZookeeperApi.Access =
    ZookeeperApi.access.hostname(configuratorHostname).port(configuratorPort)
  private[this] val bkApi: BrokerApi.Access = BrokerApi.access.hostname(configuratorHostname).port(configuratorPort)
  private[this] val containerApi: ContainerApi.Access =
    ContainerApi.access.hostname(configuratorHostname).port(configuratorPort)
  private[this] val topicApi: TopicApi.Access = TopicApi.access.hostname(configuratorHostname).port(configuratorPort)
  private[this] val jarApi: FileInfoApi.Access =
    FileInfoApi.access.hostname(configuratorHostname).port(configuratorPort)
  private[this] val access: StreamApi.Access = StreamApi.access.hostname(configuratorHostname).port(configuratorPort)
  private[this] val bkKey: ObjectKey         = serviceKeyHolder.generateClusterKey()
  private[this] var brokerConnProps: String  = _

  private[this] def waitStopFinish(objectKey: ObjectKey): Unit = {
    await(() => {
      // In configurator mode: clusters() will return the "stopped list" in normal case
      // In collie mode: clusters() will return the "cluster list except stop one" in normal case
      // we should consider these two cases by case...
      val clusters = result(access.list())
      !clusters.map(_.key).contains(objectKey) || clusters.find(_.key == objectKey).get.state.isEmpty
    })
  }

  @Before
  def setup(): Unit = {
    // create zookeeper cluster
    log.info("create zkCluster...start")
    val zkCluster = result(
      zkApi.request.key(serviceKeyHolder.generateClusterKey()).nodeNames(Set(platform.nodeNames.head)).create()
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
        .key(bkKey)
        .zookeeperClusterKey(zkCluster.key)
        .nodeNames(Set(platform.nodeNames.head))
        .create()
    )
    result(bkApi.start(bkCluster.key))
    assertCluster(
      () => result(bkApi.list()),
      () => result(containerApi.get(bkCluster.key).map(_.flatMap(_.containers))),
      bkCluster.key
    )
    log.info("create bkCluster...done")
    brokerConnProps = bkCluster.connectionProps
  }

  @Test
  def testRunSimpleStream(): Unit = {
    val from = TopicKey.of("default", CommonUtils.randomString(5))
    val to   = TopicKey.of("default", CommonUtils.randomString(5))
    val jar  = new File(CommonUtils.path(System.getProperty("user.dir"), "build", "libs", "ohara-it-stream.jar"))

    // we make sure the broker cluster exists again (for create topic)
    assertCluster(() => result(bkApi.list()), () => result(containerApi.get(bkKey).map(_.flatMap(_.containers))), bkKey)
    log.info(s"[testRunSimpleStream] broker cluster [$bkKey] assert...done")
    // create topic
    val topic1 = result(topicApi.request.key(from).brokerClusterKey(bkKey).create())
    result(topicApi.start(topic1.key))
    val topic2 = result(topicApi.request.key(to).brokerClusterKey(bkKey).create())
    result(topicApi.start(topic2.key))
    log.info(s"[testRunSimpleStream] topic creation [$topic1,$topic2]...done")

    // upload stream jar
    val jarInfo = result(jarApi.request.file(jar).upload())
    jarInfo.name shouldBe "ohara-it-stream.jar"
    log.info(s"[testRunSimpleStream] upload jar [$jarInfo]...done")

    // create stream properties
    val stream = result(
      access.request
        .key(serviceKeyHolder.generateClusterKey())
        .jarKey(jarInfo.key)
        .brokerClusterKey(bkKey)
        .nodeName(platform.nodeNames.head)
        .fromTopicKey(topic1.key)
        .toTopicKey(topic2.key)
        .create()
    )
    log.info(s"[testRunSimpleStream] stream properties creation [$stream]...done")

    stream.fromTopicKeys shouldBe Set(topic1.key)
    stream.toTopicKeys shouldBe Set(topic2.key)
    stream.state shouldBe None
    stream.error shouldBe None
    log.info(s"[testRunSimpleStream] stream properties update [$stream]...done")

    // get stream property (cluster not create yet, hence no state)
    val getProperties = result(access.get(stream.key))
    getProperties.fromTopicKeys shouldBe Set(topic1.key)
    getProperties.toTopicKeys shouldBe Set(topic2.key)
    getProperties.state shouldBe None
    getProperties.error shouldBe None

    // start stream
    log.info(s"[testRunSimpleStream] stream start [${stream.key}]")
    result(access.start(stream.key))
    await(() => result(access.get(stream.key)).state.contains(ClusterState.RUNNING))
    log.info(s"[testRunSimpleStream] stream start [${stream.key}]...done")

    val res1 = result(access.get(stream.key))
    res1.key shouldBe stream.key
    res1.error shouldBe None

    // check the cluster has the metrics data (each stream cluster has two metrics : IN_TOPIC and OUT_TOPIC)
    await(() => result(access.get(stream.key)).meters.nonEmpty)
    result(access.get(stream.key)).meters.size shouldBe 2

    // write some data into topic
    val producer = Producer
      .builder()
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
            .topicName(topic1.topicNameOnKafka)
            .send()
            .get()
            .topicName() == topic1.topicNameOnKafka
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
    await(() => result(access.get(stream.key)).meters.forall(_.value > 0))

    // check the metrics data again
    val metrics = result(access.get(stream.key)).meters
    metrics.foreach { metric =>
      metric.document should include("the number of rows")
      metric.value shouldBe 1d
    }

    await(() => result(topicApi.get(from)).meters.nonEmpty)
    await(() => result(topicApi.get(to)).meters.nonEmpty)

    //stop stream
    result(access.stop(stream.key))
    waitStopFinish(stream.key)
    result(access.get(stream.key)).state.isEmpty shouldBe true

    // after stop stream, property should still exist
    result(access.get(stream.key)).name shouldBe stream.name
  }

  @Test
  def testDeadNodes(): Unit =
    if (platform.nodeNames.size < 2) skipTest(s"requires two nodes at least")
    else {
      val from = TopicKey.of("default", CommonUtils.randomString(5))
      val to   = TopicKey.of("default", CommonUtils.randomString(5))
      val jar  = new File(CommonUtils.path(System.getProperty("user.dir"), "build", "libs", "ohara-it-stream.jar"))
      // create topic
      val topic1 = result(topicApi.request.key(from).brokerClusterKey(bkKey).create())
      result(topicApi.start(topic1.key))
      val topic2 = result(topicApi.request.key(to).brokerClusterKey(bkKey).create())
      result(topicApi.start(topic2.key))

      // upload stream jar
      val jarInfo = result(jarApi.request.file(jar).upload())
      jarInfo.name shouldBe "ohara-it-stream.jar"

      // create stream properties
      val stream = result(
        access.request
          .key(serviceKeyHolder.generateClusterKey())
          .jarKey(ObjectKey.of(jarInfo.group, jarInfo.name))
          .brokerClusterKey(bkKey)
          .nodeNames(platform.nodeNames)
          .fromTopicKey(topic1.key)
          .toTopicKey(topic2.key)
          .create()
      )

      // start stream
      result(access.start(stream.key))
      await(() => result(access.get(stream.key)).state.contains(ClusterState.RUNNING))

      result(access.get(stream.key)).nodeNames shouldBe platform.nodeNames
      result(access.get(stream.key)).deadNodes shouldBe Set.empty

      // remove a container directly
      val aliveNodes = platform.nodeNames.slice(1, platform.nodeNames.size)
      val deadNodes  = platform.nodeNames -- aliveNodes
      serviceKeyHolder.release(
        clusterKeys = Set(stream.key),
        // remove the container from first node
        excludedNodes = aliveNodes
      )

      result(access.get(stream.key)).state should not be None

      await { () =>
        val cluster = result(access.get(stream.key))
        cluster.nodeNames == platform.nodeNames &&
        cluster.deadNodes == deadNodes
      }
    }
}
