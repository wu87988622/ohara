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

import com.island.ohara.client.configurator.v0.{ConnectorApi, PipelineApi, TopicApi}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.Producer
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestMetrics extends WithBrokerWorker with Matchers {

  private[this] val configurator =
    Configurator.builder.fake(testUtil.brokersConnProps, testUtil().workersConnProps()).build()

  private[this] val connectorApi = ConnectorApi.access.hostname(configurator.hostname).port(configurator.port)
  private[this] val topicApi = TopicApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 30 seconds)

  private[this] def assertNoMetricsForTopic(topicId: String): Unit = {
    CommonUtils.await(() => BeanChannel.local().topicMeters().asScala.count(_.topicName() == topicId) == 0,
                      java.time.Duration.ofSeconds(20))
  }

  @Test
  def testTopic(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString()).create())
    println(s"[CHIA] key:${topic.key}")
    val producer = Producer
      .builder[String, String]()
      .connectionProps(testUtil().brokersConnProps())
      .keySerializer(Serializer.STRING)
      .valueSerializer(Serializer.STRING)
      .build()
    try {
      producer
        .sender()
        .topicName(topic.key.topicNameOnKafka)
        .key(CommonUtils.randomString())
        .value(CommonUtils.randomString())
        .send()
        .get()
    } finally producer.close()

    CommonUtils.await(
      () => {
        val meters = result(topicApi.get(topic.name)).metrics.meters
        // metrics should have queryTime also
        meters.nonEmpty && meters.head.queryTime > 0
      },
      java.time.Duration.ofSeconds(20)
    )

    result(topicApi.delete(topic.name))

    assertNoMetricsForTopic(topic.name)
  }

  @Test
  def testConnector(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString()).create())

    val sink = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(topic.key)
        .numberOfTasks(1)
        .create())

    sink.metrics.meters.size shouldBe 0

    result(connectorApi.start(sink.name))

    CommonUtils.await(
      () => {
        val meters = result(connectorApi.get(sink.name)).metrics.meters
        // custom metrics should have queryTime and startTime also
        meters.nonEmpty && meters.head.queryTime > 0 && meters.head.startTime.isDefined
      },
      java.time.Duration.ofSeconds(20)
    )

    result(connectorApi.stop(sink.name))

    CommonUtils.await(() => {
      result(connectorApi.get(sink.name)).metrics.meters.isEmpty
    }, java.time.Duration.ofSeconds(20))
  }

  @Test
  def testPipeline(): Unit = {
    val topicName = methodName
    val topic = result(topicApi.request.name(topicName).create())

    val sink = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(topic.key)
        .numberOfTasks(1)
        .create())

    val pipelineApi = PipelineApi.access.hostname(configurator.hostname).port(configurator.port)

    val pipeline = result(pipelineApi.request.name(CommonUtils.randomString()).flow(topic.key, sink.key).create())

    pipeline.objects.filter(_.name == sink.name).head.metrics.meters.size shouldBe 0
    result(connectorApi.start(sink.name))

    // the connector is running so we should "see" the beans.
    CommonUtils.await(
      () => result(pipelineApi.get(pipeline.name)).objects.filter(_.name == sink.name).head.metrics.meters.nonEmpty,
      java.time.Duration.ofSeconds(20))

    result(connectorApi.stop(sink.name))

    // the connector is stopped so we should NOT "see" the beans.
    CommonUtils.await(
      () => result(pipelineApi.get(pipeline.name)).objects.filter(_.name == sink.name).head.metrics.meters.isEmpty,
      java.time.Duration.ofSeconds(20))
  }

  @Test
  def testTopicMeterInPerfSource(): Unit = {
    val topicName = CommonUtils.randomString()
    val topic = result(topicApi.request.name(topicName).create())

    val source = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className("com.island.ohara.connector.perf.PerfSource")
        .topicKey(topic.key)
        .numberOfTasks(1)
        .settings(Map("perf.batch" -> "1", "perf.frequence" -> java.time.Duration.ofSeconds(1).toString))
        .create())

    val pipelineApi = PipelineApi.access.hostname(configurator.hostname).port(configurator.port)

    val pipeline = result(pipelineApi.request.name(CommonUtils.randomString()).flow(topic.key, source.key).create())

    pipeline.objects.filter(_.name == source.name).head.metrics.meters.size shouldBe 0
    result(connectorApi.start(source.name))

    // the connector is running so we should "see" the beans.
    CommonUtils.await(
      () => result(pipelineApi.get(pipeline.name)).objects.filter(_.name == source.name).head.metrics.meters.nonEmpty,
      java.time.Duration.ofSeconds(20))

    CommonUtils.await(
      () => result(pipelineApi.get(pipeline.name)).objects.filter(_.name == topic.name).head.metrics.meters.nonEmpty,
      java.time.Duration.ofSeconds(20))

    result(connectorApi.stop(source.name))

    // the connector is stopped so we should NOT "see" the beans.
    CommonUtils.await(
      () => result(pipelineApi.get(pipeline.name)).objects.filter(_.name == source.name).head.metrics.meters.isEmpty,
      java.time.Duration.ofSeconds(20))

    // remove topic
    result(topicApi.delete(topic.name))
    CommonUtils.await(() => !result(pipelineApi.get(pipeline.name)).objects.exists(_.name == topic.name),
                      java.time.Duration.ofSeconds(30))
    assertNoMetricsForTopic(topic.name)
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
