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

import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions

package object ftp {
  /**
    * used to set the order of definitions.
    */
  private[this] val COUNTER = new AtomicInteger(0)
  val FTP_HOSTNAME_KEY      = "ftp.hostname"
  val FTP_HOSTNAME_DEFINITION = SettingDef
    .builder()
    .displayName("Hostname of FTP Server")
    .documentation("hostname of ftp server")
    .required(SettingDef.Type.STRING)
    .key(FTP_HOSTNAME_KEY)
    .orderInGroup(COUNTER.getAndIncrement())
    .build()

  val FTP_PORT_KEY = "ftp.port"
  val FTP_PORT_DEFINITION = SettingDef
    .builder()
    .displayName("Port of FTP Server")
    .documentation("port of ftp server")
    .required(SettingDef.Type.PORT)
    .key(FTP_PORT_KEY)
    .orderInGroup(COUNTER.getAndIncrement())
    .build()

  val FTP_USER_NAME_KEY = "ftp.user.name"
  val FTP_USER_NAME_DEFINITION = SettingDef
    .builder()
    .displayName("User of FTP Server")
    .documentation("user of ftp server. This account must have read/delete permission of input folder and error folder")
    .required(SettingDef.Type.STRING)
    .key(FTP_USER_NAME_KEY)
    .orderInGroup(COUNTER.getAndIncrement())
    .build()

  val FTP_PASSWORD_KEY = "ftp.user.password"
  val FTP_PASSWORD_DEFINITION = SettingDef
    .builder()
    .displayName("Password of FTP Server")
    .documentation("password of ftp server")
    .required(SettingDef.Type.PASSWORD)
    .key(FTP_PASSWORD_KEY)
    .orderInGroup(COUNTER.getAndIncrement())
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

  @VisibleForTesting private[connector] val INPUT_FOLDER_KEY: String     = CsvConnectorDefinitions.INPUT_FOLDER_KEY
  @VisibleForTesting private[connector] val COMPLETED_FOLDER_KEY: String = CsvConnectorDefinitions.COMPLETED_FOLDER_KEY
  @VisibleForTesting private[connector] val ERROR_FOLDER_KEY: String     = CsvConnectorDefinitions.ERROR_FOLDER_KEY
  @VisibleForTesting private[connector] val TOPICS_DIR_KEY: String       = CsvConnectorDefinitions.TOPICS_DIR_KEY
  @VisibleForTesting private[connector] val FLUSH_SIZE_KEY: String       = CsvConnectorDefinitions.FLUSH_SIZE_KEY
  @VisibleForTesting private[connector] val FLUSH_SIZE_DEFAULT: Int      = CsvConnectorDefinitions.FLUSH_SIZE_DEFAULT
  @VisibleForTesting private[connector] val ROTATE_INTERVAL_MS_KEY: String =
    CsvConnectorDefinitions.ROTATE_INTERVAL_MS_KEY
  @VisibleForTesting private[connector] val ROTATE_INTERVAL_MS_DEFAULT: Long =
    CsvConnectorDefinitions.ROTATE_INTERVAL_MS_DEFAULT
  @VisibleForTesting private[connector] val FILE_NEED_HEADER_KEY: String = CsvConnectorDefinitions.FILE_NEED_HEADER_KEY
  @VisibleForTesting private[connector] val FILE_NEED_HEADER_DEFAULT: Boolean =
    CsvConnectorDefinitions.FILE_NEED_HEADER_DEFAULT
  @VisibleForTesting private[connector] val FILE_ENCODE_KEY: String     = CsvConnectorDefinitions.FILE_ENCODE_KEY
  @VisibleForTesting private[connector] val FILE_ENCODE_DEFAULT: String = CsvConnectorDefinitions.FILE_ENCODE_DEFAULT
}
