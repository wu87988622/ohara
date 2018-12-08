package com.island.ohara.it
import java.util.concurrent.TimeUnit

import com.island.ohara.agent.DockerClient
import com.island.ohara.agent.DockerJson.{PortPair, State}
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.CloseOnce
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

/**
  * all test cases here are executed on remote node. If no remote node is defined, all tests are skipped.
  * You can run following command to pass the information of remote node.
  * $ gradle clean ohara-it:test --tests *TestDockerClient -PskipManager -Pohara.it.docker=$user:$password@$hostname:$port
  */
class TestDockerClient extends MediumTest with Matchers {

  /**
    * form: user:password@hostname:port.
    * NOTED: this key need to be matched with another key value in ohara-it/build.gradle
    */
  private[this] val key = "ohara.it.docker"

  private[this] var client: DockerClient = _

  private[this] val webHost = "www.google.com.tw"

  private[this] var remoteHostname: String = _
  @Before
  def setup(): Unit = sys.env.get(key).foreach { info =>
    val user = info.split(":").head
    val password = info.split("@").head.split(":").last
    val hostname = info.split("@").last.split(":").head
    val port = info.split("@").last.split(":").last.toInt
    client = DockerClient.builder().hostname(hostname).port(port).user(user).password(password).build()
    remoteHostname = hostname
  }

  /**
    * make sure all test cases here are executed only if we have defined the docker server.
    * @param f test case
    */
  private[this] def runTest(f: DockerClient => Unit): Unit = if (client != null) f(client)

  @Test
  def testLog(): Unit = runTest { client =>
    val container =
      client.executor().imageName("centos:7").cleanup().command(s"""/bin/bash -c \"ping $webHost\"""").run().get
    try client.log(container.name).contains(webHost) shouldBe true
    finally client.stop(container.name)

  }

  @Test
  def testList(): Unit = runTest { client =>
    val before = client.containers()
    val container =
      client.executor().imageName("centos:7").cleanup().command(s"""/bin/bash -c \"ping $webHost\"""").run().get
    try {
      container.state shouldBe State.RUNNING
      val after = client.containers()
      before.exists(_.name == container.name) shouldBe false
      after.exists(_.name == container.name) shouldBe true
    } finally client.stop(container.name)
    client.containers().exists(_.name == container.name) shouldBe false
  }

  @Test
  def testCleanup(): Unit = runTest { client =>
    val before = client.containers()
    // ping google 3 times
    val container =
      client.executor().imageName("centos:7").cleanup().command(s"""/bin/bash -c \"ping $webHost -c 3\"""").run().get
    TimeUnit.SECONDS.sleep(3)
    client.containers().size shouldBe before.size
    client.exist(container.name) shouldBe false
    client.nonExist(container.name) shouldBe true
  }

  @Test
  def testNonCleanup(): Unit = runTest { client =>
    val before = client.containers()
    // ping google 3 times
    val container =
      client.executor().imageName("centos:7").command(s"""/bin/bash -c \"ping $webHost -c 3\"""").run().get
    try {
      TimeUnit.SECONDS.sleep(3)
      client.containers().size shouldBe before.size + 1
      client.container(container.name).get.state shouldBe State.EXITED
    } finally client.remove(container.name)
  }

  @Test
  def testStopById(): Unit = runTest { client =>
    // ping google 3 times
    val container =
      client.executor().imageName("centos:7").cleanup().command(s"""/bin/bash -c \"ping $webHost\"""").run().get
    client.stopById(container.id)
    TimeUnit.SECONDS.sleep(3)
    client.exist(container.name) shouldBe false
    client.existById(container.id) shouldBe false
  }

  @Test
  def testRemoveById(): Unit = runTest { client =>
    // ping google 3 times
    val container = client.executor().imageName("centos:7").command(s"""/bin/bash -c \"ping $webHost\"""").run().get
    try {
      client.stopById(container.id)
      TimeUnit.SECONDS.sleep(3)
      client.containerById(container.id).get.state shouldBe State.EXITED
      client.exist(container.name) shouldBe true
      client.existById(container.id) shouldBe true
    } finally if (client.exist(container.name)) {
      client.stop(container.name)
      client.remove(container.name)
    }
  }

  @Test
  def testVerify(): Unit = runTest { client =>
    client.verify() shouldBe true
  }

  @Test
  def testRoute(): Unit = runTest { client =>
    val container = client
      .executor()
      .route(Map("ABC" -> "192.168.123.123"))
      .imageName("centos:7")
      .cleanup()
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .run()
      .get
    try {
      val hostFile = client.cat(container.name, "/etc/hosts").get
      hostFile.contains("192.168.123.123") shouldBe true
      hostFile.contains("ABC") shouldBe true
    } finally client.stop(container.name)
  }

  @Test
  def testPortMapping(): Unit = runTest { client =>
    val container = client
      .executor()
      .imageName("centos:7")
      .portMappings(Map(12345 -> 12345))
      .cleanup()
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .run()
      .get
    try {
      container.portMappings.size shouldBe 1
      container.portMappings.head.portPairs.size shouldBe 1
      container.portMappings.head.portPairs.head shouldBe PortPair(12345, 12345)
    } finally client.stop(container.name)
  }

  @Test
  def testSetEnv(): Unit = runTest { client =>
    val container = client
      .executor()
      .imageName("centos:7")
      .envs(Map("abc" -> "123", "ccc" -> "ttt"))
      .cleanup()
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .run()
      .get
    try {
      container.environments("abc") shouldBe "123"
      container.environments("ccc") shouldBe "ttt"
    } finally client.stop(container.name)
  }

  @Test
  def testHostname(): Unit = runTest { client =>
    val container = client
      .executor()
      .imageName("centos:7")
      .hostname("abcdef")
      .cleanup()
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .run()
      .get
    try container.hostname shouldBe "abcdef"
    finally client.stop(container.name)
  }

  @Test
  def testNodeName(): Unit = runTest { client =>
    val container =
      client.executor().imageName("centos:7").cleanup().command(s"""/bin/bash -c \"ping $webHost\"""").run().get
    try container.nodeName shouldBe remoteHostname
    finally client.stop(container.name)
  }

  @After
  def tearDown(): Unit = CloseOnce.close(client)

}
