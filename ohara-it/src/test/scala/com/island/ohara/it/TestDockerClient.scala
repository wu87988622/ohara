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

package com.island.ohara.it

import java.util.concurrent.TimeUnit

import com.island.ohara.agent.Agent
import com.island.ohara.agent.docker.{ContainerState, DockerClient}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import org.junit.{After, Before, Test}
import org.scalatest.Matchers._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * all test cases here are executed on remote node. If no remote node is defined, all tests are skipped.
  * You can run following command to pass the information of remote node.
  * $ gradle clean ohara-it:test --tests *TestDockerClient -PskipManager -Pohara.it.docker=$user:$password@$hostname:$port
  */
class TestDockerClient extends IntegrationTest {
  private[this] var client: DockerClient = _

  private[this] val webHost = "www.google.com.tw"

  private[this] var remoteHostname: String = _

  private[this] val imageName = "centos:7"

  private[this] val name = CommonUtils.randomString(10)

  @Before
  def setup(): Unit =
    EnvTestingUtils.dockerNodes().headOption.foreach { node =>
      client = DockerClient(
        Agent.builder.hostname(node.hostname).port(node._port).user(node._user).password(node._password).build
      )
      remoteHostname = node.hostname
    }

  /**
    * make sure all test cases here are executed only if we have defined the docker server.
    * @param f test case
    */
  private[this] def runTest(f: DockerClient => Unit): Unit =
    if (client == null)
      skipTest(s"no available nodes are passed from env variables")
    else f(client)

  @Test
  def testLog(): Unit = runTest { client =>
    client
      .containerCreator()
      .name(name)
      .imageName(imageName)
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .create()
    client.log(name, None).contains(webHost) shouldBe true
  }

  @Test
  def testList(): Unit = runTest { client =>
    client.containerNames().map(_.name).contains(name) shouldBe false
    client
      .containerCreator()
      .name(name)
      .imageName(imageName)
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .create()
    val container = result(client.containers()).find(_.name == name).get
    client.containerNames().map(_.name).contains(container.name) shouldBe true
  }

  @Test
  def testNonCleanup(): Unit = runTest { client =>
    // ping google 3 times
    client
      .containerCreator()
      .name(name)
      .imageName(imageName)
      .command(s"""/bin/bash -c \"ping $webHost -c 3\"""")
      .create()
    client.containerNames().map(_.name).contains(name) shouldBe true
    TimeUnit.SECONDS.sleep(3)
    result(client.containers()).find(_.name == name).get.state.toUpperCase shouldBe ContainerState.EXITED.name
  }

  @Test
  def testVerify(): Unit = runTest(_.verify() shouldBe true)

  @Test
  def testRoute(): Unit = runTest { client =>
    client
      .containerCreator()
      .name(name)
      .route(Map("ABC" -> "192.168.123.123"))
      .imageName(imageName)
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .create()
    val hostFile = client.containerInspector(name).cat("/etc/hosts").get
    hostFile.contains("192.168.123.123") shouldBe true
    hostFile.contains("ABC") shouldBe true
  }

  @Test
  def testPortMapping(): Unit = runTest { client =>
    val availablePort = CommonUtils.availablePort()
    client
      .containerCreator()
      .name(name)
      .imageName(imageName)
      .portMappings(Map(availablePort -> availablePort))
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .create()

    val container = result(client.containers()).find(_.name == name).get
    container.portMappings.size shouldBe 1
    container.portMappings.size shouldBe 1
    container.portMappings.head.hostPort shouldBe availablePort
    container.portMappings.head.containerPort shouldBe availablePort
  }

  @Test
  def testSetEnv(): Unit = runTest { client =>
    client
      .containerCreator()
      .name(name)
      .imageName(imageName)
      .envs(Map("abc" -> "123", "ccc" -> "ttt"))
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .create()
    val container = result(client.containers()).find(_.name == name).get
    container.environments("abc") shouldBe "123"
    container.environments("ccc") shouldBe "ttt"
  }

  @Test
  def testHostname(): Unit = runTest { client =>
    val hostname = CommonUtils.randomString(5)
    client
      .containerCreator()
      .name(name)
      .imageName(imageName)
      .hostname(hostname)
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .create()
    result(client.containers()).find(_.name == name).get.hostname shouldBe hostname
  }

  @Test
  def testNodeName(): Unit = runTest { client =>
    client
      .containerCreator()
      .name(name)
      .imageName(imageName)
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .create()
    result(client.containers()).find(_.name == name).get.nodeName shouldBe remoteHostname
  }

  @Test
  def testAppend(): Unit = runTest { client =>
    client
      .containerCreator()
      .name(name)
      .imageName(imageName)
      .command(s"""/bin/bash -c \"ping $webHost\"""")
      .create()
    val container = result(client.containers()).find(_.name == name).get
    client.containerInspector(container.name).append("/tmp/ttt", "abc") shouldBe "abc\n"
    client.containerInspector(container.name).append("/tmp/ttt", "abc") shouldBe "abc\nabc\n"
    client.containerInspector(container.name).append("/tmp/ttt", Seq("t", "z")) shouldBe "abc\nabc\nt\nz\n"
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(() => client.forceRemove(name))
    Releasable.close(client)
  }
}
