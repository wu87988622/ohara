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

package com.island.ohara.kafka.connector.csv;

public interface CsvConnector {

  String TOPICS_DIR_CONFIG = "topics.dir";

  String FLUSH_SIZE_CONFIG = "flush.size";
  int FLUSH_SIZE_DEFAULT = 1000;

  String ROTATE_INTERVAL_MS_CONFIG = "rotate.interval.ms";
  long ROTATE_INTERVAL_MS_DEFAULT = 60000;

  String FILE_NEED_HEADER_CONFIG = "file.need.header";
  boolean FILE_NEED_HEADER_DEFAULT = true;

  String FILE_ENCODE_CONFIG = "file.encode";
  String FILE_ENCODE_DEFAULT = "UTF-8";
}
