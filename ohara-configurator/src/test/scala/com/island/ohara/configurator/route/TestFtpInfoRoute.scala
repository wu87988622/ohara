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
import com.island.ohara.client.configurator.v0.FtpApi.Creation
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestFtpInfoRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val ftpApi = FtpApi.access().hostname(configurator.hostname).port(configurator.port)
  @Test
  def testValidateField(): Unit = {
    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      Creation(
        name = "",
        hostname = "hostname",
        port = 1234,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      Creation(
        name = "aa",
        hostname = "",
        port = 1234,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      Creation(
        name = "aa",
        hostname = "hostname",
        port = -1,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      Creation(
        name = "aa",
        hostname = "hostname",
        port = 0,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      Creation(
        name = "aaa",
        hostname = "hostname",
        port = 99999,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      Creation(
        name = "aaa",
        hostname = "hostname",
        port = 12345,
        user = "",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInfoRoute.validateField(
      Creation(
        name = "aaa",
        hostname = "hostname",
        port = 12345,
        user = "aa",
        password = ""
      ))

    FtpInfoRoute.validateField(
      Creation(
        name = "aaa",
        hostname = "hostname",
        port = 12345,
        user = "aa",
        password = "aaa"
      ))
  }

  @Test
  def test(): Unit = {
    // test add
    result(ftpApi.list).size shouldBe 0

    val name = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val hostname = CommonUtils.randomString()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val response = result(
      ftpApi.request().name(name).hostname(hostname).port(port).user(user).password(password).create())
    response.name shouldBe name
    response.port shouldBe port
    response.hostname shouldBe hostname
    response.user shouldBe user
    response.password shouldBe password

    // test get
    response shouldBe result(ftpApi.get(response.name))

    // test update
    val port2 = CommonUtils.availablePort()
    val hostname2 = CommonUtils.randomString()
    val user2 = CommonUtils.randomString()
    val password2 = CommonUtils.randomString()
    val response2 = result(
      ftpApi.request().name(name).hostname(hostname2).port(port2).user(user2).password(password2).update())
    response2.name shouldBe name
    response2.port shouldBe port2
    response2.hostname shouldBe hostname2
    response2.user shouldBe user2
    response2.password shouldBe password2

    // test get
    response2 shouldBe result(ftpApi.get(response2.name))

    // test delete
    result(ftpApi.list).size shouldBe 1
    result(ftpApi.delete(response.id))
    result(ftpApi.list).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(ftpApi.get("asdadas"))
  }

  @Test
  def duplicateDelete(): Unit =
    (0 to 10).foreach(_ => result(ftpApi.delete(CommonUtils.randomString(5))))

  @Test
  def duplicateUpdate(): Unit = {
    val count = 10
    (0 until count).foreach(
      _ =>
        result(
          ftpApi
            .request()
            .name(CommonUtils.randomString())
            .hostname(CommonUtils.randomString())
            .port(CommonUtils.availablePort())
            .user(CommonUtils.randomString())
            .password(CommonUtils.randomString())
            .update()))
    result(ftpApi.list).size shouldBe count
  }

  @Test
  def testInvalidNameOnUpdate(): Unit = {
    val invalidStrings = Seq("a_", "a-", "a.", "a~")
    invalidStrings.foreach { invalidString =>
      an[IllegalArgumentException] should be thrownBy result(
        ftpApi
          .request()
          .name(invalidString)
          .hostname(CommonUtils.randomString())
          .port(CommonUtils.availablePort())
          .user(CommonUtils.randomString())
          .password(CommonUtils.randomString())
          .update())
    }
  }

  @Test
  def testInvalidNameOnCreation(): Unit = {
    val invalidStrings = Seq("a_", "a-", "a.", "a~")
    invalidStrings.foreach { invalidString =>
      an[IllegalArgumentException] should be thrownBy result(
        ftpApi
          .request()
          .name(invalidString)
          .hostname(CommonUtils.randomString())
          .port(CommonUtils.availablePort())
          .user(CommonUtils.randomString())
          .password(CommonUtils.randomString())
          .create())
    }
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
