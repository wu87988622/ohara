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

import java.time.Duration
import java.util.concurrent.ExecutionException

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.it.IntegrationTest
import com.island.ohara.kafka.exception.OharaExecutionException
import com.island.ohara.kafka.{BrokerClient, Consumer, Producer}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.errors.{InvalidReplicationFactorException, UnknownTopicOrPartitionException}
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * This abstract class extracts the "required" information of running tests on true env.
  * All checks are verified in this class but we do run all test cases on different test in order to avoid
  * slow test cases run by single test jvm.
  *
  * NOTED: this test will forward random ports so it would be better to "close" firewall of remote node.
  */
abstract class BasicTests4Collie extends IntegrationTest with Matchers {
  private[this] val log = Logger(classOf[BasicTests4Collie])
  private[this] val numberOfClusters = 2
  protected val nodeCache: Seq[Node]

  //--------------------------------------------------[zk operations]--------------------------------------------------//
  protected def zk_exist(clusterName: String): Future[Boolean]
  protected def zk_create(clusterName: String,
                          clientPort: Int,
                          electionPort: Int,
                          peerPort: Int,
                          nodeNames: Seq[String]): Future[ZookeeperClusterInfo]
  protected def zk_cluster(clusterName: String): Future[ZookeeperClusterInfo] =
    zk_clusters().map(_.find(_.name == clusterName).get)
  protected def zk_clusters(): Future[Seq[ZookeeperClusterInfo]]
  protected def zk_logs(clusterName: String): Future[Seq[String]]
  protected def zk_containers(clusterName: String): Future[Seq[ContainerInfo]]
  protected def zk_delete(clusterName: String): Future[ZookeeperClusterInfo]

  //--------------------------------------------------[bk operations]--------------------------------------------------//
  protected def bk_exist(clusterName: String): Future[Boolean]
  protected def bk_create(clusterName: String,
                          clientPort: Int,
                          exporterPort: Int,
                          zkClusterName: String,
                          nodeNames: Seq[String]): Future[BrokerClusterInfo]
  protected def bk_cluster(clusterName: String): Future[BrokerClusterInfo] =
    bk_clusters().map(_.find(_.name == clusterName).get)
  protected def bk_clusters(): Future[Seq[BrokerClusterInfo]]
  protected def bk_logs(clusterName: String): Future[Seq[String]]
  protected def bk_containers(clusterName: String): Future[Seq[ContainerInfo]]
  protected def bk_delete(clusterName: String): Future[BrokerClusterInfo]
  protected def bk_addNode(clusterName: String, nodeName: String): Future[BrokerClusterInfo]
  protected def bk_removeNode(clusterName: String, nodeName: String): Future[BrokerClusterInfo]

  //--------------------------------------------------[wk operations]--------------------------------------------------//
  protected def wk_exist(clusterName: String): Future[Boolean]
  protected def wk_create(clusterName: String,
                          clientPort: Int,
                          bkClusterName: String,
                          nodeNames: Seq[String]): Future[WorkerClusterInfo]
  protected def wk_create(clusterName: String,
                          clientPort: Int,
                          groupId: String,
                          configTopicName: String,
                          statusTopicName: String,
                          offsetTopicName: String,
                          bkClusterName: String,
                          nodeNames: Seq[String]): Future[WorkerClusterInfo]
  protected def wk_cluster(clusterName: String): Future[WorkerClusterInfo] =
    wk_clusters().map(_.find(_.name == clusterName).get)
  protected def wk_clusters(): Future[Seq[WorkerClusterInfo]]
  protected def wk_logs(clusterName: String): Future[Seq[String]]
  protected def wk_containers(clusterName: String): Future[Seq[ContainerInfo]]
  protected def wk_delete(clusterName: String): Future[WorkerClusterInfo]
  protected def wk_addNode(clusterName: String, nodeName: String): Future[WorkerClusterInfo]
  protected def wk_removeNode(clusterName: String, nodeName: String): Future[WorkerClusterInfo]

  /**
    * used to debug...
    */
  protected val cleanup: Boolean = true

  protected def generateClusterName(): String

