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

import java.io.File

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorCreationRequest
import com.island.ohara.client.configurator.v0.PipelineApi.{ObjectState, PipelineCreationRequest}
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, PipelineApi, StreamApi, TopicApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class TestPipelineRule extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  @Test
  def testPipelineStateAfterStartingSource(): Unit = {
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
    val sourceRequest = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some(classOf[DumbSink].getName),
      columns = Seq.empty,
      topicNames = Seq(topic.id),
      settings = Map.empty,
      numberOfTasks = Some(1)
    )

    val source = Await.result(access.add(sourceRequest), 30 seconds)
    val pipeline = Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            workerClusterName = None,
            rules = Map(source.id -> Seq(PipelineApi.UNKNOWN_ID))
          )
        ),
      30 seconds
    )
    pipeline.objects.foreach(obj => obj.state shouldBe None)

    // start source and pipeline should "see" what happen in source
    // we don't want to compare state since the state may be changed
    Await.result(access.start(source.id), 30 seconds).copy(state = None) shouldBe source.copy(state = None)
    val pipeline2 = Await.result(
      PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.id),
      30 seconds
    )
    pipeline2.objects.foreach(
      obj => obj.state.get shouldBe ObjectState.RUNNING
    )
  }

  @Test
  def testPipelineAllowObject(): Unit = {
    val pipeline = Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "testPipelineAllowData",
            workerClusterName = None,
            rules = Map.empty
          )
        ),
      30 seconds
    )

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

    val sourceRequest = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some("jdbc"),
      columns = Seq.empty,
      topicNames = Seq(topic.id),
      settings = Map.empty,
      numberOfTasks = Some(1)
    )
    val source = Await.result(access.add(sourceRequest), 30 seconds)

    val filePath = File.createTempFile("empty_", ".jar").getPath
    val streamapp = Await.result(
      StreamApi
        .accessOfList()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .upload(pipeline.id, Seq(filePath)),
      30 seconds
    )

    Await
      .result(
        PipelineApi
          .access()
          .hostname(configurator.hostname)
          .port(configurator.port)
          .update(
            pipeline.id,
            PipelineCreationRequest(
              name = "abc",
              workerClusterName = None,
              rules = Map(source.id -> Seq(topic.id))
            )
          ),
        30 seconds
      )
      .objects
      .size shouldBe 2
    Await
      .result(
        PipelineApi
          .access()
          .hostname(configurator.hostname)
          .port(configurator.port)
          .update(
            pipeline.id,
            PipelineCreationRequest(
              name = "abc",
              workerClusterName = None,
              rules = Map(topic.id -> Seq(streamapp.head.id))
            )
          ),
        30 seconds
      )
      .objects
      .size shouldBe 2
  }

  @Test
  def testUnknownObject(): Unit = {
    val sourceRequest = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some("jdbc"),
      columns = Seq.empty,
      topicNames = Seq.empty,
      settings = Map.empty,
      numberOfTasks = Some(1)
    )

    val source = Await.result(access.add(sourceRequest), 30 seconds)

    Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            workerClusterName = None,
            rules = Map(source.id -> Seq(PipelineApi.UNKNOWN_ID))
          )
        ),
      30 seconds
    )

    val sinkRequest = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some("jdbc"),
      columns = Seq.empty,
      topicNames = Seq.empty,
      settings = Map.empty,
      numberOfTasks = Some(1)
    )

    val sink = Await.result(access.add(sinkRequest), 30 seconds)

    Await.result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            workerClusterName = None,
            rules = Map(PipelineApi.UNKNOWN_ID -> Seq(sink.id))
          )
        ),
      30 seconds
    )
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
