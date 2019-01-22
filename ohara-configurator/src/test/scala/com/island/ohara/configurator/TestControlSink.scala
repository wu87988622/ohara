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

import java.time.Duration

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfigurationRequest
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, TopicApi}
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.integration.WithBrokerWorker
import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkRecord, RowSinkTask, TaskConfig}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestControlSink extends WithBrokerWorker with Matchers {

  private[this] val configurator = Configurator
    .builder()
    .hostname("localhost")
    .port(0)
    .fake(testUtil.brokersConnProps, testUtil().workersConnProps())
    .build()

  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)
  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val topic = Await.result(TopicApi
                               .access()
                               .hostname(configurator.hostname)
                               .port(configurator.port)
                               .add(TopicCreationRequest(topicName, 1, 1)),
                             10 seconds)
    val request = ConnectorConfigurationRequest(name = methodName,
                                                className = classOf[DumbSink].getName,
                                                schema = Seq.empty,
                                                topics = Seq(topic.id),
                                                numberOfTasks = 1,
                                                configs = Map.empty)

    val sink = result(access.add(request))

    // test idempotent start
    (0 until 3).foreach(_ => Await.result(access.start(sink.id), 10 seconds).state.get shouldBe ConnectorState.RUNNING)
    val workerClient = WorkerClient(testUtil.workersConnProps)
    try {

      CommonUtil.await(() =>
                         try workerClient.exist(sink.id)
                         catch {
                           case _: Throwable => false
                       },
                       Duration.ofSeconds(30))
      CommonUtil
        .await(() => workerClient.status(sink.id).connector.state == ConnectorState.RUNNING, Duration.ofSeconds(20))
      result(access.get(sink.id)).state.get shouldBe ConnectorState.RUNNING

      // test idempotent pause
      (0 until 3).foreach(_ => Await.result(access.pause(sink.id), 10 seconds).state.get shouldBe ConnectorState.PAUSED)
      CommonUtil
        .await(() => workerClient.status(sink.id).connector.state == ConnectorState.PAUSED, Duration.ofSeconds(20))
      result(access.get(sink.id)).state.get shouldBe ConnectorState.PAUSED

      // test idempotent resume
      (0 until 3).foreach(_ =>
        Await.result(access.resume(sink.id), 10 seconds).state.get shouldBe ConnectorState.RUNNING)
      CommonUtil
        .await(() => workerClient.status(sink.id).connector.state == ConnectorState.RUNNING, Duration.ofSeconds(20))
      result(access.get(sink.id)).state.get shouldBe ConnectorState.RUNNING

      // test idempotent stop. the connector should be removed
      (0 until 3).foreach(_ => Await.result(access.stop(sink.id), 10 seconds))
      CommonUtil.await(() => workerClient.nonExist(sink.id), Duration.ofSeconds(20))
      result(access.get(sink.id)).state shouldBe None
    } finally {
      if (workerClient.exist(sink.id)) workerClient.delete(sink.id)
    }
  }

  @Test
  def testUpdateRunningSink(): Unit = {
    val topicName = methodName
    val topic = Await.result(TopicApi
                               .access()
                               .hostname(configurator.hostname)
                               .port(configurator.port)
                               .add(TopicCreationRequest(topicName, 1, 1)),
                             10 seconds)
    val request = ConnectorConfigurationRequest(name = methodName,
                                                className = classOf[DumbSink].getName,
                                                schema = Seq.empty,
                                                topics = Seq(topic.id),
                                                numberOfTasks = 1,
                                                configs = Map.empty)

    val sink = result(access.add(request))
    // test start
    Await.result(access.start(sink.id), 10 seconds)
    val workerClient = WorkerClient(testUtil.workersConnProps)
    try {
      CommonUtil.await(() =>
                         try workerClient.exist(sink.id)
                         catch {
                           case _: Throwable => false
                       },
                       Duration.ofSeconds(30))
      CommonUtil
        .await(() => workerClient.status(sink.id).connector.state == ConnectorState.RUNNING, Duration.ofSeconds(20))

      an[IllegalArgumentException] should be thrownBy result(access.update(sink.id, request.copy(numberOfTasks = 2)))
      an[IllegalArgumentException] should be thrownBy result(access.delete(sink.id))

      // test stop. the connector should be removed
      Await.result(access.stop(sink.id), 10 seconds)
      CommonUtil.await(() => workerClient.nonExist(sink.id), Duration.ofSeconds(20))
      result(access.get(sink.id)).state shouldBe None
    } finally {
      if (workerClient.exist(sink.id)) workerClient.delete(sink.id)
    }
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}

class DumbSink extends RowSinkConnector {
  private[this] var config: TaskConfig = _
  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[DumbSinkTask]
  override protected def _taskConfigs(maxTasks: Int): java.util.List[TaskConfig] = Seq.fill(maxTasks)(config).asJava
  override protected def _start(config: TaskConfig): Unit = {
    this.config = config
  }
  override protected def _stop(): Unit = {}
}

class DumbSinkTask extends RowSinkTask {
  override protected def _start(config: TaskConfig): Unit = {}

  override protected def _stop(): Unit = {}

  override protected def _put(records: java.util.List[RowSinkRecord]): Unit = {}
}
