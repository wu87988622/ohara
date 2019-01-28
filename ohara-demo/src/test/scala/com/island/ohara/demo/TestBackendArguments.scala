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
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers
@deprecated("embedded services are deprecated. We all should love docker, shouldn't we?", "0.2")
class TestBackendArguments extends SmallTest with Matchers {

  @Test
  def ftpPortRangeShouldWork(): Unit = {
    val dataPorts = 12345 to 12350
    val (_, ports) = Backend.parse(Array(Backend.FTP_DATA_PORT_KEY, s"${dataPorts.head}-${dataPorts.last}"))
    ports.ftpDataPorts shouldBe dataPorts
  }

  @Test
  def brokersPortRangeShouldWork(): Unit = {
    val dataPorts = 12345 to 12350
    val (_, ports) = Backend.parse(Array(Backend.BROKERS_PORT_KEY, s"${dataPorts.head}-${dataPorts.last}"))
    ports.brokersPort shouldBe dataPorts
  }

  @Test
  def workersPortRangeShouldWork(): Unit = {
    val dataPorts = 12345 to 12350
    val (_, ports) = Backend.parse(Array(Backend.WORKERS_PORT_KEY, s"${dataPorts.head}-${dataPorts.last}"))
    ports.workersPort shouldBe dataPorts
  }
}
