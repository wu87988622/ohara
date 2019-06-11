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
import com.island.ohara.client.configurator.v0.PipelineApi.Flow
import com.island.ohara.client.configurator.v0.{ConnectorApi, PipelineApi, TopicApi}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestErrorMessageOfConnector extends WithBrokerWorker with Matchers {
  private[this] val configurator =
    Configurator.builder().fake(testUtil.brokersConnProps, testUtil().workersConnProps()).build()

  private[this] val connectorApi = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def failToRun(): Unit = {
    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request()
        .name(CommonUtils.randomString(10))
        .create()
    )
    val connector = result(
      connectorApi.add(ConnectorCreationRequest(
        workerClusterName = None,
        className = Some(classOf[DumbSink].getName),
        columns = Seq.empty,
        topicNames = Seq(topic.id),
        numberOfTasks = Some(1),
        settings = Map("you_should_fail" -> "true")
      )))

    result(connectorApi.start(connector.id))

    CommonUtils.await(() => result(connectorApi.get(connector.id)).state.isDefined, java.time.Duration.ofSeconds(10))

    result(connectorApi.get(connector.id)).state.get shouldBe ConnectorState.FAILED
    result(connectorApi.get(connector.id)).error.isDefined shouldBe true

    // test state in pipeline
    val pipeline = result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request()
        .name(CommonUtils.randomString())
        .flows(
          Seq(
            Flow(
              from = topic.id,
              to = Set(connector.id)
            )))
        .create())

    result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.id)).objects
      .filter(_.id == connector.id)
      .head
      .state
      .get shouldBe ConnectorState.FAILED.name

    result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.id)).objects
      .filter(_.id == connector.id)
      .head
      .error
      .isDefined shouldBe true
  }

  @Test
  def succeedToRun(): Unit = {
    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request()
        .name(CommonUtils.randomString(10))
        .create()
    )
    val connector = result(
      connectorApi.add(
        ConnectorCreationRequest(
          workerClusterName = None,
          className = Some(classOf[DumbSink].getName),
          columns = Seq.empty,
          topicNames = Seq(topic.id),
          numberOfTasks = Some(1),
          settings = Map.empty
        )))

    result(connectorApi.start(connector.id))

    CommonUtils.await(() => result(connectorApi.get(connector.id)).state.isDefined, java.time.Duration.ofSeconds(10))

    result(connectorApi.get(connector.id)).state.get shouldBe ConnectorState.RUNNING
    result(connectorApi.get(connector.id)).error.isEmpty shouldBe true

    // test state in pipeline
    val pipeline = result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request()
        .name(CommonUtils.randomString())
        .flows(
          Seq(
            Flow(
              from = topic.id,
              to = Set(connector.id)
            )))
        .create())

    result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.id)).objects
      .filter(_.id == connector.id)
      .head
      .state
      .get shouldBe ConnectorState.RUNNING.name

    result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.id)).objects
      .filter(_.id == connector.id)
      .head
      .error
      .isEmpty shouldBe true
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
