package com.island.ohara.agent

import com.island.ohara.agent.DockerClient.LIST_PROCESS_FORMAT
import com.island.ohara.agent.DockerJson.{ContainerDescription, PortPair, State}
import com.island.ohara.agent.SshdServer.CommandHandler
import com.island.ohara.client.util.CloseOnce
import com.island.ohara.common.rule.MediumTest
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.util.Random

class TestDockerClientWithoutDockerServer extends MediumTest with Matchers {

  private[this] val containers = State.all.map(
    s =>
      ContainerDescription(
        id = s"id-${s.name}",
        image = s"image-${s.name}",
        created = s"created-${s.name}",
        state = s,
        name = s"name-${s.name}",
        size = s"size-${s.name}",
        portMappings = Seq.empty
    ))

  private[this] def containerToString(container: ContainerDescription): String = Seq(
    container.id,
    container.image,
    container.created,
    container.state,
    container.name,
    container.size
  ).mkString(DockerClient.DIVIDER)
  private[this] val handlers = Seq(
    // handle normal
    new CommandHandler {
      override def belong(command: String): Boolean = command == s"docker ps -a --format $LIST_PROCESS_FORMAT"
      override def execute(command: String): Seq[String] = if (belong(command)) containers.map(containerToString)
      else throw new IllegalArgumentException(s"$command doesn't support")
    },
    // final
    new CommandHandler {
      override def belong(command: String): Boolean = true
      override def execute(command: String): Seq[String] =
        throw new IllegalArgumentException(s"$command doesn't support")
    }
  )
  private[this] val server = SshdServer.local(0, handlers)

  @Test
  def checkCleanupOption(): Unit = {
    val client =
      DockerClient
        .builder()
        .hostname(server.hostname)
        .port(server.port)
        .user(server.user)
        .password(server.password)
        .build()

    try {
      client
        .executor()
        .command("/bin/bash -c \"ls\"")
        .image("centos:latest")
        .dockerCommand()
        .contains("--rm") shouldBe false
      client
        .executor()
        .command("/bin/bash -c \"ls\"")
        .image("centos:latest")
        .cleanup()
        .dockerCommand()
        .contains("--rm") shouldBe true
    } finally client.close()
  }

  private[this] def testSpecifiedContainer(expectedState: State): Unit = {
    val client =
      DockerClient
        .builder()
        .hostname(server.hostname)
        .port(server.port)
        .user(server.user)
        .password(server.password)
        .build()

    try {
      val rContainers = client.containers().filter(_.state == expectedState)
      rContainers.size shouldBe 1
      rContainers.head shouldBe containers.find(_.state == expectedState).get
    } finally client.close()
  }
  @Test
  def testCreatedContainers(): Unit = {
    testSpecifiedContainer(State.CREATED)
  }

  @Test
  def testRestartingContainers(): Unit = {
    testSpecifiedContainer(State.RESTARTING)
  }

  @Test
  def testRunningContainers(): Unit = {
    testSpecifiedContainer(State.RUNNING)
  }

  @Test
  def testRemovingContainers(): Unit = {
    testSpecifiedContainer(State.REMOVING)
  }

  @Test
  def testPausedContainers(): Unit = {
    testSpecifiedContainer(State.PAUSED)
  }

  @Test
  def testExitedContainers(): Unit = {
    testSpecifiedContainer(State.EXITED)
  }

  @Test
  def testDeadContainers(): Unit = {
    testSpecifiedContainer(State.DEAD)
  }

  @Test
  def testActiveContainers(): Unit = {
    val client =
      DockerClient
        .builder()
        .hostname(server.hostname)
        .port(server.port)
        .user(server.user)
        .password(server.password)
        .build()

    try {
      val rContainers = client.activeContainers()
      rContainers.size shouldBe 1
      rContainers.head shouldBe containers.find(_.state == State.RUNNING).get
    } finally client.close()
  }

  @Test
  def testAllContainers(): Unit = {
    val client =
      DockerClient
        .builder()
        .hostname(server.hostname)
        .port(server.port)
        .user(server.user)
        .password(server.password)
        .build()

    try {
      val rContainers = client.containers()
      rContainers shouldBe containers
    } finally client.close()
  }

  @Test
  def testSetHostname(): Unit = {
    val client =
      DockerClient
        .builder()
        .hostname(server.hostname)
        .port(server.port)
        .user(server.user)
        .password(server.password)
        .build()

    val hostname = methodName()
    try client.executor().image("aaa").hostname(hostname).dockerCommand().contains(s"-h $hostname") shouldBe true
    finally client.close()
  }

  @Test
  def testSetEnvs(): Unit = {
    val client =
      DockerClient
        .builder()
        .hostname(server.hostname)
        .port(server.port)
        .user(server.user)
        .password(server.password)
        .build()

    val key = s"key-${methodName()}"
    val value = s"value-${methodName()}"
    try client
      .executor()
      .image("aaa")
      .envs(Map(key -> value))
      .dockerCommand()
      .contains(s"""-e \"$key=$value\"""") shouldBe true
    finally client.close()
  }

  @Test
  def testSetRoute(): Unit = {
    val client =
      DockerClient
        .builder()
        .hostname(server.hostname)
        .port(server.port)
        .user(server.user)
        .password(server.password)
        .build()

    val hostname = methodName()
    val ip = "192.168.103.1"
    try client
      .executor()
      .image("aaa")
      .route(Map(hostname -> ip))
      .dockerCommand()
      .contains(s"--add-host $hostname:$ip") shouldBe true
    finally client.close()
  }

  @Test
  def testSetForwardPorts(): Unit = {
    val client =
      DockerClient
        .builder()
        .hostname(server.hostname)
        .port(server.port)
        .user(server.user)
        .password(server.password)
        .build()

    val hostname = methodName()
    val port0 = 12345
    val port1 = 12346
    try client
      .executor()
      .image("aaa")
      .portMappings(Map(port0 -> port0, port1 -> port1))
      .dockerCommand()
      .contains(s"-p $port0:$port0 -p $port1:$port1") shouldBe true
    finally client.close()
  }

  @Test
  def testParseForwardPorts(): Unit = {
    val ip = "0.0.0.0"
    val minPort = 12345
    val maxPort = 12350
    val ports = DockerClient.parsePortMapping(s"$ip:$minPort-$maxPort->$minPort-$maxPort/tcp")
    ports.size shouldBe 1
    ports.find(_.hostIp == ip).get.portPairs.size shouldBe maxPort - minPort + 1
    ports.find(_.hostIp == ip).get.portPairs shouldBe (minPort to maxPort).map(port => PortPair(port, port)).toSeq
  }

  @Test
  def testParseForwardPorts2(): Unit = {
    val ip = "0.0.0.0"
    val hostPorts = Seq.fill(5)(Random.nextInt(10000))
    val containerPorts = Seq.fill(5)(Random.nextInt(10000))
    val ports = DockerClient.parsePortMapping(
      hostPorts.zipWithIndex
        .map {
          case (p, index) => s"$ip:$p->${containerPorts(index)}/tcp"
        }
        .mkString(", "))
    ports.size shouldBe 1
    ports.find(_.hostIp == ip).get.portPairs.size shouldBe hostPorts.size
    hostPorts.zipWithIndex.foreach {
      case (p, index) =>
        ports.find(_.hostIp == ip).get.portPairs.find(_.hostPort == p).get.containerPort shouldBe containerPorts(index)
    }
  }

  @After
  def tearDown(): Unit = CloseOnce.close(server)
}
