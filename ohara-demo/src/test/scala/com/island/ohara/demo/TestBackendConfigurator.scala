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

package com.island.ohara.demo

import com.island.ohara.client.configurator.v0.InfoApi
import com.island.ohara.common.rule.LargeTest
import com.island.ohara.demo.Backend.ServicePorts
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestBackendConfigurator extends LargeTest with Matchers {

  @Test
  def testConfigurator(): Unit = {
    Backend.run(
      ServicePorts.default,
      (configurator, _, _, _, _, _) => {
        // it should pass
        Await.result(InfoApi.access().hostname("localhost").port(configurator.port).get(), 10 seconds)
      }
    )
  }
}
