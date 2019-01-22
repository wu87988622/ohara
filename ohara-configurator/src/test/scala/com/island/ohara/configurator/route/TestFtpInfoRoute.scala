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

import com.island.ohara.client.configurator.v0.FtpApi
import com.island.ohara.client.configurator.v0.FtpApi.{FtpInfo, FtpInfoRequest}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestFtpInfoRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testValidateField(): Unit = {
    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      FtpInfoRequest(
        name = "",
        hostname = "hostname",
        port = 1234,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      FtpInfoRequest(
        name = "aa",
        hostname = "",
        port = 1234,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      FtpInfoRequest(
        name = "aa",
        hostname = "hostname",
        port = -1,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      FtpInfoRequest(
        name = "aa",
        hostname = "hostname",
        port = 0,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      FtpInfoRequest(
        name = "aaa",
        hostname = "hostname",
        port = 99999,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      FtpInfoRequest(
        name = "aaa",
        hostname = "hostname",
        port = 12345,
        user = "",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      FtpInfoRequest(
        name = "aaa",
        hostname = "hostname",
        port = 12345,
        user = "aa",
        password = ""
      ))

    FtpInfoRoute.validateField(
      FtpInfoRequest(
        name = "aaa",
        hostname = "hostname",
        port = 12345,
        user = "aa",
        password = "aaa"
      ))
  }

  @Test
  def test(): Unit = {
    def compareRequestAndResponse(request: FtpInfoRequest, response: FtpInfo): FtpInfo = {
      request.name shouldBe response.name
      request.hostname shouldBe response.hostname
      request.port shouldBe response.port
      request.user shouldBe response.user
      request.password shouldBe response.password
      response
    }

    def compare2Response(lhs: FtpInfo, rhs: FtpInfo): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe lhs.name
      lhs.hostname shouldBe lhs.hostname
      lhs.port shouldBe lhs.port
      lhs.user shouldBe lhs.user
      lhs.password shouldBe lhs.password
      lhs.lastModified shouldBe rhs.lastModified
    }

    val access = FtpApi.access().hostname(configurator.hostname).port(configurator.port)
    // test add
    result(access.list()).size shouldBe 0

    val request = FtpInfoRequest("test", "152.22.23.12", 5, "test", "test")
    val response = compareRequestAndResponse(request, result(access.add(request)))

    // test get
    compare2Response(response, result(access.get(response.id)))

    // test update
    val anotherRequest = FtpInfoRequest("test2", "152.22.23.125", 1222, "test", "test")
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(access.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(access.get(newResponse.id)))

    // test delete
    result(access.list()).size shouldBe 1
    result(access.delete(response.id)) shouldBe newResponse
    result(access.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(access.get("asdadas"))
    an[IllegalArgumentException] should be thrownBy result(access.update("asdadas", anotherRequest))
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
