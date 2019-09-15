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

package com.island.ohara.it.connector.smb

import com.island.ohara.client.filesystem.{FileSystem, FileSystemTestBase}
import com.island.ohara.common.util.CommonUtils

class TestSmbFileSystem extends FileSystemTestBase {

  private[this] val hostname: Option[String] = sys.env.get(SMB_HOSTNAME_KEY)
  private[this] val port: Option[String] = sys.env.get(SMB_PORT_KEY)
  private[this] val user: Option[String] = sys.env.get(SMB_USERNAME_KEY)
  private[this] val password: Option[String] = sys.env.get(SMB_PASSWORD_KEY)
  private[this] val shareName: Option[String] = sys.env.get(SMB_SHARE_NAME_KEY)

  private[this] val TEMP_DIR = CommonUtils.randomString(10)

  override protected val fileSystem: FileSystem = {
    checkSmbProperties()
    FileSystem.smbBuilder
      .hostname(hostname.get)
      .port(Integer.parseInt(port.get))
      .user(user.get)
      .password(password.get)
      .shareName(shareName.get)
      .build
  }

  override protected val rootDir: String = TEMP_DIR

  private[this] def checkSmbProperties(): Unit =
    if (hostname.isEmpty || port.isEmpty || user.isEmpty || password.isEmpty || shareName.isEmpty)
      skipTest(
        s"skip SmbFileSystem test, Please setting $SMB_HOSTNAME_KEY, $SMB_PORT_KEY, $SMB_USERNAME_KEY, $SMB_PASSWORD_KEY and $SMB_SHARE_NAME_KEY properties")
}
