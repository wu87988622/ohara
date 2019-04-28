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

package com.island.ohara.client.kafka

import java.util

import com.island.ohara.kafka.connector.{RowSourceRecord, RowSourceTask, TaskSetting}

class MyConnectorTask extends RowSourceTask {
  private[this] var lastSent: Long = 0
  private[this] var topicName: String = _

  override protected def _start(settings: TaskSetting): Unit = this.topicName = settings.topicNames().get(0)

  override protected def _stop(): Unit = {
    // do nothing
  }

  override protected def _poll(): util.List[RowSourceRecord] = {
    val current = System.currentTimeMillis()
    if (current - lastSent >= 1000) {
      lastSent = current
      java.util.Collections.singletonList(RowSourceRecord.builder().topicName(topicName).row(ROW).build())
    } else java.util.Collections.emptyList()
  }
}
