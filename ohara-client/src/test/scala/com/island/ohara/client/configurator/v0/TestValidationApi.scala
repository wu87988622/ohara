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
import ValidationApi.FTP_VALIDATION_JSON_FORMAT
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

    val request = FTP_VALIDATION_JSON_FORMAT.read(json.parseJson)
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

    val request = FTP_VALIDATION_JSON_FORMAT.read(json.parseJson)
    request.hostname shouldBe hostname
    request.user shouldBe user
    request.password shouldBe password
    request.port shouldBe port
  }

  @Test
  def nullHostname(): Unit = an[NullPointerException] should be thrownBy ValidationApi.access.hostname(null)

  @Test
  def emptyHostname(): Unit = an[IllegalArgumentException] should be thrownBy ValidationApi.access.hostname("")

  @Test
  def negativePort(): Unit = an[IllegalArgumentException] should be thrownBy ValidationApi.access.port(-1)

  @Test
  def nullHostnameOnFtp(): Unit =
    an[NullPointerException] should be thrownBy ValidationApi.access.ftpRequest.hostname(null)

  @Test
  def emptyHostnameOnFtp(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.ftpRequest.hostname("")

  @Test
  def negativePortOnFtp(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.ftpRequest.port(-1)

  @Test
  def zeoPortOnFtp(): Unit = an[IllegalArgumentException] should be thrownBy ValidationApi.access.ftpRequest.port(0)

  @Test
  def nullUserOnFtp(): Unit = an[NullPointerException] should be thrownBy ValidationApi.access.ftpRequest.user(null)

  @Test
  def emptyUserOnFtp(): Unit = an[IllegalArgumentException] should be thrownBy ValidationApi.access.ftpRequest.user("")

  @Test
  def nullPasswordOnFtp(): Unit =
    an[NullPointerException] should be thrownBy ValidationApi.access.ftpRequest.password(null)

  @Test
  def emptyPasswordOnFtp(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.ftpRequest.password("")

  @Test
  def nullWorkerClusterNameOnFtp(): Unit =
    an[NullPointerException] should be thrownBy ValidationApi.access.ftpRequest.workerClusterName(null)

  @Test
  def emptyWorkerClusterNameOnFtp(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.ftpRequest.workerClusterName("")

  @Test
  def testFtpValidation(): Unit = {
    val hostname = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val workerClusterName = CommonUtils.randomString()

    val validation = ValidationApi.access.ftpRequest
      .hostname(hostname)
      .port(port)
      .user(user)
      .password(password)
      .workerClusterName(workerClusterName)
      .validation

    validation.hostname shouldBe hostname
    validation.port shouldBe port
    validation.user shouldBe user
    validation.password shouldBe password
    validation.workerClusterName.get shouldBe workerClusterName

    ValidationApi.access.ftpRequest
      .hostname(hostname)
      .port(port)
      .user(user)
      .password(password)
      .validation
      .workerClusterName shouldBe None
  }

  @Test
  def nullUriOnHdfs(): Unit = an[NullPointerException] should be thrownBy ValidationApi.access.hdfsRequest.uri(null)

  @Test
  def emptyUriOnHdfs(): Unit = an[IllegalArgumentException] should be thrownBy ValidationApi.access.hdfsRequest.uri("")

  @Test
  def nullWorkerClusterNameOnHdfs(): Unit =
    an[NullPointerException] should be thrownBy ValidationApi.access.hdfsRequest.workerClusterName(null)

  @Test
  def emptyWorkerClusterNameOnHdfs(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.hdfsRequest.workerClusterName("")

  @Test
  def testHdfsValidation(): Unit = {
    val uri = CommonUtils.randomString()
    val workerClusterName = CommonUtils.randomString()

    val validation = ValidationApi.access.hdfsRequest.uri(uri).workerClusterName(workerClusterName).validation

    validation.uri shouldBe uri
    validation.workerClusterName.get shouldBe workerClusterName

    ValidationApi.access.hdfsRequest.uri(uri).validation.workerClusterName shouldBe None
  }

  @Test
  def nullUrlOnRdb(): Unit = an[NullPointerException] should be thrownBy ValidationApi.access.rdbRequest.url(null)

  @Test
  def emptyUrlOnRdb(): Unit = an[IllegalArgumentException] should be thrownBy ValidationApi.access.rdbRequest.url("")

  @Test
  def nullUserOnRdb(): Unit = an[NullPointerException] should be thrownBy ValidationApi.access.rdbRequest.user(null)

  @Test
  def emptyUserOnRdb(): Unit = an[IllegalArgumentException] should be thrownBy ValidationApi.access.rdbRequest.user("")

  @Test
  def nullPasswordOnRdb(): Unit =
    an[NullPointerException] should be thrownBy ValidationApi.access.rdbRequest.password(null)

  @Test
  def emptyPasswordOnRdb(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.rdbRequest.password("")

  @Test
  def nullWorkerClusterNameOnRdb(): Unit =
    an[NullPointerException] should be thrownBy ValidationApi.access.rdbRequest.workerClusterName(null)

  @Test
  def emptyWorkerClusterNameOnRdb(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.rdbRequest.workerClusterName("")

  @Test
  def testRdbValidation(): Unit = {
    val url = CommonUtils.randomString()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val workerClusterName = CommonUtils.randomString()

    val validation = ValidationApi.access.rdbRequest
      .url(url)
      .user(user)
      .password(password)
      .workerClusterName(workerClusterName)
      .validation

    validation.url shouldBe url
    validation.user shouldBe user
    validation.password shouldBe password
    validation.workerClusterName.get shouldBe workerClusterName

    ValidationApi.access.rdbRequest.url(url).user(user).password(password).validation.workerClusterName shouldBe None
  }

  @Test
  def nullHostnameOnNode(): Unit =
    an[NullPointerException] should be thrownBy ValidationApi.access.nodeRequest.hostname(null)

  @Test
  def emptyHostnameOnNode(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.nodeRequest.hostname("")

  @Test
  def negativePortOnNode(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.nodeRequest.port(-1)

  @Test
  def zeoPortOnNode(): Unit = an[IllegalArgumentException] should be thrownBy ValidationApi.access.nodeRequest.port(0)

  @Test
  def nullUserOnNode(): Unit = an[NullPointerException] should be thrownBy ValidationApi.access.nodeRequest.user(null)

  @Test
  def emptyUserOnNode(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.nodeRequest.user("")

  @Test
  def nullPasswordOnNode(): Unit =
    an[NullPointerException] should be thrownBy ValidationApi.access.nodeRequest.password(null)

  @Test
  def emptyPasswordOnNode(): Unit =
    an[IllegalArgumentException] should be thrownBy ValidationApi.access.nodeRequest.password("")

  @Test
  def testNodeValidation(): Unit = {
    val hostname = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()

    val validation =
      ValidationApi.access.nodeRequest.hostname(hostname).port(port).user(user).password(password).validation

    validation.hostname shouldBe hostname
    validation.port shouldBe port
    validation.user shouldBe user
    validation.password shouldBe password
  }
}
