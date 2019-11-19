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

import com.island.ohara.common.data.{Cell, Row}
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers._
import spray.json.{JsObject, _}

import scala.collection.JavaConverters._

class TestPackage extends OharaTest {
  @Test
  def testConversion(): Unit = {
    val s =
      """
        |  {
        |    "a": "b",
        |    "b": 123,
        |    "c": false,
        |    "d": null,
        |    "e": [
        |      "a",
        |      "c"
        |    ],
        |    "f": [
        |      {
        |        "f0": "v",
        |        "f1": 123,
        |        "tags": []
        |      }
        |    ],
        |    "g": {
        |      "a": "c",
        |      "d": 123,
        |      "dd": true,
        |      "tags": []
        |    },
        |    "tags": []
        |  }
        |""".stripMargin

    val json  = s.parseJson.asJsObject
    val row   = toRow(json)
    val json2 = toJson(row)
    JsObject(noJsNull(json.fields)) shouldBe json2
  }

  @Test
  def testTags(): Unit = {
    val tags = Seq(CommonUtils.randomString(), CommonUtils.randomString())
    val row  = Row.of(tags.asJava, Cell.of("a", "b"))
    val json = toJson(row)
    json.fields(TAGS_KEY).asInstanceOf[JsArray].elements.map(_.asInstanceOf[JsString].value) shouldBe tags

    val row2 = toRow(json)
    row2.tags().asScala shouldBe tags
  }
}
