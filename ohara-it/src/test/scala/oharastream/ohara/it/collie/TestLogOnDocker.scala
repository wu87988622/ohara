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

import oharastream.ohara.agent.DataCollie
import oharastream.ohara.agent.docker.DockerClient
import oharastream.ohara.common.util.{CommonUtils, Releasable}
import oharastream.ohara.it.EnvTestingUtils
import org.junit.After
import scala.concurrent.ExecutionContext.Implicits.global
class TestLogOnDocker extends BasicTests4Log {
  private[this] val name         = CommonUtils.randomString(10)
  private[this] val node         = EnvTestingUtils.dockerNodes().head
  private[this] val dockerClient = DockerClient(DataCollie(EnvTestingUtils.dockerNodes()))

  override protected def createBusyBox(imageName: String, arguments: Seq[String]): Unit =
    result(
      dockerClient.containerCreator
        .nodeName(node.hostname)
        .name(name)
        .imageName(imageName)
        .arguments(arguments)
        .create()
    )

  override protected def log(sinceSeconds: Option[Long]): String = result(dockerClient.log(name, sinceSeconds))._2

  @After
  def tearDown(): Unit = {
    Releasable.close(() => result(dockerClient.forceRemove(name)))
    Releasable.close(dockerClient)
  }
}
