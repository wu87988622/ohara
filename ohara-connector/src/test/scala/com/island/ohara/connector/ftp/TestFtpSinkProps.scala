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
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers._

class TestFtpSinkProps extends OharaTest {

  @Test
  def testGetter(): Unit = {
    val topicsDir = CommonUtils.randomString()
    val needHeader = true
    val encode = CommonUtils.randomString()
    val hostname = CommonUtils.randomString()
    val port = 12345
    val user = CommonUtils.randomString()
    val password = CommonUtils.randomString()
    val props = FtpSinkProps(
      topicsDir = topicsDir,
      needHeader = needHeader,
      user = user,
      password = password,
      hostname = hostname,
      port = port,
      encode = encode
    ).toMap

    props(TOPICS_DIR_KEY) shouldBe topicsDir
    props(FILE_NEED_HEADER_KEY).toBoolean shouldBe needHeader
    props(FILE_ENCODE_KEY) shouldBe encode
    props(FTP_HOSTNAME_KEY) shouldBe hostname
    props(FTP_PORT_KEY).toInt shouldBe port
    props(FTP_USER_NAME_KEY) shouldBe user
    props(FTP_PASSWORD_KEY) shouldBe password
  }
}
