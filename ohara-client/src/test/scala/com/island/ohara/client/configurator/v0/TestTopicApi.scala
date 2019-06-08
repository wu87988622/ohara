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

import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.JsString

import scala.concurrent.ExecutionContext.Implicits.global
class TestTopicApi extends SmallTest with Matchers {

  @Test
  def testId(): Unit = {
    val topicInfo = TopicInfo(
      name = CommonUtils.randomString(),
      brokerClusterName = CommonUtils.randomString(),
      numberOfPartitions = 1,
      numberOfReplications = 1,
      metrics = Metrics(Seq.empty),
      lastModified = CommonUtils.current()
    )
    topicInfo.id shouldBe topicInfo.name
  }

  @Test
  def testIdInJson(): Unit = {
    val name = CommonUtils.randomString()
    val topicInfo = TopicInfo(
      name = name,
      brokerClusterName = CommonUtils.randomString(),
      numberOfPartitions = 1,
      numberOfReplications = 1,
      metrics = Metrics(Seq.empty),
      lastModified = CommonUtils.current()
    )
    TopicApi.TOPIC_INFO_FORMAT.write(topicInfo).asJsObject.fields("id").asInstanceOf[JsString].value shouldBe name
  }

  @Test
  def ignoreNameOnCreation(): Unit = an[NullPointerException] should be thrownBy TopicApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .create()

  @Test
  def ignoreNameOnUpdate(): Unit = an[NullPointerException] should be thrownBy TopicApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .update()

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy TopicApi.access().request().name("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy TopicApi.access().request().name(null)

  @Test
  def emptyBrokerClusterName(): Unit =
    an[IllegalArgumentException] should be thrownBy TopicApi.access().request().brokerClusterName("")

  @Test
  def nullBrokerClusterName(): Unit =
    an[NullPointerException] should be thrownBy TopicApi.access().request().brokerClusterName(null)

  @Test
  def negativeNumberOfPartitions(): Unit =
    an[IllegalArgumentException] should be thrownBy TopicApi.access().request().numberOfPartitions(-1)

  @Test
  def negativeNumberOfReplications(): Unit =
    an[IllegalArgumentException] should be thrownBy TopicApi.access().request().numberOfReplications(-1)
}
