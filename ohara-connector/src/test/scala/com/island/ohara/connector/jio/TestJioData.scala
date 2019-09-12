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

package com.island.ohara.connector.jio

import com.island.ohara.common.data.{Cell, Row}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.{DeserializationException, JsArray, JsString}

class TestJioData extends SmallTest with Matchers {

  @Test
  def rowToJioData(): Unit = {
    val row = Row.of(
      Cell.of(CommonUtils.randomString(5), false),
      Cell.of(CommonUtils.randomString(5), "b"),
      Cell.of(CommonUtils.randomString(5), 10.asInstanceOf[Short]),
      Cell.of(CommonUtils.randomString(5), 100),
      Cell.of(CommonUtils.randomString(5), 100.asInstanceOf[Long]),
      Cell.of(CommonUtils.randomString(5), 100.asInstanceOf[Float]),
      Cell.of(CommonUtils.randomString(5), 100.asInstanceOf[Double])
    )
    JioData(row).row shouldBe row
  }

  @Test
  def testUnsupportedTypesInRow(): Unit =
    an[IllegalArgumentException] should be thrownBy JioData(Row.of(Cell.of(CommonUtils.randomString(5), Map.empty)))

  @Test
  def testUnsupportedTypesInJson(): Unit =
    an[DeserializationException] should be thrownBy JioData(
      Map(
        "a" -> JsArray(Vector(JsString("b")))
      ))
}
