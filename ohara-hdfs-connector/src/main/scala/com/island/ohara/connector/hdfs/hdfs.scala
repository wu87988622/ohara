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

package object hdfs {
  val PREFIX_FILENAME_PATTERN: String = "[a-zA-Z0-9]*"
  val HDFS_URL: String = "hdfs.url"
  val FLUSH_LINE_COUNT: String = "flush.line.count"
  val ROTATE_INTERVAL_MS: String = "rotate.interval.ms"
  val TMP_DIR: String = "tmp.dir"
  val DATA_DIR: String = "data.dir"
  val DATAFILE_PREFIX_NAME: String = "datafile.prefix.name"
  val DATAFILE_NEEDHEADER: String = "datafile.needheader"
  val DATA_BUFFER_COUNT: String = "data.buffer.size"
  val HDFS_STORAGE_CREATOR_CLASS: String = "hdfs.storage.creator.class"
  val DATAFILE_ENCODE = "datafile.encode"
}