  @Test
  def testZk(): Unit = {
    log.info("start to run zookeeper cluster")
    val nodeName: String = nodeCache.head.name
    val clusterName = generateClusterName()
    result(zk_exist(clusterName)) shouldBe false
    val clientPort = CommonUtils.availablePort()
    val electionPort = CommonUtils.availablePort()
    val peerPort = CommonUtils.availablePort()
    def assert(zkCluster: ZookeeperClusterInfo): ZookeeperClusterInfo = {
      zkCluster.name shouldBe clusterName
      zkCluster.nodeNames.head shouldBe nodeName
      zkCluster.clientPort shouldBe clientPort
      zkCluster.peerPort shouldBe peerPort
      zkCluster.electionPort shouldBe electionPort
      zkCluster
    }

    val zkCluster = assert(
      result(
        zk_create(
          clusterName = clusterName,
          clientPort = clientPort,
          electionPort = electionPort,
          peerPort = peerPort,
          nodeNames = Seq(nodeName)
        )))
    try {
      assertCluster(() => result(zk_clusters()), zkCluster.name)
      assert(result(zk_cluster(zkCluster.name)))
      log.info("start to run zookeeper cluster ... done")
      result(zk_exist(zkCluster.name)) shouldBe true
      // we can't assume the size since other tests may create zk cluster at the same time
      result(zk_clusters()).isEmpty shouldBe false
      log.info(s"verify number of zk clusters... done")
      result(zk_logs(clusterName)).size shouldBe 1
      log.info(s"verify number of log... done")
      result(zk_logs(clusterName)).foreach(log =>
        withClue(log) {
          log.contains("exception") shouldBe false
          log.isEmpty shouldBe false
      })
      log.info(s"verify log of zk clusters... done")
      val container = result(zk_containers(clusterName)).head
      log.info(s"get containers from zk:$clusterName... done")
      container.nodeName shouldBe nodeName
      container.name.contains(clusterName) shouldBe true
      container.hostname.contains(nodeName) shouldBe true
      container.portMappings.head.portPairs.size shouldBe 3
      container.portMappings.head.portPairs.exists(_.containerPort == clientPort) shouldBe true
      container.portMappings.head.portPairs.exists(_.containerPort == electionPort) shouldBe true
      container.portMappings.head.portPairs.exists(_.containerPort == peerPort) shouldBe true
      container.environments.exists(_._2 == clientPort.toString) shouldBe true
      container.environments.exists(_._2 == electionPort.toString) shouldBe true
      container.environments.exists(_._2 == peerPort.toString) shouldBe true
    } finally if (cleanup) result(zk_delete(zkCluster.name))
  }

  @Test
  def testBroker(): Unit = {
    val zkCluster = result(
      zk_create(
        clusterName = generateClusterName(),
        clientPort = CommonUtils.availablePort(),
        electionPort = CommonUtils.availablePort(),
        peerPort = CommonUtils.availablePort(),
        nodeNames = Seq(nodeCache.head.name)
      ))
    assertCluster(() => result(zk_clusters()), zkCluster.name)
    try {
      log.info("[BROKER] start to run broker cluster")
      val clusterName = generateClusterName()
      result(bk_exist(clusterName)) shouldBe false
      log.info(s"[BROKER] verify existence of broker cluster:$clusterName...done")
      val nodeName: String = nodeCache.head.name
      val clientPort = CommonUtils.availablePort()
      val exporterPort = CommonUtils.availablePort()
      def assert(brokerCluster: BrokerClusterInfo): BrokerClusterInfo = {
        brokerCluster.zookeeperClusterName shouldBe zkCluster.name
        brokerCluster.name shouldBe clusterName
        brokerCluster.nodeNames.head shouldBe nodeName
        brokerCluster.clientPort shouldBe clientPort
        brokerCluster.exporterPort shouldBe exporterPort
        brokerCluster
      }

      val bkCluster = assert(
        result(
          bk_create(
            clusterName = clusterName,
            clientPort = clientPort,
            exporterPort = exporterPort,
            zkClusterName = zkCluster.name,
            nodeNames = Seq(nodeName)
          )))
      log.info("[BROKER] start to run broker cluster...done")
      assertCluster(() => result(bk_clusters()), bkCluster.name)
      assert(result(bk_cluster(bkCluster.name)))
      log.info("[BROKER] verify cluster api...done")
      try {
        result(bk_exist(bkCluster.name)) shouldBe true
        // we can't assume the size since other tests may create zk cluster at the same time
        result(bk_clusters()).isEmpty shouldBe false
        result(bk_logs(clusterName)).size shouldBe 1
        result(bk_logs(clusterName)).foreach(log =>
          withClue(log) {
            log.contains("exception") shouldBe false
            log.isEmpty shouldBe false
        })
        val container = result(bk_containers(clusterName)).head
        container.nodeName shouldBe nodeName
        container.name.contains(clusterName) shouldBe true
        container.hostname.contains(clusterName) shouldBe true
        container.portMappings.head.portPairs.size shouldBe 2
        container.portMappings.head.portPairs.exists(_.containerPort == clientPort) shouldBe true
        container.environments.exists(_._2 == clientPort.toString) shouldBe true
        testTopic(
          testRemoveNodeToRunningBrokerCluster(testTopic(testAddNodeToRunningBrokerCluster(testTopic(bkCluster)))))
      } finally if (cleanup) {
        result(bk_delete(bkCluster.name))
        assertNoCluster(() => result(bk_clusters()), bkCluster.name)
      }
    } finally if (cleanup) result(zk_delete(zkCluster.name))
  }

