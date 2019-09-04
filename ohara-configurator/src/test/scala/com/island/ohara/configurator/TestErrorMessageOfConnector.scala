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
import com.island.ohara.client.configurator.v0.PipelineApi.Flow
import com.island.ohara.client.configurator.v0.{ConnectorApi, PipelineApi, TopicApi}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers
import spray.json.JsBoolean

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestErrorMessageOfConnector extends WithBrokerWorker with Matchers {
  private[this] val configurator =
    Configurator.builder.fake(testUtil.brokersConnProps, testUtil().workersConnProps()).build()

  private[this] val connectorApi = ConnectorApi.access.hostname(configurator.hostname).port(configurator.port)
  private[this] val topicApi = TopicApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def failToRun(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())
    result(topicApi.start(topic.key))
    val connector = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(topic.key)
        .numberOfTasks(1)
        .setting("you_should_fail", JsBoolean(true))
        .create())

    result(connectorApi.start(connector.key))

    CommonUtils.await(() => result(connectorApi.get(connector.key)).status.isDefined, java.time.Duration.ofSeconds(10))

    result(connectorApi.get(connector.key)).status.get.state shouldBe State.FAILED
    result(connectorApi.get(connector.key)).status.flatMap(_.error) should not be None

    // test state in pipeline
    val pipeline = result(
      PipelineApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString())
        .flows(
          Seq(
            Flow(
              from = topic.key,
              to = Set(connector.key)
            )))
        .create())

    result(PipelineApi.access.hostname(configurator.hostname).port(configurator.port).get(pipeline.key)).objects
      .filter(_.key == connector.key)
      .head
      .state
      .get shouldBe State.FAILED.name

    result(PipelineApi.access.hostname(configurator.hostname).port(configurator.port).get(pipeline.key)).objects
      .filter(_.key == connector.key)
      .head
      .error
      .isDefined shouldBe true
  }

  @Test
  def succeedToRun(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())
    result(topicApi.start(topic.key))

    val connector = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(topic.key)
        .numberOfTasks(1)
        .create())

    result(connectorApi.start(connector.key))

    CommonUtils.await(() => result(connectorApi.get(connector.key)).status.isDefined, java.time.Duration.ofSeconds(10))

    result(connectorApi.get(connector.key)).status.get.state shouldBe State.RUNNING
    result(connectorApi.get(connector.key)).status.flatMap(_.error) shouldBe None

    // test state in pipeline
    val pipeline = result(
      PipelineApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString())
        .flows(
          Seq(
            Flow(
              from = topic.key,
              to = Set(connector.key)
            )))
        .create())

    result(PipelineApi.access.hostname(configurator.hostname).port(configurator.port).get(pipeline.key)).objects
      .filter(_.key == connector.key)
      .head
      .state
      .get shouldBe State.RUNNING.name

    result(PipelineApi.access.hostname(configurator.hostname).port(configurator.port).get(pipeline.key)).objects
      .filter(_.key == connector.key)
      .head
      .error
      .isEmpty shouldBe true
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
