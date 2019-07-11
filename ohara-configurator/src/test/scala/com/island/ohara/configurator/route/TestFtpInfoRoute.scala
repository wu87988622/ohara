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
import com.island.ohara.client.configurator.v0.FtpApi.{FtpInfo, Request}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestFtpInfoRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder.fake().build()

  private[this] val ftpApi = FtpApi.access.hostname(configurator.hostname).port(configurator.port)

  @Test
  def test(): Unit = {
    // test add
    result(ftpApi.list()).size shouldBe 0

    val name = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val hostname = CommonUtils.randomString()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val response = result(
      ftpApi.request.name(name).hostname(hostname).port(port).user(user).password(password).create())
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
      ftpApi.request.name(name).hostname(hostname2).port(port2).user(user2).password(password2).update())
    response2.name shouldBe name
    response2.port shouldBe port2
    response2.hostname shouldBe hostname2
    response2.user shouldBe user2
    response2.password shouldBe password2

    // test get
    response2 shouldBe result(ftpApi.get(response2.name))

    // test delete
    result(ftpApi.list()).size shouldBe 1
    result(ftpApi.delete(response.name))
    result(ftpApi.list()).size shouldBe 0

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
          ftpApi.request
            .name(CommonUtils.randomString())
            .hostname(CommonUtils.randomString())
            .port(CommonUtils.availablePort())
            .user(CommonUtils.randomString())
            .password(CommonUtils.randomString())
            .update()))
    result(ftpApi.list()).size shouldBe count
  }

  @Test
  def testInvalidNameOnUpdate(): Unit = {
    val invalidStrings = Seq("a@", "a=", "a\\", "a~", "a//")
    invalidStrings.foreach { invalidString =>
      an[IllegalArgumentException] should be thrownBy result(
        ftpApi.request
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
    val invalidStrings = Seq("a@", "a=", "a\\", "a~", "a//")
    invalidStrings.foreach { invalidString =>
      an[IllegalArgumentException] should be thrownBy result(
        ftpApi.request
          .name(invalidString)
          .hostname(CommonUtils.randomString())
          .port(CommonUtils.availablePort())
          .user(CommonUtils.randomString())
          .password(CommonUtils.randomString())
          .create())
    }
  }

  @Test
  def testUpdateHostname(): Unit = {
    val hostname = CommonUtils.randomString()
    updatePartOfField(_.hostname(hostname), _.copy(hostname = hostname))
  }

  @Test
  def testUpdatePort(): Unit = {
    val port = CommonUtils.availablePort()
    updatePartOfField(_.port(port), _.copy(port = port))
  }

  @Test
  def testUpdateUser(): Unit = {
    val user = CommonUtils.randomString()
    updatePartOfField(_.user(user), _.copy(user = user))
  }

  @Test
  def testUpdatePassword(): Unit = {
    val password = CommonUtils.randomString()
    updatePartOfField(_.password(password), _.copy(password = password))
  }

  private[this] def updatePartOfField(req: Request => Request, _expected: FtpInfo => FtpInfo): Unit = {
    val previous = result(
      ftpApi.request
        .name(CommonUtils.randomString())
        .hostname(CommonUtils.randomString())
        .port(CommonUtils.availablePort())
        .user(CommonUtils.randomString())
        .password(CommonUtils.randomString())
        .update())
    val updated = result(req(ftpApi.request.name(previous.name)).update())
    val expected = _expected(previous)
    updated.name shouldBe expected.name
    updated.hostname shouldBe expected.hostname
    updated.port shouldBe expected.port
    updated.user shouldBe expected.user
    updated.password shouldBe expected.password
  }

  @Test
  def updateTags(): Unit = {
    val tags = Set(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val ftpDesc = result(
      ftpApi.request.hostname("hostname").port(22).user("user").password("password").tags(tags).create())
    ftpDesc.tags shouldBe tags

    val tags2 = Set(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val ftpDesc2 = result(ftpApi.request.name(ftpDesc.name).tags(tags2).update())
    ftpDesc2.tags shouldBe tags2

    val ftpDesc3 = result(ftpApi.request.name(ftpDesc.name).update())
    ftpDesc3.tags shouldBe tags2

    val ftpDesc4 = result(ftpApi.request.name(ftpDesc.name).tags(Set.empty).update())
    ftpDesc4.tags shouldBe Set.empty
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
