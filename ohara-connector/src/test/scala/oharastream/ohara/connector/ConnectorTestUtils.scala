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

package oharastream.ohara.connector

import java.time.Duration

import oharastream.ohara.client.configurator.v0.ConnectorApi.State
import oharastream.ohara.client.kafka.ConnectorAdmin
import oharastream.ohara.common.setting.ConnectorKey
import oharastream.ohara.common.util.CommonUtils
import oharastream.ohara.testing.OharaTestUtils

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
object ConnectorTestUtils {
  private[this] val TIMEOUT = Duration.ofSeconds(60)

  def assertFailedConnector(testingUtil: OharaTestUtils, connectorKey: ConnectorKey): Unit =
    assertFailedConnector(testingUtil.workersConnProps(), connectorKey)

  def assertFailedConnector(workersConnProps: String, connectorKey: ConnectorKey): Unit = CommonUtils.await(
    () => {
      val client = ConnectorAdmin(workersConnProps)
      try Await.result(client.status(connectorKey), 10 seconds).connector.state == State.FAILED.name
      catch {
        case _: Throwable => false
      }
    },
    TIMEOUT
  )

  def checkConnector(testingUtil: OharaTestUtils, connectorKey: ConnectorKey): Unit =
    checkConnector(testingUtil.workersConnProps(), connectorKey)

  def checkConnector(workersConnProps: String, connectorKey: ConnectorKey): Unit =
    CommonUtils.await(
      () => {
        val connectorAdmin = ConnectorAdmin(workersConnProps)
        try {
          Await.result(connectorAdmin.activeConnectors(), 10 seconds).contains(connectorKey.connectorNameOnKafka())
          val status = Await.result(connectorAdmin.status(connectorKey), 10 seconds)
          status.connector.state == State.RUNNING.name && status.tasks.nonEmpty && status.tasks
            .forall(_.state == State.RUNNING.name)
        } catch {
          case _: Throwable => false
        }
      },
      TIMEOUT
    )
}
