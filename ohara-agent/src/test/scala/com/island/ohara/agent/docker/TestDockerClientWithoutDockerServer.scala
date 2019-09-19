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
import com.island.ohara.client.configurator.v0.ContainerApi.PortPair
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.util.Random
class TestDockerClientWithoutDockerServer extends OharaTest with Matchers {

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

  @Test
  def testSetHostname(): Unit = {
    val hostname = CommonUtils.randomString(10)
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
    val key = s"key-${CommonUtils.randomString(10)}"
    val value = s"value-${CommonUtils.randomString(10)}"
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