  private[this] def testAddNodeToRunningBrokerCluster(previousCluster: BrokerClusterInfo): BrokerClusterInfo = {
    result(bk_exist(previousCluster.name)) shouldBe true
    log.info(s"[BROKER] nodeCache:$nodeCache previous:${previousCluster.nodeNames}")
    an[IllegalArgumentException] should be thrownBy result(
      bk_removeNode(previousCluster.name, previousCluster.nodeNames.head))
    val freeNodes = nodeCache.filterNot(node => previousCluster.nodeNames.contains(node.name))
    if (freeNodes.nonEmpty) {
      // we can't add duplicate node
      an[IllegalArgumentException] should be thrownBy result(
        bk_addNode(previousCluster.name, previousCluster.nodeNames.head))
      // we can't add a nonexistent node
      // we always get IllegalArgumentException if we sent request by restful api
      // However, if we use collie impl, an NoSuchElementException will be thrown...
      an[Throwable] should be thrownBy result(bk_addNode(previousCluster.name, CommonUtils.randomString()))
      val newNode = freeNodes.head.name
      log.info(s"[BROKER] add new node:$newNode to cluster:${previousCluster.name}")
      val newCluster = result(bk_addNode(previousCluster.name, newNode))
      log.info(s"[BROKER] add new node:$newNode to cluster:${previousCluster.name}...done")
      newCluster.name shouldBe previousCluster.name
      newCluster.imageName shouldBe previousCluster.imageName
      newCluster.zookeeperClusterName shouldBe previousCluster.zookeeperClusterName
      newCluster.exporterPort shouldBe previousCluster.exporterPort
      newCluster.clientPort shouldBe previousCluster.clientPort
      newCluster.nodeNames.size - previousCluster.nodeNames.size shouldBe 1
      await(() => result(bk_cluster(newCluster.name)).nodeNames.contains(newNode))
      newCluster
    } else previousCluster
  }

  private[this] def testTopic(cluster: BrokerClusterInfo): BrokerClusterInfo = {
    val topicName = CommonUtils.randomString()
    val brokers = cluster.nodeNames.map(_ + s":${cluster.clientPort}").mkString(",")
    log.info(s"[BROKER] start to create topic:$topicName on broker cluster:$brokers")
    val brokerClient = BrokerClient.of(brokers)
    try {
      log.info(s"[BROKER] start to check the sync information. active broker nodes:${cluster.nodeNames}")
      // make sure all active broker nodes are sync!
      await(
        () => {
          brokerClient.brokerPorts().size() == cluster.nodeNames.size
        }
      )
      log.info(s"[BROKER] start to check the sync information. active broker nodes:${cluster.nodeNames} ... done")
      await(
        () => {
          try {
            brokerClient
              .topicCreator()
              .numberOfPartitions(1)
              .numberOfReplications(cluster.nodeNames.size.asInstanceOf[Short])
              .topicName(topicName)
              .create()
            true
          } catch {
            case e: OharaExecutionException =>
              e.getCause match {
                // the new broker needs time to sync information to other existed bk nodes.
                case _: InvalidReplicationFactorException => false
                case _: Throwable                         => throw e.getCause
              }
          }
        }
      )
      log.info(s"[BROKER] start to create topic:$topicName on broker cluster:$brokers ... done")
      val producer = Producer
        .builder[String, String]()
        .connectionProps(brokers)
        .allAcks()
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .build()
      log.info(s"[BROKER] start to send data")
      try {
        await(
          () => {
            try producer
              .sender()
              .key("abc")
              .value("abc_value")
              .topicName(topicName)
              .send()
              .get()
              .topicName() == topicName
            catch {
              case e: ExecutionException =>
                e.getCause match {
                  case _: UnknownTopicOrPartitionException => false
                }
            }
          }
        )

      } finally producer.close()
      log.info(s"[BROKER] start to send data ... done")
      log.info(s"[BROKER] start to receive data")
      val consumer = Consumer
        .builder[String, String]()
        .connectionProps(brokers)
        .offsetFromBegin()
        .topicName(topicName)
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .build()
      try {
        val records = consumer.poll(Duration.ofSeconds(30), 1)
        records.size() shouldBe 1
        records.get(0).key().get shouldBe "abc"
        records.get(0).value().get shouldBe "abc_value"
        log.info(s"[BROKER] start to receive data ... done")
      } finally consumer.close()
      brokerClient.deleteTopic(topicName)
    } finally brokerClient.close()
    cluster
  }

