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

package com.island.ohara.configurator.route

import com.island.ohara.client.configurator.v0.{ValidationApi, WorkerApi}
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.TopicKey
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.{Configurator, FallibleSink}
import org.junit.{After, Test}
import org.scalatest.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
class TestValidationRoute extends OharaTest {
  private[this] val configurator = Configurator.builder.fake().build()

  private[this] val wkCluster = result(WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head

  private[this] def result[T](f: Future[T]): T = Await.result(f, Duration("20 seconds"))
  @Test
  def validateConnector(): Unit = {
    val className = classOf[FallibleSink].getName
    val response = result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .connectorRequest
        .name(CommonUtils.randomString(10))
        .className(className)
        .topicKey(TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5)))
        .workerClusterKey(wkCluster.key)
        .verify()
    )
    response.className.get() shouldBe className
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
