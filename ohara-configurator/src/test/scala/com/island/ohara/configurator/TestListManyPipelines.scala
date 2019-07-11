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
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestListManyPipelines extends WithBrokerWorker with Matchers {

  private[this] val configurator =
    Configurator.builder.fake(testUtil().brokersConnProps(), testUtil().workersConnProps()).build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 20 seconds)

  private[this] val numberOfPipelines = 30
  @Test
  def test(): Unit = {
    val topic = result(
      TopicApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .create()
    )

    val connector = result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className("com.island.ohara.connector.perf.PerfSource")
        .topicName(topic.name)
        .numberOfTasks(1)
        .create())

    val pipelines = (0 until numberOfPipelines).map { _ =>
      result(
        PipelineApi.access
          .hostname(configurator.hostname)
          .port(configurator.port)
          .request
          .name(CommonUtils.randomString())
          .flow(connector.name, topic.name)
          .create()
      )
    }

    val listPipeline =
      Await.result(PipelineApi.access.hostname(configurator.hostname).port(configurator.port).list(), 20 seconds)
    pipelines.size shouldBe listPipeline.size
    pipelines.foreach { p =>
      listPipeline.exists(_.name == p.name) shouldBe true
    }
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
