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
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtil, Releasable}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await

class TestOhara450 extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  import scala.concurrent.duration._
  @Test
  def testUncreatablePipeline(): Unit = {
    val source = Await.result(
      access.add(
        ConnectorCreationRequest(
          name = Some(CommonUtil.randomString(10)),
          workerClusterName = None,
          className = "aaa.class",
          topics = Seq.empty,
          numberOfTasks = 1,
          schema = Seq.empty,
          configs = Map.empty
        )),
      10 seconds
    )
    Await.result(access.list(), 10 seconds).size shouldBe 1
    import scala.concurrent.duration._
    val topic = Await.result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtil.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None)),
      10 seconds
    )
    val sink = Await.result(
      access.add(
        ConnectorCreationRequest(
          name = Some(CommonUtil.randomString(10)),
          workerClusterName = None,
          className = "aaa.class",
          schema = Seq.empty,
          configs = Map.empty,
          topics = Seq.empty,
          numberOfTasks = 1
        )),
      10 seconds
    )
    Await.result(access.list(), 10 seconds).size shouldBe 2
    Await
      .result(TopicApi.access().hostname(configurator.hostname).port(configurator.port).list(), 10 seconds)
      .size shouldBe 1

    Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            workerClusterName = None,
            rules = Map(source.id -> Seq(topic.id), topic.id -> Seq(sink.id))
          )),
      10 seconds
    )
    Await
      .result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).list(), 10 seconds)
      .size shouldBe 1
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
