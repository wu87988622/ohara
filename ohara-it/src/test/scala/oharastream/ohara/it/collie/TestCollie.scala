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

package oharastream.ohara.it.collie

import java.time.Duration
import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.Logger
import oharastream.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import oharastream.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import oharastream.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import oharastream.ohara.client.configurator.v0.{BrokerApi, ClusterInfo, ContainerApi, LogApi, WorkerApi, ZookeeperApi}
import oharastream.ohara.client.kafka.ConnectorAdmin
import oharastream.ohara.common.data.Serializer
import oharastream.ohara.common.exception.{ExecutionException, TimeoutException}
import oharastream.ohara.common.util.CommonUtils
import oharastream.ohara.it.category.CollieGroup
import oharastream.ohara.it.{ContainerPlatform, WithRemoteConfigurator}
import oharastream.ohara.kafka.{Consumer, Producer, TopicAdmin}
import oharastream.ohara.metrics.BeanChannel
import org.apache.kafka.common.errors.InvalidReplicationFactorException
import org.junit.Test
import org.junit.experimental.categories.Category
import org.scalatest.matchers.should.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * This abstract class extracts the "required" information of running tests on true env.
  * All checks are verified in this class but we do run all test cases on different test in order to avoid
  * slow test cases run by single test jvm.
  *
  * NOTED: this test will forward random ports so it would be better to "close" firewall of remote node.
  *
  * Noted: this test depends on the ClusterNameHolder which helps us to cleanup all containers for all tests cases.
  * Hence, you don't need to add "finally" hook to do the cleanup.
  */
@Category(Array(classOf[CollieGroup]))
class TestCollie(platform: ContainerPlatform) extends WithRemoteConfigurator(platform: ContainerPlatform) {
  private[this] val log              = Logger(classOf[TestCollie])
  private[this] val numberOfClusters = 2

  private[this] def zkApi = ZookeeperApi.access.hostname(configuratorHostname).port(configuratorPort)

  private[this] def bkApi = BrokerApi.access.hostname(configuratorHostname).port(configuratorPort)

  private[this] def wkApi = WorkerApi.access.hostname(configuratorHostname).port(configuratorPort)

  private[this] def logApi = LogApi.access.hostname(configuratorHostname).port(configuratorPort)

  private[this] def containerApi = ContainerApi.access.hostname(configuratorHostname).port(configuratorPort)

  @Test
  def testZk(): Unit = {
    log.info("start to run zookeeper cluster")
    val nodeName: String = platform.nodeNames.head
    val clusterKey       = serviceKeyHolder.generateClusterKey()
    val jmxPort          = CommonUtils.availablePort()
    val clientPort       = CommonUtils.availablePort()
    val electionPort     = CommonUtils.availablePort()
    val peerPort         = CommonUtils.availablePort()
    def assert(zkCluster: ZookeeperClusterInfo): ZookeeperClusterInfo = {
      zkCluster.key shouldBe clusterKey
      zkCluster.nodeNames.head shouldBe nodeName
      zkCluster.jmxPort shouldBe jmxPort
      zkCluster.clientPort shouldBe clientPort
      zkCluster.peerPort shouldBe peerPort
      zkCluster.electionPort shouldBe electionPort
      zkCluster
    }

    val zkCluster = assert(
      result(
        zkApi.request
          .key(clusterKey)
          .jmxPort(jmxPort)
          .clientPort(clientPort)
          .electionPort(electionPort)
          .peerPort(peerPort)
          .nodeName(nodeName)
          .create()
      )
    )
    result(zkApi.start(zkCluster.key))
    assertCluster(
      () => result(zkApi.list()),
      () => result(containerApi.get(zkCluster.key)).flatMap(_.containers),
      zkCluster.key
    )
    assert(result(zkApi.get(zkCluster.key)))
    log.info("start to run zookeeper cluster ... done")
    result(zkApi.get(clusterKey)).key shouldBe clusterKey
    log.info(s"verify number of zk clusters... done")
    result(logApi.log4ZookeeperCluster(clusterKey)).logs.size shouldBe 1
    log.info(s"verify number of log... done")
    result(logApi.log4ZookeeperCluster(clusterKey)).logs
      .map(_.value)
      .foreach(
        log =>
          withClue(log) {
            log.toLowerCase().contains("exception") shouldBe false
            log.isEmpty shouldBe false
          }
      )
    log.info(s"verify log of zk clusters... done")
    result(containerApi.get(clusterKey)).flatMap(_.containers).foreach { container =>
      container.nodeName shouldBe nodeName
      container.name.contains(clusterKey.name()) shouldBe true
      container.name should not be container.hostname
      container.name.length should be > container.hostname.length
      container.portMappings.size shouldBe 4
      container.portMappings.exists(_.containerPort == clientPort) shouldBe true
      container.portMappings.exists(_.containerPort == electionPort) shouldBe true
      container.portMappings.exists(_.containerPort == peerPort) shouldBe true
    }
    log.info(s"get containers from zk:$clusterKey... done")
    testJmx(zkCluster)
    log.info(s"test jmx from zk:$clusterKey... done")
    result(zkApi.stop(clusterKey))
    await(() => {
      // In configurator mode: clusters() will return the "stopped list" in normal case
      // In collie mode: clusters() will return the "cluster list except stop one" in normal case
      // we should consider these two cases by case...
      val clusters = result(zkApi.list())
      !clusters.map(_.key).contains(clusterKey) || clusters.find(_.key == clusterKey).get.state.isEmpty
    })
    // the cluster is stopped actually, delete the data
    result(zkApi.delete(clusterKey))
  }

