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

package com.island.ohara.agent

import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers
import spray.json.{JsArray, JsNumber, JsObject, JsString}

class TestEnvConversion extends SmallTest with Matchers {

  @Test
  def testConversion(): Unit = {
    val settings = Map("a" -> JsString("b"),
                       "b" -> JsNumber(123),
                       "c" -> JsArray(Vector(JsString("b"), JsString("c"))),
                       "d" -> JsObject(Map("a" -> JsString("b"), "b" -> JsNumber(123))))

    seekSettings(Map(toEnvString(settings))) shouldBe settings
  }

  @Test
  def emptyConversion(): Unit = an[NoSuchElementException] should be thrownBy seekSettings(Map.empty)
}
