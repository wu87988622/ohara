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
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.common.setting.SettingDef.Reference
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestFtpSinkDefinitions extends SmallTest with Matchers {

  private[this] val ftpSink = new FtpSink
  @Test
  def checkOutputFolder(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == TOPICS_DIR_KEY).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe Reference.NONE
    definition.valueType() shouldBe SettingDef.Type.STRING
  }

  @Test
  def checkNeedHeader(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FILE_NEED_HEADER_KEY).get
    definition.required() shouldBe false
    definition.defaultValue() shouldBe "true"
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe Reference.NONE
    definition.valueType() shouldBe SettingDef.Type.BOOLEAN
  }

  @Test
  def checkEncode(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FILE_ENCODE_KEY).get
    definition.required() shouldBe false
    definition.defaultValue() shouldBe "UTF-8"
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe Reference.NONE
    definition.valueType() shouldBe SettingDef.Type.STRING
  }

  @Test
  def checkHostname(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_HOSTNAME_KEY).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe Reference.NONE
    definition.valueType() shouldBe SettingDef.Type.STRING
  }

  @Test
  def checkPort(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_PORT_KEY).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe Reference.NONE
    definition.valueType() shouldBe SettingDef.Type.PORT
  }

  @Test
  def checkUser(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_USER_NAME_KEY).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe Reference.NONE
    definition.valueType() shouldBe SettingDef.Type.STRING
  }

  @Test
  def checkPassword(): Unit = {
    val definition = ftpSink.definitions().asScala.find(_.key() == FTP_PASSWORD_KEY).get
    definition.required() shouldBe true
    definition.defaultValue() shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe Reference.NONE
    definition.valueType() shouldBe SettingDef.Type.PASSWORD
  }
}
