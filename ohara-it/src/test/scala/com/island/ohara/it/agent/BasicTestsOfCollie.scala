package com.island.ohara.it.agent

import java.time.Duration

import com.island.ohara.agent._
import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.island.ohara.it.IntegrationTest
import com.island.ohara.kafka.{Consumer, KafkaUtil, Producer}
import com.typesafe.scalalogging.Logger
import org.junit.{After, Before}
import org.scalatest.Matchers

import scala.collection.mutable.ArrayBuffer
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

  /**
    * form: user:password@hostname:port.
    * NOTED: this key need to be matched with another key value in ohara-it/build.gradle
    */
  private[this] val key = "ohara.it.docker"
  private[this] val nodeCache = new ArrayBuffer[Node]()
  private[this] val nodeCollie: NodeCollie = new NodeCollie {
    override def nodes(): Future[Seq[Node]] = Future.successful(nodeCache)
    override def node(name: String): Future[Node] = Future.successful(
      nodeCache.find(_.name == name).getOrElse(throw new NoSuchElementException(s"expected:$name actual:$nodeCache")))
  }
  private[this] val clusterCollie: ClusterCollie = ClusterCollie(nodeCollie)

  /**
    * used to debug...
    */
  private[this] val cleanup = true

  @Before
  final def setup(): Unit = sys.env.get(key).foreach { info =>
    info.split(",").foreach { nodeInfo =>
      val user = nodeInfo.split(":").head
      val password = nodeInfo.split("@").head.split(":").last
      val hostname = nodeInfo.split("@").last.split(":").head
      val port = nodeInfo.split("@").last.split(":").last.toInt
      nodeCache.append(Node(hostname, port, user, password, Seq.empty, CommonUtil.current()))
      val dockerClient = DockerClient.builder().hostname(hostname).port(port).user(user).password(password).build()
      try {
        withClue(s"failed to find ${ZookeeperCollie.IMAGE_NAME_DEFAULT}")(
          dockerClient.images().contains(ZookeeperCollie.IMAGE_NAME_DEFAULT) shouldBe true)
        withClue(s"failed to find ${BrokerCollie.IMAGE_NAME_DEFAULT}")(
          dockerClient.images().contains(BrokerCollie.IMAGE_NAME_DEFAULT) shouldBe true)
        withClue(s"failed to find ${WorkerCollie.IMAGE_NAME_DEFAULT}")(
          dockerClient.images().contains(WorkerCollie.IMAGE_NAME_DEFAULT) shouldBe true)
      } finally dockerClient.close()
    }
  }

  private[this] def result[T](f: Future[T]): T = Await.result(f, 60 seconds)

  /**
    * make sure all test cases here are executed only if we have defined the docker server.
    * @param f test case
    */
  private[this] def runTest(f: () => Unit): Unit = if (nodeCache.isEmpty)
    skipTest(s"$key doesn't exist so all tests in BasicTestsOfCollie are ignored")
  else f()

  /**
    * create a zk cluster env in running test case.
    * @param f test case
    */
  private[this] def testZk(f: ZookeeperClusterInfo => Unit): Unit = runTest { () =>
    log.info("start to run zookeeper cluster")
    val zookeeperCollie = clusterCollie.zookeepersCollie()
    val nodeName: String = result(nodeCollie.nodes()).head.name
    val clusterName = random()
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
      container.hostname.contains(clusterName) shouldBe true
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
    val nodeName = result(nodeCollie.nodes()).head.name
    val clusterName = random()
    result(brokerCollie.nonExists(clusterName)) shouldBe true
    log.info(s"verify existence of broker cluster:$clusterName...done")
    val clientPort = CommonUtil.availablePort()
    val exporterPort = CommonUtil.availablePort()
    def assert(brokerCluster: BrokerClusterInfo): BrokerClusterInfo = {
      brokerCluster.zookeeperClusterName shouldBe zkCluster.name
      brokerCluster.name shouldBe clusterName
      brokerCluster.nodeNames.head shouldBe nodeName
      brokerCluster.clientPort shouldBe clientPort
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
      KafkaUtil.createTopic(brokers, topicName, 1, 1)
      val producer = Producer.builder().brokers(brokers).build(Serializer.STRING, Serializer.STRING)
      try {
        producer.sender().key("abc").value("abc_value").send(topicName)
      } finally producer.close()
      val consumer = Consumer
        .builder()
        .brokers(brokers)
        .offsetFromBegin()
        .topicName(topicName)
        .build(Serializer.STRING, Serializer.STRING)
      try {
        val records = consumer.poll(Duration.ofSeconds(10), 1)
        records.size() shouldBe 1
        records.get(0).key().get shouldBe "abc"
        records.get(0).value().get shouldBe "abc_value"
      } finally consumer.close()
      KafkaUtil.deleteTopic(brokers, topicName)
      f(brokerCluster)
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
        newCluster.clientPort shouldBe previousCluster.clientPort
        newCluster.nodeNames.size - previousCluster.nodeNames.size shouldBe 1
        f(newCluster)
      }
  }

  protected def testRemoveNodeToRunningBrokerCluster(): Unit = testAddNodeToRunningBrokerCluster { previousCluster =>
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
    val clusterName = random()
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
    assert(result(workerCollie.cluster(workerCluster.name))._1)
    log.info("[WORKER] verify:create done")
    try {
      result(workerCollie.exists(workerCluster.name)) shouldBe true
      log.info("[WORKER] verify:exist done")
      val connectorClient = ConnectorClient(s"${workerCluster.nodeNames.head}:${workerCluster.clientPort}")
      try connectorClient.plugins().isEmpty shouldBe false
      finally connectorClient.close()
      // we can't assume the size since other tests may create zk cluster at the same time
      result(workerCollie.clusters()).isEmpty shouldBe false
      log.info("[WORKER] verify:list done")
      result(workerCollie.logs(clusterName)).size shouldBe 1
      result(workerCollie.logs(clusterName)).values.foreach(log =>
        withClue(log) {
          log.contains("exception") shouldBe false
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
        // worker is starting...
        CommonUtil.await(
          () =>
            try {
              val workersProps = s"${freeNodes.head.name}:${newCluster.clientPort}"
              val connectorClient = ConnectorClient(workersProps)
              try connectorClient.plugins().nonEmpty
              finally connectorClient.close()
            } catch {
              case _: Throwable => false
          },
          Duration.ofSeconds(10)
        )
        f(newCluster)
      }
  }

  protected def testRemoveNodeToRunningWorkerCluster(): Unit = testAddNodeToRunningWorkerCluster { previousCluster =>
    val workerCollie = clusterCollie.workerCollie()
    result(workerCollie.exists(previousCluster.name)) shouldBe true

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

  @After
  final def tearDown(): Unit = ReleaseOnce.close(clusterCollie)
}
