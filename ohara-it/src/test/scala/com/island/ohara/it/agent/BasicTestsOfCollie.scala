package com.island.ohara.it.agent

import java.time.Duration

import com.island.ohara.agent._
import com.island.ohara.client.ConfiguratorJson.{
  BrokerClusterDescription,
  Node,
  WorkerClusterDescription,
  ZookeeperClusterDescription
}
import com.island.ohara.client.ConnectorClient
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.rule.LargeTest
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.island.ohara.kafka.{Consumer, KafkaUtil, Producer}
import org.junit.{After, Before}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * This abstract class extracts the "required" information of running tests on true env.
  * All checks are verified in this class but we do run all test cases on different test in order to avoid
  * slow test cases run by single test jvm.
  *
  * NOTED: this test will forward random ports so it would be better to "close" firewall of remote node.
  */
abstract class BasicTestsOfCollie extends LargeTest with Matchers {

  /**
    * form: user:password@hostname:port.
    * NOTED: this key need to be matched with another key value in ohara-it/build.gradle
    */
  private[this] val key = "ohara.it.docker"

  private[this] val nodeCollie: NodeCollie = NodeCollie.inMemory()
  private[this] val clusterCollie: ClusterCollie = ClusterCollie(nodeCollie)

  private[this] val timeout = 60 seconds

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
      nodeCollie.add(Node(hostname, port, user, password, CommonUtil.current()))
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

  /**
    * make sure all test cases here are executed only if we have defined the docker server.
    * @param f test case
    */
  private[this] def runTest(f: () => Unit): Unit = if (nodeCollie.isEmpty)
    skipTest(s"$key doesn't exist so all tests in BasicTestsOfCollie are ignored")
  else f()

  /**
    * create a zk cluster env in running test case.
    * @param f test case
    */
  private[this] def testZk(f: ZookeeperClusterDescription => Unit): Unit = runTest { () =>
    val zookeeperCollie = clusterCollie.zookeepersCollie()
    try {
      val nodeName = nodeCollie.head.name
      val clusterName = random()
      zookeeperCollie.nonExists(clusterName) shouldBe true
      val clientPort = CommonUtil.availablePort()
      val electionPort = CommonUtil.availablePort()
      val peerPort = CommonUtil.availablePort()
      val zkCluster = Await.result(zookeeperCollie
                                     .creator()
                                     .clientPort(clientPort)
                                     .electionPort(electionPort)
                                     .peerPort(peerPort)
                                     .clusterName(clusterName)
                                     .create(nodeName),
                                   timeout)
      try {
        zookeeperCollie.exists(_.name == zkCluster.name) shouldBe true
        zkCluster.name shouldBe clusterName
        zkCluster.nodeNames.head shouldBe nodeName
        zkCluster.clientPort shouldBe clientPort
        zkCluster.peerPort shouldBe peerPort
        zkCluster.electionPort shouldBe electionPort
        // we can't assume the size since other tests may create zk cluster at the same time
        zookeeperCollie.isEmpty shouldBe false
        zookeeperCollie.logs(clusterName).size shouldBe 1
        zookeeperCollie
          .logs(clusterName)
          .values
          .foreach(log =>
            withClue(log) {
              log.contains("exception") shouldBe false
              log.isEmpty shouldBe false
          })
        val container = zookeeperCollie.containers(clusterName).head
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
      } finally if (cleanup) Await.result(zookeeperCollie.remove(zkCluster.name), timeout)
    } finally zookeeperCollie.close()
  }

  private[this] def testBroker(f: BrokerClusterDescription => Unit): Unit = testZk { zkCluster =>
    val brokerCollie = clusterCollie.brokerCollie()
    try {
      val nodeName = nodeCollie.head.name
      val clusterName = random()
      brokerCollie.nonExists(clusterName) shouldBe true
      val clientPort = CommonUtil.availablePort()
      val brokerCluster = Await.result(brokerCollie
                                         .creator()
                                         .clusterName(clusterName)
                                         .clientPort(clientPort)
                                         .zookeeperClusterName(zkCluster.name)
                                         .create(nodeName),
                                       timeout)
      try {
        brokerCollie.exists(_.name == brokerCluster.name) shouldBe true
        brokerCluster.zookeeperClusterName shouldBe zkCluster.name
        brokerCluster.name shouldBe clusterName
        brokerCluster.nodeNames.head shouldBe nodeName
        brokerCluster.clientPort shouldBe clientPort
        // we can't assume the size since other tests may create zk cluster at the same time
        brokerCollie.isEmpty shouldBe false
        brokerCollie.logs(clusterName).size shouldBe 1
        brokerCollie
          .logs(clusterName)
          .values
          .foreach(log =>
            withClue(log) {
              log.contains("exception") shouldBe false
              log.isEmpty shouldBe false
          })
        val container = brokerCollie.containers(clusterName).head
        container.nodeName shouldBe nodeName
        container.name.contains(clusterName) shouldBe true
        container.hostname.contains(clusterName) shouldBe true
        container.portMappings.head.portPairs.size shouldBe 1
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
      } finally if (cleanup) Await.result(brokerCollie.remove(brokerCluster.name), timeout)
    } finally brokerCollie.close()
  }

