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

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorState
import com.island.ohara.client.configurator.v0.{ConnectorApi, TopicApi}
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestControlConnector extends WithBrokerWorker with Matchers {

  private[this] val configurator =
    Configurator.builder.fake(testUtil.brokersConnProps, testUtil().workersConnProps()).build()

  private[this] val connectorApi = ConnectorApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val topicApi = TopicApi.access.hostname(configurator.hostname).port(configurator.port)
  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val topic = result(topicApi.request.name(topicName).create())

    val sink = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .topicKey(topic.key)
        .create())

    // test idempotent start
    result(topicApi.start(topic.key))
    (0 until 3).foreach(_ => result(connectorApi.start(sink.key)))
    val workerClient = WorkerClient(testUtil.workersConnProps)
    try {
      CommonUtils.await(() =>
                          try if (result(workerClient.exist(sink.key))) true else false
                          catch {
                            case _: Throwable => false
                        },
                        Duration.ofSeconds(30))
      CommonUtils.await(() => result(workerClient.status(sink.key)).connector.state == ConnectorState.RUNNING,
                        Duration.ofSeconds(20))
      result(connectorApi.get(sink.key)).state.get shouldBe ConnectorState.RUNNING

      // test idempotent pause
      (0 until 3).foreach(_ => result(connectorApi.pause(sink.key)))
      CommonUtils.await(() => result(workerClient.status(sink.key)).connector.state == ConnectorState.PAUSED,
                        Duration.ofSeconds(20))
      result(connectorApi.get(sink.key)).state.get shouldBe ConnectorState.PAUSED

      // test idempotent resume
      (0 until 3).foreach(_ => result(connectorApi.resume(sink.key)))
      CommonUtils.await(() => result(workerClient.status(sink.key)).connector.state == ConnectorState.RUNNING,
                        Duration.ofSeconds(20))
      result(connectorApi.get(sink.key)).state.get shouldBe ConnectorState.RUNNING

      // test idempotent stop. the connector should be removed
      (0 until 3).foreach(_ => result(connectorApi.stop(sink.key)))
      CommonUtils.await(() => if (result(workerClient.nonExist(sink.key))) true else false, Duration.ofSeconds(20))
      result(connectorApi.get(sink.key)).state shouldBe None
    } finally {
      if (result(workerClient.exist(sink.key))) result(workerClient.delete(sink.key))
    }
  }

  @Test
  def testUpdateRunningConnector(): Unit = {
    val topicName = methodName
    val topic = result(topicApi.request.name(topicName).create())
    val sink = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(topic.key)
        .numberOfTasks(1)
        .create())
    // test start
    result(topicApi.start(topic.key))
    result(connectorApi.start(sink.key))
    val workerClient = WorkerClient(testUtil.workersConnProps)
    try {
      CommonUtils.await(() =>
                          try if (result(workerClient.exist(sink.key))) true else false
                          catch {
                            case _: Throwable => false
                        },
                        Duration.ofSeconds(30))
      CommonUtils.await(() => result(workerClient.status(sink.key)).connector.state == ConnectorState.RUNNING,
                        Duration.ofSeconds(20))

      an[IllegalArgumentException] should be thrownBy result(
        connectorApi.request
          .name(sink.name)
          .group(sink.group)
          .className(classOf[DumbSink].getName)
          .numberOfTasks(1)
          .create())

      // test stop. the connector should be removed
      result(connectorApi.stop(sink.key))
      CommonUtils.await(() => if (result(workerClient.nonExist(sink.key))) true else false, Duration.ofSeconds(20))
      result(connectorApi.get(sink.key)).state shouldBe None
    } finally if (result(workerClient.exist(sink.key))) result(workerClient.delete(sink.key))
  }

  @Test
  def deleteRunningConnector(): Unit = {
    val topicName = methodName
    val topic = result(topicApi.request.name(topicName).create())
    val sink = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(topic.key)
        .numberOfTasks(1)
        .create())
    // test start
    result(topicApi.start(topic.key))
    result(connectorApi.start(sink.key))
    val workerClient = WorkerClient(testUtil.workersConnProps)
    try {
      CommonUtils.await(() =>
                          try if (result(workerClient.exist(sink.key))) true else false
                          catch {
                            case _: Throwable => false
                        },
                        Duration.ofSeconds(30))
      result(workerClient.delete(sink.key))
      result(workerClient.exist(sink.key)) shouldBe false
    } finally if (result(workerClient.exist(sink.key))) result(workerClient.delete(sink.key))
  }
  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
