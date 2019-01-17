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

package com.island.ohara.configurator.call

import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorConfiguration, ConnectorConfigurationRequest}
import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.integration.With3Brokers
import com.island.ohara.kafka.KafkaUtil
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class TestCallQueueInClosingOnFlyRequests extends With3Brokers with Matchers {
  private[this] val requestData: ConnectorConfigurationRequest =
    ConnectorConfigurationRequest(name = "name",
                                  className = "jdbc",
                                  topics = Seq.empty,
                                  numberOfTasks = 1,
                                  schema = Seq(Column.of("cf", DataType.BOOLEAN, 1)),
                                  configs = Map("a" -> "b"))

  @Test
  def test(): Unit = {
    val requestCount = 10
    val requestTopic = newTopic()
    val responseTopic = newTopic()
    val invalidClient: CallQueueClient[ConnectorConfigurationRequest, ConnectorConfiguration] =
      CallQueue
        .clientBuilder()
        .brokers(testUtil.brokersConnProps)
        .requestTopic(requestTopic)
        .responseTopic(responseTopic)
        .build[ConnectorConfigurationRequest, ConnectorConfiguration]()
    val requests = try 0 until requestCount map { _ =>
      invalidClient.request(requestData)
    } finally invalidClient.close()
    requests.foreach(Await.result(_, 15 seconds) match {
      case Left(exception) => exception.message shouldBe CallQueue.TERMINATE_TIMEOUT_EXCEPTION.getMessage
      case _               => throw new RuntimeException("All requests should fail")
    })
  }

  private[this] def newTopic(): String = {
    val name = random()
    KafkaUtil.createTopic(testUtil.brokersConnProps, name, 1, 1)
    name
  }
}
