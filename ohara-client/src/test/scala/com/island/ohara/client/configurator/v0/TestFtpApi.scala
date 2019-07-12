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
import spray.json.DeserializationException

import scala.concurrent.ExecutionContext.Implicits.global
import spray.json._
class TestFtpApi extends SmallTest with Matchers {

  @Test
  def ignoreNameOnCreation(): Unit = FtpApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .creation
    .name
    .length should not be 0

  @Test
  def ignoreNameOnUpdate(): Unit = an[NullPointerException] should be thrownBy FtpApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .update()

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi.access.request.name("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy FtpApi.access.request.name(null)

  @Test
  def ignoreHostnameOnCreation(): Unit = an[NullPointerException] should be thrownBy FtpApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def emptyHostname(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi.access.request.hostname("")

  @Test
  def nullHostname(): Unit = an[NullPointerException] should be thrownBy FtpApi.access.request.hostname(null)

  @Test
  def ignorePortOnCreation(): Unit = an[NullPointerException] should be thrownBy FtpApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(CommonUtils.randomString())
    .hostname(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def negativePort(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi.access.request.port(-1)

  @Test
  def ignoreUserOnCreation(): Unit = an[NullPointerException] should be thrownBy FtpApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(CommonUtils.randomString())
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def emptyUser(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi.access.request.user("")

  @Test
  def nullUser(): Unit = an[NullPointerException] should be thrownBy FtpApi.access.request.user(null)

  @Test
  def ignorePasswordOnCreation(): Unit = an[NullPointerException] should be thrownBy FtpApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(CommonUtils.randomString())
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .create()

  @Test
  def emptyPassword(): Unit = an[IllegalArgumentException] should be thrownBy FtpApi.access.request.password("")

  @Test
  def nullPassword(): Unit = an[NullPointerException] should be thrownBy FtpApi.access.request.password(null)

  @Test
  def testCreation(): Unit = {
    val name = CommonUtils.randomString(10)
    val hostname = CommonUtils.randomString(10)
    val port = CommonUtils.availablePort()
    val user = CommonUtils.randomString(10)
    val password = CommonUtils.randomString(10)

    val creation =
      FtpApi.access.request.name(name).hostname(hostname).port(port).user(user).password(password).creation
    creation.name shouldBe name
    creation.hostname shouldBe hostname
    creation.port shouldBe port
    creation.user shouldBe user
    creation.password shouldBe password
  }

  @Test
  def testUpdate(): Unit = {
    val name = CommonUtils.randomString(10)
    val hostname = CommonUtils.randomString(10)
    val port = CommonUtils.availablePort()
    val user = CommonUtils.randomString(10)
    val password = CommonUtils.randomString(10)

    val update = FtpApi.access.request.name(name).hostname(hostname).port(port).user(user).password(password).update
    update.hostname.get shouldBe hostname
    update.port.get shouldBe port
    update.user.get shouldBe user
    update.password.get shouldBe password

    FtpApi.access.request.update.hostname shouldBe None
    FtpApi.access.request.update.port shouldBe None
    FtpApi.access.request.update.user shouldBe None
    FtpApi.access.request.update.password shouldBe None
  }

  @Test
  def testEmptyHostnameInUpdate(): Unit =
    an[DeserializationException] should be thrownBy FtpApi.FTP_UPDATE_JSON_FORMAT.read("""
         |{
         | "hostname": ""
         |}
       """.stripMargin.parseJson)

  @Test
  def testEmptyUserInUpdate(): Unit =
    an[DeserializationException] should be thrownBy FtpApi.FTP_UPDATE_JSON_FORMAT.read("""
             |{
             | "user": ""
             |}
           """.stripMargin.parseJson)

  @Test
  def testNegativePortInUpdate(): Unit =
    an[DeserializationException] should be thrownBy FtpApi.FTP_UPDATE_JSON_FORMAT.read("""
                                                                                         |{
                                                                                         | "port": -1
                                                                                         |}
                                                                                       """.stripMargin.parseJson)

  @Test
  def testEmptyPasswordInUpdate(): Unit =
    an[DeserializationException] should be thrownBy FtpApi.FTP_UPDATE_JSON_FORMAT.read("""
                               |{
                               | "password": ""
                               |}
                             """.stripMargin.parseJson)

  @Test
  def testParseUpdate(): Unit = {
    val hostname = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val update = FtpApi.FTP_UPDATE_JSON_FORMAT.read(s"""
                                         |{
                                         | "hostname": "$hostname",
                                         | "port": $port,
                                         | "user": "$user",
                                         | "password": "$password"
                                         |}
                                       """.stripMargin.parseJson)
    update.hostname.get shouldBe hostname
    update.port.get shouldBe port
    update.user.get shouldBe user
    update.password.get shouldBe password
  }

  @Test
  def testEmptyNameInCreation(): Unit =
    an[DeserializationException] should be thrownBy FtpApi.FTP_CREATION_JSON_FORMAT.read("""
                                                                                           |{
                                                                                           | "name": "",
                                                                                           | "hostname": "hostname",
                                                                                           | "port": 123,
                                                                                           | "user": "user",
                                                                                           | "password": "password"
                                                                                           |}
                                                                                         """.stripMargin.parseJson)

  @Test
  def testEmptyHostnameInCreation(): Unit =
    an[DeserializationException] should be thrownBy FtpApi.FTP_CREATION_JSON_FORMAT.read("""
                                                                                           |{
                                                                                           | "name": "name",
                                                                                           | "hostname": "",
                                                                                           | "port": 123,
                                                                                           | "user": "user",
                                                                                           | "password": "password"
                                                                                           |}
                                                                                         """.stripMargin.parseJson)
  @Test
  def testNegativePortInCreation(): Unit =
    an[DeserializationException] should be thrownBy FtpApi.FTP_CREATION_JSON_FORMAT.read("""
                                                                                           |{
                                                                                           | "name": "name",
                                                                                           | "hostname": "123",
                                                                                           | "port": -1,
                                                                                           | "user": "user",
                                                                                           | "password": "password"
                                                                                           |}
                                                                                         """.stripMargin.parseJson)

  @Test
  def testZeroPortInCreation(): Unit =
    an[DeserializationException] should be thrownBy FtpApi.FTP_CREATION_JSON_FORMAT.read("""
                                                                                           |{
                                                                                           | "name": "name",
                                                                                           | "hostname": "123",
                                                                                           | "port": 0,
                                                                                           | "user": "user",
                                                                                           | "password": "password"
                                                                                           |}
                                                                                         """.stripMargin.parseJson)

  @Test
  def testEmptyUserInCreation(): Unit =
    an[DeserializationException] should be thrownBy FtpApi.FTP_CREATION_JSON_FORMAT.read("""
                                                                                           |{
                                                                                           | "name": "name",
                                                                                           | "hostname": "hostname",
                                                                                           | "port": 123,
                                                                                           | "user": "",
                                                                                           | "password": "password"
                                                                                           |}
                                                                                         """.stripMargin.parseJson)
  @Test
  def testEmptyPasswordInCreation(): Unit =
    an[DeserializationException] should be thrownBy FtpApi.FTP_CREATION_JSON_FORMAT.read("""
                                                                                           |{
                                                                                           | "name": "name",
                                                                                           | "hostname": "hostname",
                                                                                           | "port": 123,
                                                                                           | "user": "user",
                                                                                           | "password": ""
                                                                                           |}
                                                                                         """.stripMargin.parseJson)

  @Test
  def testParseCreation(): Unit = {
    val hostname = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val creation = FtpApi.FTP_CREATION_JSON_FORMAT.read(s"""
                                                           |{
                                                           | "hostname": "$hostname",
                                                           | "port": $port,
                                                           | "user": "$user",
                                                           | "password": "$password"
                                                           |}
                                       """.stripMargin.parseJson)
    creation.name.length shouldBe 10
    creation.hostname shouldBe hostname
    creation.port shouldBe port
    creation.user shouldBe user
    creation.password shouldBe password

    val name = CommonUtils.randomString()
    val creation2 = FtpApi.FTP_CREATION_JSON_FORMAT.read(s"""
                                                       |{
                                                       | "name": "$name",
                                                       | "hostname": "$hostname",
                                                       | "port": $port,
                                                       | "user": "$user",
                                                       | "password": "$password"
                                                       |}
                                       """.stripMargin.parseJson)
    creation2.name shouldBe name
    creation2.hostname shouldBe hostname
    creation2.port shouldBe port
    creation2.user shouldBe user
    creation2.password shouldBe password
  }

  @Test
  def nullTags(): Unit = an[NullPointerException] should be thrownBy FtpApi.access.request.tags(null)

  @Test
  def emptyTags(): Unit = FtpApi.access.request.tags(Map.empty)
}
