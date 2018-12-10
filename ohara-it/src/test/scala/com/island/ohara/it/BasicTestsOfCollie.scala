package com.island.ohara.it

import com.island.ohara.agent.AgentJson.{BrokerCluster, Node, ZookeeperCluster}
import com.island.ohara.agent._
import com.island.ohara.common.rule.LargeTest
import com.island.ohara.common.util.CloseOnce
import com.island.ohara.integration.Integration
import org.junit.{After, Before}
import org.scalatest.Matchers

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

  protected val nodeCollie: NodeCollie = NodeCollie.inMemory()
  protected val clusterCollie: ClusterCollie = ClusterCollie(nodeCollie)

  protected var remoteHostname: String = _

  private[this] val cleanup = true
  @Before
  final def setup(): Unit = sys.env.get(key).foreach { info =>
    val user = info.split(":").head
    val password = info.split("@").head.split(":").last
    val hostname = info.split("@").last.split(":").head
    val port = info.split("@").last.split(":").last.toInt
    nodeCollie.add(Node(hostname, port, user, password))
    val dockerClient = DockerClient.builder().hostname(hostname).port(port).user(user).password(password).build()
    try {
      withClue(s"failed to find ${ZookeeperCollie.IMAGE_NAME_DEFAULT}")(
        dockerClient.images().contains(ZookeeperCollie.IMAGE_NAME_DEFAULT) shouldBe true)
      withClue(s"failed to find ${BrokerCollie.IMAGE_NAME_DEFAULT}")(
        dockerClient.images().contains(BrokerCollie.IMAGE_NAME_DEFAULT) shouldBe true)
      withClue(s"failed to find ${WorkerCollie.IMAGE_NAME_DEFAULT}")(
        dockerClient.images().contains(WorkerCollie.IMAGE_NAME_DEFAULT) shouldBe true)
    } finally dockerClient.close()
    remoteHostname = hostname
  }

  /**
    * make sure all test cases here are executed only if we have defined the docker server.
    * @param f test case
    */
  protected def runTest(f: String => Unit): Unit = if (nodeCollie.nonEmpty) f(nodeCollie.head.name)

  /**
    * create a zk cluster env in running test case.
    * @param f test case
    */
  protected def testZk(f: (String, ZookeeperCluster) => Unit): Unit = runTest { nodeName =>
    val zookeeperCollie = clusterCollie.zookeepersCollie()
    try {
      val clusterName = random()
      zookeeperCollie.nonExist(clusterName) shouldBe true
      val clientPort = Integration.availablePort()
      val electionPort = Integration.availablePort()
      val peerPort = Integration.availablePort()
      val zkCluster = zookeeperCollie
        .creator()
        .clientPort(clientPort)
        .electionPort(electionPort)
        .peerPort(peerPort)
        .clusterName(clusterName)
        .create(nodeName)
      try {
        zkCluster.name shouldBe clusterName
        zkCluster.nodeNames.head shouldBe remoteHostname
        zkCluster.clientPort shouldBe clientPort
        zkCluster.peerPort shouldBe peerPort
        zkCluster.electionPort shouldBe electionPort
        // we can't assume the size since other tests may create zk cluster at the same time
        zookeeperCollie.isEmpty shouldBe false
        zookeeperCollie.logs(clusterName).size shouldBe 1
        zookeeperCollie.logs(clusterName).values.foreach(log => withClue(log)(log.contains("exception") shouldBe false))
        val container = zookeeperCollie.containers(clusterName).head
        container.nodeName shouldBe remoteHostname
        container.name.contains(clusterName) shouldBe true
        container.hostname.contains(clusterName) shouldBe true
        container.portMappings.head.portPairs.size shouldBe 3
        container.portMappings.head.portPairs.exists(_.containerPort == clientPort) shouldBe true
        container.portMappings.head.portPairs.exists(_.containerPort == electionPort) shouldBe true
        container.portMappings.head.portPairs.exists(_.containerPort == peerPort) shouldBe true
        container.environments.exists(_._2 == clientPort.toString) shouldBe true
        container.environments.exists(_._2 == electionPort.toString) shouldBe true
        container.environments.exists(_._2 == peerPort.toString) shouldBe true
        f(nodeName, zkCluster)
      } finally if (cleanup) zookeeperCollie.remove(zkCluster.name)
    } finally zookeeperCollie.close()
  }

  protected def testBroker(f: (String, BrokerCluster) => Unit): Unit = testZk { (nodeName, zkCluster) =>
    val brokerCollie = clusterCollie.brokerCollie()
    try {
      val clusterName = random()
      brokerCollie.nonExist(clusterName) shouldBe true
      val clientPort = Integration.availablePort()
      val brokerCluster = brokerCollie
        .creator()
        .clusterName(clusterName)
        .clientPort(clientPort)
        .zookeeperClusterName(zkCluster.name)
        .create(nodeName)
      try {
        brokerCluster.zookeeperClusterName shouldBe zkCluster.name
        brokerCluster.name shouldBe clusterName
        brokerCluster.nodeNames.head shouldBe remoteHostname
        brokerCluster.clientPort shouldBe clientPort
        // we can't assume the size since other tests may create zk cluster at the same time
        brokerCollie.isEmpty shouldBe false
        brokerCollie.logs(clusterName).size shouldBe 1
        brokerCollie.logs(clusterName).values.foreach(log => withClue(log)(log.contains("exception") shouldBe false))
        val container = brokerCollie.containers(clusterName).head
        container.nodeName shouldBe remoteHostname
        container.name.contains(clusterName) shouldBe true
        container.hostname.contains(clusterName) shouldBe true
        container.portMappings.head.portPairs.size shouldBe 1
        container.portMappings.head.portPairs.exists(_.containerPort == clientPort) shouldBe true
        container.environments.exists(_._2 == clientPort.toString) shouldBe true
        f(nodeName, brokerCluster)
      } finally if (cleanup) brokerCollie.remove(brokerCluster.name)
    } finally brokerCollie.close()
  }

  protected def testWorker(): Unit = testBroker { (nodeName, brokerCluster) =>
    val workerCollie = clusterCollie.workerCollie()
    try {
      val clusterName = random()
      workerCollie.nonExist(clusterName) shouldBe true
      val clientPort = Integration.availablePort()
      val workerCluster = workerCollie
        .creator()
        .clusterName(clusterName)
        .clientPort(clientPort)
        .brokerClusterName(brokerCluster.name)
        .create(nodeName)
      try {
        workerCluster.brokerClusterName shouldBe brokerCluster.name
        workerCluster.name shouldBe clusterName
        workerCluster.nodeNames.head shouldBe remoteHostname
        workerCluster.clientPort shouldBe clientPort
        // we can't assume the size since other tests may create zk cluster at the same time
        workerCollie.isEmpty shouldBe false
        workerCollie.logs(clusterName).size shouldBe 1
        workerCollie.logs(clusterName).values.foreach(log => withClue(log)(log.contains("exception") shouldBe false))
        val container = workerCollie.containers(clusterName).head
        container.nodeName shouldBe remoteHostname
        container.name.contains(clusterName) shouldBe true
        container.hostname.contains(clusterName) shouldBe true
        container.portMappings.head.portPairs.size shouldBe 1
        container.portMappings.head.portPairs.exists(_.containerPort == clientPort) shouldBe true
        container.environments.exists(_._2 == clientPort.toString) shouldBe true
      } finally if (cleanup) workerCollie.remove(workerCluster.name)
    } finally workerCollie.close()
  }

  @After
  final def tearDown(): Unit = {
    CloseOnce.close(clusterCollie)
    CloseOnce.close(nodeCollie)
  }
}
