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
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
class TestTopicApi extends SmallTest with Matchers {

  @Test
  def emptyGroup(): Unit = an[IllegalArgumentException] should be thrownBy TopicApi.access.request.group("")

  @Test
  def nullGroup(): Unit = an[NullPointerException] should be thrownBy TopicApi.access.request.group(null)

  @Test
  def ignoreNameOnCreation(): Unit = TopicApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .creation
    .name
    .length should not be 0

  @Test
  def ignoreNameOnUpdate(): Unit = an[NullPointerException] should be thrownBy TopicApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .update()

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy TopicApi.access.request.name("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy TopicApi.access.request.name(null)

  @Test
  def emptyBrokerClusterName(): Unit =
    an[IllegalArgumentException] should be thrownBy TopicApi.access.request.brokerClusterName("")

  @Test
  def nullBrokerClusterName(): Unit =
    an[NullPointerException] should be thrownBy TopicApi.access.request.brokerClusterName(null)

  @Test
  def negativeNumberOfPartitions(): Unit =
    an[IllegalArgumentException] should be thrownBy TopicApi.access.request.numberOfPartitions(-1)

  @Test
  def negativeNumberOfReplications(): Unit =
    an[IllegalArgumentException] should be thrownBy TopicApi.access.request.numberOfReplications(-1)

  @Test
  def parseCreation(): Unit = {
    val brokerClusterName = CommonUtils.randomString()
    val numberOfPartitions = 100
    val numberOfReplications = 10

    val creation = TopicApi.TOPIC_CREATION_FORMAT.read(s"""
                                                           |{
                                                           |}
       """.stripMargin.parseJson)

    creation.name.length shouldBe 10
    creation.brokerClusterName shouldBe None
    creation.numberOfPartitions shouldBe TopicApi.DEFAULT_NUMBER_OF_PARTITIONS
    creation.numberOfReplications shouldBe TopicApi.DEFAULT_NUMBER_OF_REPLICATIONS
    creation.configs shouldBe Map.empty

    val name = CommonUtils.randomString()
    val key = CommonUtils.randomString()
    val value = CommonUtils.randomString()
    val creation2 = TopicApi.TOPIC_CREATION_FORMAT.read(s"""
         |{
         | "name": "$name",
         | "brokerClusterName": "$brokerClusterName",
         | "numberOfPartitions": $numberOfPartitions,
         | "numberOfReplications": $numberOfReplications,
         | "configs": {
         |   "$key": "$value"
         | }
         |}
       """.stripMargin.parseJson)

    creation2.name shouldBe name
    creation2.brokerClusterName.get shouldBe brokerClusterName
    creation2.numberOfPartitions shouldBe numberOfPartitions
    creation2.numberOfReplications shouldBe numberOfReplications
    creation2.configs shouldBe Map(key -> value)
  }

  @Test
  def parseJsonForUpdate(): Unit = {
    val name = CommonUtils.randomString()
    val brokerClusterName = CommonUtils.randomString()
    val numberOfPartitions = 100
    val numberOfReplications = 10
    val update = TopicApi.TOPIC_UPDATE_FORMAT.read(s"""
                                                                  |{
                                                                  | "name": "$name",
                                                                  | "brokerClusterName": "$brokerClusterName",
                                                                  | "numberOfPartitions": $numberOfPartitions,
                                                                  | "numberOfReplications": $numberOfReplications
                                                                  |}
       """.stripMargin.parseJson)

    update.brokerClusterName.get shouldBe brokerClusterName
    update.numberOfPartitions.get shouldBe numberOfPartitions
    update.numberOfReplications.get shouldBe numberOfReplications

    val update2 = TopicApi.TOPIC_UPDATE_FORMAT.read(s"""
         |{
         |}
       """.stripMargin.parseJson)

    update2.brokerClusterName shouldBe None
    update2.numberOfPartitions shouldBe None
    update2.numberOfReplications shouldBe None
  }

  @Test
  def nullTags(): Unit = an[NullPointerException] should be thrownBy TopicApi.access.request.tags(null)

  @Test
  def emptyTags(): Unit = TopicApi.access.request.tags(Map.empty)
}
