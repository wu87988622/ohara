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

import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import spray.json._
class TestJsonRefiner extends OharaTest with Matchers {
  private[this] implicit val format: RootJsonFormat[SimpleData] = jsonFormat6(SimpleData)
  private[this] val format2: RootJsonFormat[SimpleData2] = jsonFormat2(SimpleData2)

  @Test
  def nullFormat(): Unit = an[NullPointerException] should be thrownBy JsonRefiner[SimpleData].format(null)

  @Test
  def emptyConnectionPort(): Unit =
    an[IllegalArgumentException] should be thrownBy JsonRefiner[SimpleData].requireConnectionPort("")

  @Test
  def emptyToNullToEmptyArray(): Unit =
    an[IllegalArgumentException] should be thrownBy JsonRefiner[SimpleData].nullToEmptyArray("")

  @Test
  def emptyToNullToRandomPort(): Unit =
    an[IllegalArgumentException] should be thrownBy JsonRefiner[SimpleData].nullToRandomPort("")

  @Test
  def emptyToNullToRandomString(): Unit =
    an[IllegalArgumentException] should be thrownBy JsonRefiner[SimpleData].nullToRandomString("")

  @Test
  def testDuplicateKeyForFromAnotherKey(): Unit = {
    val actions: Seq[JsonRefiner[SimpleData] => Unit] = Seq(
      _.nullToAnotherValueOfKey("a", "b")
    )
    actions.foreach { action0 =>
      actions.foreach { action1 =>
        val refiner = JsonRefiner[SimpleData].format(format)
        action0(refiner)
        an[IllegalArgumentException] should be thrownBy action1(refiner)
      }
    }
  }

  @Test
  def testDuplicateKeyForDefaultValue(): Unit = {
    val actions: Seq[JsonRefiner[SimpleData] => Unit] = Seq(
      _.nullToRandomPort("a"),
      _.nullToShort("a", 1),
      _.nullToInt("a", 1),
      _.nullToLong("a", 1),
      _.nullToDouble("a", 1),
      _.nullToEmptyArray("a"),
      _.nullToRandomString("a"),
      _.nullToString("a", "ccc")
    )
    actions.foreach { action0 =>
      actions.foreach { action1 =>
        val refiner = JsonRefiner[SimpleData].format(format)
        action0(refiner)
        an[IllegalArgumentException] should be thrownBy action1(refiner)
      }
    }
  }

  @Test
  def testDuplicateKeyForChecker(): Unit = {
    val actions: Seq[JsonRefiner[SimpleData] => Unit] = Seq(
      _.requireBindPort("a"),
      _.requireConnectionPort("a")
    )
    actions.foreach { action0 =>
      actions.foreach { action1 =>
        val refiner = JsonRefiner[SimpleData].format(format)
        action0(refiner)
        an[IllegalArgumentException] should be thrownBy action1(refiner)
      }
    }
  }

  @Test
  def testRejectEmptyString(): Unit = an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
    .format(format)
    .rejectEmptyString()
    .refine
    .read("""
            |{
            | "stringValue": "",
            | "bindPort": 123,
            | "connectionPort": 12144,
            | "stringArray": ["aa"],
            | "objects":{}
            |}
          """.stripMargin.parseJson)

  @Test
  def testWithoutRejectEmptyString(): Unit =
    JsonRefiner[SimpleData].format(format).refine.read("""
            |{
            | "stringValue": "",
            | "group": "default",
            | "bindPort": 123,
            | "connectionPort": 12345,
            | "stringArray": ["aa"],
            | "objects":{}
            |}
          """.stripMargin.parseJson).stringValue shouldBe ""

  @Test
  def testConnectionPort(): Unit =
    JsonRefiner[SimpleData].format(format).requireConnectionPort("connectionPort").refine.read("""
              |{
              | "stringValue": "abc",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 77,
              | "stringArray": ["aa"],
              | "objects":{}
              |}
            """.stripMargin.parseJson).connectionPort shouldBe 77

  @Test
  def testNullConnectionPort(): Unit = an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
    .format(format)
    .requireConnectionPort("connectionPort")
    .refine
    .read("""
            |{
            | "stringValue": "abc",
            | "bindPort": 123,
            | "connectionPort": null,
            | "stringArray": ["aa"],
            | "objects":{}
            |}
          """.stripMargin.parseJson)

