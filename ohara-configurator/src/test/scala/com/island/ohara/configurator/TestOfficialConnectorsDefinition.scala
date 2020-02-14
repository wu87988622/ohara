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

import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.WithDefinitions
import org.junit.Test

/**
  * the definitions of official connectors should define the "orderInGroup"
  */
class TestOfficialConnectorsDefinition extends OharaTest {
  @Test
  def testOrderInGroup(): Unit = {
    val illegalConnectors =
      ReflectionUtils.localConnectorDefinitions.filter(_.settingDefinitions.exists(_.orderInGroup() < 0))
    if (illegalConnectors.nonEmpty)
      throw new AssertionError(
        illegalConnectors
          .map(
            d =>
              s"the following definitions in ${d.className} have illegal orderInGroup. ${d.settingDefinitions
                .map(d => s"${d.key()} has orderInGroup:${d.orderInGroup()}")
                .mkString(",")}"
          )
          .mkString(",")
      )
  }

  private[this] def localConnectorDefinitions =
    ReflectionUtils.localConnectorDefinitions.filter(_.className != classOf[DumbSink].getName)

  @Test
  def testVersion(): Unit = {
    val illegalConnectors = localConnectorDefinitions
      .map(c => c.className -> c.settingDefinitions.find(_.key == WithDefinitions.VERSION_KEY).get.defaultString())
      .toMap
      .filter(_._2 == "unknown")
    if (illegalConnectors.nonEmpty)
      throw new AssertionError(
        illegalConnectors
          .map {
            case (className, version) => s"$className has illegal version:$version"
          }
          .mkString(",")
      )
  }

  @Test
  def testRevision(): Unit = {
    val illegalConnectors = localConnectorDefinitions
      .map(c => c.className -> c.settingDefinitions.find(_.key == WithDefinitions.VERSION_KEY).get.defaultString())
      .toMap
      .filter(_._2 == "unknown")
    if (illegalConnectors.nonEmpty)
      throw new AssertionError(
        illegalConnectors
          .map {
            case (className, version) => s"$className has illegal revision:$version"
          }
          .mkString(",")
      )
  }

  @Test
  def testAuthor(): Unit = {
    val illegalConnectors = localConnectorDefinitions
      .map(c => c.className -> c.settingDefinitions.find(_.key == WithDefinitions.AUTHOR_KEY).get.defaultString())
      .toMap
      .filter(_._2 == "unknown")
    if (illegalConnectors.nonEmpty)
      throw new AssertionError(
        illegalConnectors
          .map {
            case (className, version) => s"$className has illegal author:$version"
          }
          .mkString(",")
      )
  }
}
