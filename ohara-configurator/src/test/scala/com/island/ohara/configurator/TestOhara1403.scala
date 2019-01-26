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
import com.island.ohara.client.configurator.v0.PipelineApi.PipelineCreationRequest
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, PipelineApi, TopicApi}
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestOhara1403 extends SmallTest with Matchers {

  private[this] val configurator =
    Configurator.builder().fake().build()
  @Test
  def test(): Unit = {
    val topic = Await.result(TopicApi
                               .access()
                               .hostname(configurator.hostname)
                               .port(configurator.port)
                               .add(TopicCreationRequest(methodName(), 1, 1)),
                             10 seconds)

    val connector = Await.result(
      ConnectorApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          ConnectorCreationRequest(name = methodName(),
                                   className = classOf[DumbSink].getName,
                                   schema = Seq.empty,
                                   topics = Seq(topic.id),
                                   numberOfTasks = 1,
                                   configs = Map.empty)),
      10 seconds
    )

    val pipeline = Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(PipelineCreationRequest(methodName(), Map(topic.id -> connector.id))),
      10 seconds
    )

    // start the connector
    Await
      .result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).start(connector.id),
              10 seconds)
      .state shouldBe Some(ConnectorState.RUNNING)

    // we can't delete a pipeline having a running connector

    an[IllegalArgumentException] should be thrownBy Await.result(
      PipelineApi.access().hostname(configurator.hostname).port(configurator.port).delete(pipeline.id),
      10 seconds)

    // now we stop the connector
    Await
      .result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).stop(connector.id),
              10 seconds)
      .state shouldBe None

    // and then it is ok to delete pipeline
    Await.result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).delete(pipeline.id),
                 10 seconds)

    // let check the existence of topic
    Await
      .result(TopicApi.access().hostname(configurator.hostname).port(configurator.port).list(), 10 seconds)
      .size shouldBe 1

    // let check the existence of connector
    Await
      .result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).list(), 10 seconds)
      .size shouldBe 0
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
