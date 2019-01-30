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

import com.island.ohara.client.configurator.v0.TopicApi
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
class TestTopicRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake().build()

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

    val access = TopicApi.access().hostname(configurator.hostname).port(configurator.port)

    // test add
    result(access.list()).size shouldBe 0
    val request = TopicApi.creationRequest(methodName)
    val response = compareRequestAndResponse(request, result(access.add(request)))

    // test get
    compare2Response(response, result(access.get(response.id)))

    // test update
    val anotherRequest = TopicApi.creationRequest(methodName)
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(access.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(access.get(newResponse.id)))

    // test delete
    result(access.list()).size shouldBe 1
    result(access.delete(response.id)) shouldBe newResponse
    result(access.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(access.get("123"))
    an[IllegalArgumentException] should be thrownBy result(access.update("777", anotherRequest))

    // test same name
    val topicNames: Set[String] =
      (0 until 5).map(index => result(access.add(TopicApi.creationRequest(s"topic-$index"))).name).toSet
    topicNames.size shouldBe 5
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
