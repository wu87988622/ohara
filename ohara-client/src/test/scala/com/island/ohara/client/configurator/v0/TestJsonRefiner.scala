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
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import spray.json._
class TestJsonRefiner extends SmallTest with Matchers {
  private[this] implicit val format: RootJsonFormat[SimpleData] = jsonFormat4(SimpleData)
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
            | "stringArray": ["aa"]
            |}
          """.stripMargin.parseJson)

  @Test
  def testWithoutRejectEmptyString(): Unit =
    JsonRefiner[SimpleData].format(format).refine.read("""
            |{
            | "stringValue": "",
            | "bindPort": 123,
            | "connectionPort": 12345,
            | "stringArray": ["aa"]
            |}
          """.stripMargin.parseJson).stringValue shouldBe ""

  @Test
  def testConnectionPort(): Unit =
    JsonRefiner[SimpleData].format(format).requireConnectionPort("connectionPort").refine.read("""
              |{
              | "stringValue": "abc",
              | "bindPort": 123,
              | "connectionPort": 77,
              | "stringArray": ["aa"]
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
            | "stringArray": ["aa"]
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
            | "stringArray": ["aa"]
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
              | "stringArray": ["aa"]
              |}
            """.stripMargin.parseJson)

  @Test
  def testNegativeConnectionPortWithoutCheck(): Unit =
    JsonRefiner[SimpleData].format(format).refine.read("""
              |{
              | "stringValue": "abc",
              | "bindPort": 123,
              | "connectionPort": -1,
              | "stringArray": ["aa"]
              |}
            """.stripMargin.parseJson).connectionPort shouldBe -1

  @Test
  def testBindPort(): Unit =
    JsonRefiner[SimpleData].format(format).nullToRandomPort("bindPort").refine.read("""
          |{
          | "stringValue": "abc",
          | "bindPort": 11111,
          | "connectionPort": 77,
          | "stringArray": ["aa"]
          |}
        """.stripMargin.parseJson).bindPort shouldBe 11111

  @Test
  def testNullBindPort(): Unit =
    JsonRefiner[SimpleData].format(format).nullToRandomPort("bindPort").refine.read("""
        |{
        | "stringValue": "abc",
        | "bindPort": null,
        | "connectionPort": 77,
        | "stringArray": ["aa"]
        |}
      """.stripMargin.parseJson).bindPort should not be 0

  @Test
  def testIgnoreBindPort(): Unit =
    JsonRefiner[SimpleData].format(format).nullToRandomPort("bindPort").refine.read("""
      |{
      | "stringValue": "abc",
      | "connectionPort": 77,
      | "stringArray": ["aa"]
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
               | "stringArray": ["aa"]
               |}
            """.stripMargin.parseJson)

  @Test
  def testNegativeBindPortWithoutCheck(): Unit =
    JsonRefiner[SimpleData].format(format).refine.read("""
                     |{
                     | "stringValue": "abc",
                     | "bindPort": -1,
                     | "connectionPort": 123,
                     | "stringArray": ["aa"]
                     |}
                   """.stripMargin.parseJson).bindPort shouldBe -1

  @Test
  def testNullToRandomString(): Unit =
    JsonRefiner[SimpleData].format(format).nullToRandomString("stringValue").refine.read("""
                                                         |{
                                                         | "bindPort": -1,
                                                         | "connectionPort": 123,
                                                         | "stringArray": ["aa"]
                                                         |}
                                                       """.stripMargin.parseJson).stringValue.length should not be 0

  @Test
  def testNullToEmptyArray(): Unit =
    JsonRefiner[SimpleData].format(format).nullToEmptyArray("stringArray").refine.read("""
             |{
             | "stringValue": "abc",
             | "bindPort": -1,
             | "connectionPort": 123
             |}
           """.stripMargin.parseJson).stringArray shouldBe Seq.empty

  @Test
  def defaultInt(): Unit =
    JsonRefiner[SimpleData].format(format).nullToInt("bindPort", 777).refine.read("""
         |{
         | "stringValue": "abc",
         | "connectionPort": 123,
         | "stringArray": []
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
        | "connectionPort": 123,
        | "stringArray": []
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
      | "stringArray": []
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
              | "bindPort": 9999,
              | "connectionPort": 123,
              | "stringArray": []
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
            | "stringArray": []
            |}
          """.stripMargin.parseJson)

  @Test
  def testNestedObjectForEmptyString(): Unit =
    JsonRefiner[SimpleData2].format(format2).rejectEmptyString().refine.read("""
            |{
            |  "data": {
            |    "stringValue": "abc",
            |    "bindPort": 22,
            |    "connectionPort": 123,
            |    "stringArray": []
            |  },
            |  "data2": [
            |    {
            |      "stringValue": "abc",
            |      "bindPort": 22,
            |      "connectionPort": 123,
            |      "stringArray": []
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
            |    "bindPort": 22,
            |    "connectionPort": 123,
            |    "stringArray": []
            |  },
            |  "data2": [
            |    {
            |      "stringValue": "abc",
            |      "bindPort": 22,
            |      "connectionPort": 123,
            |      "stringArray": []
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
            |    "stringArray": []
            |  },
            |  "data2": [
            |    {
            |      "stringValue": "",
            |      "bindPort": 22,
            |      "connectionPort": 123,
            |      "stringArray": []
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
              |    "stringArray": []
              |  },
              |  "data2": [
              |    {
              |      "stringValue": "abc",
              |      "bindPort": 22,
              |      "connectionPort": 123,
              |      "stringArray": []
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
              |    "stringArray": []
              |  },
              |  "data2": [
              |    {
              |      "stringValue": "aaa",
              |      "bindPort": -1,
              |      "connectionPort": 123,
              |      "stringArray": []
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
            | "stringArray": []
            |}
          """.stripMargin.parseJson)

  @Test
  def testRejectEmptyArrayForSpecificKey(): Unit = {
    JsonRefiner[SimpleData]
      .format(format)
      // the key "stringArray" is not in rejection list so it pass
      .rejectEmptyArray("connectionPort")
      .refine
      .read("""
              |{
              | "stringValue": "abc",
              | "bindPort": 9999,
              | "connectionPort": 123,
              | "stringArray": []
              |}
            """.stripMargin.parseJson)
      .stringArray shouldBe Seq.empty

    an[DeserializationException] should be thrownBy JsonRefiner[SimpleData]
      .format(format)
      .rejectEmptyArray("stringArray")
      .refine
      .read("""
              |{
              | "stringValue": "abc",
              | "bindPort": 9999,
              | "connectionPort": 123,
              | "stringArray": []
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
              | "bindPort": "$bindPort",
              | "connectionPort": "$connectionPort",
              | "stringArray": []
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
            | "stringArray": []
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
            | "stringArray": []
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
            | "stringArray": []
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
              | "bindPort": 123,
              | "connectionPort": 111,
              | "ttt": ["abc"]
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
              | "bindPort": 123,
              | "connectionPort": 111
              |}
            """.stripMargin.parseJson)
      .stringArray shouldBe Seq.empty
}
