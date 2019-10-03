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

package com.island.ohara.agent.fake

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.ContainerApi.{PortMapping, PortPair}
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestFakeDockerClient extends OharaTest with Matchers {

  private[this] val fake = new FakeDockerClient("fake_node")

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] val containerName: String = CommonUtils.randomString(10)

  @Before
  def setup(): Unit = {
    // create a fake container
    fake
      .containerCreator()
      .imageName("fake_image")
      .hostname("localhost")
      .name(containerName)
      .envs(Map("bar" -> "foo"))
      .portMappings(Map(1234 -> 5678))
      .create()
  }
  @Test
  def testFakeClient(): Unit = {
    result(fake.containers()).size shouldBe 1

    fake.imageNames().head shouldBe "fake_image"

    result(fake.containers(_ == containerName)).size shouldBe 1
    result(fake.containers(_ == containerName)).head.state shouldBe ContainerState.RUNNING.name
    result(fake.containers(_ == containerName)).head.id shouldBe containerName
    result(fake.containers(_ == containerName)).head.nodeName shouldBe "fake_node"
    result(fake.containers(_ == containerName)).head.hostname shouldBe "localhost"
    result(fake.containers(_ == containerName)).head.environments shouldBe Map("bar" -> "foo")
    result(fake.containers(_ == containerName)).head.portMappings.head shouldBe PortMapping("localhost",
                                                                                            Seq(PortPair(1234, 5678)))

    fake.stop(containerName)
    result(fake.containers(_ == containerName)).head.state shouldBe ContainerState.EXITED.name

    fake.remove(containerName)

    result(fake.containers()).size shouldBe 0
  }

  @Test
  def testAddConfigs(): Unit = {
    val configs = (for (i <- 1 to 10) yield i.toString -> CommonUtils.randomString()).toMap
    // using generate name
    val n1 = fake.addConfig(configs)
    // using specific name
    val n2 = fake.addConfig(CommonUtils.randomString(), configs)

    n1 should not be n2
    configs.keySet.forall(fake.inspectConfig(n1).contains) shouldBe true
    fake.inspectConfig(n1).keySet.forall(fake.inspectConfig(n2).contains) shouldBe true

    // remove a config list
    fake.removeConfig(n1)
    an[Exception] should be thrownBy fake.inspectConfig(n1)
  }
}