  @Test
  def testBroker(): Unit = {
    val zkCluster = result(
      zkApi.request
        .key(serviceKeyHolder.generateClusterKey())
        .jmxPort(CommonUtils.availablePort())
        .clientPort(CommonUtils.availablePort())
        .electionPort(CommonUtils.availablePort())
        .peerPort(CommonUtils.availablePort())
        .nodeName(platform.nodeNames.head)
        .create()
    )
    result(zkApi.start(zkCluster.key))
    assertCluster(
      () => result(zkApi.list()),
      () => result(containerApi.get(zkCluster.key)).flatMap(_.containers),
      zkCluster.key
    )
    log.info("[BROKER] start to run broker cluster")
    val clusterKey = serviceKeyHolder.generateClusterKey()
    log.info(s"[BROKER] verify existence of broker cluster:$clusterKey...done")
    val nodeName: String = platform.nodeNames.head
    val clientPort       = CommonUtils.availablePort()
    val jmxPort          = CommonUtils.availablePort()
    def assert(brokerCluster: BrokerClusterInfo): BrokerClusterInfo = {
      brokerCluster.key shouldBe clusterKey
      brokerCluster.zookeeperClusterKey shouldBe zkCluster.key
      brokerCluster.nodeNames.head shouldBe nodeName
      brokerCluster.clientPort shouldBe clientPort
      brokerCluster.jmxPort shouldBe jmxPort
      brokerCluster
    }

    val bkCluster = assert(
      result(
        bkApi.request
          .key(clusterKey)
          .clientPort(clientPort)
          .jmxPort(jmxPort)
          .zookeeperClusterKey(zkCluster.key)
          .nodeName(nodeName)
          .create()
      )
    )
    result(bkApi.start(bkCluster.key))
    log.info("[BROKER] start to run broker cluster...done")
    assertCluster(
      () => result(bkApi.list()),
      () => result(containerApi.get(bkCluster.key)).flatMap(_.containers),
      bkCluster.key
    )
    assert(result(bkApi.get(bkCluster.key)))
    log.info("[BROKER] verify cluster api...done")
    result(bkApi.get(bkCluster.key)).key shouldBe bkCluster.key
    result(containerApi.get(clusterKey)).flatMap(_.containers).foreach { container =>
      container.nodeName shouldBe nodeName
      container.name.contains(clusterKey.name) shouldBe true
      container.name should not be container.hostname
      container.name.length should be > container.hostname.length
      container.portMappings.size shouldBe 2
      container.portMappings.exists(_.containerPort == clientPort) shouldBe true
    }
    result(logApi.log4BrokerCluster(clusterKey)).logs.size shouldBe 1
    result(logApi.log4BrokerCluster(clusterKey)).logs
      .map(_.value)
      .foreach(
        log =>
          withClue(log) {
            log.toLowerCase().contains("exception") shouldBe false
            log.isEmpty shouldBe false
          }
      )
    log.info("[BROKER] verify:log done")
    var curCluster = bkCluster
    testTopic(curCluster)
    testJmx(curCluster)
    curCluster = testAddNodeToRunningBrokerCluster(curCluster)
    testTopic(curCluster)
    testJmx(curCluster)
    // we can't remove the node used to set up broker cluster since the internal topic is hosted by it.
    // In this test case, the internal topic has single replication. If we remove the node host the internal topic
    // the broker cluster will be out-of-sync and all data operation will be frozen.
    curCluster = testRemoveNodeToRunningBrokerCluster(curCluster, nodeName)
    testTopic(curCluster)
    testJmx(curCluster)
    result(bkApi.stop(clusterKey))
    await(() => {
      // In configurator mode: clusters() will return the "stopped list" in normal case
      // In collie mode: clusters() will return the "cluster list except stop one" in normal case
      // we should consider these two cases by case...
      val clusters = result(bkApi.list())
      !clusters.map(_.key).contains(clusterKey) || clusters.find(_.key == clusterKey).get.state.isEmpty
    })
    // the cluster is stopped actually, delete the data
    result(bkApi.delete(clusterKey))
  }

