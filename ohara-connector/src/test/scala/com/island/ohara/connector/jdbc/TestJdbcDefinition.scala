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

package com.island.ohara.connector.jdbc

import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.ConnectorUtils
import com.island.ohara.testing.WithBrokerWorker
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestJdbcDefinition extends WithBrokerWorker with Matchers {

  private[this] val workerClient = WorkerClient(testUtil().workersConnProps())

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testSource(): Unit = {
    val response = result(
      workerClient
        .connectorValidator()
        .name(CommonUtils.randomString(5))
        .numberOfTasks(1)
        .topicName(CommonUtils.randomString(5))
        .connectorClass(classOf[JDBCSourceConnector])
        .run())

    response.definitions.size should not be 0
    response.definitions.find(_.name == ConnectorUtils.TOPIC_NAMES_KEY).get.required shouldBe true
    response.definitions.find(_.name == ConnectorUtils.CONNECTOR_CLASS_KEY).get.required shouldBe true
    response.definitions.find(_.name == ConnectorUtils.NUMBER_OF_TASKS_KEY).get.required shouldBe false
    response.definitions.find(_.name == ConnectorUtils.COLUMNS_KEY).get.required shouldBe false
    response.validatedValues.size should not be 0
    response.validatedValues.foreach(_.errors.size shouldBe 0)
  }
}
