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

import scala.concurrent.ExecutionContext.Implicits.global

class TestJdbcApi extends SmallTest with Matchers {

  @Test
  def ignoreNameOnCreation(): Unit = an[NullPointerException] should be thrownBy JdbcApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .url(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def ignoreNameOnUpdate(): Unit = an[NullPointerException] should be thrownBy JdbcApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .url(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .update()

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy JdbcApi.access().request().name("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy JdbcApi.access().request().name(null)

  @Test
  def emptyUrl(): Unit = an[IllegalArgumentException] should be thrownBy JdbcApi.access().request().url("")

  @Test
  def nullUrl(): Unit = an[NullPointerException] should be thrownBy JdbcApi.access().request().url(null)

  @Test
  def ignoreUrlOnCreation(): Unit = an[NullPointerException] should be thrownBy JdbcApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def ignoreUrlOnUpdate(): Unit = an[NullPointerException] should be thrownBy JdbcApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .update()

  @Test
  def ignoreUserOnCreation(): Unit = an[NullPointerException] should be thrownBy JdbcApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .url(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def ignoreUserOnUpdate(): Unit = an[NullPointerException] should be thrownBy JdbcApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .url(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .update()

  @Test
  def emptyUser(): Unit = an[IllegalArgumentException] should be thrownBy JdbcApi.access().request().user("")

  @Test
  def nullUser(): Unit = an[NullPointerException] should be thrownBy JdbcApi.access().request().user(null)

  @Test
  def ignorePasswordOnCreation(): Unit = an[NullPointerException] should be thrownBy JdbcApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .url(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .create()

  @Test
  def ignorePasswordOnUpdate(): Unit = an[NullPointerException] should be thrownBy JdbcApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .url(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .update()

  @Test
  def emptyPassword(): Unit =
    an[IllegalArgumentException] should be thrownBy JdbcApi.access().request().password("")

  @Test
  def nullPassword(): Unit = an[NullPointerException] should be thrownBy JdbcApi.access().request().password(null)
}
