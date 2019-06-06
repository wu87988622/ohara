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

class TestFtpApi extends SmallTest with Matchers {

  @Test
  def ignoreNameOnCreation(): Unit = an[NullPointerException] should be thrownBy FtpApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def ignoreNameOnUpdate(): Unit = an[NullPointerException] should be thrownBy FtpApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .update()

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi.access().request().name("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy FtpApi.access().request().name(null)

  @Test
  def ignoreHostnameOnCreation(): Unit = an[NullPointerException] should be thrownBy FtpApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def ignoreHostnameOnUpdate(): Unit = an[NullPointerException] should be thrownBy FtpApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .update()

  @Test
  def emptyHostname(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi.access().request().hostname("")

  @Test
  def nullHostname(): Unit = an[NullPointerException] should be thrownBy FtpApi.access().request().hostname(null)

  @Test
  def ignorePortOnCreation(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .hostname(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def ignorePortOnUpdate(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .hostname(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .update()

  @Test
  def negativePort(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi.access().request().port(-1)

  @Test
  def ignoreUserOnCreation(): Unit = an[NullPointerException] should be thrownBy FtpApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def ignoreUserOnUpdate(): Unit = an[NullPointerException] should be thrownBy FtpApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .password(CommonUtils.randomString())
    .update()

  @Test
  def emptyUser(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi.access().request().user("")

  @Test
  def nullUser(): Unit = an[NullPointerException] should be thrownBy FtpApi.access().request().user(null)

  @Test
  def ignorePasswordOnCreation(): Unit = an[NullPointerException] should be thrownBy FtpApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .create()

  @Test
  def ignorePasswordOnUpdate(): Unit = an[NullPointerException] should be thrownBy FtpApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .name(CommonUtils.randomString())
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .update()

  @Test
  def emptyPassword(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi.access().request().password("")

  @Test
  def nullPassword(): Unit = an[NullPointerException] should be thrownBy FtpApi.access().request().password(null)
}
