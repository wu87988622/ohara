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

package com.island.ohara.agent.docker

import java.util.Objects

import com.island.ohara.agent.NetworkDriver
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

// TODO: check all setter .. by chia
class TestContainerCreator extends SmallTest with Matchers {

  private[this] def fake(): ContainerCreator = (hostname: String,
                                                imageName: String,
                                                name: String,
                                                command: String,
                                                removeContainerOnExit: Boolean,
                                                ports: Map[Int, Int],
                                                envs: Map[String, String],
                                                route: Map[String, String],
                                                volumeMapping: Map[String, String],
                                                networkDriver: NetworkDriver) => {
    // we check only the required arguments
    CommonUtils.requireNonEmpty(imageName)
    Objects.requireNonNull(ports)
    Objects.requireNonNull(envs)
    Objects.requireNonNull(route)
    Objects.requireNonNull(volumeMapping)
    Objects.requireNonNull(networkDriver)
  }

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy fake().name(null)

  @Test
  def testExecute(): Unit = fake().name(CommonUtils.randomString()).imageName(CommonUtils.randomString(5)).execute()
}
