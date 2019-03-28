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

import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

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
    CommonUtils.requireNonEmpty(hostname)
    CommonUtils.requireNonEmpty(imageName)
    CommonUtils.requireNonEmpty(name)
    Objects.requireNonNull(ports)
    Objects.requireNonNull(envs)
    Objects.requireNonNull(route)
    Objects.requireNonNull(volumeMapping)
    Objects.requireNonNull(networkDriver)
  }

  @Test
  def nullHostname(): Unit = an[NullPointerException] should be thrownBy fake().hostname(null)

  @Test
  def emptyHostname(): Unit = an[IllegalArgumentException] should be thrownBy fake().hostname("")

  @Test
  def nullImageName(): Unit = an[NullPointerException] should be thrownBy fake().imageName(null)

  @Test
  def emptyImageName(): Unit = an[IllegalArgumentException] should be thrownBy fake().imageName("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy fake().name(null)

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy fake().name("")

  @Test
  def nullCommand(): Unit = an[NullPointerException] should be thrownBy fake().command(null)

  @Test
  def emptyCommand(): Unit = an[IllegalArgumentException] should be thrownBy fake().command("")

  @Test
  def nullPorts(): Unit = an[NullPointerException] should be thrownBy fake().portMappings(null)

  @Test
  def emptyPorts(): Unit = an[IllegalArgumentException] should be thrownBy fake().portMappings(Map.empty)

  @Test
  def nullEnvs(): Unit = an[NullPointerException] should be thrownBy fake().envs(null)

  @Test
  def emptyEnvs(): Unit = an[IllegalArgumentException] should be thrownBy fake().envs(Map.empty)

  @Test
  def nullRoute(): Unit = an[NullPointerException] should be thrownBy fake().route(null)

  @Test
  def emptyRoute(): Unit = an[IllegalArgumentException] should be thrownBy fake().route(Map.empty)

  @Test
  def nullVolumeMapping(): Unit = an[NullPointerException] should be thrownBy fake().volumeMapping(null)

  @Test
  def emptyVolumeMapping(): Unit = an[IllegalArgumentException] should be thrownBy fake().volumeMapping(Map.empty)

  @Test
  def nullNetworkDriver(): Unit = an[NullPointerException] should be thrownBy fake().command(null)

  @Test
  def testExecuteNormalCases(): Unit = {

    fake().imageName(CommonUtils.randomString(5)).execute()
    fake().name(CommonUtils.randomString()).imageName(CommonUtils.randomString(5)).execute()
    fake()
      .name(CommonUtils.randomString())
      .hostname(CommonUtils.randomString(5))
      .imageName(CommonUtils.randomString(5))
      .execute()
  }

  @Test
  def testExecuteWithoutRequireArguments(): Unit = {

    // At least assign imageName
    an[NullPointerException] should be thrownBy fake().execute()
  }
}
