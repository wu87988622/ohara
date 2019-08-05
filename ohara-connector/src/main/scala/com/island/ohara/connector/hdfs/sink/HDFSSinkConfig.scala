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

package com.island.ohara.connector.hdfs.sink

import com.island.ohara.kafka.connector.TaskSetting
import org.apache.hadoop.conf.Configuration

case class HDFSSinkConfig(hdfsURL: String) {
  def toMap: Map[String, String] = Map(HDFS_URL_CONFIG -> hdfsURL)

  def hadoopConfiguration(): Configuration = {
    val config = new Configuration()
    config.set(HDFSSinkConfig.FS_DEFAULT, hdfsURL)
    config
  }
}

object HDFSSinkConfig {
  val FS_DEFAULT: String = "fs.defaultFS"
  def apply(settings: TaskSetting): HDFSSinkConfig = {
    HDFSSinkConfig(hdfsURL = settings.stringValue(HDFS_URL_CONFIG))
  }
}
