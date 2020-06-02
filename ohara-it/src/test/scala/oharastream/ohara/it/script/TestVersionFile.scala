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

package oharastream.ohara.it.script

import oharastream.ohara.common.util.{Releasable, VersionUtils}
import oharastream.ohara.it.{ContainerPlatform, IntegrationTest, ServiceKeyHolder}
import org.junit.{After, Test}
import org.scalatest.matchers.should.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * test command "-v" for all ohara images.
  * Noted: the "version" may be changed at runtime by jenkins so we check only revision.
  */
class TestVersionFile extends IntegrationTest {
  private[this] val platform                       = ContainerPlatform.dockerMode
  private[this] val resourceRef                    = platform.setup()
  private[this] val containerClient                = resourceRef.containerClient
  protected val serviceKeyHolder: ServiceKeyHolder = ServiceKeyHolder(containerClient)

  /**
    * see VersionUtils for following fields
    */
  private[this] val expectedStrings = Set(
    "version",
    "revision",
    "branch",
    "user",
    "date"
  )

  @Test
  def testConfigurator(): Unit =
    testVersion(s"oharastream/configurator:${VersionUtils.VERSION}", expectedStrings)

  @Test
  def testWorker(): Unit =
    testVersion(s"oharastream/connect-worker:${VersionUtils.VERSION}", expectedStrings)

  @Test
  def testStream(): Unit =
    testVersion(s"oharastream/stream:${VersionUtils.VERSION}", expectedStrings)

  @Test
  def testShabondi(): Unit =
    testVersion(s"oharastream/shabondi:${VersionUtils.VERSION}", expectedStrings)

  @Test
  def testManager(): Unit =
    testVersion(s"oharastream/manager:${VersionUtils.VERSION}", expectedStrings)

  /**
    * we don't embed any ohara code to zookeeper so zk image show only revision.
    */
  @Test
  def testZookeeper(): Unit = testVersion(s"oharastream/zookeeper:${VersionUtils.VERSION}", Set("ohara"))

  /**
    * we don't embed any ohara code to broker so zk image show only revision.
    */
  @Test
  def testBroker(): Unit = testVersion(s"oharastream/broker:${VersionUtils.VERSION}", Set("ohara"))

  private[this] def testVersion(imageName: String, expectedStrings: Set[String]): Unit = platform.nodeNames.foreach {
    hostname =>
      val key           = serviceKeyHolder.generateClusterKey()
      val containerName = s"${key.group()}-${key.name()}"
      val versionString: String = result(
        containerClient.containerCreator
          .imageName(imageName)
          .command("-v")
          .name(containerName)
          .nodeName(hostname)
          .create()
          .flatMap(_ => containerClient.log(containerName).map(_.head._2))
      )
      expectedStrings.foreach(s => versionString should include(s))
  }

  @After
  def releaseConfigurator(): Unit = {
    Releasable.close(serviceKeyHolder)
    Releasable.close(resourceRef)
  }
}
