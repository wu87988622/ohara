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

package com.island.ohara.client.ftp

import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.service.FtpServer
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestIllegalFtpClient extends MediumTest with Matchers {

  private[this] val server = FtpServer.local()

  private[this] val client =
    FtpClient
      .builder()
      // login ftp server with an invalid account and then see what happens :)
      .user(CommonUtils.randomString(10))
      .password(server.password)
      .hostname(server.hostname)
      .port(server.port)
      .build()

  @Test
  def testList(): Unit = an[Throwable] should be thrownBy client.listFileNames("/")

  @Test
  def testExist(): Unit = an[Throwable] should be thrownBy client.exist("/")

  @Test
  def testNonExist(): Unit = an[Throwable] should be thrownBy client.nonExist("/")

  @Test
  def testMkDir(): Unit = an[Throwable] should be thrownBy client.mkdir(s"/${CommonUtils.randomString(10)}")

  @Test
  def testWorkingFolder(): Unit = an[Throwable] should be thrownBy client.workingFolder()

  @Test
  def testFileType(): Unit = an[Throwable] should be thrownBy client.fileType("/")

  @Test
  def testStatus(): Unit = an[Throwable] should be thrownBy client.status()

  @Test
  def testTmpFolder(): Unit = an[Throwable] should be thrownBy client.tmpFolder()

  @Test
  def testOpen(): Unit = an[Throwable] should be thrownBy client.open(s"/${CommonUtils.randomString(10)}")

  @Test
  def testWrite(): Unit = an[Throwable] should be thrownBy client.append(s"/${CommonUtils.randomString(10)}")

  @Test
  def testReMkdir(): Unit = an[Throwable] should be thrownBy client.reMkdir(s"/${CommonUtils.randomString(10)}")

  @Test
  def testDelete(): Unit = an[Throwable] should be thrownBy client.delete(s"/${CommonUtils.randomString(10)}")

  @Test
  def testAttach(): Unit = an[Throwable] should be thrownBy client.attach(s"/${CommonUtils.randomString(10)}", "abc")

  @After
  def tearDown(): Unit = {
    Releasable.close(client)
    Releasable.close(server)
  }
}
