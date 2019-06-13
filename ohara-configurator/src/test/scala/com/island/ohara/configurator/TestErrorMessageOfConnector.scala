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

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorState
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
      connectorApi
        .request()
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicName(topic.name)
        .numberOfTasks(1)
        .setting("you_should_fail", "true")
        .create())

    result(connectorApi.start(connector.name))

    CommonUtils.await(() => result(connectorApi.get(connector.name)).state.isDefined, java.time.Duration.ofSeconds(10))

    result(connectorApi.get(connector.name)).state.get shouldBe ConnectorState.FAILED
    result(connectorApi.get(connector.name)).error.isDefined shouldBe true

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
              from = topic.name,
              to = Set(connector.name)
            )))
        .create())

    result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.name)).objects
      .filter(_.name == connector.name)
      .head
      .state
      .get shouldBe ConnectorState.FAILED.name

    result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.name)).objects
      .filter(_.name == connector.name)
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
      connectorApi
        .request()
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicName(topic.name)
        .numberOfTasks(1)
        .create())

    result(connectorApi.start(connector.name))

    CommonUtils.await(() => result(connectorApi.get(connector.name)).state.isDefined, java.time.Duration.ofSeconds(10))

    result(connectorApi.get(connector.name)).state.get shouldBe ConnectorState.RUNNING
    result(connectorApi.get(connector.name)).error.isEmpty shouldBe true

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
              from = topic.name,
              to = Set(connector.name)
            )))
        .create())

    result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.name)).objects
      .filter(_.name == connector.name)
      .head
      .state
      .get shouldBe ConnectorState.RUNNING.name

    result(PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.name)).objects
      .filter(_.name == connector.name)
      .head
      .error
      .isEmpty shouldBe true
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
