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

import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestConfiguratorArgument extends SmallTest with Matchers {
  @Test
  def testErrorArgument(): Unit = {
    intercept[IllegalArgumentException] {
      Configurator.main(Array[String](Configurator.K8S_KEY, ""))
    }
    intercept[IllegalArgumentException] {
      Configurator.main(Array[String](Configurator.K8S_KEY))
    }
    intercept[IllegalArgumentException] {
      Configurator.main(
        Array[String](Configurator.K8S_KEY,
                      "http://ohara-it-02:8080/api/v1",
                      Configurator.NODE_KEY,
                      "ohara:111@ohara-it-100:22"))
    }
  }
}
