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

package oharastream.ohara.client.filesystem.ftp

import oharastream.ohara.common.rule.OharaTest
import org.junit.Test
import org.scalatest.Matchers._

import scala.concurrent.duration._
class TestFtpFileSystemBuilder extends OharaTest {
  @Test
  def ignoreHostname(): Unit = {
    an[NullPointerException] should be thrownBy FtpFileSystem.builder
      .port(123)
      .user("adasd")
      .password("asda")
      .retryTimeout(5 seconds)
      .retryBackoff(5 seconds)
      .build()
  }

  @Test
  def nullHostname(): Unit = {
    an[NullPointerException] should be thrownBy FtpFileSystem.builder.hostname(null)
  }

  @Test
  def emptyHostname(): Unit = {
    an[IllegalArgumentException] should be thrownBy FtpFileSystem.builder.hostname("")
  }

  @Test
  def ignorePort(): Unit = {
    // pass since ftp port has default value
    FtpFileSystem.builder
      .hostname("abc")
      .user("adasd")
      .password("asda")
      .retryTimeout(5 seconds)
      .retryBackoff(5 seconds)
      .build()
  }

  @Test
  def negativePort(): Unit = {
    an[IllegalArgumentException] should be thrownBy FtpFileSystem.builder.port(-1)
  }

  @Test
  def ignoreUser(): Unit = {
    an[NullPointerException] should be thrownBy FtpFileSystem.builder
      .port(123)
      .hostname("adasd")
      .password("asda")
      .retryTimeout(5 seconds)
      .retryBackoff(5 seconds)
      .build()
  }

  @Test
  def nullUser(): Unit = {
    an[NullPointerException] should be thrownBy FtpFileSystem.builder.user(null)
  }

  @Test
  def emptyUser(): Unit = {
    an[IllegalArgumentException] should be thrownBy FtpFileSystem.builder.user("")
  }

  @Test
  def ignorePassword(): Unit = {
    an[NullPointerException] should be thrownBy FtpFileSystem.builder
      .port(123)
      .hostname("adasd")
      .user("asda")
      .retryTimeout(5 seconds)
      .retryBackoff(5 seconds)
      .build()
  }

  @Test
  def nullPassword(): Unit = {
    an[NullPointerException] should be thrownBy FtpFileSystem.builder.password(null)
  }

  @Test
  def emptyPassword(): Unit = {
    an[IllegalArgumentException] should be thrownBy FtpFileSystem.builder.password("")
  }

  @Test
  def ignoreRetryTimeout(): Unit = {
    // pass
    FtpFileSystem.builder.hostname("aa").port(123).password("adasd").user("asda").retryBackoff(5 seconds).build()
  }
  @Test
  def nullRetryTimeout(): Unit = {
    an[NullPointerException] should be thrownBy FtpFileSystem.builder.retryTimeout(null)
  }

  @Test
  def ignoreRetryBackoff(): Unit = {
    // pass
    FtpFileSystem.builder.hostname("aa").port(123).password("adasd").user("asda").retryTimeout(5 seconds).build()
  }
  @Test
  def nullRetryBackoff(): Unit = {
    an[NullPointerException] should be thrownBy FtpFileSystem.builder.retryBackoff(null)
  }
}
