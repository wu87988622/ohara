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

package com.island.ohara.it.collie

import com.island.ohara.agent.Agent
import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.it.EnvTestingUtils
import org.junit.After

class TestLogOnDocker extends BasicTests4Log {
  private[this] val name       = CommonUtils.randomString(10)
  private[this] val node: Node = EnvTestingUtils.dockerNodes().head
  private[this] val dockerClient = DockerClient(
    Agent.builder.hostname(node.hostname).port(node.port.get).user(node.user.get).password(node.password.get).build
  )

  override protected def createBusyBox(imageName: String, arguments: Seq[String]): Unit =
    dockerClient
      .containerCreator()
      .name(name)
      .imageName(imageName)
      .arguments(arguments)
      .create()

  override protected def log(sinceSeconds: Option[Long]): String = dockerClient.log(name, sinceSeconds)

  @After
  def tearDown(): Unit = {
    Releasable.close(() => dockerClient.forceRemove(name))
    Releasable.close(dockerClient)
  }
}
