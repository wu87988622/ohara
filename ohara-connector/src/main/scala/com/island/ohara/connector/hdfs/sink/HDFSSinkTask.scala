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

import java.net.URI

import com.island.ohara.kafka.connector.TaskSetting
import com.island.ohara.kafka.connector.csv.{CsvSinkConfig, CsvSinkTask}
import com.island.ohara.kafka.connector.storage.Storage
import org.apache.hadoop.fs.FileSystem

class HDFSSinkTask extends CsvSinkTask {

  var hdfsSinkConfig: HDFSSinkConfig = _

  override def getConfig(setting: TaskSetting): CsvSinkConfig = {
    CsvSinkConfig.of(setting, setting.columns())
  }

  override def getStorage(setting: TaskSetting): Storage = {
    hdfsSinkConfig = HDFSSinkConfig(setting)
    new HDFSStorage(FileSystem.newInstance(URI.create(hdfsSinkConfig.hdfsURL), hdfsSinkConfig.hadoopConfiguration()))
  }
}
