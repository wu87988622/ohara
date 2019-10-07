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
import org.junit.Test

import scala.concurrent.ExecutionContext.Implicits.global

class TestValidationOfHdfs extends WithConfigurator {
  private[this] def request =
    ValidationApi.access.hostname(configuratorHostname).port(configuratorPort).hdfsRequest

  @Test
  def goodCase(): Unit =
    assertSuccess(
      result(request.uri("file:///tmp").workerClusterKey(workerClusterKey).verify())
    )

  @Test
  def badCase(): Unit = assertFailure(
    result(request.uri("hdfs:///tmp").workerClusterKey(workerClusterKey).verify())
  )
}
