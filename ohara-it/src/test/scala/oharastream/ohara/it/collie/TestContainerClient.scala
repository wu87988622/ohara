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

package oharastream.ohara.it.collie

import java.util.concurrent.TimeUnit

import oharastream.ohara.agent.DataCollie
import oharastream.ohara.agent.docker.DockerClient
import oharastream.ohara.client.configurator.NodeApi.Node
import oharastream.ohara.common.util.{CommonUtils, Releasable}
import oharastream.ohara.it.{ContainerPlatform, IntegrationTest}
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.{After, Test}
import org.scalatest.matchers.should.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._

@RunWith(value = classOf[Parameterized])
class TestContainerClient(platform: ContainerPlatform) extends IntegrationTest {
  private[this] val containerClient = platform.setupContainerClient()
  private[this] val name            = CommonUtils.randomString(5)
  private[this] val imageName       = "centos:7"
  private[this] val webHost         = "www.google.com.tw"

  private[this] def createBusyBox(arguments: Seq[String]): Unit =
    result(
      containerClient.containerCreator
        .nodeName(platform.nodeNames.head)
        .name(name)
        .imageName("busybox")
        .arguments(arguments)
        .create()
    )

  @Test
  def testLog(): Unit = {
    def log(name: String, sinceSeconds: Option[Long]): String =
      result(containerClient.log(name, sinceSeconds)).head._2

    createBusyBox(Seq("sh", "-c", "while true; do $(echo date); sleep 1; done"))
    try {
      // wait the container
      await(() => log(name, None).contains("UTC"))
      val lastLine = log(name, None).split("\n").last
      TimeUnit.SECONDS.sleep(3)
      log(name, Some(1)) should not include lastLine
      log(name, Some(10)) should include(lastLine)
    } finally Releasable.close(() => result(containerClient.forceRemove(name)))
  }

  @Test
  def testVolume(): Unit = {
    def checkVolumeExists(names: Seq[String]): Unit = {
      names.foreach { volumeName =>
        await(() => !result(containerClient.volumes()).exists(_.name == volumeName))
      }
    }
    val path  = s"/tmp/${CommonUtils.randomString(10)}"
    val names = Seq(CommonUtils.randomString(), CommonUtils.randomString())
    checkVolumeExists(names)
    try {
      names.foreach(
        name =>
          result(
            containerClient.volumeCreator
              .name(name)
              .nodeName(platform.nodeNames.head)
              .path(path)
              .create()
          )
      )
      names.foreach { name =>
        result(containerClient.volumes(name)).head.path shouldBe path
        result(containerClient.volumes(name)).head.name shouldBe name
        result(containerClient.volumes(name)).head.nodeName shouldBe platform.nodeNames.head
      }
    } finally {
      names.foreach(name => Releasable.close(() => result(containerClient.removeVolumes(name))))
      checkVolumeExists(names)
    }
  }

  @Test
  def testList(): Unit = {
    result(
      containerClient.containerCreator
        .nodeName(platform.nodeNames.head)
        .name(name)
        .imageName(imageName)
        .command(s"""/bin/bash -c \"ping $webHost\"""")
        .create()
    )
    result(containerClient.containerNames()).map(_.name) should contain(name)
  }

  @Test
  def testRoute(): Unit = containerClient match {
    case _: DockerClient =>
      result(
        containerClient.containerCreator
          .nodeName(platform.nodeNames.head)
          .name(name)
          .routes(Map("ABC" -> "192.168.123.123"))
          .imageName(imageName)
          .command(s"""/bin/bash -c \"ping $webHost\"""")
          .create()
      )
      val hostFile =
        result(containerClient.asInstanceOf[DockerClient].containerInspector.name(name).cat("/etc/hosts")).head._2
      hostFile should include("192.168.123.123")
      hostFile should include("ABC")
    case _ => skipTest(s"${containerClient.getClass.getName} is skipped")
  }

