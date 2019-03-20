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

package com.island.ohara.configurator.validation

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorCreationRequest
import com.island.ohara.client.configurator.v0.{ValidationApi, WorkerApi}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.{Configurator, DumbSink}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestValidationOfConnector extends With3Brokers3Workers with Matchers {
  private[this] val configurator =
    Configurator.builder().fake(testUtil().brokersConnProps(), testUtil().workersConnProps()).build()

  private[this] val wkCluster = result(WorkerApi.access().hostname(configurator.hostname).port(configurator.port).list).head

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def goodCase(): Unit = {
    val name = CommonUtils.randomString(10)
    val topicNames = Seq(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val response = result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(ConnectorCreationRequest(
          name = Some(name),
          className = Some(classOf[DumbSink].getName),
          topicNames = topicNames,
          numberOfTasks = Some(1),
          workerClusterName = Some(wkCluster.name),
          columns = Seq.empty,
          settings = Map.empty
        )))
    response.className.get() shouldBe classOf[DumbSink].getName
    response.settings().size() should not be 0
    response.numberOfTasks().get() shouldBe 1
    import scala.collection.JavaConverters._
    response.topicNames().asScala shouldBe topicNames
  }
  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
