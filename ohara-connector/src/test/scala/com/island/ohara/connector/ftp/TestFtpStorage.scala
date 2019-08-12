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

import java.nio.file.Path

import com.island.ohara.client.ftp.FtpClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.storage.{Storage, StorageTestBase}
import com.island.ohara.testing.service.FtpServer
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestFtpStorage extends StorageTestBase with Matchers {
  private[this] val ftpServer = FtpServer.builder().controlPort(0).dataPorts(java.util.Arrays.asList(0, 0, 0)).build()
  private[this] val hostname = ftpServer.hostname
  private[this] val port = ftpServer.port
  private[this] val user = ftpServer.user
  private[this] val password = ftpServer.password
  private[this] val ftpClient = FtpClient.builder().hostname(hostname).port(port).user(user).password(password).build
  private[this] val ftpStorage = new FtpStorage(ftpClient)

  override protected def createStorage(): Storage = ftpStorage

  override protected def getRootFolder: Path = StorageTestBase.ROOT_FOLDER_DEFAULT

  @Test
  def testDeleteWithRecursive(): Unit = {
    val folder1 = CommonUtils.path("/test")
    val folder2 = CommonUtils.path(folder1, CommonUtils.randomString(5))
    val folder3 = CommonUtils.path(folder2, CommonUtils.randomString(5))
    val folder4 = CommonUtils.path(folder3, CommonUtils.randomString(5))
    val file1 = CommonUtils.path(folder4, CommonUtils.randomString(10))
    val file2 = CommonUtils.path(folder4, CommonUtils.randomString(10))

    ftpClient.mkdir(folder1)
    ftpClient.mkdir(folder2)
    ftpClient.mkdir(folder3)
    ftpClient.mkdir(folder4)
    ftpClient.create(file1).close()
    ftpClient.create(file2).close()

    ftpStorage.list(folder1).asScala.size shouldBe 1
    ftpStorage.list(folder2).asScala.size shouldBe 1
    ftpStorage.list(folder3).asScala.size shouldBe 1
    ftpStorage.list(folder4).asScala.size shouldBe 2

    // It's ok to delete a file.
    ftpStorage.delete(file2, true)
    ftpStorage.exists(file2) shouldBe false

    // Delete a folder containing objects
    ftpStorage.delete(folder1, true)

    ftpStorage.exists(folder1) shouldBe false
    ftpStorage.exists(folder2) shouldBe false
    ftpStorage.exists(folder3) shouldBe false
    ftpStorage.exists(folder4) shouldBe false
  }

  @Test
  def testDeleteWithoutRecursive(): Unit = {
    val folder = CommonUtils.path("/test")
    val file = CommonUtils.path(folder, CommonUtils.randomString(5))
    ftpClient.mkdir(folder)
    ftpClient.create(file).close()

    ftpStorage.list(folder).asScala.size shouldBe 1
    an[IllegalStateException] should be thrownBy ftpStorage.delete(folder, false)
  }

  @Test
  def testDeleteFolderWithContainingFiles(): Unit = {
    val folder = CommonUtils.path("/test")
    val file = CommonUtils.path(folder, CommonUtils.randomString(5))
    ftpClient.mkdir(folder)
    ftpClient.create(file).close()

    ftpStorage.list(folder).asScala.size shouldBe 1
    an[IllegalStateException] should be thrownBy ftpStorage.delete(folder)
  }
}
