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

package com.island.ohara.connector

import java.time.Duration

import com.island.ohara.client.configurator.v0.ConnectorApi.State
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.setting.ConnectorKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.testing.OharaTestUtils

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
object ConnectorTestUtils {
  private[this] val TIMEOUT = Duration.ofSeconds(60)

  def assertFailedConnector(testingUtil: OharaTestUtils, connectorKey: ConnectorKey): Unit =
    assertFailedConnector(testingUtil.workersConnProps(), connectorKey)

  def assertFailedConnector(workersConnProps: String, connectorKey: ConnectorKey): Unit = CommonUtils.await(
    () => {
      val client = WorkerClient(workersConnProps)
      try Await.result(client.status(connectorKey), 10 seconds).connector.state == State.FAILED.name
      catch {
        case _: Throwable => false
      }
    },
    TIMEOUT
  )

  def checkConnector(testingUtil: OharaTestUtils, connectorKey: ConnectorKey): Unit =
    checkConnector(testingUtil.workersConnProps(), connectorKey)

  def checkConnector(workersConnProps: String, connectorKey: ConnectorKey): Unit = {
    CommonUtils.await(
      () => {
        val workerClient = WorkerClient(workersConnProps)
        Await.result(workerClient.activeConnectors(), 10 seconds).contains(connectorKey.connectorNameOnKafka())
      },
      TIMEOUT
    )
    CommonUtils.await(
      () => {
        val workerClient = WorkerClient(workersConnProps)
        try Await.result(workerClient.status(connectorKey), 10 seconds).connector.state == State.RUNNING.name
        catch {
          case _: Throwable => false
        }
      },
      TIMEOUT
    )
  }
}
