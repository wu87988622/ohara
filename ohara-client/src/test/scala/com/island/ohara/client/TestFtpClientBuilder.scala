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

package com.island.ohara.client
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestFtpClientBuilder extends SmallTest with Matchers {

  @Test
  def testArguments(): Unit = {
    // pass
    FtpClient
      .builder()
      .hostname("abc")
      .port(123)
      .user("adasd")
      .password("asda")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()
      .close()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .port(123)
      .user("adasd")
      .password("asda")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .hostname("abc")
      .user("adasd")
      .password("asda")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .hostname("abc")
      .port(123)
      .password("asda")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .hostname("abc")
      .port(123)
      .user("adasd")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .hostname("abc")
      .port(123)
      .user("adasd")
      .password("asda")
      .retryCount(10)
      .retryInternal(-5 seconds)
      .build()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .hostname("abc")
      .port(-123)
      .user("adasd")
      .password("asda")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()
  }
}