  private[this] def testAddNodeToRunningBrokerCluster(previousCluster: BrokerClusterInfo): BrokerClusterInfo = {
    await(() => result(bkApi.list()).exists(_.key == previousCluster.key))
    log.info(s"[BROKER] nodes:${platform.nodeNames} previous:${previousCluster.nodeNames}")
    an[IllegalArgumentException] should be thrownBy result(
      bkApi.removeNode(previousCluster.key, previousCluster.nodeNames.head)
    )
    val freeNodes = platform.nodeNames.diff(previousCluster.nodeNames)
    if (freeNodes.nonEmpty) {
      await { () =>
        // nothing happens if we add duplicate nodes
        result(bkApi.addNode(previousCluster.key, previousCluster.nodeNames.head))
        // we can't add a nonexistent node
        // we always get IllegalArgumentException if we sent request by restful api
        // However, if we use collie impl, an NoSuchElementException will be thrown...
        an[Throwable] should be thrownBy result(bkApi.addNode(previousCluster.key, CommonUtils.randomString()))
        val newNode = freeNodes.head
        log.info(s"[BROKER] add new node:$newNode to cluster:${previousCluster.key}")
        val newCluster = result(
          bkApi
            .addNode(previousCluster.key, newNode)
            .flatMap(_ => bkApi.get(previousCluster.key))
        )
        log.info(s"[BROKER] add new node:$newNode to cluster:${previousCluster.key}...done")
        newCluster.key shouldBe previousCluster.key
        newCluster.imageName shouldBe previousCluster.imageName
        newCluster.zookeeperClusterKey shouldBe previousCluster.zookeeperClusterKey
        newCluster.clientPort shouldBe previousCluster.clientPort
        newCluster.nodeNames.size - previousCluster.nodeNames.size shouldBe 1
        result(bkApi.get(newCluster.key)).aliveNodes.contains(newNode)
      }
      result(bkApi.get(previousCluster.key))
    } else previousCluster
  }

