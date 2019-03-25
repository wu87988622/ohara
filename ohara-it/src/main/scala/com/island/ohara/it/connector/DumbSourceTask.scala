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

package com.island.ohara.it.connector

import java.util
import java.util.Collections

import com.island.ohara.kafka.connector.{RowSourceRecord, RowSourceTask, TaskConfig}

class DumbSourceTask extends RowSourceTask {
  override protected def _start(config: TaskConfig): Unit = {}
  override protected def _stop(): Unit = {}
  override protected def _poll(): util.List[RowSourceRecord] = Collections.emptyList();
}
