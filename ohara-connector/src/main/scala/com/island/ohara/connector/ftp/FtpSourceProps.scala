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

import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.TaskSetting

case class FtpSourceProps(inputFolder: String,
                          completedFolder: Option[String],
                          errorFolder: String,
                          encode: String,
                          hostname: String,
                          port: Int,
                          user: String,
                          password: String) {
  def toMap: Map[String, String] = Map(
    INPUT_FOLDER_KEY -> inputFolder,
    COMPLETED_FOLDER_KEY -> completedFolder.getOrElse(""),
    ERROR_FOLDER_KEY -> errorFolder,
    FILE_ENCODE_KEY -> encode,
    FTP_HOSTNAME_KEY -> hostname,
    FTP_PORT_KEY -> port.toString,
    FTP_USER_NAME_KEY -> user,
    FTP_PASSWORD_KEY -> password,
  ).filter(_._2.nonEmpty)
}

object FtpSourceProps {
  def apply(settings: TaskSetting): FtpSourceProps = FtpSourceProps(
    inputFolder = settings.stringValue(INPUT_FOLDER_KEY),
    completedFolder = Option(settings.stringOption(COMPLETED_FOLDER_KEY).orElse(null)).filterNot(CommonUtils.isEmpty),
    errorFolder = settings.stringValue(ERROR_FOLDER_KEY),
    encode = settings.stringOption(FILE_ENCODE_KEY).orElse(FILE_ENCODE_DEFAULT),
    hostname = settings.stringValue(FTP_HOSTNAME_KEY),
    port = settings.intValue(FTP_PORT_KEY),
    user = settings.stringValue(FTP_USER_NAME_KEY),
    password = settings.stringValue(FTP_PASSWORD_KEY)
  )
}