  private[this] def testRemoveNodeToRunningBrokerCluster(previousCluster: BrokerClusterInfo): BrokerClusterInfo = {
    result(bk_exist(previousCluster.name)) shouldBe true
    if (previousCluster.nodeNames.size > 1) {
      val beRemovedNode = previousCluster.nodeNames.head
      log.info(s"[BROKER] start to remove node:$beRemovedNode from bk cluster:${previousCluster.name}")
      val newCluster = result(bk_removeNode(previousCluster.name, beRemovedNode))
      log.info(s"[BROKER] start to remove node:$beRemovedNode from bk cluster:${previousCluster.name} ... done")
      newCluster.name shouldBe previousCluster.name
      newCluster.imageName shouldBe previousCluster.imageName
      newCluster.zookeeperClusterName shouldBe previousCluster.zookeeperClusterName
      newCluster.clientPort shouldBe previousCluster.clientPort
      previousCluster.nodeNames.size - newCluster.nodeNames.size shouldBe 1
      await(() => !result(bk_cluster(newCluster.name)).nodeNames.contains(beRemovedNode))
      newCluster
    } else previousCluster
  }

  @Test
  def testWorker(): Unit = {
    val zkCluster = result(
      zk_create(
        clusterName = generateClusterName(),
        clientPort = CommonUtils.availablePort(),
        electionPort = CommonUtils.availablePort(),
        peerPort = CommonUtils.availablePort(),
        nodeNames = Seq(nodeCache.head.name)
      ))
    try {
      assertCluster(() => result(zk_clusters()), zkCluster.name)
      val bkCluster = result(
        bk_create(
          clusterName = generateClusterName(),
          clientPort = CommonUtils.availablePort(),
          exporterPort = CommonUtils.availablePort(),
          zkClusterName = zkCluster.name,
          nodeNames = Seq(nodeCache.head.name)
        ))
      try {
        assertCluster(() => result(bk_clusters()), bkCluster.name)
        log.info("[WORKER] start to test worker")
        val nodeName = nodeCache.head.name
        val clusterName = generateClusterName()
        result(wk_exist(clusterName)) shouldBe false
        log.info("[WORKER] verify:nonExists done")
        val clientPort = CommonUtils.availablePort()
        def assert(workerCluster: WorkerClusterInfo): WorkerClusterInfo = {
          workerCluster.brokerClusterName shouldBe bkCluster.name
          workerCluster.name shouldBe clusterName
          workerCluster.nodeNames.head shouldBe nodeName
          workerCluster.clientPort shouldBe clientPort
          workerCluster.configTopicPartitions shouldBe 1
          workerCluster.configTopicReplications shouldBe 1
          workerCluster.statusTopicPartitions shouldBe 1
          workerCluster.statusTopicReplications shouldBe 1
          workerCluster.offsetTopicPartitions shouldBe 1
          workerCluster.offsetTopicReplications shouldBe 1
          workerCluster
        }

        val wkCluster = assert(
          result(
            wk_create(
              clusterName = clusterName,
              clientPort = clientPort,
              bkClusterName = bkCluster.name,
              nodeNames = Seq(nodeName)
            )))
        assertCluster(() => result(wk_clusters()), wkCluster.name)
        log.info("[WORKER] create done")
        assert(result(wk_cluster(wkCluster.name)))
        log.info("[WORKER] verify:create done")
        try {
          result(wk_exist(wkCluster.name)) shouldBe true
          log.info("[WORKER] verify:exist done")
          // we can't assume the size since other tests may create zk cluster at the same time
          result(wk_clusters()).isEmpty shouldBe false
          log.info("[WORKER] verify:list done")
          result(wk_logs(clusterName)).size shouldBe 1
          result(wk_logs(clusterName)).foreach(log =>
            withClue(log) {
              log.contains("- ERROR") shouldBe false
              log.isEmpty shouldBe false
          })
          log.info("[WORKER] verify:log done")
          val container = result(wk_containers(clusterName)).head
          container.nodeName shouldBe nodeName
          container.name.contains(clusterName) shouldBe true
          container.hostname.contains(clusterName) shouldBe true
          // ClusterCollieImpl applies --network=host to all worker containers so there is no port mapping.
          // The following checks are disabled rather than deleted since it seems like a bug if we don't check the port mapping.
          // container.portMappings.head.portPairs.size shouldBe 1
          // container.portMappings.head.portPairs.exists(_.containerPort == clientPort) shouldBe true
          container.environments.exists(_._2 == clientPort.toString) shouldBe true
          testPlugins(
            testRemoveNodeToRunningWorkerCluster(
              testPlugins(testAddNodeToRunningWorkerCluster(testPlugins(wkCluster)))))
        } finally if (cleanup) {
          result(wk_delete(wkCluster.name))
          assertNoCluster(() => result(wk_clusters()), wkCluster.name)
        }
      } finally if (cleanup) {
        result(bk_delete(bkCluster.name))
        assertNoCluster(() => result(bk_clusters()), bkCluster.name)
      }
    } finally if (cleanup) result(zk_delete(zkCluster.name))

  }

