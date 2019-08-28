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

package com.island.ohara.kafka.connector.csv.sink;

import com.island.ohara.common.util.Releasable;
import com.island.ohara.kafka.connector.RowSinkRecord;

/** Storage specific RecordWriter. */
public interface RecordWriter extends Releasable {
  /**
   * Write a record to storage.
   *
   * @param record the record to persist
   */
  void write(RowSinkRecord record);

  /**
   * Flush writer's data and commit the records in Kafka. Optionally, this operation might also
   * close the writer.
   */
  void commit();

  /** Close this writer. */
  void close();
}
