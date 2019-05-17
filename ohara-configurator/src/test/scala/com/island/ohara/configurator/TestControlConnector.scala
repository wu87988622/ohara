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

import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorCreationRequest, ConnectorState}
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, TopicApi}
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers
import spray.json.JsString

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
class TestControlConnector extends WithBrokerWorker with Matchers {

  private[this] val configurator =
    Configurator.builder().fake(testUtil.brokersConnProps, testUtil().workersConnProps()).build()

  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testNormalCase(): Unit = {
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

    // test idempotent start
    (0 until 3).foreach(_ => Await.result(access.start(sink.id), 30 seconds).state should not be None)
    val workerClient = WorkerClient(testUtil.workersConnProps)
    try {
      CommonUtils.await(() =>
                          try if (result(workerClient.exist(sink.id))) true else false
                          catch {
                            case _: Throwable => false
                        },
                        Duration.ofSeconds(30))
      CommonUtils.await(() => result(workerClient.status(sink.id)).connector.state == ConnectorState.RUNNING,
                        Duration.ofSeconds(20))
      result(access.get(sink.id)).state.get shouldBe ConnectorState.RUNNING

      // test idempotent pause
      (0 until 3).foreach(_ => Await.result(access.pause(sink.id), 10 seconds).state.get shouldBe ConnectorState.PAUSED)
      CommonUtils.await(() => result(workerClient.status(sink.id)).connector.state == ConnectorState.PAUSED,
                        Duration.ofSeconds(20))
      result(access.get(sink.id)).state.get shouldBe ConnectorState.PAUSED

      // test idempotent resume
      (0 until 3).foreach(_ =>
        Await.result(access.resume(sink.id), 10 seconds).state.get shouldBe ConnectorState.RUNNING)
      CommonUtils.await(() => result(workerClient.status(sink.id)).connector.state == ConnectorState.RUNNING,
                        Duration.ofSeconds(20))
      result(access.get(sink.id)).state.get shouldBe ConnectorState.RUNNING

      // test idempotent stop. the connector should be removed
      (0 until 3).foreach(_ => Await.result(access.stop(sink.id), 10 seconds))
      CommonUtils.await(() => if (result(workerClient.nonExist(sink.id))) true else false, Duration.ofSeconds(20))
      result(access.get(sink.id)).state shouldBe None
    } finally {
      if (result(workerClient.exist(sink.id))) result(workerClient.delete(sink.id))
    }
  }

  @Test
  def testUpdateRunningConnector(): Unit = {
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
    // test start
    Await.result(access.start(sink.id), 10 seconds)
    val workerClient = WorkerClient(testUtil.workersConnProps)
    try {
      CommonUtils.await(() =>
                          try if (result(workerClient.exist(sink.id))) true else false
                          catch {
                            case _: Throwable => false
                        },
                        Duration.ofSeconds(30))
      CommonUtils.await(() => result(workerClient.status(sink.id)).connector.state == ConnectorState.RUNNING,
                        Duration.ofSeconds(20))

      an[IllegalArgumentException] should be thrownBy result(
        access.update(sink.id, request.copy(settings = Map("a" -> JsString("b")))))

      // test stop. the connector should be removed
      Await.result(access.stop(sink.id), 10 seconds)
      CommonUtils.await(() => if (result(workerClient.nonExist(sink.id))) true else false, Duration.ofSeconds(20))
      result(access.get(sink.id)).state shouldBe None
    } finally if (result(workerClient.exist(sink.id))) result(workerClient.delete(sink.id))
  }

  @Test
  def deleteRunningConnector(): Unit = {
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
    // test start
    Await.result(access.start(sink.id), 10 seconds)
    val workerClient = WorkerClient(testUtil.workersConnProps)
    try {
      CommonUtils.await(() =>
                          try if (result(workerClient.exist(sink.id))) true else false
                          catch {
                            case _: Throwable => false
                        },
                        Duration.ofSeconds(30))
      result(workerClient.delete(sink.id))
      result(workerClient.exist(sink.id)) shouldBe false
    } finally if (result(workerClient.exist(sink.id))) result(workerClient.delete(sink.id))
  }
  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