  private[this] def testTopic(cluster: BrokerClusterInfo): Unit = {
    val topicName = CommonUtils.randomString()
    val brokers   = cluster.nodeNames.map(_ + s":${cluster.clientPort}").mkString(",")
    log.info(s"[BROKER] start to create topic:$topicName on broker cluster:$brokers")
    val topicAdmin = TopicAdmin.of(brokers)
    try {
      log.info(s"[BROKER] start to check the sync information. active broker nodes:${cluster.nodeNames}")
      // make sure all active broker nodes are sync!
      await(() => topicAdmin.brokerPorts().toCompletableFuture.get().size() == cluster.nodeNames.size)
      log.info(s"[BROKER] start to check the sync information. active broker nodes:${cluster.nodeNames} ... done")
      log.info(s"[BROKER] number of replications:${cluster.nodeNames.size}")
      await(
        () =>
          try {
            topicAdmin.topicCreator().numberOfPartitions(1).numberOfReplications(1).topicName(topicName).create()
            true
          } catch {
            case e: ExecutionException =>
              e.getCause match {
                // the new broker needs time to sync information to other existed bk nodes.
                case _: InvalidReplicationFactorException => false
                case _: Throwable                         => throw e.getCause
              }
            case e: TimeoutException =>
              log.error(s"[BROKER] create topic error ${e.getCause}")
              false
          }
      )
      log.info(s"[BROKER] start to create topic:$topicName on broker cluster:$brokers ... done")
      val producer = Producer
        .builder()
        .connectionProps(brokers)
        .allAcks()
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .build()
      val numberOfRecords = 5
      log.info(s"[BROKER] start to send $numberOfRecords data")
      (0 until numberOfRecords).foreach(
        index => producer.sender().key(index.toString).value(index.toString).topicName(topicName).send()
      )
      producer.flush()
      producer.close()
      log.info(s"[BROKER] start to send data ... done")
      log.info(s"[BROKER] start to receive data")
      val consumer = Consumer
        .builder()
        .connectionProps(brokers)
        .offsetFromBegin()
        .topicName(topicName)
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .build()
      try {
        val records = consumer.poll(Duration.ofSeconds(30), numberOfRecords)
        records.size() shouldBe numberOfRecords
        records.stream().forEach(record => record.key().get() shouldBe record.value().get())
        log.info(s"[BROKER] start to receive data ... done")
      } finally consumer.close()
      topicAdmin.deleteTopic(topicName)
    } finally topicAdmin.close()
  }

