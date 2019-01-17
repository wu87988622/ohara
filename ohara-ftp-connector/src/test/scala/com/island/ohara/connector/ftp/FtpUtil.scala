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

import com.island.ohara.client.WorkerClient
import com.island.ohara.common.data.ConnectorState
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.integration.OharaTestUtil
object FtpUtil {
  private[this] val TIMEOUT = Duration.ofSeconds(60)
  def assertFailedConnector(testUtil: OharaTestUtil, name: String): Unit = CommonUtil.await(
    () => {
      val workerClient = WorkerClient(testUtil.workersConnProps)
      try workerClient.status(name).connector.state == ConnectorState.FAILED
      catch {
        case _: Throwable => false
      } finally workerClient.close()
    },
    TIMEOUT
  )

  def checkConnector(testUtil: OharaTestUtil, name: String): Unit = {
    CommonUtil.await(
      () => {
        val workerClient = WorkerClient(testUtil.workersConnProps)
        try workerClient.activeConnectors().contains(name)
        finally workerClient.close()
      },
      TIMEOUT
    )
    CommonUtil.await(
      () => {
        val workerClient = WorkerClient(testUtil.workersConnProps)
        try workerClient.status(name).connector.state == ConnectorState.RUNNING
        catch {
          case _: Throwable => false
        } finally workerClient.close()
      },
      TIMEOUT
    )
  }
}