  @Test
  def testIgnoreConnectionPort(): Unit = an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
    .format(format)
    .requireConnectionPort("connectionPort")
    .refine
    .read("""
            |{
            | "stringValue": "abc",
            | "bindPort": 123,
            | "stringArray": ["aa"],
            | "objects":{}
            |}
          """.stripMargin.parseJson)

  @Test
  def testNegativeConnectionPort(): Unit = testIllegalConnectionPort(-1)

  @Test
  def testZeroConnectionPort(): Unit = testIllegalConnectionPort(0)

  @Test
  def testLargeConnectionPort(): Unit = testIllegalConnectionPort(1000000)

  private[this] def testIllegalConnectionPort(port: Int): Unit =
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .requireConnectionPort("connectionPort")
      .refine
      .read(s"""
              |{
              | "stringValue": "abc",
              | "bindPort": 123,
              | "connectionPort": $port,
              | "stringArray": ["aa"],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

  @Test
  def testNegativeConnectionPortWithoutCheck(): Unit =
    JsonRefiner[SimpleData].format(format).refine.read("""
              |{
              | "stringValue": "abc",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": -1,
              | "stringArray": ["aa"],
              | "objects":{}
              |}
            """.stripMargin.parseJson).connectionPort shouldBe -1

  @Test
  def testBindPort(): Unit =
    JsonRefiner[SimpleData].format(format).nullToRandomPort("bindPort").refine.read("""
          |{
          | "stringValue": "abc",
          | "group": "default",
          | "bindPort": 11111,
          | "connectionPort": 77,
          | "stringArray": ["aa"],
          | "objects":{}
          |}
        """.stripMargin.parseJson).bindPort shouldBe 11111

  @Test
  def testNullBindPort(): Unit =
    JsonRefiner[SimpleData].format(format).nullToRandomPort("bindPort").refine.read("""
        |{
        | "stringValue": "abc",
        | "group": "default",
        | "bindPort": null,
        | "connectionPort": 77,
        | "stringArray": ["aa"],
        | "objects":{}
        |}
      """.stripMargin.parseJson).bindPort should not be 0

  @Test
  def testIgnoreBindPort(): Unit =
    JsonRefiner[SimpleData].format(format).nullToRandomPort("bindPort").refine.read("""
      |{
      | "stringValue": "abc",
      | "group": "default",
      | "connectionPort": 77,
      | "stringArray": ["aa"],
      | "objects":{}
      |}
    """.stripMargin.parseJson).bindPort should not be 0

  @Test
  def testNegativeBindPort(): Unit = testIllegalBindPort(-1)

  @Test
  def testPrivilegePort(): Unit = testIllegalBindPort(123)

  @Test
  def testLargeBindPort(): Unit = testIllegalBindPort(1000000)

  private[this] def testIllegalBindPort(port: Int): Unit =
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .requireBindPort("bindPort")
      .refine
      .read(s"""
               |{
               | "stringValue": "abc",
               | "bindPort": $port,
               | "connectionPort": 111,
               | "stringArray": ["aa"],
               | "objects":{}
               |}
            """.stripMargin.parseJson)

  @Test
  def testNegativeBindPortWithoutCheck(): Unit =
    JsonRefiner[SimpleData].format(format).refine.read("""
                     |{
                     | "stringValue": "abc",
                     | "group": "default",
                     | "bindPort": -1,
                     | "connectionPort": 123,
                     | "stringArray": ["aa"],
                     | "objects":{}
                     |}
                   """.stripMargin.parseJson).bindPort shouldBe -1

  @Test
  def testNullToRandomString(): Unit =
    JsonRefiner[SimpleData].format(format).nullToRandomString("stringValue").refine.read("""
                                                         |{
                                                         | "bindPort": -1,
                                                         | "group": "default",
                                                         | "connectionPort": 123,
                                                         | "stringArray": ["aa"],
                                                         | "objects":{}
                                                         |}
                                                       """.stripMargin.parseJson).stringValue.length should not be 0

  @Test
  def testNullToEmptyArray(): Unit =
    JsonRefiner[SimpleData].format(format).nullToEmptyArray("stringArray").refine.read("""
             |{
             | "stringValue": "abc",
             | "group": "default",
             | "bindPort": -1,
             | "connectionPort": 123,
             | "objects":{}
             |}
           """.stripMargin.parseJson).stringArray shouldBe Seq.empty

  @Test
  def defaultInt(): Unit =
    JsonRefiner[SimpleData].format(format).nullToInt("bindPort", 777).refine.read("""
         |{
         | "stringValue": "abc",
         | "group": "default",
         | "connectionPort": 123,
         | "stringArray": [],
         | "objects":{}
         |}
       """.stripMargin.parseJson).bindPort shouldBe 777

  @Test
  def testNullStringInDefaultToAnother(): Unit = {
    an[NullPointerException] should be thrownBy JsonRefiner[SimpleData]
      .nullToAnotherValueOfKey(null, CommonUtils.randomString())
    an[NullPointerException] should be thrownBy JsonRefiner[SimpleData]
      .nullToAnotherValueOfKey(CommonUtils.randomString(), null)
  }

  @Test
  def testEmptyStringInDefaultToAnother(): Unit = {
    an[IllegalArgumentException] should be thrownBy JsonRefiner[SimpleData]
      .nullToAnotherValueOfKey("", CommonUtils.randomString())
    an[IllegalArgumentException] should be thrownBy JsonRefiner[SimpleData]
      .nullToAnotherValueOfKey(CommonUtils.randomString(), "")
  }

  @Test
  def testDefaultToAnother(): Unit =
    JsonRefiner[SimpleData]
      .format(format)
      .nullToAnotherValueOfKey("bindPort", "connectionPort")
      .refine
      .read("""
        |{
        | "stringValue": "abc",
        | "group": "default",
        | "connectionPort": 123,
        | "stringArray": [],
        | "objects":{}
        |}
      """.stripMargin.parseJson)
      .bindPort shouldBe 123

  @Test
  def testNonexistentAnotherKeyForDefaultToAnother(): Unit =
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .nullToAnotherValueOfKey("bindPort", CommonUtils.randomString())
      .refine
      .read("""
      |{
      | "stringValue": "abc",
      | "connectionPort": 123,
      | "stringArray": [],
      | "objects":{}
      |}
    """.stripMargin.parseJson)

  /**
    * JsonRefiner doesn't another key if the origin key exists!!!
    */
  @Test
  def testNonexistentAnotherKeyButOriginKeyExistForDefaultToAnother(): Unit =
    JsonRefiner[SimpleData]
      .format(format)
      .nullToAnotherValueOfKey("bindPort", CommonUtils.randomString())
      .refine
      .read("""
              |{
              | "stringValue": "abc",
              | "group": "default",
              | "bindPort": 9999,
              | "connectionPort": 123,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)
      .bindPort shouldBe 9999

  @Test
  def testNegativeNumber(): Unit = an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
    .format(format)
    .rejectNegativeNumber()
    .refine
    .read("""
            |{
            | "stringValue": "abc",
            | "bindPort": -1,
            | "connectionPort": 123,
            | "stringArray": [],
            | "objects":{}
            |}
          """.stripMargin.parseJson)

  @Test
  def testNestedObjectForEmptyString(): Unit =
    JsonRefiner[SimpleData2].format(format2).rejectEmptyString().refine.read("""
            |{
            |  "data": {
            |    "stringValue": "abc",
            |    "group": "default",
            |    "bindPort": 22,
            |    "connectionPort": 123,
            |    "stringArray": [],
            |    "objects":{}
            |  },
            |  "data2": [
            |    {
            |      "stringValue": "abc",
            |      "group": "default",
            |      "bindPort": 22,
            |      "connectionPort": 123,
            |      "stringArray": [],
            |      "objects":{}
            |    }
            |  ]
            |
            |}
          """.stripMargin.parseJson)

  @Test
  def testNestedObjectForEmptyStringWithEmptyInFirstElement(): Unit =
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData2]
      .format(format2)
      .rejectEmptyString()
      .refine
      .read("""
            |{
            |  "data": {
            |    "stringValue": "",
            |    "group": "default",
            |    "bindPort": 22,
            |    "connectionPort": 123,
            |    "stringArray": [],
            |    "objects":{}
            |  },
            |  "data2": [
            |    {
            |      "stringValue": "abc",
            |      "bindPort": 22,
            |      "connectionPort": 123,
            |      "stringArray": [],
            |      "objects":{}
            |    }
            |  ]
            |
            |}
          """.stripMargin.parseJson)