  private[this] def testAddNodeToRunningBrokerCluster(f: BrokerClusterDescription => Unit): Unit = testBroker {
    previousCluster =>
      val brokerCollie = clusterCollie.brokerCollie()
      brokerCollie.exists(_.name == previousCluster.name) shouldBe true

      an[IllegalArgumentException] should be thrownBy Await
        .result(brokerCollie.removeNode(previousCluster.name, previousCluster.nodeNames.head), timeout)
      val freeNodes = nodeCollie.filterNot(node => previousCluster.nodeNames.contains(node.name))
      if (freeNodes.nonEmpty) {
        // we can't add duplicate node
        an[IllegalArgumentException] should be thrownBy Await
          .result(brokerCollie.addNode(previousCluster.name, previousCluster.nodeNames.head), timeout)
        // we can't add a nonexistent node
        an[IllegalArgumentException] should be thrownBy Await
          .result(brokerCollie.addNode(previousCluster.name, CommonUtil.randomString()), timeout)
        val newCluster = Await.result(brokerCollie.addNode(previousCluster.name, freeNodes.head.name), timeout)
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
    brokerCollie.exists(_.name == previousCluster.name) shouldBe true

    val newCluster =
      Await.result(brokerCollie.removeNode(previousCluster.name, previousCluster.nodeNames.head), timeout)
    newCluster.name shouldBe previousCluster.name
    newCluster.imageName shouldBe previousCluster.imageName
    newCluster.zookeeperClusterName shouldBe previousCluster.zookeeperClusterName
    newCluster.clientPort shouldBe previousCluster.clientPort
    previousCluster.nodeNames.size - newCluster.nodeNames.size shouldBe 1
  }

  private[this] def testWorker(f: WorkerClusterDescription => Unit): Unit = testBroker { brokerCluster =>
    val workerCollie = clusterCollie.workerCollie()
    try {
      val nodeName = nodeCollie.head.name
      val clusterName = random()
      workerCollie.nonExists(clusterName) shouldBe true
      val clientPort = CommonUtil.availablePort()
      val workerCluster = Await.result(workerCollie
                                         .creator()
                                         .clusterName(clusterName)
                                         .clientPort(clientPort)
                                         .brokerClusterName(brokerCluster.name)
                                         .create(nodeName),
                                       timeout)
      try {
        workerCollie.exists(_.name == workerCluster.name) shouldBe true
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
        Set(
          workerCluster.groupId,
          workerCluster.configTopicName,
          workerCluster.offsetTopicName,
          workerCluster.statusTopicName
        ).size shouldBe 4
        val connectorClient = ConnectorClient(s"${workerCluster.nodeNames.head}:${workerCluster.clientPort}")
        try connectorClient.plugins().isEmpty shouldBe false
        finally connectorClient.close()
        // we can't assume the size since other tests may create zk cluster at the same time
        workerCollie.isEmpty shouldBe false
        workerCollie.logs(clusterName).size shouldBe 1
        workerCollie
          .logs(clusterName)
          .values
          .foreach(log =>
            withClue(log) {
              log.contains("exception") shouldBe false
              log.isEmpty shouldBe false
          })
        val container = workerCollie.containers(clusterName).head
        container.nodeName shouldBe nodeName
        container.name.contains(clusterName) shouldBe true
        container.hostname.contains(clusterName) shouldBe true
        container.portMappings.head.portPairs.size shouldBe 1
        container.portMappings.head.portPairs.exists(_.containerPort == clientPort) shouldBe true
        container.environments.exists(_._2 == clientPort.toString) shouldBe true
        f(workerCluster)
      } finally if (cleanup) Await.result(workerCollie.remove(workerCluster.name), timeout)
    } finally workerCollie.close()
  }

  private[this] def testAddNodeToRunningWorkerCluster(f: WorkerClusterDescription => Unit): Unit = testWorker {
    previousCluster =>
      val workerCollie = clusterCollie.workerCollie()
      workerCollie.exists(_.name == previousCluster.name) shouldBe true
      an[IllegalArgumentException] should be thrownBy Await
        .result(workerCollie.removeNode(previousCluster.name, previousCluster.nodeNames.head), timeout)
      val freeNodes = nodeCollie.filterNot(node => previousCluster.nodeNames.contains(node.name))
      if (freeNodes.nonEmpty) {
        // we can't add duplicate node
        an[IllegalArgumentException] should be thrownBy Await
          .result(workerCollie.addNode(previousCluster.name, previousCluster.nodeNames.head), timeout)
        // we can't add a nonexistent node
        an[IllegalArgumentException] should be thrownBy Await
          .result(workerCollie.addNode(previousCluster.name, CommonUtil.randomString()), timeout)
        val newCluster = Await.result(workerCollie.addNode(previousCluster.name, freeNodes.head.name), timeout)
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
    workerCollie.exists(_.name == previousCluster.name) shouldBe true

    val newCluster =
      Await.result(workerCollie.removeNode(previousCluster.name, previousCluster.nodeNames.head), timeout)
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
  final def tearDown(): Unit = {
    ReleaseOnce.close(clusterCollie)
    ReleaseOnce.close(nodeCollie)
  }
}
