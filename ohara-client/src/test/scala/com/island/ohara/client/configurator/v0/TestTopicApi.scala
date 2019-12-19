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

import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.{ObjectKey, SettingDef}
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
class TestTopicApi extends OharaTest {
  @Test
  def nullKeyInGet(): Unit =
    an[NullPointerException] should be thrownBy TopicApi.access.get(null)

  @Test
  def nullKeyInDelete(): Unit =
    an[NullPointerException] should be thrownBy TopicApi.access.delete(null)

  @Test
  def emptyGroup(): Unit = an[IllegalArgumentException] should be thrownBy TopicApi.access.request.group("")

  @Test
  def nullGroup(): Unit = an[NullPointerException] should be thrownBy TopicApi.access.request.group(null)

  @Test
  def brokerClusterKeyShouldBeRequired(): Unit =
    intercept[DeserializationException] {
      TopicApi.access.hostname(CommonUtils.randomString(10)).port(CommonUtils.availablePort()).request.creation
    }.getMessage should include(TopicApi.BROKER_CLUSTER_KEY_DEFINITION.key())

  @Test
  def ignoreNameOnCreation(): Unit =
    TopicApi.access
      .hostname(CommonUtils.randomString(10))
      .port(CommonUtils.availablePort())
      .request
      .brokerClusterKey(ObjectKey.of("fake", "fake"))
      .creation
      .name
      .length should not be 0

  @Test
  def ignoreNameOnUpdate(): Unit =
    an[NoSuchElementException] should be thrownBy TopicApi.access
      .hostname(CommonUtils.randomString(10))
      .port(CommonUtils.availablePort())
      .request
      .update()

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy TopicApi.access.request.name("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy TopicApi.access.request.name(null)

  @Test
  def nullBrokerClusterKey(): Unit =
    an[NullPointerException] should be thrownBy TopicApi.access.request.brokerClusterKey(null)

  @Test
  def negativeNumberOfPartitions(): Unit =
    an[IllegalArgumentException] should be thrownBy TopicApi.access.request.numberOfPartitions(-1)

  @Test
  def negativeNumberOfReplications(): Unit =
    an[IllegalArgumentException] should be thrownBy TopicApi.access.request.numberOfReplications(-1)

  @Test
  def parseCreation(): Unit = {
    val brokerClusterName    = CommonUtils.randomString(10)
    val numberOfPartitions   = 100
    val numberOfReplications = 10
    val group                = CommonUtils.randomString(10)
    val name                 = CommonUtils.randomString(10)
    val creation             = TopicApi.CREATION_FORMAT.read(s"""
         |{
         | "$GROUP_KEY": "$group",
         | "$NAME_KEY": "$name",
         | "${BROKER_CLUSTER_KEY_DEFINITION.key()}": "$brokerClusterName",
         | "${NUMBER_OF_PARTITIONS_DEFINITION.key()}": $numberOfPartitions,
         | "${NUMBER_OF_REPLICATIONS_DEFINITION.key()}": $numberOfReplications
         |}
       """.stripMargin.parseJson)

    creation.group shouldBe group
    creation.name shouldBe name
    creation.brokerClusterKey.name() shouldBe brokerClusterName
    creation.numberOfPartitions shouldBe numberOfPartitions
    creation.numberOfReplications shouldBe numberOfReplications
  }

  @Test
  def parseJsonForUpdate(): Unit = {
    val name                 = CommonUtils.randomString(10)
    val brokerClusterName    = CommonUtils.randomString(10)
    val numberOfPartitions   = 100
    val numberOfReplications = 10
    val update               = TopicApi.UPDATING_FORMAT.read(s"""
      |{
      | "$NAME_KEY": "$name",
      | "${BROKER_CLUSTER_KEY_DEFINITION.key()}": "$brokerClusterName",
      | "${NUMBER_OF_PARTITIONS_DEFINITION.key()}": $numberOfPartitions,
      | "${NUMBER_OF_REPLICATIONS_DEFINITION.key()}": $numberOfReplications
      |}
       """.stripMargin.parseJson)

    update.brokerClusterKey.get.name() shouldBe brokerClusterName
    update.settings(NUMBER_OF_PARTITIONS_DEFINITION.key()) shouldBe JsNumber(numberOfPartitions)
    update.settings(NUMBER_OF_REPLICATIONS_DEFINITION.key()) shouldBe JsNumber(numberOfReplications)

    val update2 = TopicApi.UPDATING_FORMAT.read(s"""
         |{
         |}
       """.stripMargin.parseJson)

    update2.brokerClusterKey shouldBe None
    update2.settings should not contain NUMBER_OF_PARTITIONS_DEFINITION.key()
    update2.settings should not contain NUMBER_OF_REPLICATIONS_DEFINITION.key()
  }

  @Test
  def nullTags(): Unit = an[NullPointerException] should be thrownBy TopicApi.access.request.tags(null)

  @Test
  def emptyTags(): Unit = TopicApi.access.request.tags(Map.empty)

  @Test
  def testNameLimit(): Unit =
    an[DeserializationException] should be thrownBy
      TopicApi.access
        .hostname(CommonUtils.randomString(10))
        .port(CommonUtils.availablePort())
        .request
        .name(CommonUtils.randomString(SettingDef.STRING_LENGTH_LIMIT))
        .group(CommonUtils.randomString(SettingDef.STRING_LENGTH_LIMIT))
        .creation

  @Test
  def negativeReplicationsIsIllegalInCreation(): Unit =
    intercept[DeserializationException] {
      TopicApi.CREATION_FORMAT.read(s"""
                                           |  {
                                           |    "${BROKER_CLUSTER_KEY_DEFINITION.key()}": {
                                           |      "group": "g",
                                           |      "name": "n"
                                           |    },
                                           |    "${NUMBER_OF_REPLICATIONS_DEFINITION.key()}": -1
                                           |  }
       """.stripMargin.parseJson)
    }.getMessage should include("the number must")

  @Test
  def negativePartitionsIsIllegalInCreation(): Unit =
    intercept[DeserializationException] {
      TopicApi.CREATION_FORMAT.read(s"""
                                           |  {
                                           |    "${BROKER_CLUSTER_KEY_DEFINITION.key()}": {
                                           |      "group": "g",
                                           |      "name": "n"
                                           |    },
                                           |    "${NUMBER_OF_PARTITIONS_DEFINITION.key()}": -1
                                           |  }
       """.stripMargin.parseJson)
    }.getMessage should include("the number must")

  @Test
  def zeroReplicationsIsIllegalInCreation(): Unit =
    intercept[DeserializationException] {
      TopicApi.CREATION_FORMAT.read(s"""
                                           |  {
                                           |    "${BROKER_CLUSTER_KEY_DEFINITION.key()}": {
                                           |      "group": "g",
                                           |      "name": "n"
                                           |    },
                                           |    "${NUMBER_OF_REPLICATIONS_DEFINITION.key()}": 0
                                           |  }
       """.stripMargin.parseJson)
    }.getMessage should include("the number must")

  @Test
  def zeroPartitionsIsIllegalInCreation(): Unit =
    intercept[DeserializationException] {
      TopicApi.CREATION_FORMAT.read(s"""
                                           |  {
                                           |    "${BROKER_CLUSTER_KEY_DEFINITION.key()}": {
                                           |      "group": "g",
                                           |      "name": "n"
                                           |    },
                                           |    "${NUMBER_OF_PARTITIONS_DEFINITION.key()}": 0
                                           |  }
       """.stripMargin.parseJson)
    }.getMessage should include("the number must")
}
