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

import com.island.ohara.client.configurator.v0.ConnectorApi.State
import com.island.ohara.client.configurator.v0.{BrokerApi, ConnectorApi, TopicApi, WorkerApi}
import com.island.ohara.client.kafka.ConnectorAdmin
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestControlConnector extends WithBrokerWorker {
  private[this] val configurator =
    Configurator.builder.fake(testUtil.brokersConnProps, testUtil().workersConnProps()).build()

  private[this] val connectorApi = ConnectorApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val topicApi = TopicApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] def await(f: () => Boolean): Unit = CommonUtils.await(() => f(), java.time.Duration.ofSeconds(20))

  private[this] val workerClusterInfo = result(
    WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list()
  ).head

  @Test
  def testNormalCase(): Unit = {
    val topic = result(
      topicApi.request
        .name(CommonUtils.randomString(10))
        .brokerClusterKey(
          result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head.key
        )
        .create()
    )

    val sink = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .topicKey(topic.key)
        .workerClusterKey(workerClusterInfo.key)
        .create()
    )

    // test idempotent start
    result(topicApi.start(topic.key))
    (0 until 3).foreach(_ => result(connectorApi.start(sink.key)))

    result(connectorApi.get(sink.key)).state.get shouldBe State.RUNNING
    result(connectorApi.get(sink.key)).nodeName.get shouldBe CommonUtils.hostname()
    result(connectorApi.get(sink.key)).error shouldBe None

    val connectorAdmin = ConnectorAdmin(testUtil.workersConnProps)
    try {
      await(
        () =>
          try result(connectorAdmin.exist(sink.key))
          catch {
            case _: Throwable => false
          }
      )
      await(
        () =>
          try result(connectorAdmin.status(sink.key)).connector.state == State.RUNNING.name
          catch {
            case _: Throwable => false
          }
      )
      result(connectorApi.get(sink.key)).state.get shouldBe State.RUNNING

      // test idempotent pause
      (0 until 3).foreach(_ => result(connectorApi.pause(sink.key)))
      await(
        () =>
          try result(connectorAdmin.status(sink.key)).connector.state == State.PAUSED.name
          catch {
            case _: Throwable => false
          }
      )
      result(connectorApi.get(sink.key)).state.get shouldBe State.PAUSED

      // test idempotent resume
      (0 until 3).foreach(_ => result(connectorApi.resume(sink.key)))
      await(
        () =>
          try result(connectorAdmin.status(sink.key)).connector.state == State.RUNNING.name
          catch {
            case _: Throwable => false
          }
      )
      result(connectorApi.get(sink.key)).state.get shouldBe State.RUNNING

      // test idempotent stop. the connector should be removed
      (0 until 3).foreach(_ => result(connectorApi.stop(sink.key)))
      await(() => result(connectorAdmin.nonExist(sink.key)))
      await(() => result(connectorApi.get(sink.key)).state.nonEmpty)
    } finally if (result(connectorAdmin.exist(sink.key))) result(connectorAdmin.delete(sink.key))
  }

  @Test
  def testUpdateRunningConnector(): Unit = {
    val topicName = CommonUtils.randomString(10)
    val topic = result(
      topicApi.request
        .name(topicName)
        .brokerClusterKey(
          result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head.key
        )
        .create()
    )
    val sink = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(topic.key)
        .numberOfTasks(1)
        .workerClusterKey(workerClusterInfo.key)
        .create()
    )
    // test start
    result(topicApi.start(topic.key))
    result(connectorApi.start(sink.key))
    val connectorAdmin = ConnectorAdmin(testUtil.workersConnProps)
    try {
      await(
        () =>
          try result(connectorAdmin.exist(sink.key))
          catch {
            case _: Throwable => false
          }
      )
      await(
        () =>
          try result(connectorAdmin.status(sink.key)).connector.state == State.RUNNING.name
          catch {
            case _: Throwable => false
          }
      )

      an[IllegalArgumentException] should be thrownBy result(
        connectorApi.request
          .name(sink.name)
          .group(sink.group)
          .className(classOf[DumbSink].getName)
          .numberOfTasks(1)
          .topicKey(topic.key)
          .workerClusterKey(
            Await
              .result(WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list(), 20 seconds)
              .head
              .key
          )
          .create()
      )

      // test stop. the connector should be removed
      result(connectorApi.stop(sink.key))
      await(() => result(connectorAdmin.nonExist(sink.key)))
      await(() => result(connectorApi.get(sink.key)).state.isEmpty)
    } finally if (result(connectorAdmin.exist(sink.key))) result(connectorAdmin.delete(sink.key))
  }

  @Test
  def deleteRunningConnector(): Unit = {
    val topicName = CommonUtils.randomString(10)
    val topic = result(
      topicApi.request
        .name(topicName)
        .brokerClusterKey(
          result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head.key
        )
        .create()
    )
    val sink = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(topic.key)
        .numberOfTasks(1)
        .workerClusterKey(workerClusterInfo.key)
        .create()
    )
    // test start
    result(topicApi.start(topic.key))
    result(connectorApi.start(sink.key))
    val connectorAdmin = ConnectorAdmin(testUtil.workersConnProps)
    try {
      await(
        () =>
          try result(connectorAdmin.exist(sink.key))
          catch {
            case _: Throwable => false
          }
      )
      result(connectorAdmin.delete(sink.key))
      result(connectorAdmin.exist(sink.key)) shouldBe false
    } finally if (result(connectorAdmin.exist(sink.key))) result(connectorAdmin.delete(sink.key))
  }

  @Test
  def testNodeName(): Unit = {
    val topic = result(
      topicApi.request
        .name(CommonUtils.randomString(10))
        .brokerClusterKey(
          result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head.key
        )
        .create()
    )

    val sink = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .topicKey(topic.key)
        .workerClusterKey(workerClusterInfo.key)
        .create()
    )

    result(topicApi.start(topic.key))
    result(connectorApi.start(sink.key))
    await(() => result(connectorApi.get(sink.key)).state.nonEmpty)
    await(() => result(connectorApi.get(sink.key)).tasksStatus.nonEmpty)
    result(connectorApi.get(sink.key)).nodeName.get == CommonUtils.hostname()
    result(connectorApi.get(sink.key)).tasksStatus.foreach(_.nodeName shouldBe CommonUtils.hostname())
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
