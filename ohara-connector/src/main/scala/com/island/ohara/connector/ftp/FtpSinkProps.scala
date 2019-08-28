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

import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.kafka.connector.TaskSetting

case class FtpSinkProps(hostname: String,
                        port: Int,
                        user: String,
                        password: String,
                        @VisibleForTesting topicsDir: String,
                        @VisibleForTesting needHeader: Boolean,
                        @VisibleForTesting encode: String) {
  def toMap: Map[String, String] = Map(
    FTP_HOSTNAME -> hostname,
    FTP_PORT -> port.toString,
    FTP_USER_NAME -> user,
    FTP_PASSWORD -> password,
    TOPICS_DIR_CONFIG -> topicsDir,
    FILE_NEED_HEADER_CONFIG -> needHeader.toString,
    FILE_ENCODE_CONFIG -> encode
  ).filter(_._2.nonEmpty)
}

object FtpSinkProps {
  def apply(settings: TaskSetting): FtpSinkProps = FtpSinkProps(
    hostname = settings.stringValue(FTP_HOSTNAME),
    port = settings.intValue(FTP_PORT),
    user = settings.stringValue(FTP_USER_NAME),
    password = settings.stringValue(FTP_PASSWORD),
    topicsDir = settings.stringValue(TOPICS_DIR_CONFIG),
    needHeader = settings.booleanOption(FILE_NEED_HEADER_CONFIG).orElse(FILE_NEED_HEADER_DEFAULT),
    encode = settings.stringOption(FILE_ENCODE_CONFIG).orElse(FILE_ENCODE_DEFAULT)
  )
}
