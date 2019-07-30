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

package com.island.ohara.connector.ftp
import java.time.Duration

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorState
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.ConnectorKey
import com.island.ohara.testing.OharaTestUtils

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
object FtpUtils {
  private[this] val TIMEOUT = Duration.ofSeconds(60)
  def assertFailedConnector(testUtil: OharaTestUtils, connectorKey: ConnectorKey): Unit = CommonUtils.await(
    () => {
      val workerClient = WorkerClient(testUtil.workersConnProps)
      try Await.result(workerClient.status(connectorKey), 10 seconds).connector.state == ConnectorState.FAILED
      catch {
        case _: Throwable => false
      }
    },
    TIMEOUT
  )

  def checkConnector(testUtil: OharaTestUtils, connectorKey: ConnectorKey): Unit = {
    CommonUtils.await(
      () => {
        val workerClient = WorkerClient(testUtil.workersConnProps)
        Await.result(workerClient.activeConnectors(), 10 seconds).contains(connectorKey.connectorNameOnKafka())
      },
      TIMEOUT
    )
    CommonUtils.await(
      () => {
        val workerClient = WorkerClient(testUtil.workersConnProps)
        try Await.result(workerClient.status(connectorKey), 10 seconds).connector.state == ConnectorState.RUNNING
        catch {
          case _: Throwable => false
        }
      },
      TIMEOUT
    )
  }
}
