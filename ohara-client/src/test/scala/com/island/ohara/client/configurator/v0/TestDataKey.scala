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

package com.island.ohara.client.configurator.v0

import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers
import spray.json._

class TestDataKey extends SmallTest with Matchers {

  @Test
  def parseJson(): Unit = DataKey.DATA_KEY_JSON_FORMAT.read(s"""
       |  {
       |    "${Data.GROUP_KEY}": "group",
       |    "${Data.NAME_KEY}": "name"
       |  }
    """.stripMargin.parseJson) shouldBe DataKey(
    group = "group",
    name = "name"
  )

  @Test
  def noGroup(): Unit = an[DeserializationException] should be thrownBy DataKey.DATA_KEY_JSON_FORMAT.read(s"""
      |  {
      |    "${Data.NAME_KEY}": "name"
      |  }
    """.stripMargin.parseJson)

  @Test
  def noName(): Unit = an[DeserializationException] should be thrownBy DataKey.DATA_KEY_JSON_FORMAT.read(s"""
       |  {
       |    "${Data.GROUP_KEY}": "group"
       |  }
    """.stripMargin.parseJson)
}
