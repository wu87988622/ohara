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

import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorCreationRequest, ConnectorState}
import com.island.ohara.client.configurator.v0.PipelineApi.PipelineCreationRequest
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, PipelineApi, TopicApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class TestOhara1403 extends SmallTest with Matchers {

  private[this] val configurator =
    Configurator.builder().fake().build()
  @Test
  def test(): Unit = {
    val topic = Await.result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None)),
      30 seconds
    )

    val connector = Await.result(
      ConnectorApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          ConnectorCreationRequest(
            workerClusterName = None,
            className = Some(classOf[DumbSink].getName),
            columns = Seq.empty,
            topicNames = Seq(topic.id),
            numberOfTasks = Some(1),
            settings = Map.empty
          )),
      30 seconds
    )

    val pipeline = Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(name = methodName(),
                                  workerClusterName = None,
                                  rules = Map(topic.id -> Seq(connector.id)))),
      30 seconds
    )

    // start the connector
    Await
      .result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).start(connector.id),
              30 seconds)
      .state shouldBe Some(ConnectorState.RUNNING)

    // we can't delete a pipeline having a running connector

    an[IllegalArgumentException] should be thrownBy Await.result(
      PipelineApi.access().hostname(configurator.hostname).port(configurator.port).delete(pipeline.id),
      30 seconds)

    // now we stop the connector
    Await
      .result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).stop(connector.id),
              30 seconds)
      .state shouldBe None

    // and then it is ok to delete pipeline
    Await.result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).delete(pipeline.id),
                 30 seconds)

    // let check the existence of topic
    Await
      .result(TopicApi.access().hostname(configurator.hostname).port(configurator.port).list, 30 seconds)
      .size shouldBe 1

    // let check the existence of connector
    Await
      .result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).list, 30 seconds)
      .size shouldBe 0
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
