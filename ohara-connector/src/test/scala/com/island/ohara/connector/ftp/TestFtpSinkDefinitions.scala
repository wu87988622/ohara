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

package com.island.ohara.connector.ftp

import com.island.ohara.common.rule.SmallTest
import com.island.ohara.kafka.connector.json.SettingDefinition
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestFtpSinkDefinitions extends SmallTest with Matchers {

  private[this] val ftpSink = new FtpSink
  @Test
  def checkInputFolder(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_OUTPUT).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkErrorFolder(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_NEED_HEADER).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.BOOLEAN.name()
  }

  @Test
  def checkEncode(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_ENCODE).get
    definition.required() shouldBe false
    definition.defaultValue() shouldBe "UTF-8"
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkHostname(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_HOSTNAME).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkPort(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_PORT).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.INT.name()
  }

  @Test
  def checkUser(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_USER_NAME).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkPassword(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_PASSWORD).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.PASSWORD.name()
  }
}
