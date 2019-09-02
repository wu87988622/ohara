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
    INPUT_FOLDER_CONFIG -> inputFolder,
    COMPLETED_FOLDER_CONFIG -> completedFolder.getOrElse(""),
    ERROR_FOLDER_CONFIG -> errorFolder,
    FILE_ENCODE_CONFIG -> encode,
    FTP_HOSTNAME -> hostname,
    FTP_PORT -> port.toString,
    FTP_USER_NAME -> user,
    FTP_PASSWORD -> password,
  ).filter(_._2.nonEmpty)
}

object FtpSourceProps {
  def apply(settings: TaskSetting): FtpSourceProps = FtpSourceProps(
    inputFolder = settings.stringValue(INPUT_FOLDER_CONFIG),
    completedFolder = Option(settings.stringOption(COMPLETED_FOLDER_CONFIG).orElse(null)).filterNot(CommonUtils.isEmpty),
    errorFolder = settings.stringValue(ERROR_FOLDER_CONFIG),
    encode = settings.stringOption(FILE_ENCODE_CONFIG).orElse(FILE_ENCODE_DEFAULT),
    hostname = settings.stringValue(FTP_HOSTNAME),
    port = settings.intValue(FTP_PORT),
    user = settings.stringValue(FTP_USER_NAME),
    password = settings.stringValue(FTP_PASSWORD)
  )
}
