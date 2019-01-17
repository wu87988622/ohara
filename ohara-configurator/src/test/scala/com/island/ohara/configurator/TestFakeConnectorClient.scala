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
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestFakeConnectorClient extends SmallTest with Matchers {

  @Test
  def testControlConnector(): Unit = {
    val connectorName = methodName
    val topicName = methodName
    val className = methodName
    val fake = new FakeConnectorClient()
    fake.connectorCreator().name(connectorName).topic(topicName).numberOfTasks(1).connectorClass(className).create()

    fake.exist(connectorName) shouldBe true

    fake.status(connectorName).connector.state shouldBe ConnectorState.RUNNING

    fake.pause(connectorName)
    fake.status(connectorName).connector.state shouldBe ConnectorState.PAUSED

    fake.resume(connectorName)
    fake.status(connectorName).connector.state shouldBe ConnectorState.RUNNING

    fake.delete(connectorName)
    fake.exist(connectorName) shouldBe false
  }
}
