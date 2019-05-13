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
import ValidationApi.FTP_VALIDATION_REQUEST_JSON_FORMAT
import spray.json._
class TestValidationApi extends SmallTest with Matchers {

  @Test
  def testIntPortOfFtpRequest(): Unit = {
    val hostname = CommonUtils.randomString()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val json =
      s"""
         | {
         |   "hostname": \"$hostname\",
         |   "user": \"$user\",
         |   "password": \"$password\",
         |   "port": $port
         | }
       """.stripMargin

    val request = FTP_VALIDATION_REQUEST_JSON_FORMAT.read(json.parseJson)
    request.hostname shouldBe hostname
    request.user shouldBe user
    request.password shouldBe password
    request.port shouldBe port
  }

  @Test
  def testStringPortOfFtpRequest(): Unit = {
    val hostname = CommonUtils.randomString()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val json =
      s"""
         | {
         |   "hostname": \"$hostname\",
         |   "user": \"$user\",
         |   "password": \"$password\",
         |   "port": \"$port\"
         | }
       """.stripMargin

    val request = FTP_VALIDATION_REQUEST_JSON_FORMAT.read(json.parseJson)
    request.hostname shouldBe hostname
    request.user shouldBe user
    request.password shouldBe password
    request.port shouldBe port
  }
}
