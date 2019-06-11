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
import com.island.ohara.client.configurator.v0.PipelineApi.Flow
import com.island.ohara.client.configurator.v0.{ConnectorApi, PipelineApi, TopicApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
class TestOhara450 extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  import scala.concurrent.duration._
  @Test
  def testUncreatablePipeline(): Unit = {
    val source = Await.result(
      access.add(
        ConnectorCreationRequest(
          workerClusterName = None,
          className = Some("aaa.class"),
          topicNames = Seq.empty,
          numberOfTasks = Some(1),
          columns = Seq.empty,
          settings = Map.empty
        )),
      30 seconds
    )
    Await.result(access.list, 30 seconds).size shouldBe 1
    import scala.concurrent.duration._
    val topic = Await.result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request()
        .name(CommonUtils.randomString(10))
        .create(),
      30 seconds
    )
    val sink = Await.result(
      access.add(
        ConnectorCreationRequest(
          workerClusterName = None,
          className = Some("aaa.class"),
          columns = Seq.empty,
          settings = Map.empty,
          topicNames = Seq.empty,
          numberOfTasks = Some(1)
        )),
      30 seconds
    )
    Await.result(access.list, 30 seconds).size shouldBe 2
    Await
      .result(TopicApi.access().hostname(configurator.hostname).port(configurator.port).list, 30 seconds)
      .size shouldBe 1

    Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request()
        .name(CommonUtils.randomString())
        .flows(
          Seq(
            Flow(source.id, Set(topic.name)),
            Flow(topic.name, Set(sink.id))
          ))
        .create(),
      30 seconds
    )
    Await
      .result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).list, 30 seconds)
      .size shouldBe 1
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
