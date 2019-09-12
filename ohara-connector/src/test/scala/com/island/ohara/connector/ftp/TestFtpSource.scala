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

import com.island.ohara.client.filesystem.FileSystem
import com.island.ohara.connector.CsvSourceTestBase
import com.island.ohara.kafka.connector.csv.CsvSourceConnector

class TestFtpSource extends CsvSourceTestBase {
  private[this] val ftpServer = testUtil.ftpServer

  override def setupFileSystem(): FileSystem =
    FileSystem.ftpBuilder
      .hostname(ftpServer.hostname)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .password(ftpServer.password)
      .build

  override protected def setupConnectorClass(): Class[_ <: CsvSourceConnector] = classOf[FtpSource]

  override protected def setupProps(): Map[String, String] =
    Map(FTP_HOSTNAME -> ftpServer.hostname,
        FTP_PORT -> ftpServer.port.toString,
        FTP_USER_NAME -> ftpServer.user,
        FTP_PASSWORD -> ftpServer.password)
}
