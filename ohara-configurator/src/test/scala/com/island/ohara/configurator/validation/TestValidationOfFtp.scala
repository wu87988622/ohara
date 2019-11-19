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

import com.island.ohara.client.configurator.v0.ValidationApi
import com.island.ohara.common.util.CommonUtils
import org.junit.Test

import scala.concurrent.ExecutionContext.Implicits.global

class TestValidationOfFtp extends WithConfigurator {
  private[this] val ftpServer = testUtil.ftpServer

  private[this] def request =
    ValidationApi.access.hostname(configuratorHostname).port(configuratorPort).ftpRequest

  @Test
  def goodCase(): Unit =
    assertSuccess(
      result(
        request
          .hostname(ftpServer.hostname)
          .port(ftpServer.port)
          .user(ftpServer.user)
          .password(ftpServer.password())
          .workerClusterKey(workerClusterKey)
          .verify()
      )
    )

  @Test
  def basCase(): Unit =
    assertFailure(
      result(
        request
          .hostname(ftpServer.hostname)
          .port(ftpServer.port)
          .user(ftpServer.user)
          .password(CommonUtils.randomString())
          .workerClusterKey(workerClusterKey)
          .verify()
      )
    )
}