  private[this] def testPlugins(cluster: WorkerClusterInfo): WorkerClusterInfo = {
    val workerClient = WorkerClient(s"${cluster.nodeNames.head}:${cluster.clientPort}")
    await(
      () =>
        try result(workerClient.plugins).nonEmpty
        catch {
          case e: Throwable =>
            log.info(s"[WORKER] worker cluster:${cluster.name} is starting ... retry", e)
            false
      }
    )
    cluster
  }
  private[this] def testAddNodeToRunningWorkerCluster(previousCluster: WorkerClusterInfo): WorkerClusterInfo = {
    result(wk_exist(previousCluster.name)) shouldBe true
    an[IllegalArgumentException] should be thrownBy result(
      wk_removeNode(previousCluster.name, previousCluster.nodeNames.head))
    val freeNodes = nodeCache.filterNot(node => previousCluster.nodeNames.contains(node.name))
    if (freeNodes.nonEmpty) {
      val newNode = freeNodes.head.name
      // we can't add duplicate node
      an[IllegalArgumentException] should be thrownBy result(
        wk_addNode(previousCluster.name, previousCluster.nodeNames.head))
      // we can't add a nonexistent node
      // we always get IllegalArgumentException if we sent request by restful api
      // However, if we use collie impl, an NoSuchElementException will be thrown...
      an[Throwable] should be thrownBy result(wk_addNode(previousCluster.name, CommonUtils.randomString()))
      log.info(s"[WORKER] start to add node:$newNode to a running worker cluster")
      val newCluster = result(wk_addNode(previousCluster.name, newNode))
      newCluster.name shouldBe previousCluster.name
      newCluster.imageName shouldBe previousCluster.imageName
      newCluster.configTopicName shouldBe previousCluster.configTopicName
      newCluster.statusTopicName shouldBe previousCluster.statusTopicName
      newCluster.offsetTopicName shouldBe previousCluster.offsetTopicName
      newCluster.groupId shouldBe previousCluster.groupId
      newCluster.brokerClusterName shouldBe previousCluster.brokerClusterName
      newCluster.clientPort shouldBe previousCluster.clientPort
      newCluster.nodeNames.size - previousCluster.nodeNames.size shouldBe 1
      await(() => result(wk_cluster(newCluster.name)).nodeNames.contains(newNode))
      newCluster
    } else previousCluster
  }

