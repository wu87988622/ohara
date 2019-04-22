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
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.configurator.fake.FakeWorkerClient
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class TestFakeWorkerClient extends SmallTest with Matchers {

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)
  @Test
  def testControlConnector(): Unit = {
    val connectorName = methodName
    val topicName = methodName
    val className = methodName
    val fake = new FakeWorkerClient()
    result(fake.connectorCreator().id(connectorName).topicName(topicName).numberOfTasks(1).className(className).create)

    result(fake.exist(connectorName)) shouldBe true

    result(fake.status(connectorName)).connector.state shouldBe ConnectorState.RUNNING

    result(fake.pause(connectorName))
    result(fake.status(connectorName)).connector.state shouldBe ConnectorState.PAUSED

    result(fake.resume(connectorName))
    result(fake.status(connectorName)).connector.state shouldBe ConnectorState.RUNNING

    result(fake.delete(connectorName))
    result(fake.exist(connectorName)) shouldBe false
  }
}
