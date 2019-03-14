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

package com.island.ohara.client.kafka

import com.island.ohara.client.kafka.WorkerJson._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json._
class TestWorkerJson extends SmallTest with Matchers {

  @Test
  def testDefinition(): Unit = {
    val definition = Definition(
      name = CommonUtils.randomString(5),
      valueType = "String",
      required = true,
      valueDefault = None,
      documentation = CommonUtils.randomString(10)
    )
    DEFINITION_FORMAT.read(DEFINITION_FORMAT.write(definition)) shouldBe definition

    val definition2 = Definition(
      name = CommonUtils.randomString(5),
      valueType = "String",
      required = false,
      valueDefault = Some(CommonUtils.randomString(5)),
      documentation = CommonUtils.randomString(10)
    )
    DEFINITION_FORMAT.read(DEFINITION_FORMAT.write(definition2)) shouldBe definition2

  }

  @Test
  def testDefinitionFromString(): Unit = {
    val name = CommonUtils.randomString(5)
    val valueType = CommonUtils.randomString(5)
    val documentation = CommonUtils.randomString(5)
    val definition = DEFINITION_FORMAT.read(s"""
                                              |{
                                              |  "name":"$name",
                                              |  "type":"$valueType",
                                              |  "required": true,
                                              |  "default_value":"",
                                              |  "documentation":"$documentation"
                                              |}
                                            """.stripMargin.parseJson)
    definition.name shouldBe name
    definition.valueType shouldBe valueType
    definition.valueDefault shouldBe None
    definition.documentation shouldBe documentation
  }

  @Test
  def testDefinitionFromStringWithNullDefaultValue(): Unit = {
    val name = CommonUtils.randomString(5)
    val valueType = CommonUtils.randomString(5)
    val documentation = CommonUtils.randomString(5)
    val definition = DEFINITION_FORMAT.read(s"""
                                               |{
                                               |  "name":"$name",
                                               |  "type":"$valueType",
                                               |  "required": true,
                                               |  "default_value":null,
                                               |  "documentation":"$documentation"
                                               |}
                                            """.stripMargin.parseJson)
    definition.name shouldBe name
    definition.valueType shouldBe valueType
    definition.valueDefault shouldBe None
    definition.documentation shouldBe documentation
  }

  @Test
  def testDefinitionFromStringWithoutDefaultValue(): Unit = {
    val name = CommonUtils.randomString(5)
    val valueType = CommonUtils.randomString(5)
    val documentation = CommonUtils.randomString(5)
    val definition = DEFINITION_FORMAT.read(s"""
                                               |{
                                               |  "name":"$name",
                                               |  "type":"$valueType",
                                               |  "required": true,
                                               |  "documentation":"$documentation"
                                               |}
                                            """.stripMargin.parseJson)
    definition.name shouldBe name
    definition.valueType shouldBe valueType
    definition.valueDefault shouldBe None
    definition.documentation shouldBe documentation
  }

  @Test
  def testValidatedValue(): Unit = {
    val validatedValue = ValidatedValue(
      name = CommonUtils.randomString(5),
      value = Some("String"),
      errors = Seq(CommonUtils.randomString(5), CommonUtils.randomString(5))
    )
    VALIDATED_VALUE_FORMAT.read(VALIDATED_VALUE_FORMAT.write(validatedValue)) shouldBe validatedValue
  }

  @Test
  def testValidatedValueFromString(): Unit = {
    val name = CommonUtils.randomString(5)
    val value = CommonUtils.randomString(5)
    val error = CommonUtils.randomString(5)
    val validatedValue = VALIDATED_VALUE_FORMAT.read(s"""
                                               |{
                                               |  "name":"$name",
                                               |  "value":"$value",
                                               |  "errors":["$error", "$error"]
                                               |}
                                            """.stripMargin.parseJson)
    validatedValue.name shouldBe name
    validatedValue.value shouldBe Some(value)
    validatedValue.errors shouldBe Seq(error, error)
  }

  @Test
  def testValidatedValueFromStringWithoutValue(): Unit = {
    val name = CommonUtils.randomString(5)
    val error = CommonUtils.randomString(5)
    val validatedValue = VALIDATED_VALUE_FORMAT.read(s"""
                                                        |{
                                                        |  "name":"$name",
                                                        |  "errors":["$error", "$error"]
                                                        |}
                                            """.stripMargin.parseJson)
    validatedValue.name shouldBe name
    validatedValue.value shouldBe None
    validatedValue.errors shouldBe Seq(error, error)
  }

  @Test
  def testValidatedValueFromStringWithEmptyValue(): Unit = {
    val name = CommonUtils.randomString(5)
    val error = CommonUtils.randomString(5)
    val validatedValue = VALIDATED_VALUE_FORMAT.read(s"""
                                                        |{
                                                        |  "name":"$name",
                                                        |  "value":"",
                                                        |  "errors":["$error", "$error"]
                                                        |}
                                            """.stripMargin.parseJson)
    validatedValue.name shouldBe name
    validatedValue.value shouldBe None
    validatedValue.errors shouldBe Seq(error, error)
  }

  @Test
  def testValidatedValueFromStringWithNullValue(): Unit = {
    val name = CommonUtils.randomString(5)
    val error = CommonUtils.randomString(5)
    val validatedValue = VALIDATED_VALUE_FORMAT.read(s"""
                                                        |{
                                                        |  "name":"$name",
                                                        |  "value":null,
                                                        |  "errors":["$error", "$error"]
                                                        |}
                                            """.stripMargin.parseJson)
    validatedValue.name shouldBe name
    validatedValue.value shouldBe None
    validatedValue.errors shouldBe Seq(error, error)
  }
  @Test
  def testConfigValidation(): Unit = {
    val configNames = Seq(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val configValidation = ConfigValidationResponse(
      className = CommonUtils.randomString(5),
      definitions = configNames.map(
        name =>
          Definition(
            name = name,
            valueType = "String",
            required = true,
            valueDefault = None,
            documentation = CommonUtils.randomString(10)
        )),
      validatedValues = configNames.map(
        name =>
          ValidatedValue(
            name = name,
            value = Some("String"),
            errors = Seq(CommonUtils.randomString(5), CommonUtils.randomString(5))
        ))
    )
    val another = CONFIG_VALIDATED_RESPONSE_FORMAT.read(CONFIG_VALIDATED_RESPONSE_FORMAT.write(configValidation))
    another.className shouldBe configValidation.className
    another.definitions.size shouldBe configValidation.definitions.size
    another.definitions.foreach(d => d shouldBe configValidation.definitions.find(_.name == d.name).get)
    another.validatedValues.size shouldBe configValidation.validatedValues.size
    another.validatedValues.foreach(d => d shouldBe configValidation.validatedValues.find(_.name == d.name).get)
  }

  @Test
  def testConfigValidationWithUnmatchedSetting(): Unit = {
    val configNames = Seq(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val configValidation = ConfigValidationResponse(
      className = CommonUtils.randomString(5),
      definitions = configNames.map(
        name =>
          Definition(
            name = name,
            valueType = "String",
            required = true,
            valueDefault = None,
            documentation = CommonUtils.randomString(10)
        )),
      validatedValues = Seq(
        ValidatedValue(
          name = configNames.head,
          value = Some("String"),
          errors = Seq(CommonUtils.randomString(5), CommonUtils.randomString(5))
        ))
    )

    an[NoSuchElementException] should be thrownBy CONFIG_VALIDATED_RESPONSE_FORMAT.write(configValidation)
  }
}