  private[this] def testRemoveNodeToRunningWorkerCluster(previousCluster: WorkerClusterInfo): WorkerClusterInfo = {
    result(wk_exist(previousCluster.name)) shouldBe true
    if (previousCluster.nodeNames.size > 1) {
      val beRemovedNode = previousCluster.nodeNames.head
      log.info(s"[WORKER] start to remove node:$beRemovedNode from ${previousCluster.name}")
      val newCluster = result(wk_removeNode(previousCluster.name, beRemovedNode))
      newCluster.name shouldBe previousCluster.name
      newCluster.imageName shouldBe previousCluster.imageName
      newCluster.configTopicName shouldBe previousCluster.configTopicName
      newCluster.statusTopicName shouldBe previousCluster.statusTopicName
      newCluster.offsetTopicName shouldBe previousCluster.offsetTopicName
      newCluster.groupId shouldBe previousCluster.groupId
      newCluster.brokerClusterName shouldBe previousCluster.brokerClusterName
      newCluster.clientPort shouldBe previousCluster.clientPort
      previousCluster.nodeNames.size - newCluster.nodeNames.size shouldBe 1
      await(() => !result(wk_cluster(newCluster.name)).nodeNames.contains(beRemovedNode))
      newCluster
    } else previousCluster
  }

  @Test
  def testMultiZkClustersOnSingleNode(): Unit = {
    val names = (0 until numberOfClusters).map(_ => generateClusterName())
    try {
      val clusters = names.map { name =>
        result(
          zk_create(
            clusterName = name,
            clientPort = CommonUtils.availablePort(),
            electionPort = CommonUtils.availablePort(),
            peerPort = CommonUtils.availablePort(),
            nodeNames = nodeCache.map(_.name)
          ))
      }
      assertClusters(() => result(zk_clusters()), clusters.map(_.name))
      val clusters2 = result(zk_clusters())
      clusters.foreach { c =>
        val another = clusters2.find(_.name == c.name).get
        another.name shouldBe c.name
        another.peerPort shouldBe c.peerPort
        another.clientPort shouldBe c.clientPort
        another.imageName shouldBe c.imageName
        another.electionPort shouldBe c.electionPort
        another.nodeNames.toSet shouldBe c.nodeNames.toSet
        result(zk_logs(c.name)).foreach { log =>
          withClue(log)(log.contains("- ERROR") shouldBe false)
          log.isEmpty shouldBe false
        }
      }
    } finally if (cleanup) names.foreach { name =>
      try result(zk_delete(name))
      catch {
        case _: Throwable =>
        // do nothing
      }
    }
  }

  @Test
  def testMultiBkClustersOnSingleNode(): Unit = {
    val zkNames = (0 until numberOfClusters).map(_ => generateClusterName())
    val bkNames = (0 until numberOfClusters).map(_ => generateClusterName())
    try {
      // NOTED: It is illegal to run multi bk clusters on same zk cluster so we have got to instantiate multi zk clusters first.
      val zks = zkNames.map { name =>
        result(
          zk_create(
            clusterName = name,
            clientPort = CommonUtils.availablePort(),
            electionPort = CommonUtils.availablePort(),
            peerPort = CommonUtils.availablePort(),
            nodeNames = Seq(nodeCache.head.name)
          ))
      }
      assertClusters(() => result(zk_clusters()), zks.map(_.name))
      val bks = zks.zipWithIndex.map {
        case (zk, index) =>
          result(
            bk_create(
              clusterName = bkNames(index),
              clientPort = CommonUtils.availablePort(),
              exporterPort = CommonUtils.availablePort(),
              zkClusterName = zk.name,
              nodeNames = Seq(nodeCache.head.name)
            ))
      }
      assertClusters(() => result(bk_clusters()), bks.map(_.name))
      val clusters2 = result(bk_clusters())
      bks.foreach { c =>
        val another = clusters2.find(_.name == c.name).get
        another.clientPort shouldBe c.clientPort
        another.exporterPort shouldBe c.exporterPort
        another.zookeeperClusterName shouldBe c.zookeeperClusterName
        another.connectionProps shouldBe c.connectionProps
        testTopic(c)
      }
    } finally if (cleanup) {
      bkNames.foreach { name =>
        try result(bk_delete(name))
        catch {
          case _: Throwable =>
          // do nothing
        }
      }
      assertNoClusters(() => result(bk_clusters()), bkNames)
      zkNames.foreach { name =>
        try result(zk_delete(name))
        catch {
          case _: Throwable =>
          // do nothing
        }
      }
    }
  }

