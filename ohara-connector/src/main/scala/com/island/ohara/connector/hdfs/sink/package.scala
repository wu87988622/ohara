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

import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector.csv.CsvConnector

package object sink {
  val HDFS_URL_KEY: String = "hdfs.url"
  val HDFS_URL_DEFINITION = SettingDef
    .builder()
    .displayName("HDFS URL")
    .documentation("Input HDFS namenode location")
    .valueType(SettingDef.Type.STRING)
    .key(HDFS_URL_KEY)
    .build()

  /**
    * the core settings for HDFSSink.
    */
  val DEFINITIONS: Seq[SettingDef] = Seq(
    HDFS_URL_DEFINITION
  )

  @VisibleForTesting val TOPICS_DIR_KEY: String = CsvConnector.TOPICS_DIR_KEY
  @VisibleForTesting val FLUSH_SIZE_KEY: String = CsvConnector.FLUSH_SIZE_KEY
  @VisibleForTesting val FLUSH_SIZE_DEFAULT: Int = CsvConnector.FLUSH_SIZE_DEFAULT
  @VisibleForTesting val ROTATE_INTERVAL_MS_KEY: String = CsvConnector.ROTATE_INTERVAL_MS_KEY
  @VisibleForTesting val ROTATE_INTERVAL_MS_DEFAULT: Long = CsvConnector.ROTATE_INTERVAL_MS_DEFAULT
  @VisibleForTesting val FILE_NEED_HEADER_KEY: String = CsvConnector.FILE_NEED_HEADER_KEY
  @VisibleForTesting val FILE_NEED_HEADER_DEFAULT: Boolean = CsvConnector.FILE_NEED_HEADER_DEFAULT
  @VisibleForTesting val FILE_ENCODE_KEY: String = CsvConnector.FILE_ENCODE_KEY
  @VisibleForTesting val FILE_ENCODE_DEFAULT: String = CsvConnector.FILE_ENCODE_DEFAULT
}
