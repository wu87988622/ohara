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
import spray.json.{DeserializationException, _}

import scala.concurrent.ExecutionContext.Implicits.global

class TestNodeApi extends SmallTest with Matchers {

  @Test
  def ignoreNameOnCreation(): Unit = an[NullPointerException] should be thrownBy NodeApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def ignoreNameOnUpdate(): Unit = an[NullPointerException] should be thrownBy NodeApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .update()

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy NodeApi.access.request.name("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy NodeApi.access.request.name(null)

  @Test
  def ignorePortOnCreation(): Unit = an[NullPointerException] should be thrownBy NodeApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def negativePort(): Unit = an[IllegalArgumentException] should be thrownBy NodeApi.access.request.port(-1)

  @Test
  def ignoreUserOnCreation(): Unit = an[NullPointerException] should be thrownBy NodeApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .password(CommonUtils.randomString())
    .create()

  @Test
  def emptyUser(): Unit = an[IllegalArgumentException] should be thrownBy NodeApi.access.request.user("")

  @Test
  def nullUser(): Unit = an[NullPointerException] should be thrownBy NodeApi.access.request.user(null)

  @Test
  def ignorePasswordOnCreation(): Unit = an[NullPointerException] should be thrownBy NodeApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .name(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .user(CommonUtils.randomString())
    .create()

  @Test
  def emptyPassword(): Unit = an[IllegalArgumentException] should be thrownBy NodeApi.access.request.password("")

  @Test
  def nullPassword(): Unit = an[NullPointerException] should be thrownBy NodeApi.access.request.password(null)

  @Test
  def testCreation(): Unit = {

    val name = CommonUtils.randomString()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val creation = NodeApi.access.request.name(name).user(user).password(password).port(port).creation
    creation.name shouldBe name
    creation.user shouldBe user
    creation.password shouldBe password
    creation.port shouldBe port
  }

  @Test
  def testUpdate(): Unit = {
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val update = NodeApi.access.request.user(user).password(password).port(port).update
    update.user.get shouldBe user
    update.password.get shouldBe password
    update.port.get shouldBe port

    NodeApi.access.request.update.port shouldBe None
    NodeApi.access.request.update.user shouldBe None
    NodeApi.access.request.update.password shouldBe None
  }

  @Test
  def testNegativePortInUpdate(): Unit =
    an[DeserializationException] should be thrownBy NodeApi.NODE_UPDATE_JSON_FORMAT.read("""
                                                                                           |{
                                                                                           | "port": -1
                                                                                           |}
                                                                                         """.stripMargin.parseJson)

  @Test
  def testZeroPortInUpdate(): Unit =
    an[DeserializationException] should be thrownBy NodeApi.NODE_UPDATE_JSON_FORMAT.read("""
                                                                                           |{
                                                                                           | "port": 0
                                                                                           |}
                                                                                         """.stripMargin.parseJson)

  @Test
  def testEmptyUserInUpdate(): Unit =
    an[DeserializationException] should be thrownBy NodeApi.NODE_UPDATE_JSON_FORMAT.read("""
                                                                                         |{
                                                                                         | "user": ""
                                                                                         |}
                                                                                       """.stripMargin.parseJson)

  @Test
  def testEmptyPasswordInUpdate(): Unit =
    an[DeserializationException] should be thrownBy NodeApi.NODE_UPDATE_JSON_FORMAT.read("""
                                                                                           |{
                                                                                           | "password": ""
                                                                                           |}
                                                                                         """.stripMargin.parseJson)

  @Test
  def testParseUpdate(): Unit = {
    val port = CommonUtils.availablePort()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val update = NodeApi.NODE_UPDATE_JSON_FORMAT.read(s"""
                                                       |{
                                                       | "port": $port,
                                                       | "user": "$user",
                                                       | "password": "$password"
                                                       |}
                                       """.stripMargin.parseJson)
    update.port.get shouldBe port
    update.user.get shouldBe user
    update.password.get shouldBe password
  }

  @Test
  def testParseDefaultPortInCreation(): Unit = {
    val creation = NodeApi.NODE_CREATION_JSON_FORMAT.read("""
                                                            |{
                                                            | "name": "name",
                                                            | "user": "user",
                                                            | "password": "password"
                                                            |}
                                                          """.stripMargin.parseJson)

    creation.name shouldBe "name"
    creation.user shouldBe "user"
    creation.password shouldBe "password"
    // default is ssh port: 22
    creation.port shouldBe 22
  }

  @Test
  def testEmptyNameInCreation(): Unit =
    an[DeserializationException] should be thrownBy NodeApi.NODE_CREATION_JSON_FORMAT.read("""
                                                                                           |{
                                                                                           | "name": "",
                                                                                           | "port": 123,
                                                                                           | "user": "user",
                                                                                           | "password": "password"
                                                                                           |}
                                                                                         """.stripMargin.parseJson)

  @Test
  def testNegativePortInCreation(): Unit =
    an[DeserializationException] should be thrownBy NodeApi.NODE_CREATION_JSON_FORMAT.read("""
                                                                                             |{
                                                                                             | "name": "name",
                                                                                             | "port": -1,
                                                                                             | "user": "user",
                                                                                             | "password": "password"
                                                                                             |}
                                                                                           """.stripMargin.parseJson)

  @Test
  def testZeroPortInCreation(): Unit =
    an[DeserializationException] should be thrownBy NodeApi.NODE_CREATION_JSON_FORMAT.read("""
                                                                                             |{
                                                                                             | "name": "name",
                                                                                             | "port": 0,
                                                                                             | "user": "user",
                                                                                             | "password": "password"
                                                                                             |}
                                                                                           """.stripMargin.parseJson)

  @Test
  def testEmptyUserInCreation(): Unit =
    an[DeserializationException] should be thrownBy NodeApi.NODE_CREATION_JSON_FORMAT.read("""
                                                                                             |{
                                                                                             | "name": "name",
                                                                                             | "port": 123,
                                                                                             | "user": "",
                                                                                             | "password": "password"
                                                                                             |}
                                                                                           """.stripMargin.parseJson)

  @Test
  def testEmptyPasswordInCreation(): Unit =
    an[DeserializationException] should be thrownBy NodeApi.NODE_CREATION_JSON_FORMAT.read("""
                                                                                             |{
                                                                                             | "name": "name",
                                                                                             | "port": 123,
                                                                                             | "user": "user",
                                                                                             | "password": ""
                                                                                             |}
                                                                                           """.stripMargin.parseJson)

  @Test
  def testParseCreation(): Unit = {
    val name = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val creation = NodeApi.NODE_CREATION_JSON_FORMAT.read(s"""
                                                         |{
                                                         | "name": "$name",
                                                         | "port": $port,
                                                         | "user": "$user",
                                                         | "password": "$password"
                                                         |}
                                       """.stripMargin.parseJson)
    creation.name shouldBe name
    creation.port shouldBe port
    creation.user shouldBe user
    creation.password shouldBe password
  }
}
