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

package com.island.ohara.configurator.endpoint

import com.island.ohara.client.configurator.v0.ValidationApi.ConnectorValidationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, ValidationApi, WorkerApi}
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.configurator.{Configurator, DumbSink}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestValidationOfConnector extends With3Brokers3Workers with Matchers {
  private[this] val configurator =
    Configurator.builder().fake(testUtil().brokersConnProps(), testUtil().workersConnProps()).build()

  private[this] val wkCluster = result(
    WorkerApi.access().hostname(configurator.hostname).port(configurator.port).list()).head

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def goodCase(): Unit = {
    val name = CommonUtil.randomString(10)
    val topicNames = Seq(CommonUtil.randomString(10), CommonUtil.randomString(10))
    val response = result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(
          ConnectorValidationRequest(
            name = name,
            className = classOf[DumbSink].getName,
            topicNames = topicNames,
            numberOfTasks = 1,
            workerClusterName = wkCluster.name,
            configs = Map.empty
          )))
    response.className shouldBe classOf[DumbSink].getName
    response.definitions.size should not be 0
    response.validatedValues.size should not be 0
    response.validatedValues
      .find(_.name == ConnectorApi.CLASS_NAME_KEY)
      .get
      .value
      .get shouldBe classOf[DumbSink].getName
    response.validatedValues.find(_.name == ConnectorApi.NUMBER_OF_TASKS_KEY).get.value.get shouldBe "1"
    response.validatedValues.find(_.name == ConnectorApi.TOPIC_NAME_KEY).get.value.get shouldBe topicNames.mkString(",")
  }
  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
