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
import com.island.ohara.common.rule.SmallTest
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestFakeDockerClient extends SmallTest with Matchers {

  val fake = new FakeDockerClient

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)
  @Before
  def setup(): Unit = {
    // create a fake container
    fake
      .containerCreator()
      .imageName("fake_image")
      .hostname("localhost")
      .name(methodName())
      .envs(Map("bar" -> "foo"))
      .portMappings(Map(1234 -> 5678))
      .execute()
  }
  @Test
  def testFakeClient(): Unit = {
    result(fake.containers).size shouldBe 1

    fake.imageNames().head shouldBe "fake_image"

    result(fake.containers(_ == methodName())).size shouldBe 1
    result(fake.containers(_ == methodName())).head.state shouldBe ContainerState.RUNNING.name
    result(fake.containers(_ == methodName())).head.id shouldBe methodName()
    result(fake.containers(_ == methodName())).head.environments shouldBe Map("bar" -> "foo")
    result(fake.containers(_ == methodName())).head.portMappings.head shouldBe PortMapping("localhost",
                                                                                           Seq(PortPair(1234, 5678)))

    fake.stop(methodName())
    result(fake.containers(_ == methodName())).head.state shouldBe ContainerState.EXITED.name

    fake.remove(methodName())

    result(fake.containers).size shouldBe 0
  }
}
