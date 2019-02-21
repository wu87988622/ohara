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

package com.island.ohara.configurator.route

import com.island.ohara.client.configurator.v0.{BrokerApi, TopicApi}
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class TestTopicRoute extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake(1, 0).build()

  private[this] val topicApi = TopicApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def test(): Unit = {
    def compareRequestAndResponse(request: TopicCreationRequest, response: TopicInfo): TopicInfo = {
      request.name shouldBe response.name
      request.numberOfReplications.foreach(_ shouldBe response.numberOfReplications)
      request.numberOfPartitions.foreach(_ shouldBe response.numberOfPartitions)
      response
    }

    def compare2Response(lhs: TopicInfo, rhs: TopicInfo): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe rhs.name
      lhs.numberOfReplications shouldBe rhs.numberOfReplications
      lhs.numberOfPartitions shouldBe rhs.numberOfPartitions
      lhs.lastModified shouldBe rhs.lastModified
    }

    // test add
    result(topicApi.list()).size shouldBe 0
    val request = TopicApi.creationRequest(methodName)
    val response = compareRequestAndResponse(request, result(topicApi.add(request)))

    // test get
    compare2Response(response, result(topicApi.get(response.id)))

    // test update
    val anotherRequest = TopicApi.creationRequest(methodName)
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(topicApi.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(topicApi.get(newResponse.id)))

    // test delete
    result(topicApi.list()).size shouldBe 1
    result(topicApi.delete(response.id)) shouldBe newResponse
    result(topicApi.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(topicApi.get("123"))
    an[IllegalArgumentException] should be thrownBy result(topicApi.update("777", anotherRequest))

    // test same name
    val topicNames: Set[String] =
      (0 until 5).map(index => result(topicApi.add(TopicApi.creationRequest(s"topic-$index"))).name).toSet
    topicNames.size shouldBe 5
  }

  @Test
  def removeTopicFromNonexistentBrokerCluster(): Unit = {
    val name = methodName()
    result(
      topicApi
        .add(TopicApi.creationRequest(name))
        .flatMap { topicInfo =>
          BrokerApi
            .access()
            .hostname(configurator.hostname)
            .port(configurator.port)
            .delete(topicInfo.brokerClusterName)
            .flatMap(_ => topicApi.delete(topicInfo.id))
        }
        .flatMap(_ => topicApi.list())
        .map(topics => topics.exists(_.name == name))) shouldBe false
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
