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

class TestFtpSinkTaskProps extends SmallTest with Matchers {

  @Test
  def emptyEncodeShouldDisappear(): Unit = {
    val props = FtpSinkTaskProps(
      output = CommonUtils.randomString(),
      needHeader = false,
      encode = None,
      hostname = CommonUtils.randomString(),
      port = 22,
      user = CommonUtils.randomString(),
      password = CommonUtils.randomString()
    ).toMap
    props.contains(FTP_ENCODE) shouldBe false
  }
}
