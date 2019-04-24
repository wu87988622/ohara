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
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, PipelineApi, TopicApi}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestMetrics extends WithBrokerWorker with Matchers {

  private[this] val configurator =
    Configurator.builder().fake(testUtil.brokersConnProps, testUtil().workersConnProps()).build()

  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testConnector(): Unit = {
    val topicName = methodName
    val topic = Await.result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(topicName),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None)),
      10 seconds
    )
    val request = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some(classOf[DumbSink].getName),
      columns = Seq.empty,
      topicNames = Seq(topic.id),
      numberOfTasks = Some(1),
      settings = Map.empty
    )

    val sink = result(access.add(request))

    sink.metrics.counters.size shouldBe 0

    result(access.start(sink.id))

    CommonUtils.await(() => {
      result(access.get(sink.id)).metrics.counters.nonEmpty
    }, java.time.Duration.ofSeconds(20))

    result(access.stop(sink.id))

    CommonUtils.await(() => {
      result(access.get(sink.id)).metrics.counters.isEmpty
    }, java.time.Duration.ofSeconds(20))
  }

  @Test
  def testPipeline(): Unit = {
    val topicName = methodName
    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(topicName),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )
    val request = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some(classOf[DumbSink].getName),
      columns = Seq.empty,
      topicNames = Seq(topic.id),
      numberOfTasks = Some(1),
      settings = Map.empty
    )

    val sink = result(access.add(request))

    val pipelineApi = PipelineApi.access().hostname(configurator.hostname).port(configurator.port)

    val pipeline = result(
      pipelineApi.add(
        PipelineApi.PipelineCreationRequest(
          name = CommonUtils.randomString(),
          workerClusterName = None,
          flows = Seq(
            Flow(
              from = topic.id,
              to = Seq(sink.id)
            ))
        )))
    pipeline.objects.filter(_.id == sink.id).head.metrics.counters.size shouldBe 0
    result(access.start(sink.id))

    // the connector is running so we should "see" the beans.
    CommonUtils.await(
      () => result(pipelineApi.get(pipeline.id)).objects.filter(_.id == sink.id).head.metrics.counters.nonEmpty,
      java.time.Duration.ofSeconds(20))

    result(access.stop(sink.id))

    // the connector is stopped so we should NOT "see" the beans.
    CommonUtils.await(
      () => result(pipelineApi.get(pipeline.id)).objects.filter(_.id == sink.id).head.metrics.counters.isEmpty,
      java.time.Duration.ofSeconds(20))
  }

  @Test
  def testTopicMeterInPerfSource(): Unit = {
    val topicName = CommonUtils.randomString()
    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(topicName),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )
    val request = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some("com.island.ohara.connector.perf.PerfSource"),
      columns = Seq.empty,
      topicNames = Seq(topic.id),
      numberOfTasks = Some(1),
      settings = Map(
        "perf.batch" -> "1",
        "perf.frequence" -> "1 second"
      )
    )

    val source = result(access.add(request))

    val pipelineApi = PipelineApi.access().hostname(configurator.hostname).port(configurator.port)

    val pipeline = result(
      pipelineApi.add(
        PipelineApi.PipelineCreationRequest(
          name = CommonUtils.randomString(),
          workerClusterName = None,
          flows = Seq(
            Flow(
              from = topic.id,
              to = Seq(source.id)
            ))
        )))
    pipeline.objects.filter(_.id == source.id).head.metrics.counters.size shouldBe 0
    result(access.start(source.id))

    // the connector is running so we should "see" the beans.
    CommonUtils.await(
      () => result(pipelineApi.get(pipeline.id)).objects.filter(_.id == source.id).head.metrics.counters.nonEmpty,
      java.time.Duration.ofSeconds(20))

    CommonUtils.await(
      () => result(pipelineApi.get(pipeline.id)).objects.filter(_.id == topic.id).head.metrics.counters.nonEmpty,
      java.time.Duration.ofSeconds(20))

    result(access.stop(source.id))

    // the connector is stopped so we should NOT "see" the beans.
    CommonUtils.await(
      () => result(pipelineApi.get(pipeline.id)).objects.filter(_.id == source.id).head.metrics.counters.isEmpty,
      java.time.Duration.ofSeconds(20))

    // remove topic
    result(
      TopicApi.access().hostname(configurator.hostname).port(configurator.port).delete(topic.id)
    )
    CommonUtils.await(() => !result(pipelineApi.get(pipeline.id)).objects.exists(_.id == topic.id),
                      java.time.Duration.ofSeconds(30))
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
