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

import com.island.ohara.client.configurator.v0.JdbcApi
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestJdbcInfoRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val jdbcApi = JdbcApi.access().hostname(configurator.hostname).port(configurator.port)

  @Test
  def test(): Unit = {
    // test add
    result(jdbcApi.list).size shouldBe 0

    val name = CommonUtils.randomString()
    val url = CommonUtils.randomString()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val response = result(jdbcApi.request().name(name).url(url).user(user).password(password).create())
    response.name shouldBe name
    response.url shouldBe url
    response.user shouldBe user
    response.password shouldBe password

    // test get
    response shouldBe result(jdbcApi.get(response.name))

    // test update
    val url2 = CommonUtils.randomString()
    val user2 = CommonUtils.randomString()
    val password2 = CommonUtils.randomString()
    val response2 = result(jdbcApi.request().name(name).url(url2).user(user2).password(password2).update())
    response2.name shouldBe name
    response2.url shouldBe url2
    response2.user shouldBe user2
    response2.password shouldBe password2

    // test get
    response2 shouldBe result(jdbcApi.get(response2.name))

    // test delete
    result(jdbcApi.list).size shouldBe 1
    result(jdbcApi.delete(response.id))
    result(jdbcApi.list).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(jdbcApi.get("asdadas"))
  }

  @Test
  def duplicateDelete(): Unit =
    (0 to 10).foreach(_ => result(jdbcApi.delete(CommonUtils.randomString(5))))

  @Test
  def duplicateUpdate(): Unit = {
    val count = 10
    (0 until count).foreach(
      _ =>
        result(
          jdbcApi
            .request()
            .name(CommonUtils.randomString())
            .url(CommonUtils.randomString())
            .user(CommonUtils.randomString())
            .password(CommonUtils.randomString())
            .update()))
    result(jdbcApi.list).size shouldBe count
  }

  @Test
  def testInvalidNameOnUpdate(): Unit = {
    val invalidStrings = Seq("a_", "a-", "a.", "a~")
    invalidStrings.foreach { invalidString =>
      an[IllegalArgumentException] should be thrownBy result(
        jdbcApi
          .request()
          .name(invalidString)
          .url(CommonUtils.randomString())
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
        jdbcApi
          .request()
          .name(invalidString)
          .url(CommonUtils.randomString())
          .user(CommonUtils.randomString())
          .password(CommonUtils.randomString())
          .create())
    }
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
