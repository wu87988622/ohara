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

package com.island.ohara.connector

import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector.csv.CsvConnector

package object ftp {
  val FTP_HOSTNAME_KEY = "ftp.hostname"
  val FTP_HOSTNAME_DEFINITION = SettingDef
    .builder()
    .displayName("Hostname of FTP Server")
    .documentation("hostname of ftp server")
    .valueType(SettingDef.Type.STRING)
    .key(FTP_HOSTNAME_KEY)
    .build()

  val FTP_PORT_KEY = "ftp.port"
  val FTP_PORT_DEFINITION = SettingDef
    .builder()
    .displayName("Port of FTP Server")
    .documentation("port of ftp server")
    .valueType(SettingDef.Type.PORT)
    .key(FTP_PORT_KEY)
    .build()

  val FTP_USER_NAME_KEY = "ftp.user.name"
  val FTP_USER_NAME_DEFINITION = SettingDef
    .builder()
    .displayName("User of FTP Server")
    .documentation("user of ftp server. This account must have read/delete permission of input folder and error folder")
    .valueType(SettingDef.Type.STRING)
    .key(FTP_USER_NAME_KEY)
    .build()

  val FTP_PASSWORD_KEY = "ftp.user.password"
  val FTP_PASSWORD_DEFINITION = SettingDef
    .builder()
    .displayName("Password of FTP Server")
    .documentation("password of ftp server")
    .valueType(SettingDef.Type.PASSWORD)
    .key(FTP_PASSWORD_KEY)
    .build()

  /**
    * the settings for Ftp Source and Sink.
    */
  val DEFINITIONS: Seq[SettingDef] = Seq(
    FTP_HOSTNAME_DEFINITION,
    FTP_PORT_DEFINITION,
    FTP_USER_NAME_DEFINITION,
    FTP_PASSWORD_DEFINITION
  )

  @VisibleForTesting private[connector] val INPUT_FOLDER_KEY: String = CsvConnector.INPUT_FOLDER_KEY
  @VisibleForTesting private[connector] val COMPLETED_FOLDER_KEY: String = CsvConnector.COMPLETED_FOLDER_KEY
  @VisibleForTesting private[connector] val ERROR_FOLDER_KEY: String = CsvConnector.ERROR_FOLDER_KEY
  @VisibleForTesting private[connector] val TOPICS_DIR_KEY: String = CsvConnector.TOPICS_DIR_KEY
  @VisibleForTesting private[connector] val FLUSH_SIZE_KEY: String = CsvConnector.FLUSH_SIZE_KEY
  @VisibleForTesting private[connector] val FLUSH_SIZE_DEFAULT: Int = CsvConnector.FLUSH_SIZE_DEFAULT
  @VisibleForTesting private[connector] val ROTATE_INTERVAL_MS_KEY: String = CsvConnector.ROTATE_INTERVAL_MS_KEY
  @VisibleForTesting private[connector] val ROTATE_INTERVAL_MS_DEFAULT: Long = CsvConnector.ROTATE_INTERVAL_MS_DEFAULT
  @VisibleForTesting private[connector] val FILE_NEED_HEADER_KEY: String = CsvConnector.FILE_NEED_HEADER_KEY
  @VisibleForTesting private[connector] val FILE_NEED_HEADER_DEFAULT: Boolean = CsvConnector.FILE_NEED_HEADER_DEFAULT
  @VisibleForTesting private[connector] val FILE_ENCODE_KEY: String = CsvConnector.FILE_ENCODE_KEY
  @VisibleForTesting private[connector] val FILE_ENCODE_DEFAULT: String = CsvConnector.FILE_ENCODE_DEFAULT
}
