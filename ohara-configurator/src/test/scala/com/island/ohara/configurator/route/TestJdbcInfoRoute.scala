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

import com.island.ohara.client.configurator.v0.DatabaseApi
import com.island.ohara.client.configurator.v0.DatabaseApi.{JdbcInfo, JdbcInfoRequest}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestJdbcInfoRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val jdbcApi = DatabaseApi.access().hostname(configurator.hostname).port(configurator.port)
  @Test
  def test(): Unit = {
    def compareRequestAndResponse(request: JdbcInfoRequest, response: JdbcInfo): JdbcInfo = {
      request.name shouldBe response.name
      request.url shouldBe response.url
      request.user shouldBe response.user
      request.password shouldBe response.password
      response
    }

    def compare2Response(lhs: JdbcInfo, rhs: JdbcInfo): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe lhs.name
      lhs.url shouldBe lhs.url
      lhs.user shouldBe lhs.user
      lhs.password shouldBe lhs.password
      lhs.lastModified shouldBe rhs.lastModified
    }

    // test add
    result(jdbcApi.list).size shouldBe 0

    val request = JdbcInfoRequest("test", "oracle://152.22.23.12:4222", "test", "test")
    val response = compareRequestAndResponse(request, result(jdbcApi.add(request)))

    // test get
    compare2Response(response, result(jdbcApi.get(response.id)))

    // test update
    val anotherRequest = JdbcInfoRequest("test2", "msSQL://152.22.23.12:4222", "test", "test")
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(jdbcApi.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(jdbcApi.get(newResponse.id)))

    // test delete
    result(jdbcApi.list).size shouldBe 1
    result(jdbcApi.delete(response.id))
    result(jdbcApi.list).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(jdbcApi.get("adasd"))
    an[IllegalArgumentException] should be thrownBy result(jdbcApi.update("adasd", anotherRequest))
  }

  @Test
  def duplicateDeleteStreamProperty(): Unit =
    (0 to 10).foreach(_ => result(jdbcApi.delete(CommonUtils.randomString(5))))

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
