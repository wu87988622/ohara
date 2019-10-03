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

package com.island.ohara.it.client

import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.client.configurator.v0.{LogApi, NodeApi}
import com.island.ohara.common.util.{CommonUtils, Releasable, VersionUtils}
import com.island.ohara.it.{EnvTestingUtils, IntegrationTest}
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class TestQueryConfiguratorLog extends IntegrationTest with Matchers {
  private[this] val nodes = EnvTestingUtils.sshNodes()

  @Test
  def test(): Unit = {
    val node = nodes.head
    val imageName = s"oharastream/configurator:${VersionUtils.VERSION}"
    val containerName = CommonUtils.randomString(10)
    val dockerClient = DockerClient.builder
      .hostname(node.hostname)
      .port(node.port.get)
      .user(node.user.get)
      .password(node.password.get)
      .build
    try {
      dockerClient.imageNames() should contain(imageName)
      val clientPort = CommonUtils.availablePort()
      dockerClient
        .containerCreator()
        .imageName(imageName)
        .portMappings(Map(clientPort -> clientPort))
        .command(s"--port $clientPort")
        .route(Map(node.hostname -> CommonUtils.address(node.hostname)))
        .name(containerName)
        .create()
      await { () =>
        try {
          result(
            NodeApi.access
              .hostname(node.hostname)
              .port(clientPort)
              .request
              .hostname(node.hostname)
              .port(node.port.get)
              .user(node.user.get)
              .password(node.password.get)
              .create())
          true
        } catch {
          // wait for the configurator container
          case _: Throwable => false
        }
      }

      val log = result(LogApi.access.hostname(node.hostname).port(clientPort).log4Configurator())
      log.clusterKey.name() shouldBe containerName
      log.logs.size shouldBe 1
      log.logs.head.hostname shouldBe node.hostname
      log.logs.head.value.length should not be 0
    } finally {
      Releasable.close(() => dockerClient.forceRemove(containerName))
      Releasable.close(dockerClient)
    }
  }
}