  private[this] def testRemoveNodeToRunningBrokerCluster(
    previousCluster: BrokerClusterInfo,
    excludedNode: String
  ): BrokerClusterInfo = {
    await(() => result(bkApi.list()).exists(_.key == previousCluster.key))
    if (previousCluster.nodeNames.size > 1) {
      val beRemovedNode: String = previousCluster.nodeNames.filter(_ != excludedNode).head
      await { () =>
        log.info(
          s"[BROKER] start to remove node:$beRemovedNode from bk cluster:${previousCluster.key} nodes:${previousCluster.nodeNames
            .mkString(",")}"
        )
        result(bkApi.removeNode(previousCluster.key, beRemovedNode))
        log.info(s"[BROKER] start to remove node:$beRemovedNode from bk cluster:${previousCluster.key} ... done")
        val newCluster = result(bkApi.get(previousCluster.key))
        newCluster.key == previousCluster.key &&
        newCluster.imageName == previousCluster.imageName &&
        newCluster.zookeeperClusterKey == previousCluster.zookeeperClusterKey &&
        newCluster.clientPort == previousCluster.clientPort &&
        previousCluster.nodeNames.size - newCluster.nodeNames.size == 1 &&
        !result(bkApi.get(newCluster.key)).aliveNodes.contains(beRemovedNode)
      }
      result(bkApi.get(previousCluster.key))
    } else previousCluster
  }

  @Test
  def testWorker(): Unit = {
    val zkCluster = result(
      zkApi.request
        .key(serviceKeyHolder.generateClusterKey())
        .jmxPort(CommonUtils.availablePort())
        .clientPort(CommonUtils.availablePort())
        .electionPort(CommonUtils.availablePort())
        .peerPort(CommonUtils.availablePort())
        .nodeName(platform.nodeNames.head)
        .create()
    )
    result(zkApi.start(zkCluster.key))
    assertCluster(
      () => result(zkApi.list()),
      () => result(containerApi.get(zkCluster.key)).flatMap(_.containers),
      zkCluster.key
    )
    val bkCluster = result(
      bkApi.request
        .key(serviceKeyHolder.generateClusterKey())
        .clientPort(CommonUtils.availablePort())
        .jmxPort(CommonUtils.availablePort())
        .zookeeperClusterKey(zkCluster.key)
        .nodeName(platform.nodeNames.head)
        .create()
    )
    result(bkApi.start(bkCluster.key))
    assertCluster(
      () => result(bkApi.list()),
      () => result(containerApi.get(bkCluster.key)).flatMap(_.containers),
      bkCluster.key
    )
    log.info("[WORKER] start to test worker")
    val nodeName   = platform.nodeNames.head
    val clusterKey = serviceKeyHolder.generateClusterKey()
    log.info("[WORKER] verify:nonExists done")
    val clientPort = CommonUtils.availablePort()
    val jmxPort    = CommonUtils.availablePort()
    def assert(workerCluster: WorkerClusterInfo): WorkerClusterInfo = {
      workerCluster.brokerClusterKey shouldBe bkCluster.key
      workerCluster.key shouldBe clusterKey
      workerCluster.nodeNames.head shouldBe nodeName
      workerCluster.clientPort shouldBe clientPort
      workerCluster.jmxPort shouldBe jmxPort
      workerCluster.configTopicPartitions shouldBe 1
      workerCluster.configTopicReplications shouldBe 1
      workerCluster.statusTopicPartitions shouldBe 1
      workerCluster.statusTopicReplications shouldBe 1
      workerCluster.offsetTopicPartitions shouldBe 1
      workerCluster.offsetTopicReplications shouldBe 1
      workerCluster
    }
    log.info("[WORKER] create ...")

    val wkCluster = assert(
      result(
        wkApi.request
          .key(clusterKey)
          .clientPort(clientPort)
          .jmxPort(jmxPort)
          .brokerClusterKey(bkCluster.key)
          .nodeName(nodeName)
          .create()
      )
    )
    log.info("[WORKER] create done")
    result(wkApi.start(wkCluster.key))
    log.info("[WORKER] start done")
    assertCluster(
      () => result(wkApi.list()),
      () => result(containerApi.get(wkCluster.key)).flatMap(_.containers),
      wkCluster.key
    )
    log.info("[WORKER] check existence")
    assert(result(wkApi.get(wkCluster.key)))
    log.info("[WORKER] verify:create done")
    result(wkApi.get(wkCluster.key)).key shouldBe wkCluster.key
    log.info("[WORKER] verify:exist done")
    // we can't assume the size since other tests may create zk cluster at the same time
    result(wkApi.list()).isEmpty shouldBe false
    log.info("[WORKER] verify:list done")
    result(containerApi.get(clusterKey)).flatMap(_.containers).foreach { container =>
      container.nodeName shouldBe nodeName
      container.name.contains(clusterKey.name) shouldBe true
      container.name should not be container.hostname
      container.name.length should be > container.hostname.length
      // [BEFORE] ServiceCollieImpl applies --network=host to all worker containers so there is no port mapping.
      // The following checks are disabled rather than deleted since it seems like a bug if we don't check the port mapping.
      // [AFTER] ServiceCollieImpl use bridge network now
      container.portMappings.size shouldBe 2
      container.portMappings.exists(_.containerPort == clientPort) shouldBe true
    }
    val logs = result(logApi.log4WorkerCluster(clusterKey)).logs.map(_.value)
    logs.size shouldBe 1
    logs.foreach(
      log =>
        withClue(log) {
          log.contains("- ERROR") shouldBe false
          // we cannot assume "k8s get logs" are complete since log may rotate
          // so log could be empty in k8s environment
          // also see : https://github.com/kubernetes/kubernetes/issues/11046#issuecomment-121140315
        }
    )
    log.info("[WORKER] verify:log done")
    var curCluster = wkCluster
    testConnectors(curCluster)
    testJmx(curCluster)
    curCluster = testAddNodeToRunningWorkerCluster(curCluster)
    testConnectors(curCluster)
    testJmx(curCluster)
    // The issue of inconsistent settings after adding/removing nodes.
    // This issue is going to resolve in #2677.
    curCluster = testRemoveNodeToRunningWorkerCluster(curCluster, nodeName)
    testConnectors(curCluster)
    testJmx(curCluster)
    result(wkApi.stop(clusterKey))
    await(() => {
      // In configurator mode: clusters() will return the "stopped list" in normal case
      // In collie mode: clusters() will return the "cluster list except stop one" in normal case
      // we should consider these two cases by case...
      val clusters = result(wkApi.list())
      !clusters.map(_.key).contains(clusterKey) || clusters.find(_.key == clusterKey).get.state.isEmpty
    })
    // the cluster is stopped actually, delete the data
    result(wkApi.delete(clusterKey))
  }

  private[this] def testConnectors(cluster: WorkerClusterInfo): Unit =
    await(
      () =>
        try {
          log.info(s"worker node head: ${cluster.nodeNames.head}:${cluster.clientPort}")
          result(ConnectorAdmin(cluster).connectorDefinitions()).nonEmpty
        } catch {
          case e: Throwable =>
            log.info(s"[WORKER] worker cluster:${cluster.name} is starting ... retry", e)
            false
        }
    )

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

  private[this] def testAddNodeToRunningWorkerCluster(previousCluster: WorkerClusterInfo): WorkerClusterInfo = {
    await(() => result(wkApi.list()).exists(_.key == previousCluster.key))
    an[IllegalArgumentException] should be thrownBy result(
      wkApi.removeNode(previousCluster.key, previousCluster.nodeNames.head)
    )
    val freeNodes = platform.nodeNames.diff(previousCluster.nodeNames)
    if (freeNodes.nonEmpty) {
      val newNode = freeNodes.head
      // it is ok to add duplicate nodes
      result(wkApi.addNode(previousCluster.key, previousCluster.nodeNames.head))
      // we can't add a nonexistent node
      // we always get IllegalArgumentException if we sent request by restful api
      // However, if we use collie impl, an NoSuchElementException will be thrown...
      an[Throwable] should be thrownBy result(wkApi.addNode(previousCluster.key, CommonUtils.randomString()))
      log.info(s"[WORKER] start to add node:$newNode to a running worker cluster")
      val newCluster = result(
        wkApi
          .addNode(previousCluster.key, newNode)
          .flatMap(_ => wkApi.get(previousCluster.key))
      )
      newCluster.key shouldBe previousCluster.key
      newCluster.imageName shouldBe previousCluster.imageName
      newCluster.configTopicName shouldBe previousCluster.configTopicName
      newCluster.statusTopicName shouldBe previousCluster.statusTopicName
      newCluster.offsetTopicName shouldBe previousCluster.offsetTopicName
      newCluster.groupId shouldBe previousCluster.groupId
      newCluster.brokerClusterKey shouldBe previousCluster.brokerClusterKey
      newCluster.clientPort shouldBe previousCluster.clientPort
      newCluster.nodeNames.size - previousCluster.nodeNames.size shouldBe 1
      await(() => result(wkApi.get(newCluster.key)).nodeNames.contains(newNode))
      newCluster
    } else previousCluster
  }

  private[this] def testRemoveNodeToRunningWorkerCluster(
    previousCluster: WorkerClusterInfo,
    excludedNode: String
  ): WorkerClusterInfo = {
    await(() => result(wkApi.list()).exists(_.key == previousCluster.key))
    if (previousCluster.nodeNames.size > 1) {
      val beRemovedNode = previousCluster.nodeNames.filter(_ != excludedNode).head
      await(() => {
        log.info(s"[WORKER] start to remove node:$beRemovedNode from ${previousCluster.key}")
        result(wkApi.removeNode(previousCluster.key, beRemovedNode))
        log.info(s"[WORKER] start to remove node:$beRemovedNode from ${previousCluster.key} ... done")
        val newCluster = result(wkApi.get(previousCluster.key))
        newCluster.key == previousCluster.key &&
        newCluster.imageName == previousCluster.imageName &&
        newCluster.configTopicName == previousCluster.configTopicName &&
        newCluster.statusTopicName == previousCluster.statusTopicName &&
        newCluster.offsetTopicName == previousCluster.offsetTopicName &&
        newCluster.groupId == previousCluster.groupId &&
        newCluster.brokerClusterKey == previousCluster.brokerClusterKey &&
        newCluster.clientPort == previousCluster.clientPort &&
        previousCluster.nodeNames.size - newCluster.nodeNames.size == 1 &&
        !result(wkApi.get(newCluster.key)).nodeNames.contains(beRemovedNode)
      })
      result(wkApi.get(previousCluster.key))
    } else previousCluster
  }

  @Test
  def testMultiZkClustersOnSameNodes(): Unit = {
    if (platform.nodeNames.size < 2) skipTest("the size of nodes must be bigger than 1")
    val keys = (0 until numberOfClusters).map(_ => serviceKeyHolder.generateClusterKey())
    val zkClusters = keys.map(
      key =>
        result(
          zkApi.request
            .key(key)
            .jmxPort(CommonUtils.availablePort())
            .clientPort(CommonUtils.availablePort())
            .electionPort(CommonUtils.availablePort())
            .peerPort(CommonUtils.availablePort())
            .nodeName(platform.nodeNames.head)
            .create()
            .flatMap(
              _ =>
                zkApi
                  .start(key)
                  .flatMap(_ => zkApi.get(key))
            )
        )
    )
    // add a bit wait to make sure the cluster is up
    TimeUnit.SECONDS.sleep(5)
    zkClusters.foreach(
      zkCluster =>
        assertCluster(
          () => result(zkApi.list()),
          () => result(containerApi.get(zkCluster.key)).flatMap(_.containers),
          zkCluster.key
        )
    )
    val clusters2 = result(zkApi.list())
    zkClusters.foreach { c =>
      val another = clusters2.find(_.key == c.key).get
      another.key shouldBe c.key
      another.peerPort shouldBe c.peerPort
      another.clientPort shouldBe c.clientPort
      another.imageName shouldBe c.imageName
      another.electionPort shouldBe c.electionPort
      another.nodeNames shouldBe c.nodeNames
      result(logApi.log4ZookeeperCluster(c.key)).logs.map(_.value).foreach { log =>
        // If we start a single-node zk cluster, zk print a "error" warning to us to say that you are using a single-node,
        // and we won't see the connection exception since there is only a node.
        if (platform.nodeNames.size == 1) withClue(log)(log.toLowerCase.contains("exception") shouldBe false)
        // By contrast, if we start a true zk cluster, the exception ensues since the connections between nodes fail in beginning.
        else withClue(log)(log.toLowerCase.contains("- ERROR") shouldBe false)
        log.isEmpty shouldBe false
      }
    }
  }

  @Test
  def testMultiBkClustersOnSameNodes(): Unit = {
    if (platform.nodeNames.size < 2) skipTest("the size of nodes must be bigger than 1")
    val zkKeys = (0 until numberOfClusters).map(_ => serviceKeyHolder.generateClusterKey())
    val bkKeys = (0 until numberOfClusters).map(_ => serviceKeyHolder.generateClusterKey())
    // NOTED: It is illegal to run multi bk clusters on same zk cluster so we have got to instantiate multi zk clusters first.
    val zkClusters = zkKeys.map(
      key =>
        result(
          zkApi.request
            .key(key)
            .jmxPort(CommonUtils.availablePort())
            .clientPort(CommonUtils.availablePort())
            .electionPort(CommonUtils.availablePort())
            .peerPort(CommonUtils.availablePort())
            .nodeName(platform.nodeNames.head)
            .create()
            .flatMap(
              _ =>
                zkApi
                  .start(key)
                  .flatMap(_ => zkApi.get(key))
            )
        )
    )
    zkClusters.foreach(
      zkCluster =>
        assertCluster(
          () => result(zkApi.list()),
          () => result(containerApi.get(zkCluster.key)).flatMap(_.containers),
          zkCluster.key
        )
    )
    zkClusters.zipWithIndex.foreach {
      case (zk, index) =>
        val bkCluster = result(
          bkApi.request
            .key(bkKeys(index))
            .clientPort(CommonUtils.availablePort())
            .jmxPort(CommonUtils.availablePort())
            .zookeeperClusterKey(zk.key)
            .nodeName(platform.nodeNames.head)
            .create()
        )
        result(bkApi.start(bkCluster.key))
        testTopic(bkCluster)
    }
  }

  @Test
  def testMultiWkClustersOnSameNodes(): Unit = {
    if (platform.nodeNames.size < 2) skipTest("the size of nodes must be bigger than 1")
    val zkKey            = serviceKeyHolder.generateClusterKey()
    val bkKey            = serviceKeyHolder.generateClusterKey()
    val wkKeys           = (0 until numberOfClusters).map(_ => serviceKeyHolder.generateClusterKey())
    val groupIds         = (0 until numberOfClusters).map(_ => CommonUtils.randomString(10))
    val configTopicNames = (0 until numberOfClusters).map(_ => CommonUtils.randomString(10))
    val offsetTopicNames = (0 until numberOfClusters).map(_ => CommonUtils.randomString(10))
    val statusTopicNames = (0 until numberOfClusters).map(_ => CommonUtils.randomString(10))
    log.info(s"start to run zk cluster:$zkKey")
    val zkCluster = result(
      zkApi.request
        .key(serviceKeyHolder.generateClusterKey())
        .jmxPort(CommonUtils.availablePort())
        .clientPort(CommonUtils.availablePort())
        .electionPort(CommonUtils.availablePort())
        .peerPort(CommonUtils.availablePort())
        .nodeName(platform.nodeNames.head)
        .create()
    )
    result(zkApi.start(zkCluster.key))
    assertCluster(
      () => result(zkApi.list()),
      () => result(containerApi.get(zkCluster.key)).flatMap(_.containers),
      zkCluster.key
    )
    log.info(s"start to run bk cluster:$bkKey")
    val bkCluster = result(
      bkApi.request
        .key(bkKey)
        .clientPort(CommonUtils.availablePort())
        .jmxPort(CommonUtils.availablePort())
        .zookeeperClusterKey(zkCluster.key)
        .nodeName(platform.nodeNames.head)
        .create()
    )
    result(bkApi.start(bkCluster.key))
    assertCluster(
      () => result(bkApi.list()),
      () => result(containerApi.get(bkCluster.key)).flatMap(_.containers),
      bkCluster.key
    )
    log.info(s"start to run multi wk clusters:$wkKeys")
    val wkClusters = wkKeys.zipWithIndex.map {
      case (wkKey, index) =>
        result(
          wkApi.request
            .key(wkKey)
            .clientPort(CommonUtils.availablePort())
            .jmxPort(CommonUtils.availablePort())
            .brokerClusterKey(bkCluster.key)
            .nodeNames(platform.nodeNames)
            .groupId(groupIds(index))
            .configTopicName(configTopicNames(index))
            .statusTopicName(statusTopicNames(index))
            .offsetTopicName(offsetTopicNames(index))
            .create()
        )
    }
    wkClusters.foreach(wk => result(wkApi.start(wk.key)))
    log.info(s"check multi wk clusters:$wkKeys")
    // add a bit wait to make sure the cluster is up
    TimeUnit.SECONDS.sleep(10)
    wkClusters.foreach(
      wkCluster =>
        assertCluster(
          () => result(wkApi.list()),
          () => result(containerApi.get(wkCluster.key)).flatMap(_.containers),
          wkCluster.key
        )
    )
    wkKeys.zipWithIndex.map {
      case (wkKey, index) =>
        wkClusters.find(_.key == wkKey).get.groupId shouldBe groupIds(index)
        wkClusters.find(_.key == wkKey).get.configTopicName shouldBe configTopicNames(index)
        wkClusters.find(_.key == wkKey).get.offsetTopicName shouldBe offsetTopicNames(index)
        wkClusters.find(_.key == wkKey).get.statusTopicName shouldBe statusTopicNames(index)
        wkClusters.find(_.key == wkKey).get.brokerClusterKey shouldBe bkCluster.key
    }

    log.info(s"check multi wk clusters:$wkKeys by list")
    result(wkApi.list()).foreach { cluster =>
      testConnectors(cluster)
      testJmx(cluster)
    }
  }
}
