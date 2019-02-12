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

import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.it.IntegrationTest
import com.island.ohara.kafka.{BrokerClient, Consumer, Producer}
import com.typesafe.scalalogging.Logger
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * This abstract class extracts the "required" information of running tests on true env.
  * All checks are verified in this class but we do run all test cases on different test in order to avoid
  * slow test cases run by single test jvm.
  *
  * NOTED: this test will forward random ports so it would be better to "close" firewall of remote node.
  */
abstract class BasicTestsOfCollie extends IntegrationTest with Matchers {
  private[this] val log = Logger(classOf[BasicTestsOfCollie])
  protected def nodeCache: Seq[Node]
  protected def clusterCollie: ClusterCollie

  /**
    * used to debug...
    */
  private[this] val cleanup = true

  private[this] def result[T](f: Future[T]): T = Await.result(f, 60 seconds)

  /**
    * create a zk cluster env in running test case.
    * @param f test case
    */
  private[this] def testZk(f: ZookeeperClusterInfo => Unit): Unit = {
    log.info("start to run zookeeper cluster")
    val zookeeperCollie = clusterCollie.zookeepersCollie()
    val nodeName: String = nodeCache.head.name
    val clusterName = CommonUtil.randomString(10)
    result(zookeeperCollie.nonExists(clusterName)) shouldBe true
    val clientPort = CommonUtil.availablePort()
    val electionPort = CommonUtil.availablePort()
    val peerPort = CommonUtil.availablePort()
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
        zookeeperCollie
          .creator()
          .clientPort(clientPort)
          .electionPort(electionPort)
          .peerPort(peerPort)
          .clusterName(clusterName)
          .nodeName(nodeName)
          .create()))
    assert(result(zookeeperCollie.cluster(zkCluster.name))._1)
    log.info("start to run zookeeper cluster ... done")
    try {
      result(zookeeperCollie.exists(zkCluster.name)) shouldBe true
      // we can't assume the size since other tests may create zk cluster at the same time
      result(zookeeperCollie.clusters()).isEmpty shouldBe false
      log.info(s"verify number of zk clusters... done")
      result(zookeeperCollie.logs(clusterName)).size shouldBe 1
      result(zookeeperCollie.logs(clusterName)).values.foreach(log =>
        withClue(log) {
          log.contains("exception") shouldBe false
          log.isEmpty shouldBe false
      })
      log.info(s"verify log of zk clusters... done")
      val container = result(zookeeperCollie.containers(clusterName)).head
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
      f(zkCluster)
    } finally if (cleanup) result(zookeeperCollie.remove(zkCluster.name))
    log.info("cleanup zookeeper cluster ... done")
  }

  private[this] def testBroker(f: BrokerClusterInfo => Unit): Unit = testZk { zkCluster =>
    log.info("start to run broker cluster")
    val brokerCollie = clusterCollie.brokerCollie()
    val nodeName = nodeCache.head.name
    val clusterName = CommonUtil.randomString(10)
    result(brokerCollie.nonExists(clusterName)) shouldBe true
    log.info(s"verify existence of broker cluster:$clusterName...done")
    val clientPort = CommonUtil.availablePort()
    val exporterPort = CommonUtil.availablePort()
    def assert(brokerCluster: BrokerClusterInfo): BrokerClusterInfo = {
      brokerCluster.zookeeperClusterName shouldBe zkCluster.name
      brokerCluster.name shouldBe clusterName
      brokerCluster.nodeNames.head shouldBe nodeName
      brokerCluster.clientPort shouldBe clientPort
      brokerCluster.exporterPort shouldBe exporterPort
      brokerCluster
    }
    val brokerCluster = assert(
      result(
        brokerCollie
          .creator()
          .clusterName(clusterName)
          .clientPort(clientPort)
          .exporterPort(exporterPort)
          .zookeeperClusterName(zkCluster.name)
          .nodeName(nodeName)
          .create()))
    log.info("start to run broker cluster...done")
    assert(result(brokerCollie.cluster(brokerCluster.name))._1)
    log.info("verify cluster api...done")
    try {
      result(brokerCollie.exists(brokerCluster.name)) shouldBe true
      // we can't assume the size since other tests may create zk cluster at the same time
      result(brokerCollie.clusters()).isEmpty shouldBe false
      result(brokerCollie.logs(clusterName)).size shouldBe 1
      result(brokerCollie.logs(clusterName)).values.foreach(log =>
        withClue(log) {
          log.contains("exception") shouldBe false
          log.isEmpty shouldBe false
      })
      val container = result(brokerCollie.containers(clusterName)).head
      container.nodeName shouldBe nodeName
      container.name.contains(clusterName) shouldBe true
      container.hostname.contains(clusterName) shouldBe true
      container.portMappings.head.portPairs.size shouldBe 2
      container.portMappings.head.portPairs.exists(_.containerPort == clientPort) shouldBe true
      container.environments.exists(_._2 == clientPort.toString) shouldBe true
      val topicName = CommonUtil.randomString()
      val brokers = brokerCluster.nodeNames.map(_ + s":${brokerCluster.clientPort}").mkString(",")
      val brokerClient = BrokerClient.of(brokers)
      try {
        brokerClient.topicCreator().numberOfPartitions(1).numberOfReplications(1).create(topicName)
        val producer = Producer.builder().connectionProps(brokers).build(Serializer.STRING, Serializer.STRING)
        try {
          producer.sender().key("abc").value("abc_value").send(topicName)
        } finally producer.close()
        val consumer = Consumer
          .builder()
          .connectionProps(brokers)
          .offsetFromBegin()
          .topicName(topicName)
          .build(Serializer.STRING, Serializer.STRING)
        try {
          val records = consumer.poll(Duration.ofSeconds(10), 1)
          records.size() shouldBe 1
          records.get(0).key().get shouldBe "abc"
          records.get(0).value().get shouldBe "abc_value"
        } finally consumer.close()
        brokerClient.deleteTopic(topicName)
        f(brokerCluster)
      } finally brokerClient.close()

    } finally if (cleanup) result(brokerCollie.remove(brokerCluster.name))
    log.info("cleanup broker cluster ... done")
  }

  private[this] def testAddNodeToRunningBrokerCluster(f: BrokerClusterInfo => Unit): Unit = testBroker {
    previousCluster =>
      val brokerCollie = clusterCollie.brokerCollie()
      result(brokerCollie.exists(previousCluster.name)) shouldBe true
      log.info(s"nodeCache:$nodeCache prvious:${previousCluster.nodeNames}")
      an[IllegalArgumentException] should be thrownBy result(
        brokerCollie.removeNode(previousCluster.name, previousCluster.nodeNames.head))
      val freeNodes = nodeCache.filterNot(node => previousCluster.nodeNames.contains(node.name))
      if (freeNodes.nonEmpty) {
        // we can't add duplicate node
        an[IllegalArgumentException] should be thrownBy result(
          brokerCollie.addNode(previousCluster.name, previousCluster.nodeNames.head))
        // we can't add a nonexistent node
        an[NoSuchElementException] should be thrownBy result(
          brokerCollie.addNode(previousCluster.name, CommonUtil.randomString()))
        log.info(s"add new node:${freeNodes.head.name} to cluster:${previousCluster.name}")
        val newCluster = result(brokerCollie.addNode(previousCluster.name, freeNodes.head.name))
        log.info(s"add new node:${freeNodes.head.name} to cluster:${previousCluster.name}...done")
        newCluster.name shouldBe previousCluster.name
        newCluster.imageName shouldBe previousCluster.imageName
        newCluster.zookeeperClusterName shouldBe previousCluster.zookeeperClusterName
        newCluster.exporterPort shouldBe previousCluster.exporterPort
        newCluster.clientPort shouldBe previousCluster.clientPort
        newCluster.nodeNames.size - previousCluster.nodeNames.size shouldBe 1
        f(newCluster)
      }
  }

  private[this] def testRemoveNodeToRunningBrokerCluster(): Unit = testAddNodeToRunningBrokerCluster {
    previousCluster =>
      val brokerCollie = clusterCollie.brokerCollie()
      result(brokerCollie.exists(previousCluster.name)) shouldBe true

      val newCluster = result(brokerCollie.removeNode(previousCluster.name, previousCluster.nodeNames.head))
      newCluster.name shouldBe previousCluster.name
      newCluster.imageName shouldBe previousCluster.imageName
      newCluster.zookeeperClusterName shouldBe previousCluster.zookeeperClusterName
      newCluster.clientPort shouldBe previousCluster.clientPort
      previousCluster.nodeNames.size - newCluster.nodeNames.size shouldBe 1
  }

  private[this] def testWorker(f: WorkerClusterInfo => Unit): Unit = testBroker { brokerCluster =>
    log.info("[WORKER] start to test worker")
    val workerCollie = clusterCollie.workerCollie()
    val nodeName = nodeCache.head.name
    val clusterName = CommonUtil.randomString(10)
    result(workerCollie.nonExists(clusterName)) shouldBe true
    log.info("[WORKER] verify:nonExists done")
    val clientPort = CommonUtil.availablePort()
    def assert(workerCluster: WorkerClusterInfo): WorkerClusterInfo = {
      workerCluster.brokerClusterName shouldBe brokerCluster.name
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
    val workerCluster = assert(
      result(
        workerCollie
          .creator()
          .clusterName(clusterName)
          .clientPort(clientPort)
          .brokerClusterName(brokerCluster.name)
          .nodeName(nodeName)
          .create()))
    log.info("[WORKER] create done")
    assert(result(workerCollie.cluster(workerCluster.name))._1)
    log.info("[WORKER] verify:create done")
    try {
      result(workerCollie.exists(workerCluster.name)) shouldBe true
      log.info("[WORKER] verify:exist done")
      val workerClient = WorkerClient(s"${workerCluster.nodeNames.head}:${workerCluster.clientPort}")
      CommonUtil.await(
        () =>
          try result(workerClient.plugins()).nonEmpty
          catch {
            case e: Throwable =>
              log.info(s"[WORKER] worker cluster:${workerCluster.name} is starting ... retry", e)
              false
        },
        Duration.ofSeconds(30)
      )
      // we can't assume the size since other tests may create zk cluster at the same time
      result(workerCollie.clusters()).isEmpty shouldBe false
      log.info("[WORKER] verify:list done")
      result(workerCollie.logs(clusterName)).size shouldBe 1
      result(workerCollie.logs(clusterName)).values.foreach(log =>
        withClue(log) {
          log.contains("- ERROR") shouldBe false
          log.isEmpty shouldBe false
      })
      log.info("[WORKER] verify:log done")
      val container = result(workerCollie.containers(clusterName)).head
      container.nodeName shouldBe nodeName
      container.name.contains(clusterName) shouldBe true
      container.hostname.contains(clusterName) shouldBe true
      container.portMappings.head.portPairs.size shouldBe 1
      container.portMappings.head.portPairs.exists(_.containerPort == clientPort) shouldBe true
      container.environments.exists(_._2 == clientPort.toString) shouldBe true
      f(workerCluster)
    } finally if (cleanup) result(workerCollie.remove(workerCluster.name))
  }

  private[this] def testAddNodeToRunningWorkerCluster(f: WorkerClusterInfo => Unit): Unit = testWorker {
    previousCluster =>
      val workerCollie = clusterCollie.workerCollie()
      result(workerCollie.exists(previousCluster.name)) shouldBe true
      an[IllegalArgumentException] should be thrownBy result(
        workerCollie.removeNode(previousCluster.name, previousCluster.nodeNames.head))
      val freeNodes = nodeCache.filterNot(node => previousCluster.nodeNames.contains(node.name))
      if (freeNodes.nonEmpty) {
        // we can't add duplicate node
        an[IllegalArgumentException] should be thrownBy result(
          workerCollie.addNode(previousCluster.name, previousCluster.nodeNames.head))
        // we can't add a nonexistent node
        an[NoSuchElementException] should be thrownBy result(
          workerCollie.addNode(previousCluster.name, CommonUtil.randomString()))
        log.info(s"[WORKER] start to add node:${freeNodes.head.name} to a running worker cluster")
        val newCluster = result(workerCollie.addNode(previousCluster.name, freeNodes.head.name))
        newCluster.name shouldBe previousCluster.name
        newCluster.imageName shouldBe previousCluster.imageName
        newCluster.configTopicName shouldBe previousCluster.configTopicName
        newCluster.statusTopicName shouldBe previousCluster.statusTopicName
        newCluster.offsetTopicName shouldBe previousCluster.offsetTopicName
        newCluster.groupId shouldBe previousCluster.groupId
        newCluster.brokerClusterName shouldBe previousCluster.brokerClusterName
        newCluster.clientPort shouldBe previousCluster.clientPort
        newCluster.nodeNames.size - previousCluster.nodeNames.size shouldBe 1
        log.info(s"[WORKER] start to verify the added node:${freeNodes.head.name}")
        // worker is starting...
        CommonUtil.await(
          () =>
            try {
              val workersProps = s"${freeNodes.head.name}:${newCluster.clientPort}"
              val workerClient = WorkerClient(workersProps)
              result(workerClient.plugins()).nonEmpty
            } catch {
              case _: Throwable => false
          },
          Duration.ofSeconds(10)
        )
        f(newCluster)
      }
  }

  private[this] def testRemoveNodeToRunningWorkerCluster(): Unit = testAddNodeToRunningWorkerCluster {
    previousCluster =>
      val workerCollie = clusterCollie.workerCollie()
      result(workerCollie.exists(previousCluster.name)) shouldBe true
      log.info(s"[WORKER] start to remove node:${previousCluster.nodeNames.head} from ${previousCluster.name}")
      val newCluster = result(workerCollie.removeNode(previousCluster.name, previousCluster.nodeNames.head))
      newCluster.name shouldBe previousCluster.name
      newCluster.imageName shouldBe previousCluster.imageName
      newCluster.configTopicName shouldBe previousCluster.configTopicName
      newCluster.statusTopicName shouldBe previousCluster.statusTopicName
      newCluster.offsetTopicName shouldBe previousCluster.offsetTopicName
      newCluster.groupId shouldBe previousCluster.groupId
      newCluster.brokerClusterName shouldBe previousCluster.brokerClusterName
      newCluster.clientPort shouldBe previousCluster.clientPort
      previousCluster.nodeNames.size - newCluster.nodeNames.size shouldBe 1
  }

  @Test
  def testBrokerCollie(): Unit = testRemoveNodeToRunningBrokerCluster()

  @Test
  def testWorkerCollie(): Unit = testRemoveNodeToRunningWorkerCluster()

  @Test
  def testMultiZkClustersOnSingleNode(): Unit = {
    val names = (0 to 2).map(_ => CommonUtil.randomString(10))
    try {
      val clusters = names.map { name =>
        Await.result(
          clusterCollie
            .zookeepersCollie()
            .creator()
            .clusterName(name)
            .clientPort(CommonUtil.availablePort())
            .electionPort(CommonUtil.availablePort())
            .peerPort(CommonUtil.availablePort())
            .nodeNames(nodeCache.map(_.name))
            .create(),
          30 seconds
        )
      }
      val clusters2 = Await.result(clusterCollie.zookeepersCollie().clusters(), 20 seconds)
      clusters.foreach { c =>
        clusters2.find(_._1.name == c.name).get._1 shouldBe c
        Await.result(clusterCollie.zookeepersCollie().logs(c.name), 10 seconds).values.foreach { log =>
          withClue(log)(log.contains("- ERROR") shouldBe false)
          log.isEmpty shouldBe false
        }
      }
    } finally names.foreach { name =>
      try Await.result(clusterCollie.zookeepersCollie().remove(name), 10 seconds)
      catch {
        case _: Throwable =>
        // do nothing
      }
    }
  }

  @Test
  def testMultiWkClustersOnSingleNode(): Unit = {
    val zkName = CommonUtil.randomString(10)
    val bkName = CommonUtil.randomString(10)
    val wkNames = (0 to 2).map(_ => CommonUtil.randomString(10))
    try {
      val zk = Await.result(
        clusterCollie
          .zookeepersCollie()
          .creator()
          .clusterName(zkName)
          .clientPort(CommonUtil.availablePort())
          .electionPort(CommonUtil.availablePort())
          .peerPort(CommonUtil.availablePort())
          .nodeName(nodeCache.head.name)
          .create(),
        30 seconds
      )
      val bk = Await.result(
        clusterCollie
          .brokerCollie()
          .creator()
          .zookeeperClusterName(zk.name)
          .clusterName(bkName)
          .clientPort(CommonUtil.availablePort())
          .exporterPort(CommonUtil.availablePort())
          .nodeName(nodeCache.head.name)
          .create(),
        30 seconds
      )
      val clusters = wkNames.map { name =>
        Await.result(
          clusterCollie
            .workerCollie()
            .creator()
            .brokerClusterName(bk.name)
            .clusterName(name)
            .clientPort(CommonUtil.availablePort())
            .groupId(CommonUtil.randomString(5))
            .configTopicName(CommonUtil.randomString(10))
            .statusTopicName(CommonUtil.randomString(10))
            .offsetTopicName(CommonUtil.randomString(10))
            .nodeNames(nodeCache.map(_.name))
            .create(),
          30 seconds
        )
      }
      val clusters2 = Await.result(clusterCollie.workerCollie().clusters(), 20 seconds)
      clusters.foreach { c =>
        val another = clusters2.find(_._1.name == c.name).get._1
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
        clusters
          .flatMap { cluster =>
            cluster.nodeNames.map(n => s"$n:${cluster.clientPort}")
          }
          .foreach { workersProps =>
            CommonUtil.await(
              () =>
                try result(WorkerClient(workersProps).plugins()).nonEmpty
                catch {
                  case _: Throwable => false
              },
              Duration.ofSeconds(10)
            )
          }

      }
    } finally {
      try Await.result(clusterCollie.zookeepersCollie().remove(zkName), 10 seconds)
      catch {
        case _: Throwable =>
        // do nothing
      }
      try Await.result(clusterCollie.brokerCollie().remove(bkName), 10 seconds)
      catch {
        case _: Throwable =>
        // do nothing
      }
      wkNames.foreach { name =>
        try Await.result(clusterCollie.workerCollie().remove(name), 10 seconds)
        catch {
          case _: Throwable =>
          // do nothing
        }
      }
    }
  }

  @After
  final def tearDown(): Unit = Releasable.close(clusterCollie)
}
