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
import com.island.ohara.client.configurator.v0.FtpApi.FtpInfoRequest
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestFtpInfoRoute extends SmallTest with Matchers {

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
}
