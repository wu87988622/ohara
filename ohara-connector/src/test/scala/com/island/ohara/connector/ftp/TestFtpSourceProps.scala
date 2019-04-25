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
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

class TestFtpSourceProps extends SmallTest with Matchers {

  @Test
  def testGetter(): Unit = {
    val inputFolder = CommonUtils.randomString()
    val completedFolder = CommonUtils.randomString()
    val errorFolder = CommonUtils.randomString()
    val encode = CommonUtils.randomString()
    val hostname = CommonUtils.randomString()
    val port = 12345
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val props = FtpSourceProps(
      inputFolder = inputFolder,
      completedFolder = Some(completedFolder),
      errorFolder = errorFolder,
      encode = encode,
      hostname = hostname,
      port = port,
      user = user,
      password = password
    ).toMap
    props(FTP_INPUT) shouldBe inputFolder
    props(FTP_COMPLETED_FOLDER) shouldBe completedFolder
    props(FTP_ERROR) shouldBe errorFolder
    props(FTP_ENCODE) shouldBe encode
    props(FTP_HOSTNAME) shouldBe hostname
    props(FTP_PORT).toInt shouldBe port
    props(FTP_USER_NAME) shouldBe user
    props(FTP_PASSWORD) shouldBe password
  }

}
