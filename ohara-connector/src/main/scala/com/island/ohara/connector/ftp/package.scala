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
import com.island.ohara.kafka.connector.csv.CsvConnector

package object ftp {
  val FTP_HOSTNAME = "ftp.hostname"
  val FTP_PORT = "ftp.port"
  val FTP_USER_NAME = "ftp.user.name"
  val FTP_PASSWORD = "ftp.user.password"

  @VisibleForTesting private[connector] val INPUT_FOLDER_CONFIG: String = CsvConnector.INPUT_FOLDER_CONFIG
  @VisibleForTesting private[connector] val COMPLETED_FOLDER_CONFIG: String = CsvConnector.COMPLETED_FOLDER_CONFIG
  @VisibleForTesting private[connector] val ERROR_FOLDER_CONFIG: String = CsvConnector.ERROR_FOLDER_CONFIG
  @VisibleForTesting private[connector] val TOPICS_DIR_CONFIG: String = CsvConnector.TOPICS_DIR_CONFIG
  @VisibleForTesting private[connector] val FLUSH_SIZE_CONFIG: String = CsvConnector.FLUSH_SIZE_CONFIG
  @VisibleForTesting private[connector] val FLUSH_SIZE_DEFAULT: Int = CsvConnector.FLUSH_SIZE_DEFAULT
  @VisibleForTesting private[connector] val ROTATE_INTERVAL_MS_CONFIG: String = CsvConnector.ROTATE_INTERVAL_MS_CONFIG
  @VisibleForTesting private[connector] val ROTATE_INTERVAL_MS_DEFAULT: Long = CsvConnector.ROTATE_INTERVAL_MS_DEFAULT
  @VisibleForTesting private[connector] val FILE_NEED_HEADER_CONFIG: String = CsvConnector.FILE_NEED_HEADER_CONFIG
  @VisibleForTesting private[connector] val FILE_NEED_HEADER_DEFAULT: Boolean = CsvConnector.FILE_NEED_HEADER_DEFAULT
  @VisibleForTesting private[connector] val FILE_ENCODE_CONFIG: String = CsvConnector.FILE_ENCODE_CONFIG
  @VisibleForTesting private[connector] val FILE_ENCODE_DEFAULT: String = CsvConnector.FILE_ENCODE_DEFAULT
}
