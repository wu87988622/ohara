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

import com.island.ohara.client.ftp.FtpClient
import com.island.ohara.kafka.connector._
import com.island.ohara.kafka.connector.csv.CsvSourceTask
import com.island.ohara.kafka.connector.storage.Storage

/**
  * Move files from FTP server to Kafka topics. The file format must be csv file, and element in same line must be separated
  * by comma. The offset is (path, line index). It means each line is stored as a "message" in connector topic. For example:
  * a file having 100 lines has 100 message in connector topic. If the file is processed correctly, the TestFtpSource
  */
class FtpSourceTask extends CsvSourceTask {

  override def _storage(config: TaskSetting): Storage = {
    val props: FtpSourceProps = FtpSourceProps(config)
    new FtpStorage(
      FtpClient.builder().hostname(props.hostname).port(props.port).user(props.user).password(props.password).build())
  }
}
