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

package com.island.ohara.configurator

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, TopicApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await

class TestOhara764 extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake().build()

  import scala.concurrent.duration._
  @Test
  def testStartSourceWithoutExistentTopic(): Unit = {
    val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)
    val source = Await.result(
      access.add(
        ConnectorCreationRequest(name = "abc",
                                 className = "aaa.class",
                                 topics = Seq("abc"),
                                 numberOfTasks = 1,
                                 schema = Seq.empty,
                                 configs = Map.empty)),
      10 seconds
    )

    an[IllegalArgumentException] should be thrownBy Await.result(access.start(source.id), 30 seconds)

    val topic = Await.result(
      TopicApi.access().hostname(configurator.hostname).port(configurator.port).add(TopicApi.creationRequest("abc")),
      10 seconds)
    val source2 = Await.result(
      access.add(
        ConnectorCreationRequest(name = "abc",
                                 className = "aaa.class",
                                 topics = Seq(topic.id),
                                 numberOfTasks = 1,
                                 schema = Seq.empty,
                                 configs = Map.empty)),
      10 seconds
    )
    Await.result(access.start(source2.id), 30 seconds).id shouldBe source2.id
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