  @Test
  def testPortMapping(): Unit = {
    val availablePort = CommonUtils.availablePort()
    result(
      containerClient.containerCreator
        .nodeName(platform.nodeNames.head)
        .name(name)
        .imageName(imageName)
        .portMappings(Map(availablePort -> availablePort))
        .command(s"""/bin/bash -c \"ping $webHost\"""")
        .create()
    )

    val container = result(containerClient.containers()).find(_.name == name).get
    container.portMappings.size shouldBe 1
    container.portMappings.size shouldBe 1
    container.portMappings.head.hostPort shouldBe availablePort
    container.portMappings.head.containerPort shouldBe availablePort
  }

  @Test
  def testSetEnv(): Unit = {
    result(
      containerClient.containerCreator
        .nodeName(platform.nodeNames.head)
        .name(name)
        .imageName(imageName)
        .envs(Map("abc" -> "123", "ccc" -> "ttt"))
        .command(s"""/bin/bash -c \"ping $webHost\"""")
        .create()
    )
    val container = result(containerClient.containers()).find(_.name == name).get
    container.environments("abc") shouldBe "123"
    container.environments("ccc") shouldBe "ttt"
  }

  @Test
  def testHostname(): Unit = {
    val hostname = CommonUtils.randomString(5)
    result(
      containerClient.containerCreator
        .nodeName(platform.nodeNames.head)
        .name(name)
        .imageName(imageName)
        .hostname(hostname)
        .command(s"""/bin/bash -c \"ping $webHost\"""")
        .create()
    )
    result(containerClient.containers()).find(_.name == name).get.hostname shouldBe hostname
  }

  @Test
  def testNodeName(): Unit = {
    result(
      containerClient.containerCreator
        .nodeName(platform.nodeNames.head)
        .name(name)
        .imageName(imageName)
        .command(s"""/bin/bash -c \"ping $webHost\"""")
        .create()
    )
    result(containerClient.containers()).find(_.name == name).get.nodeName shouldBe platform.nodeNames.head
  }

  @Test
  def testAppend(): Unit = containerClient match {
    case _: DockerClient =>
      result(
        containerClient.containerCreator
          .nodeName(platform.nodeNames.head)
          .name(name)
          .imageName(imageName)
          .command(s"""/bin/bash -c \"ping $webHost\"""")
          .create()
      )
      val container = result(containerClient.containers()).find(_.name == name).get
      result(
        containerClient.asInstanceOf[DockerClient].containerInspector.name(container.name).append("/tmp/ttt", "abc")
      ).head._2 shouldBe "abc\n"
      result(
        containerClient.asInstanceOf[DockerClient].containerInspector.name(container.name).append("/tmp/ttt", "abc")
      ).head._2 shouldBe "abc\nabc\n"
      result(
        containerClient
          .asInstanceOf[DockerClient]
          .containerInspector
          .name(container.name)
          .append("/tmp/ttt", Seq("t", "z"))
      ).head._2 shouldBe "abc\nabc\nt\nz\n"
    case _ => skipTest(s"${containerClient.getClass.getName} is skipped")
  }

  @Test
  def testResources(): Unit = result(containerClient.resources()) should not be Map.empty

  @Test
  def testResourcesOfUnavailableNode(): Unit = {
    val c = DockerClient(
      DataCollie(
        Seq(
          Node(
            hostname = "abc",
            user = "user",
            password = "password"
          )
        )
      )
    )
    try result(c.resources()) shouldBe Map.empty
    finally c.close()
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(() => result(containerClient.forceRemove(name)))
    Releasable.close(containerClient)
  }
}

object TestContainerClient {
  @Parameters(name = "{index} mode = {0}")
  def parameters: java.util.Collection[ContainerPlatform] = {
    val modes = ContainerPlatform.all
    if (modes.isEmpty) java.util.List.of(ContainerPlatform.empty)
    else modes.asJava
  }
}
