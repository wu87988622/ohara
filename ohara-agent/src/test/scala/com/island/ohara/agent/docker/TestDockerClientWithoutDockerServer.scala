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

package com.island.ohara.agent.docker
import com.island.ohara.agent.docker.TestDockerClientWithoutDockerServer._
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortPair}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.service.SshdServer
import com.island.ohara.testing.service.SshdServer.CommandHandler
import org.junit.{AfterClass, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random
class TestDockerClientWithoutDockerServer extends SmallTest with Matchers {

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def withoutCleanup(): Unit = DockerClientImpl
    .toSshCommand(
      hostname = CommonUtils.randomString(5),
      imageName = CommonUtils.randomString(5),
      name = CommonUtils.randomString(5),
      command = CommonUtils.randomString(5),
      removeContainerOnExit = false,
      ports = Map.empty,
      envs = Map.empty,
      route = Map.empty,
      volumeMapping = Map.empty,
      networkDriver = NetworkDriver.BRIDGE
    )
    .contains("--rm") shouldBe false

  @Test
  def withCleanup(): Unit = DockerClientImpl
    .toSshCommand(
      hostname = CommonUtils.randomString(5),
      imageName = CommonUtils.randomString(5),
      name = CommonUtils.randomString(5),
      command = CommonUtils.randomString(5),
      removeContainerOnExit = true,
      ports = Map.empty,
      envs = Map.empty,
      route = Map.empty,
      volumeMapping = Map.empty,
      networkDriver = NetworkDriver.BRIDGE
    )
    .contains("--rm") shouldBe true

  private[this] def testSpecifiedContainer(expectedState: ContainerState): Unit = {
    val rContainers = result(CLIENT.containers).filter(_.state == expectedState.name)
    rContainers.size shouldBe 1
    rContainers.head shouldBe CONTAINERS.find(_.state == expectedState.name).get
  }
  @Test
  def testCreatedContainers(): Unit = testSpecifiedContainer(ContainerState.CREATED)

  @Test
  def testRestartingContainers(): Unit = testSpecifiedContainer(ContainerState.RESTARTING)

  @Test
  def testRunningContainers(): Unit = testSpecifiedContainer(ContainerState.RUNNING)

  @Test
  def testRemovingContainers(): Unit = testSpecifiedContainer(ContainerState.REMOVING)

  @Test
  def testPausedContainers(): Unit = testSpecifiedContainer(ContainerState.PAUSED)

  @Test
  def testExitedContainers(): Unit = testSpecifiedContainer(ContainerState.EXITED)

  @Test
  def testDeadContainers(): Unit = testSpecifiedContainer(ContainerState.DEAD)

  @Test
  def testActiveContainers(): Unit = {
    val rContainers = result(CLIENT.activeContainers)
    rContainers.size shouldBe 1
    rContainers.head shouldBe CONTAINERS.find(_.state == ContainerState.RUNNING.name).get
  }

  @Test
  def testAllContainers(): Unit = {
    val rContainers = result(CLIENT.containers)
    rContainers shouldBe CONTAINERS
  }

  @Test
  def testSetHostname(): Unit = {
    val hostname = methodName()
    DockerClientImpl
      .toSshCommand(
        hostname = hostname,
        imageName = CommonUtils.randomString(5),
        name = CommonUtils.randomString(5),
        command = CommonUtils.randomString(5),
        removeContainerOnExit = true,
        ports = Map.empty,
        envs = Map.empty,
        route = Map.empty,
        volumeMapping = Map.empty,
        networkDriver = NetworkDriver.BRIDGE
      )
      .contains(s"-h $hostname") shouldBe true
  }

  @Test
  def testSetEnvs(): Unit = {
    val key = s"key-${methodName()}"
    val value = s"value-${methodName()}"
    DockerClientImpl
      .toSshCommand(
        hostname = CommonUtils.randomString(5),
        imageName = CommonUtils.randomString(5),
        name = CommonUtils.randomString(5),
        command = CommonUtils.randomString(5),
        removeContainerOnExit = true,
        ports = Map.empty,
        envs = Map(key -> value),
        route = Map.empty,
        volumeMapping = Map.empty,
        networkDriver = NetworkDriver.BRIDGE
      )
      .contains(s"""-e \"$key=$value\"""") shouldBe true
  }

  @Test
  def testSetRoute(): Unit = {
    val hostname = CommonUtils.randomString(5)
    val ip = "192.168.103.1"
    DockerClientImpl
      .toSshCommand(
        hostname = hostname,
        imageName = CommonUtils.randomString(5),
        name = CommonUtils.randomString(5),
        command = CommonUtils.randomString(5),
        removeContainerOnExit = true,
        ports = Map.empty,
        envs = Map.empty,
        route = Map(hostname -> ip),
        volumeMapping = Map.empty,
        networkDriver = NetworkDriver.BRIDGE
      )
      .contains(s"--add-host $hostname:$ip") shouldBe true
  }

  @Test
  def testSetForwardPorts(): Unit = {
    val port0 = 12345
    val port1 = 12346
    DockerClientImpl
      .toSshCommand(
        hostname = CommonUtils.randomString(5),
        imageName = CommonUtils.randomString(5),
        name = CommonUtils.randomString(5),
        command = CommonUtils.randomString(5),
        removeContainerOnExit = true,
        ports = Map(port0 -> port0, port1 -> port1),
        envs = Map.empty,
        route = Map.empty,
        volumeMapping = Map.empty,
        networkDriver = NetworkDriver.BRIDGE
      )
      .contains(s"-p $port0:$port0 -p $port1:$port1") shouldBe true
  }

  @Test
  def testParseForwardPorts(): Unit = {
    val ip = "0.0.0.0"
    val minPort = 12345
    val maxPort = 12350
    val ports = DockerClientImpl.parsePortMapping(s"$ip:$minPort-$maxPort->$minPort-$maxPort/tcp")
    ports.size shouldBe 1
    ports.find(_.hostIp == ip).get.portPairs.size shouldBe maxPort - minPort + 1
    ports.find(_.hostIp == ip).get.portPairs shouldBe (minPort to maxPort).map(port => PortPair(port, port))
  }

  @Test
  def testParseForwardPorts2(): Unit = {
    val ip = "0.0.0.0"
    val hostPorts = Seq.fill(5)(Random.nextInt(10000))
    val containerPorts = Seq.fill(5)(Random.nextInt(10000))
    val ports = DockerClientImpl.parsePortMapping(
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
}

/**
  * SSH server and client are shared by all test cases since the cost of newing them is not cheap...
  */
object TestDockerClientWithoutDockerServer {

  private val CONTAINERS = ContainerState.all.map(
    s =>
      ContainerInfo(
        nodeName = CommonUtils.hostname(),
        id = s"id-${s.name}",
        imageName = s"image-${s.name}",
        created = s"created-${s.name}",
        state = s.name,
        kind = "SSH",
        name = s"name-${s.name}",
        size = s"size-${s.name}",
        portMappings = Seq.empty,
        environments = Map("env0" -> "abc", "env1" -> "ccc"),
        hostname = "localhost"
    ))

  private[this] def containerToString(container: ContainerInfo): String = Seq(
    container.id,
    container.imageName,
    container.created,
    container.state,
    container.name,
    container.size
  ).mkString(DockerClientImpl.DIVIDER)

  import scala.collection.JavaConverters._
  private val SERVER = SshdServer.local(
    0,
    Seq(
      // handle normal
      new CommandHandler {
        override def belong(command: String): Boolean =
          command == s"docker ps -a --format ${DockerClientImpl.LIST_PROCESS_FORMAT}"
        override def execute(command: String): java.util.List[String] = if (belong(command))
          CONTAINERS.map(containerToString).asJava
        else throw new IllegalArgumentException(s"$command doesn't support")
      },
      // handle env
      new CommandHandler {
        override def belong(command: String): Boolean =
          command.contains("docker inspect") && command.contains("Config.Env")
        override def execute(command: String): java.util.List[String] = if (belong(command))
          Seq("[env0=abc env1=ccc]").asJava
        else throw new IllegalArgumentException(s"$command doesn't support")
      },
      // handle hostname
      new CommandHandler {
        override def belong(command: String): Boolean =
          command.contains("docker inspect") && command.contains("Config.Hostname")
        override def execute(command: String): java.util.List[String] = if (belong(command)) Seq("localhost").asJava
        else throw new IllegalArgumentException(s"$command doesn't support")
      },
      // final
      new CommandHandler {
        override def belong(command: String): Boolean = true
        override def execute(command: String): java.util.List[String] =
          throw new IllegalArgumentException(s"$command doesn't support")
      }
    ).map(_.asInstanceOf[CommandHandler]).asJava
  )

  private val CLIENT =
    DockerClient
      .builder()
      .hostname(SERVER.hostname)
      .port(SERVER.port)
      .user(SERVER.user)
      .password(SERVER.password)
      .build()

  @AfterClass
  def afterAll(): Unit = {
    Releasable.close(CLIENT)
    Releasable.close(SERVER)
  }
}
