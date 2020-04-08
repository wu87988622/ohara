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

import oharastream.ohara.common.util.{CommonUtils, Releasable}
import oharastream.ohara.it.{ContainerPlatform, IntegrationTest}
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.{After, Before, Test}
import org.scalatest.Matchers.{include, _}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(value = classOf[Parameterized])
class TestContainerClientLog(platform: ContainerPlatform) extends IntegrationTest {
  private[this] val name            = CommonUtils.randomString(10)
  private[this] val resourceRef     = platform.setup()
  private[this] val containerClient = resourceRef.containerClient

  private[this] def createBusyBox(arguments: Seq[String]): Unit =
    result(
      containerClient.containerCreator
        .nodeName(platform.nodeNames.head)
        .name(name)
        .imageName("busybox")
        .arguments(arguments)
        .create()
    )

  private[this] def log(sinceSeconds: Option[Long]): String = result(containerClient.log(name, sinceSeconds))._2

  @Before
  def before(): Unit =
    createBusyBox(Seq("sh", "-c", "while true; do $(echo date); sleep 1; done"))

  @Test
  def test(): Unit = {
    // wait the container
    await(() => log(None).contains("UTC"))
    val lastLine = log(None).split("\n").last
    TimeUnit.SECONDS.sleep(3)
    log(Some(1)) should not include lastLine
    log(Some(10)) should include(lastLine)
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(() => result(containerClient.forceRemove(name)))
    Releasable.close(resourceRef)
  }
}

object TestContainerClientLog {
  @Parameters(name = "{index} mode = {0}")
  def parameters: java.util.Collection[ContainerPlatform] = {
    val modes = ContainerPlatform.all
    if (modes.isEmpty) java.util.Collections.singletonList(ContainerPlatform.empty)
    else modes.asJava
  }
}
