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

package com.island.ohara.connector.hdfs

import com.island.ohara.kafka.connector.csv.CsvSinkConfig

package object sink {
  val HDFS_URL_CONFIG: String = "hdfs.url"
  val TOPICS_DIR_CONFIG: String = CsvSinkConfig.TOPICS_DIR_CONFIG
  val TOPICS_DIR_DEFAULT: String = "/data"
  val FLUSH_SIZE_CONFIG: String = CsvSinkConfig.FLUSH_SIZE_CONFIG
  val FLUSH_SIZE_DEFAULT: Int = CsvSinkConfig.FLUSH_SIZE_DEFAULT
  val ROTATE_INTERVAL_MS_CONFIG: String = CsvSinkConfig.ROTATE_INTERVAL_MS_CONFIG
  val ROTATE_INTERVAL_MS_DEFAULT: Long = CsvSinkConfig.ROTATE_INTERVAL_MS_DEFAULT
  val FILE_NEED_HEADER_CONFIG: String = CsvSinkConfig.FILE_NEED_HEADER_CONFIG
  val FILE_NEED_HEADER_DEFAULT: Boolean = CsvSinkConfig.FILE_NEED_HEADER_DEFAULT
  val FILE_ENCODE_CONFIG: String = CsvSinkConfig.FILE_ENCODE_CONFIG
  val FILE_ENCODE_DEFAULT: String = CsvSinkConfig.FILE_ENCODE_DEFAULT
}
