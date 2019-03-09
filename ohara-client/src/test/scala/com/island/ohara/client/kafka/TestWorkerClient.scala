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

package com.island.ohara.client.kafka

import com.island.ohara.common.data.{ConnectorState, Serializer}
import com.island.ohara.kafka.Consumer
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
class TestWorkerClient extends With3Brokers3Workers with Matchers {

  private[this] val workerClient = WorkerClient(testUtil().workersConnProps())
  @Test
  def testExist(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(workerClient.exist(connectorName)) shouldBe false

    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[MyConnector])
        .name(connectorName)
        .numberOfTasks(1)
        .disableConverter()
        .create())

    try assertExist(workerClient, connectorName)
    finally result(workerClient.delete(connectorName))
  }

  @Test
  def testExistOnUnrunnableConnector(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(workerClient.exist(connectorName)) shouldBe false

    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[BrokenConnector])
        .name(connectorName)
        .numberOfTasks(1)
        .disableConverter()
        .create())

    try assertExist(workerClient, connectorName)
    finally result(workerClient.delete(connectorName))
  }

  @Test
  def testPauseAndResumeSource(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[MyConnector])
        .name(connectorName)
        .numberOfTasks(1)
        .disableConverter()
        .create())
    try {
      assertExist(workerClient, connectorName)
      val consumer =
        Consumer
          .builder()
          .topicName(topicName)
          .offsetFromBegin()
          .connectionProps(testUtil.brokersConnProps)
          .build(Serializer.ROW, Serializer.BYTES)
      try {
        // try to receive some data from topic
        var rows = consumer.poll(java.time.Duration.ofSeconds(10), 1)
        rows.size should not be 0
        rows.asScala.foreach(_.key.get shouldBe ROW)
        // pause connector
        result(workerClient.pause(connectorName))

        await(() => result(workerClient.status(connectorName)).connector.state == ConnectorState.PAUSED)

        // try to receive all data from topic...10 seconds should be enough in this case
        rows = consumer.poll(java.time.Duration.ofSeconds(10), Int.MaxValue)
        rows.asScala.foreach(_.value.get shouldBe ROW)

        // connector is paused so there is no data
        rows = consumer.poll(java.time.Duration.ofSeconds(20), 1)
        rows.size shouldBe 0

        // resume connector
        result(workerClient.resume(connectorName))

        await(() => result(workerClient.status(connectorName)).connector.state == ConnectorState.RUNNING)

        // since connector is resumed so some data are generated
        rows = consumer.poll(java.time.Duration.ofSeconds(20), 1)
        rows.size should not be 0
      } finally consumer.close()
    } finally result(workerClient.delete(connectorName))
  }
}
