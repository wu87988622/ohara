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
import com.island.ohara.kafka.connector.storage.{Storage, StorageTestBase}
import com.island.ohara.testing.service.FtpServer
import org.scalatest.Matchers

class TestFtpStorage extends StorageTestBase with Matchers {
  private[this] val ftpServer = FtpServer.builder().controlPort(0).dataPorts(java.util.Arrays.asList(0, 0, 0)).build()
  private[this] val hostname = ftpServer.hostname
  private[this] val port = ftpServer.port
  private[this] val user = ftpServer.user
  private[this] val password = ftpServer.password

  override protected def createStorage(): Storage = new FtpStorage(
    FtpClient.builder().hostname(hostname).port(port).user(user).password(password).build)

  override protected def getRootFolder: Path = StorageTestBase.ROOT_FOLDER_DEFAULT
}
