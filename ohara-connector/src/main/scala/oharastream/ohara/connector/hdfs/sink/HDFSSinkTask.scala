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

package oharastream.ohara.connector.hdfs.sink

import oharastream.ohara.client.filesystem.FileSystem
import oharastream.ohara.common.annotations.VisibleForTesting
import oharastream.ohara.kafka.connector.{TaskSetting, storage}
import oharastream.ohara.kafka.connector.csv.CsvSinkTask

class HDFSSinkTask extends CsvSinkTask {
  @VisibleForTesting
  private[sink] var hdfsSinkProps: HDFSSinkProps = _

  override def _fileSystem(setting: TaskSetting): storage.FileSystem = {
    hdfsSinkProps = HDFSSinkProps(setting)
    FileSystem.hdfsBuilder.url(hdfsSinkProps.hdfsURL).build
  }
}