  @Test
  def testMultiWkClustersOnSingleNode(): Unit = {
    val zkName = generateClusterName()
    val bkName = generateClusterName()
    val wkNames = (0 until numberOfClusters).map(_ => generateClusterName())
    val groupIds = (0 until numberOfClusters).map(_ => CommonUtils.randomString(10))
    val configTopicNames = (0 until numberOfClusters).map(_ => CommonUtils.randomString(10))
    val offsetTopicNames = (0 until numberOfClusters).map(_ => CommonUtils.randomString(10))
    val statusTopicNames = (0 until numberOfClusters).map(_ => CommonUtils.randomString(10))
    try {
      log.info(s"start to run zk cluster:$zkName")
      val zk = result(
        zk_create(
          clusterName = zkName,
          clientPort = CommonUtils.availablePort(),
          electionPort = CommonUtils.availablePort(),
          peerPort = CommonUtils.availablePort(),
          nodeNames = Seq(nodeCache.head.name)
        )
      )
      assertCluster(() => result(zk_clusters()), zk.name)

      log.info(s"start to run bk cluster:$bkName")
      val bk = result(
        bk_create(
          clusterName = bkName,
          clientPort = CommonUtils.availablePort(),
          exporterPort = CommonUtils.availablePort(),
          zkClusterName = zk.name,
          nodeNames = Seq(nodeCache.head.name)
        ))
      assertCluster(() => result(bk_clusters()), bk.name)

      log.info(s"start to run multi wk clusters:$wkNames")
      val clusters = wkNames.zipWithIndex.map {
        case (wkName, index) =>
          result(
            wk_create(
              clusterName = wkName,
              clientPort = CommonUtils.availablePort(),
              bkClusterName = bk.name,
              groupId = groupIds(index),
              configTopicName = configTopicNames(index),
              offsetTopicName = offsetTopicNames(index),
              statusTopicName = statusTopicNames(index),
              nodeNames = nodeCache.map(_.name)
            ))
      }

      log.info(s"check multi wk clusters:$wkNames")
      assertClusters(() => result(wk_clusters()), clusters.map(_.name))
      wkNames.zipWithIndex.map {
        case (wkName, index) =>
          clusters.find(_.name == wkName).get.groupId shouldBe groupIds(index)
          clusters.find(_.name == wkName).get.configTopicName shouldBe configTopicNames(index)
          clusters.find(_.name == wkName).get.offsetTopicName shouldBe offsetTopicNames(index)
          clusters.find(_.name == wkName).get.statusTopicName shouldBe statusTopicNames(index)
          clusters.find(_.name == wkName).get.brokerClusterName shouldBe bk.name
      }

      log.info(s"check multi wk clusters:$wkNames by list")
      val clusters2 = result(wk_clusters())
      clusters.foreach { c =>
        val another = clusters2.find(_.name == c.name).get
        another.name shouldBe c.name
        another.brokerClusterName shouldBe c.brokerClusterName
        another.clientPort shouldBe c.clientPort
        another.groupId shouldBe c.groupId
        another.configTopicName shouldBe c.configTopicName
        another.configTopicPartitions shouldBe c.configTopicPartitions
        another.configTopicReplications shouldBe c.configTopicReplications
        another.statusTopicName shouldBe c.statusTopicName
        another.statusTopicPartitions shouldBe c.statusTopicPartitions
        another.statusTopicReplications shouldBe c.statusTopicReplications
        another.offsetTopicName shouldBe c.offsetTopicName
        another.offsetTopicPartitions shouldBe c.offsetTopicPartitions
        another.offsetTopicReplications shouldBe c.offsetTopicReplications
        another.jarNames shouldBe c.jarNames
        another.imageName shouldBe c.imageName
        testPlugins(c)
      }
    } finally if (cleanup) {
      wkNames.foreach { name =>
        try result(wk_delete(name))
        catch {
          case _: Throwable =>
          // do nothing
        }
      }
      assertNoClusters(() => result(wk_clusters()), wkNames)
      try result(bk_delete(bkName))
      catch {
        case _: Throwable =>
        // do nothing
      }
      assertNoCluster(() => result(bk_clusters()), bkName)
      try result(zk_delete(zkName))
      catch {
        case _: Throwable =>
        // do nothing
      }
    }
  }
}