  @Test
  def testNestedObjectForEmptyStringWithEmptyInSecondElement(): Unit =
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData2]
      .format(format2)
      .rejectEmptyString()
      .refine
      .read("""
            |{
            |  "data": {
            |    "stringValue": "aaa",
            |    "bindPort": 22,
            |    "connectionPort": 123,
            |    "stringArray": [],
            |    "objects":{}
            |  },
            |  "data2": [
            |    {
            |      "stringValue": "",
            |      "bindPort": 22,
            |      "connectionPort": 123,
            |      "stringArray": [],
            |      "objects":{}
            |    }
            |  ]
            |
            |}
          """.stripMargin.parseJson)

  @Test
  def testNestedObjectForNegativeNumberWithEmptyInFirstElement(): Unit =
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData2]
      .format(format2)
      .rejectNegativeNumber()
      .refine
      .read("""
              |{
              |  "data": {
              |    "stringValue": "aaa",
              |    "bindPort": -1,
              |    "connectionPort": 123,
              |    "stringArray": [],
              |    "objects":{}
              |  },
              |  "data2": [
              |    {
              |      "stringValue": "abc",
              |      "bindPort": 22,
              |      "connectionPort": 123,
              |      "stringArray": [],
              |      "objects":{}
              |    }
              |  ]
              |
              |}
            """.stripMargin.parseJson)

  @Test
  def testNestedObjectForNegativeNumberWithEmptyInSecondElement(): Unit =
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData2]
      .format(format2)
      .rejectNegativeNumber()
      .refine
      .read("""
              |{
              |  "data": {
              |    "stringValue": "aaa",
              |    "bindPort": 22,
              |    "connectionPort": 123,
              |    "stringArray": [],
              |    "objects":{}
              |  },
              |  "data2": [
              |    {
              |      "stringValue": "aaa",
              |      "bindPort": -1,
              |      "connectionPort": 123,
              |      "stringArray": [],
              |      "objects":{}
              |    }
              |  ]
              |
              |}
            """.stripMargin.parseJson)

  @Test
  def testRejectEmptyArray(): Unit = an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
    .format(format)
    .rejectEmptyArray()
    .refine
    .read("""
            |{
            | "stringValue": "abc",
            | "bindPort": 9999,
            | "connectionPort": 123,
            | "stringArray": [],
            | "objects":{}
            |}
          """.stripMargin.parseJson)

  @Test
  def testRejectKeyRuledByStringRestriction(): Unit =
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      // bindPort is not mapped to string type so it is rejected
      .stringRestriction("bindPort")
      .withNumber()
      .withCharset()
      .toRefiner
      .refine
      .read("""
              |{
              | "stringValue": "abc",
              | "bindPort": 9999,
              | "connectionPort": 123,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

  @Test
  def testRejectEmptyArrayForSpecificKey(): Unit = {
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      // connectionPort is not mapped to array type so it is rejected
      .rejectEmptyArray("connectionPort")
      .refine
      .read("""
              |{
              | "stringValue": "abc",
              | "bindPort": 9999,
              | "connectionPort": 123,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .rejectEmptyArray("stringArray")
      .refine
      .read("""
              |{
              | "stringValue": "abc",
              | "bindPort": 9999,
              | "connectionPort": 123,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)
  }

  @Test
  def testAcceptStringToNumber(): Unit = {
    val bindPort = CommonUtils.availablePort()
    val connectionPort = CommonUtils.availablePort()
    val data = JsonRefiner[SimpleData]
      .format(format)
      .acceptStringToNumber("bindPort")
      .acceptStringToNumber("connectionPort")
      .refine
      .read(s"""
              |{
              | "stringValue": "abc",
              | "group": "default",
              | "bindPort": "$bindPort",
              | "connectionPort": "$connectionPort",
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    data.bindPort shouldBe bindPort
    data.connectionPort shouldBe connectionPort
  }

  @Test
  def testParseStringForBindPOrt(): Unit = an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
    .format(format)
    .refine
    .read("""
            |{
            | "stringValue": "abc",
            | "bindPort": "123",
            | "connectionPort": 123,
            | "stringArray": [],
            | "objects":{}
            |}
          """.stripMargin.parseJson)

  @Test
  def testParseStringForConnectionPOrt(): Unit = an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
    .format(format)
    .refine
    .read("""
            |{
            | "stringValue": "abc",
            | "bindPort": 123,
            | "connectionPort": "123",
            | "stringArray": [],
            | "objects":{}
            |}
          """.stripMargin.parseJson)

  @Test
  def testRejectNegativeNumberInParsingString(): Unit =
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .acceptStringToNumber("connectionPort")
      .rejectNegativeNumber()
      .refine
      .read("""
            |{
            | "stringValue": "abc",
            | "bindPort": 123,
            | "connectionPort": "-1",
            | "stringArray": [],
            | "objects":{}
            |}
          """.stripMargin.parseJson)

  @Test
  def nullToAnotherValueOfKeyShouldBeBeforeNullToEmptyArray(): Unit =
    JsonRefiner[SimpleData]
      .format(format)
      .nullToAnotherValueOfKey("stringArray", "ttt")
      .nullToEmptyArray("stringArray")
      .refine
      .read("""
              |{
              | "stringValue": "abc",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "ttt": ["abc"],
              | "objects":{}
              |}
            """.stripMargin.parseJson)
      .stringArray shouldBe Seq("abc")

  @Test
  def emptyArrayInMappingToAnotherNonexistentKey(): Unit =
    JsonRefiner[SimpleData]
      .format(format)
      .nullToAnotherValueOfKey("stringArray", "ttt")
      .nullToEmptyArray("stringArray")
      .refine
      .read("""
              |{
              | "stringValue": "abc",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "objects":{}
              |}
            """.stripMargin.parseJson)
      .stringArray shouldBe Seq.empty

  @Test
  def testWithNumberOfStringRestriction(): Unit = {
    val refinedFormat =
      JsonRefiner[SimpleData].format(format).stringRestriction("stringValue").withNumber().toRefiner.refine

    an[DeserializationException] should be thrownBy refinedFormat.read("""
              |{
              | "stringValue": "abc",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    refinedFormat.read("""
              |{
              | "stringValue": "123",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)
  }

  @Test
  def testWithCharsetOfStringRestriction(): Unit = {
    val refinedFormat =
      JsonRefiner[SimpleData].format(format).stringRestriction("stringValue").withCharset().toRefiner.refine

    an[DeserializationException] should be thrownBy refinedFormat.read("""
              |{
              | "stringValue": "abc1",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    refinedFormat.read("""
              |{
              | "stringValue": "aaa",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)
  }

  @Test
  def testWithLowerCaseOfStringRestriction(): Unit = {
    val refinedFormat =
      JsonRefiner[SimpleData].format(format).stringRestriction("stringValue").withLowerCase().toRefiner.refine

    an[DeserializationException] should be thrownBy refinedFormat.read("""
              |{
              | "stringValue": "Abc",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    an[DeserializationException] should be thrownBy refinedFormat.read("""
              |{
              | "stringValue": "abc2",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    refinedFormat.read("""
              |{
              | "stringValue": "aaa",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)
  }

  @Test
  def testWithDashOfStringRestriction(): Unit = {
    val refinedFormat =
      JsonRefiner[SimpleData].format(format).stringRestriction("stringValue").withDash().toRefiner.refine

    an[DeserializationException] should be thrownBy refinedFormat.read("""
              |{
              | "stringValue": "Abc",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    refinedFormat.read("""
              |{
              | "stringValue": "-",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)
  }

  @Test
  def testWithDotOfStringRestriction(): Unit = {
    val refinedFormat =
      JsonRefiner[SimpleData].format(format).stringRestriction("stringValue").withDot().toRefiner.refine

    an[DeserializationException] should be thrownBy refinedFormat.read("""
              |{
              | "stringValue": "Abc",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    refinedFormat.read("""
              |{
              | "stringValue": ".",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)
  }

  @Test
  def testWithUnderLineOfStringRestriction(): Unit = {
    val refinedFormat =
      JsonRefiner[SimpleData].format(format).stringRestriction("stringValue").withUnderLine().toRefiner.refine

    an[DeserializationException] should be thrownBy refinedFormat.read("""
              |{
              | "stringValue": "aaa",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    refinedFormat.read("""
              |{
              | "stringValue": "_",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)
  }

  @Test
  def testComposeMultiplesRulesInStringRestriction(): Unit = {
    val refinedFormat = JsonRefiner[SimpleData]
      .format(format)
      .stringRestriction("stringValue")
      .withLowerCase()
      .withNumber()
      .toRefiner
      .refine

    an[DeserializationException] should be thrownBy refinedFormat.read("""
              |{
              | "stringValue": "ABC11",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    an[DeserializationException] should be thrownBy refinedFormat.read("""
                                                                  |{
                                                                  | "stringValue": "abc111-",
                                                                  | "group": "default",
                                                                  | "bindPort": 123,
                                                                  | "connectionPort": 111,
                                                                  | "stringArray": [],
                                                                  | "objects":{}
                                                                  |}
                                                                """.stripMargin.parseJson)

    refinedFormat.read("""
                  |{
                  | "stringValue": "abc111",
                  | "group": "default",
                  | "bindPort": 123,
                  | "connectionPort": 111,
                  | "stringArray": [],
                  | "objects":{}
                  |}
                """.stripMargin.parseJson)

    refinedFormat.read("""
                  |{
                  | "stringValue": "777abc111",
                  | "group": "default",
                  | "bindPort": 123,
                  | "connectionPort": 111,
                  | "stringArray": [],
                  | "objects":{}
                  |}
                """.stripMargin.parseJson)
  }

  @Test
  def testStringLength(): Unit = {
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .stringRestriction("stringValue")
      .withLengthLimit(3)
      .toRefiner
      .refine
      .read("""
              |{
              | "stringValue": "777abc111",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)

    // pass
    JsonRefiner[SimpleData]
      .format(format)
      .stringRestriction("stringValue")
      .withLengthLimit(100)
      .toRefiner
      .refine
      .read("""
              |{
              | "stringValue": "777abc111",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 111,
              | "stringArray": [],
              | "objects":{}
              |}
            """.stripMargin.parseJson)
  }

  @Test
  def emptyStringRestriction(): Unit =
    an[IllegalArgumentException] should be thrownBy JsonRefiner[SimpleData].stringRestriction("stringValue").toRefiner

  @Test
  def testInvalidString(): Unit = {
    val invalidStrings = Seq("a@", "a=", "a~", "a//")
    val refinedFormat = JsonRefiner[SimpleData]
      .format(format)
      .stringRestriction("stringValue")
      .withCharset()
      .withNumber()
      .withUnderLine()
      .withDot()
      .withDash()
      .toRefiner
      .refine
    invalidStrings.foreach { invalidString =>
      an[DeserializationException] should be thrownBy refinedFormat.read(s"""
                                                                              |{
                                                                              | "stringValue": "$invalidString",
                                                                              | "bindPort": 123,
                                                                              | "connectionPort": 111,
                                                                              | "stringArray": [],
                                                                              | "objects":{}
                                                                              |}
                           """.stripMargin.parseJson)
    }
  }

  @Test
  def testRejectEmptyStringForSpecificKey(): Unit = {
    // pass
    JsonRefiner[SimpleData].format(format).rejectEmptyString("aa").refine.read(s"""
                      |{
                      | "stringValue": "",
                      | "group": "default",
                      | "bindPort": 123,
                      | "connectionPort": 111,
                      | "stringArray": [],
                      | "objects":{}
                      |}
                           """.stripMargin.parseJson)

    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .rejectEmptyString("stringValue")
      .refine
      .read(s"""
                      |{
                      | "stringValue": "",
                      | "group": "default",
                      | "bindPort": 123,
                      | "connectionPort": 111,
                      | "stringArray": [],
                      | "objects":{}
                      |}
                           """.stripMargin.parseJson)
  }

  @Test
  def nullToEmptyObject(): Unit = JsonRefiner[SimpleData].format(format).nullToEmptyObject("objects").refine.read(s"""
             |{
             | "stringValue": "111",
             | "group": "default",
             | "bindPort": 123,
             | "connectionPort": 111,
             | "stringArray": []
             |}
           """.stripMargin.parseJson).objects shouldBe Map.empty

  @Test
  def testObjects(): Unit = JsonRefiner[SimpleData].format(format).refine.read(s"""
       |{
       | "stringValue": "111",
       | "group": "default",
       | "bindPort": 123,
       | "connectionPort": 111,
       | "stringArray": [],
       | "objects": {
       |   "a": "bb",
       |   "b": 123
       | }
       |}
           """.stripMargin.parseJson).objects shouldBe Map("a" -> JsString("bb"), "b" -> JsNumber(123))

  @Test
  def testRejectNegativeNumberForSpecificKey(): Unit = {
    val f = JsonRefiner[SimpleData].format(format).rejectNegativeNumber("bindPort").refine
    f.read(s"""
         |{
         | "stringValue": "111",
         | "group": "default",
         | "bindPort": 123,
         | "connectionPort": -1,
         | "stringArray": [],
         | "objects": {
         |   "a": "bb",
         |   "b": 123
         | }
         |}
           """.stripMargin.parseJson).connectionPort shouldBe -1
    an[DeserializationException] should be thrownBy f.read(s"""
         |{
         | "stringValue": "111",
         | "bindPort": -123,
         | "connectionPort": 123,
         | "stringArray": [],
         | "objects": {
         |   "a": "bb",
         |   "b": 123
         | }
         |}
           """.stripMargin.parseJson)
  }

  @Test
  def testRequirePositiveNumber(): Unit = {
    val f = JsonRefiner[SimpleData].format(format).requirePositiveNumber("bindPort").refine
    f.read(s"""
              |{
              | "stringValue": "111",
              | "group": "default",
              | "bindPort": 123,
              | "connectionPort": 100,
              | "stringArray": [],
              | "objects": {
              |   "a": "bb",
              |   "b": 123
              | }
              |}
           """.stripMargin.parseJson).bindPort shouldBe 123

    // negative number is illegal
    an[DeserializationException] should be thrownBy f.read(s"""
                                                              |{
                                                              | "stringValue": "111",
                                                              | "bindPort": -123,
                                                              | "connectionPort": 123,
                                                              | "stringArray": [],
                                                              | "objects": {
                                                              |   "a": "bb",
                                                              |   "b": 123
                                                              | }
                                                              |}
           """.stripMargin.parseJson)

    // zero is illegal
    an[DeserializationException] should be thrownBy f.read(s"""
                                                              |{
                                                              | "stringValue": "111",
                                                              | "bindPort": 0,
                                                              | "connectionPort": 123,
                                                              | "stringArray": [],
                                                              | "objects": {
                                                              |   "a": "bb",
                                                              |   "b": 123
                                                              | }
                                                              |}
           """.stripMargin.parseJson)
  }

  @Test
  def testKeywordsInArray(): Unit = {
    // pass
    JsonRefiner[SimpleData]
      .format(format)
      .arrayRestriction("stringArray")
      .rejectKeyword("start")
      .toRefiner
      .refine
      .read(s"""
                    |{
                    | "stringValue": "",
                    | "group": "default",
                    | "bindPort": 123,
                    | "connectionPort": 111,
                    | "stringArray": ["ss", "tt"],
                    | "objects":{}
                    |}
                           """.stripMargin.parseJson)

    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .arrayRestriction("stringArray")
      .rejectKeyword("stop")
      .toRefiner
      .refine
      .read(s"""
               |{
               | "stringValue": "start",
               | "bindPort": 123,
               | "connectionPort": 111,
               | "stringArray": ["stop", "abc"],
               | "objects":{}
               |}
                           """.stripMargin.parseJson)

    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .arrayRestriction("stringArray")
      .rejectKeyword("stop")
      .rejectEmpty()
      .toRefiner
      .refine
      .read(s"""
               |{
               | "stringValue": "start",
               | "bindPort": 123,
               | "connectionPort": 111,
               | "stringArray": [],
               | "objects":{}
               |}
                           """.stripMargin.parseJson)
  }

  @Test
  def testRejectKeyword(): Unit =
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .rejectKey("abc")
      .refine
      .read(s"""
               |{
               | "stringValue": "start",
               | "bindPort": 123,
               | "connectionPort": 111,
               | "stringArray": [],
               | "objects":{},
               | "abc":[]
               |}
                           """.stripMargin.parseJson)

  @Test
  def testRequireKeys(): Unit = {
    val data = SimpleData(
      stringValue = CommonUtils.randomString(),
      group = CommonUtils.randomString(),
      bindPort = CommonUtils.availablePort(),
      connectionPort = CommonUtils.availablePort(),
      stringArray = Seq.empty,
      objects = Map.empty
    )

    // make sure the normal format works well
    format.read(format.write(data))

    val f = JsonRefiner[SimpleData].format(format).requireKey("a").requireKey("b").refine
    an[DeserializationException] should be thrownBy f.read(format.write(data))

    an[DeserializationException] should be thrownBy f.read(
      JsObject(
        format.write(data).asJsObject.fields + ("a" -> JsString("bb"))
      ))

    f.read(
      JsObject(
        format.write(data).asJsObject.fields + ("a" -> JsString("bb")) + ("b" -> JsString("bb"))
      ))
  }

  @Test
  def testAllValuesCheck(): Unit = {
    val data = SimpleData(
      stringValue = CommonUtils.randomString(5),
      group = CommonUtils.randomString(10),
      bindPort = CommonUtils.availablePort(),
      connectionPort = CommonUtils.availablePort(),
      stringArray = Seq.empty,
      objects = Map.empty
    )

    val f = JsonRefiner[SimpleData].format(format).stringSumLengthLimit(Set("stringValue", "group"), 100).refine

    // pass
    f.read(format.write(data))
    // pass (only check the sum length of stringValue)
    JsonRefiner[SimpleData].format(format).stringSumLengthLimit(Set("stringValue"), 5).refine.read(format.write(data))
    // we don't support other types length checking
    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .stringSumLengthLimit(Set("group", "bindPort"), 5)
      .refine
      .read(format.write(data))

    // the sum length (stringValue, group) is bigger than 100
    an[DeserializationException] should be thrownBy f.read(
      format.write(data.copy(stringValue = CommonUtils.randomString(100))))

    // duplicate values should not be distinct
    val str = CommonUtils.randomString(100)
    an[DeserializationException] should be thrownBy f.read(format.write(data.copy(stringValue = str, group = str)))
  }

  @Test
  def testAllValuesCheckWithoutRequiredKeys(): Unit = {
    val data = SimpleData(
      stringValue = CommonUtils.randomString(5),
      group = CommonUtils.randomString(10),
      bindPort = CommonUtils.availablePort(),
      connectionPort = CommonUtils.availablePort(),
      stringArray = Seq.empty,
      objects = Map.empty
    )

    val f = JsonRefiner[SimpleData].format(format).stringSumLengthLimit(Set("abc"), 100).refine

    // the required keys should exist in data
    an[DeserializationException] should be thrownBy f.read(format.write(data))
  }

  @Test
  def testAllValuesCheckWithOtherCheckers(): Unit = {
    val data = SimpleData(
      stringValue = CommonUtils.randomString(5),
      group = CommonUtils.randomString(10),
      bindPort = CommonUtils.availablePort(),
      connectionPort = CommonUtils.availablePort(),
      stringArray = Seq.empty,
      objects = Map.empty
    )

    val f = JsonRefiner[SimpleData]
      .format(format)
      .rejectEmptyString("stringValue")
      .stringSumLengthLimit(Set("stringValue", "group"), 100)
      .refine

    // pass
    f.read(format.write(data))
    // stringValue could not be empty
    an[DeserializationException] should be thrownBy f.read(format.write(data.copy(stringValue = "")))
    // pass
    f.read(format.write(data.copy(group = "", stringValue = CommonUtils.randomString(100))))
    // the length of stringValue + group is bigger than 100
    an[DeserializationException] should be thrownBy f.read(
      format.write(data.copy(group = "aa", stringValue = CommonUtils.randomString(100))))
  }
}
